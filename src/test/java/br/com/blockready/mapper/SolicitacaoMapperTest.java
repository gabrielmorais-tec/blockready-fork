package br.com.blockready.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.com.blockready.dto.Responses.SolicitacaoResponse;
import br.com.blockready.model.entity.Necessidade;
import br.com.blockready.model.entity.Solicitacao;
import br.com.blockready.model.entity.Usuario;
import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.Perfil;
import br.com.blockready.model.enums.Prioridade;

class SolicitacaoMapperTest {

    private Solicitacao solicitacao() {
        Usuario morador = new Usuario("Maria da Silva", "maria@x.org", "hash", null, Perfil.MORADOR, null);
        Necessidade n = new Necessidade(morador, CategoriaRecurso.AGUA, "Sem água há 2 dias", new BigDecimal("40"),
                "litros", "Rua das Flores, 100", -23.4, -46.5, Prioridade.ALTA);
        return new Solicitacao(n);
    }

    @Test
    void semDetalhesPessoaisOcultaEnderecoENomes() {
        SolicitacaoResponse r = SolicitacaoMapper.toResponse(solicitacao(), false);

        assertThat(r.endereco()).isNull();
        assertThat(r.latitude()).isNull();
        assertThat(r.solicitanteNome()).isNull();
        assertThat(r.categoria()).isEqualTo(CategoriaRecurso.AGUA);
        assertThat(r.descricao()).isEqualTo("Sem água há 2 dias");
    }

    @Test
    void comDetalhesPessoaisMostraEnderecoENome() {
        SolicitacaoResponse r = SolicitacaoMapper.toResponse(solicitacao(), true);

        assertThat(r.endereco()).isEqualTo("Rua das Flores, 100");
        assertThat(r.solicitanteNome()).isEqualTo("Maria da Silva");
    }
}
