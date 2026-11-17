package br.com.blockready.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.blockready.dto.Requests.DisponibilidadeRequest;
import br.com.blockready.dto.Requests.RecursoRequest;
import br.com.blockready.dto.Responses.RecursoResponse;
import br.com.blockready.exception.BusinessException;
import br.com.blockready.model.entity.Organizacao;
import br.com.blockready.model.entity.PontoAgua;
import br.com.blockready.model.entity.Usuario;
import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.Perfil;
import br.com.blockready.model.enums.StatusRecurso;
import br.com.blockready.model.enums.TipoOrganizacao;
import br.com.blockready.repository.OrganizacaoRepository;
import br.com.blockready.repository.RecursoHistoricoRepository;
import br.com.blockready.repository.RecursoRepository;
import br.com.blockready.repository.RegistroDisponibilidadeRepository;
import br.com.blockready.repository.UsuarioRepository;
import br.com.blockready.security.UsuarioPrincipal;

class RecursoServiceTest {

    private final RecursoRepository recursos = mock(RecursoRepository.class);
    private final OrganizacaoRepository organizacoes = mock(OrganizacaoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RegistroDisponibilidadeRepository disponibilidades = mock(RegistroDisponibilidadeRepository.class);
    private final RecursoHistoricoRepository historicos = mock(RecursoHistoricoRepository.class);
    private final AuditoriaService auditoria = mock(AuditoriaService.class);
    private final RecursoService service = new RecursoService(recursos, organizacoes, usuarios, disponibilidades,
            historicos, auditoria);

    private Organizacao org1;
    private Organizacao org2;
    private PontoAgua recursoDaOrg1;

    @BeforeEach
    void preparar() {
        org1 = new Organizacao("ONG Um", TipoOrganizacao.ONG, "Ana");
        org2 = new Organizacao("ONG Dois", TipoOrganizacao.ONG, "Bia");
        ReflectionTestUtils.setField(org1, "id", 1L);
        ReflectionTestUtils.setField(org2, "id", 2L);
        org1.aprovar();
        org2.aprovar();

        recursoDaOrg1 = new PontoAgua(org1);
        recursoDaOrg1.atualizarDados("Poço", null, "Rua A", -23.4, -46.5, "litros", null, false, null);
        recursoDaOrg1.atualizarDisponibilidade(new BigDecimal("1000"), new BigDecimal("500"));
        ReflectionTestUtils.setField(recursoDaOrg1, "id", 10L);

        when(recursos.findById(10L)).thenReturn(Optional.of(recursoDaOrg1));
        when(usuarios.getReferenceById(any(Long.class)))
                .thenReturn(new Usuario("Ator", "ator@x.org", "hash", null, Perfil.ORGANIZACAO, null));
        when(organizacoes.findById(1L)).thenReturn(Optional.of(org1));
        when(organizacoes.findById(2L)).thenReturn(Optional.of(org2));
        when(recursos.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private UsuarioPrincipal usuarioDaOrg(long orgId) {
        return new UsuarioPrincipal(100L + orgId, "Org " + orgId, "o" + orgId + "@x.org", null, Perfil.ORGANIZACAO, orgId, true);
    }

    private RecursoRequest requisicao(Long organizacaoId) {
        return new RecursoRequest("Poço comunitário", CategoriaRecurso.AGUA, null, "Rua A, 1", -23.4, -46.5,
                new BigDecimal("1000"), new BigDecimal("500"), "litros", "24h", true, null, organizacaoId, null);
    }

    @Test
    void organizacaoNaoAtualizaRecursoDeOutra() {
        assertThatThrownBy(() -> service.atualizar(10L, requisicao(null), usuarioDaOrg(2)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void organizacaoNaoAtualizaDisponibilidadeDeRecursoDeOutra() {
        assertThatThrownBy(() -> service.atualizarDisponibilidade(10L,
                new DisponibilidadeRequest(null, new BigDecimal("1"), null), usuarioDaOrg(2)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void organizacaoNaoDesativaRecursoDeOutra() {
        assertThatThrownBy(() -> service.desativar(10L, usuarioDaOrg(2))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void organizacaoDonaAtualizaDisponibilidade() {
        RecursoResponse r = service.atualizarDisponibilidade(10L,
                new DisponibilidadeRequest(null, new BigDecimal("300"), "Distribuição da manhã"), usuarioDaOrg(1));

        assertThat(r.capacidadeDisponivel()).isEqualByComparingTo("300");
        assertThat(r.capacidadeTotal()).isEqualByComparingTo("1000");
    }

    @Test
    void edicaoPelaOrganizacaoDevolveORecursoParaValidacao() {
        Usuario gestor = new Usuario("Gestor", "g@x.org", "hash", null, Perfil.GESTOR, null);
        recursoDaOrg1.validar(gestor);
        assertThat(recursoDaOrg1.estaConfirmado()).isTrue();

        RecursoResponse r = service.atualizar(10L, requisicao(null), usuarioDaOrg(1));

        assertThat(r.status()).isEqualTo(StatusRecurso.PENDENTE_VALIDACAO);
        assertThat(r.confirmado()).isFalse();
    }

    @Test
    void organizacaoUsaSempreASuaPropriaOrganizacaoAoCadastrar() {
        // Mesmo enviando organizacaoId = 2 no corpo, o recurso pertence à organização 1 (a do usuário).
        RecursoResponse r = service.criar(requisicao(2L), usuarioDaOrg(1));

        assertThat(r.organizacaoId()).isEqualTo(1L);
        assertThat(r.organizacaoNome()).isEqualTo("ONG Um");
    }

    @Test
    void organizacaoNaoAprovadaNaoCadastraRecursos() {
        Organizacao pendente = new Organizacao("ONG Nova", TipoOrganizacao.ONG, "Cris");
        ReflectionTestUtils.setField(pendente, "id", 3L);
        when(organizacoes.findById(3L)).thenReturn(Optional.of(pendente));

        assertThatThrownBy(() -> service.criar(requisicao(null), usuarioDaOrg(3)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não foi aprovada");
    }

    @Test
    void gestorPrecisaInformarAOrganizacaoResponsavel() {
        UsuarioPrincipal gestor = new UsuarioPrincipal(1L, "Gestor", "g@x.org", null, Perfil.GESTOR, null, true);

        assertThatThrownBy(() -> service.criar(requisicao(null), gestor)).isInstanceOf(BusinessException.class);
    }
}
