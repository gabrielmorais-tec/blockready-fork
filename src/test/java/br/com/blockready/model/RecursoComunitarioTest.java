package br.com.blockready.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.blockready.exception.BusinessException;
import br.com.blockready.model.entity.Abrigo;
import br.com.blockready.model.entity.Organizacao;
import br.com.blockready.model.entity.PontoAgua;
import br.com.blockready.model.entity.RecursoComunitario;
import br.com.blockready.model.entity.Usuario;
import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.Perfil;
import br.com.blockready.model.enums.StatusRecurso;
import br.com.blockready.model.enums.TipoOrganizacao;

class RecursoComunitarioTest {

    private final Organizacao org = new Organizacao("ONG Bairro", TipoOrganizacao.ONG, "Responsável");

    private PontoAgua pontoAgua() {
        PontoAgua r = new PontoAgua(org);
        r.atualizarDados("Poço", null, "Rua A, 1", -23.4, -46.5, "litros", "24h", true, null);
        r.atualizarDisponibilidade(new BigDecimal("1000"), new BigDecimal("400"));
        return r;
    }

    @Test
    void disponibilidadeNaoPodeSuperarATotal() {
        PontoAgua r = pontoAgua();
        assertThatThrownBy(() -> r.atualizarDisponibilidade(new BigDecimal("10"), new BigDecimal("11")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void capacidadesNegativasSaoRejeitadas() {
        PontoAgua r = pontoAgua();
        assertThatThrownBy(() -> r.atualizarDisponibilidade(new BigDecimal("10"), new BigDecimal("-1")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void recursoNovoNaoEConfirmado() {
        assertThat(pontoAgua().getStatus()).isEqualTo(StatusRecurso.PENDENTE_VALIDACAO);
        assertThat(pontoAgua().estaConfirmado()).isFalse();
    }

    @Test
    void validacaoConfirmaEEdicaoDeOrganizacaoVoltaParaPendente() {
        PontoAgua r = pontoAgua();
        Usuario gestor = new Usuario("Gestor", "g@x.org", "hash", null, Perfil.GESTOR, null);
        r.validar(gestor);
        assertThat(r.estaConfirmado()).isTrue();
        r.exigirNovaValidacao();
        assertThat(r.getStatus()).isEqualTo(StatusRecurso.PENDENTE_VALIDACAO);
        assertThat(r.getValidadoPor()).isNull();
    }

    @Test
    void naoValidaDuasVezes() {
        PontoAgua r = pontoAgua();
        Usuario gestor = new Usuario("Gestor", "g@x.org", "hash", null, Perfil.GESTOR, null);
        r.validar(gestor);
        assertThatThrownBy(() -> r.validar(gestor)).isInstanceOf(BusinessException.class);
    }

    @Test
    void polimorfismoNaDescricaoDaCapacidade() {
        Abrigo abrigo = new Abrigo(org);
        abrigo.atualizarDados("Ginásio", null, "Av. B, 2", -23.4, -46.5, "pessoas", null, true, null);
        abrigo.atualizarDisponibilidade(new BigDecimal("50"), new BigDecimal("12"));

        RecursoComunitario comoBase = abrigo;
        RecursoComunitario agua = pontoAgua();

        assertThat(comoBase.descreverCapacidade()).isEqualTo("12 de 50 vagas livres");
        assertThat(agua.descreverCapacidade()).contains("400 de 1000 litros").contains("uso não potável");
        assertThat(new RecursoComunitario(CategoriaRecurso.SAUDE, org).detalhesEspecificos()).isEmpty();
    }

    @Test
    void detalhesEspecificosVemDoSubtipo() {
        PontoAgua r = pontoAgua();
        r.aplicarDetalhes(new DetalhesRecurso(true, "poço artesiano", null, null, null, null, null));
        assertThat(r.detalhesEspecificos()).containsEntry("potavel", true).containsEntry("tipoFonte", "poço artesiano");
    }
}
