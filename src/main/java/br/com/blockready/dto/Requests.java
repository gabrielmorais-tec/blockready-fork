package br.com.blockready.dto;

import java.math.BigDecimal;

import br.com.blockready.model.DetalhesRecurso;
import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.Perfil;
import br.com.blockready.model.enums.Prioridade;
import br.com.blockready.model.enums.StatusSolicitacao;
import br.com.blockready.model.enums.TipoOrganizacao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpos de requisição da API, agrupados por assunto. Todos validados com Bean Validation. */
public final class Requests {

    private Requests() { }

    public record LoginRequest(@NotBlank @Size(max = 160) String email, @NotBlank @Size(max = 72) String senha) { }

    public record OrganizacaoRequest(
            @NotBlank @Size(max = 160) String nome,
            @Size(max = 2000) String descricao,
            @NotNull TipoOrganizacao tipo,
            @Email @Size(max = 160) String contatoEmail,
            @Size(max = 30) String contatoTelefone,
            @Size(max = 300) String endereco,
            @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @NotBlank @Size(max = 120) String responsavelNome) { }

    /** Cadastro público: apenas MORADOR ou ORGANIZACAO. Para ORGANIZACAO, {@code organizacao} é obrigatório. */
    public record RegistroRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 8, max = 72) String senha,
            @Size(max = 30) String telefone,
            @NotNull Perfil perfil,
            @Valid OrganizacaoRequest organizacao) { }

    public record CriarUsuarioRequest(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 8, max = 72) String senha,
            @Size(max = 30) String telefone,
            @NotNull Perfil perfil) { }

    public record PerfilRequest(@NotNull Perfil perfil) { }

    public record AtivoRequest(@NotNull Boolean ativo) { }

    /** Capacidades só são lidas na criação; depois são alteradas por {@link DisponibilidadeRequest}. */
    public record RecursoRequest(
            @NotBlank @Size(max = 160) String nome,
            @NotNull CategoriaRecurso categoria,
            @Size(max = 2000) String descricao,
            @NotBlank @Size(max = 300) String endereco,
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal capacidadeTotal,
            @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal capacidadeDisponivel,
            @NotBlank @Size(max = 30) String unidadeMedida,
            @Size(max = 200) String horarioFuncionamento,
            boolean acessivel,
            @Size(max = 300) String acessibilidadeDescricao,
            Long organizacaoId,
            @Valid DetalhesRecurso detalhes) { }

    public record DisponibilidadeRequest(
            @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal capacidadeTotal,
            @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal capacidadeDisponivel,
            @Size(max = 500) String observacao) { }

    public record MotivoRequest(@Size(max = 500) String motivo) { }

    public record NecessidadeRequest(
            @NotNull CategoriaRecurso categoria,
            @NotBlank @Size(max = 2000) String descricao,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal quantidade,
            @NotBlank @Size(max = 30) String unidade,
            @NotBlank @Size(max = 300) String endereco,
            @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @NotNull Prioridade prioridade) { }

    public record AtualizarStatusRequest(
            @NotNull StatusSolicitacao status,
            @Size(max = 1000) String observacao,
            @Size(max = 2000) String registroAtendimento) { }

    public record ResponsavelRequest(@NotNull Long usuarioId) { }

    public record VoluntarioRequest(
            @NotBlank @Size(max = 1000) String habilidades,
            @NotBlank @Size(max = 500) String disponibilidade,
            @NotNull Boolean ativo) { }
}
