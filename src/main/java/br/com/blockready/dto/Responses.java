package br.com.blockready.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.Perfil;
import br.com.blockready.model.enums.Prioridade;
import br.com.blockready.model.enums.StatusAprovacao;
import br.com.blockready.model.enums.StatusRecurso;
import br.com.blockready.model.enums.StatusSolicitacao;
import br.com.blockready.model.enums.TipoOrganizacao;

/** Respostas da API. Dados pessoais só aparecem nos campos documentados como restritos. */
public final class Responses {

    private Responses() { }

    public record PageResponse<T>(List<T> itens, int pagina, int tamanho, long total, int totalPaginas) {

        public static <T> PageResponse<T> de(Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages());
        }

        public static <T> PageResponse<T> vazia(int pagina, int tamanho) {
            return new PageResponse<>(List.of(), pagina, tamanho, 0, 0);
        }

        /** Paginação com limite de tamanho; a ordenação é sempre definida pelo servidor. */
        public static Pageable pageable(int pagina, int tamanho, Sort sort) {
            return PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100), sort);
        }
    }

    public record UsuarioResponse(Long id, String nome, String email, Perfil perfil, boolean ativo,
            Long organizacaoId, Instant criadoEm) { }

    public record MeResponse(Long id, String nome, String email, Perfil perfil, Long organizacaoId,
            StatusAprovacao organizacaoStatus) { }

    public record OrganizacaoResponse(Long id, String nome, String descricao, TipoOrganizacao tipo,
            String contatoEmail, String contatoTelefone, String endereco, Double latitude, Double longitude,
            String responsavelNome, StatusAprovacao statusAprovacao, Instant criadoEm) { }

    /** {@code validadoPor*} só é preenchido para gestores, administradores e a organização dona. */
    public record RecursoResponse(Long id, String nome, CategoriaRecurso categoria, String descricao,
            String endereco, double latitude, double longitude, BigDecimal capacidadeTotal,
            BigDecimal capacidadeDisponivel, String unidadeMedida, String descricaoCapacidade,
            String horarioFuncionamento, boolean acessivel, String acessibilidadeDescricao,
            StatusRecurso status, boolean confirmado, Instant atualizadoEm, Long organizacaoId,
            String organizacaoNome, Long validadoPorId, String validadoPorNome, Instant validadoEm,
            Map<String, Object> detalhes) { }

    public record DisponibilidadeResponse(Long id, BigDecimal capacidadeTotal, BigDecimal capacidadeDisponivel,
            String observacao, String registradoPorNome, Instant registradoEm) { }

    public record HistoricoRecursoResponse(Long id, String acao, String descricao, String usuarioNome,
            Instant criadoEm) { }

    /** {@code endereco}, coordenadas, {@code solicitanteNome} e {@code responsavelNome} só para quem atende. */
    public record SolicitacaoResponse(Long id, Long necessidadeId, CategoriaRecurso categoria, String descricao,
            BigDecimal quantidade, String unidade, Prioridade prioridade, StatusSolicitacao status,
            Instant criadoEm, Instant atualizadoEm, String registroAtendimento, Instant atendidaEm,
            String endereco, Double latitude, Double longitude, String solicitanteNome,
            Long responsavelId, String responsavelNome, List<StatusSolicitacao> proximosStatus) { }

    public record HistoricoSolicitacaoResponse(Long id, StatusSolicitacao statusAnterior,
            StatusSolicitacao statusNovo, String observacao, String usuarioNome, Instant criadoEm) { }

    public record VoluntarioResponse(Long id, String nome, String telefone, String habilidades,
            String disponibilidade, boolean ativo, Instant atualizadoEm) { }

    public record AuditoriaResponse(Long id, Long usuarioId, String acao, String entidade, Long entidadeId,
            String detalhes, Instant criadoEm) { }

    public record DashboardResponse(Instant geradoEm, long recursosValidados, long recursosPendentesValidacao,
            long solicitacoesEmAberto, long solicitacoesAtendidas, long voluntariosAtivos,
            long organizacoesPendentes, Map<String, Long> recursosValidadosPorCategoria,
            Map<String, Long> solicitacoesPorStatus) { }
}
