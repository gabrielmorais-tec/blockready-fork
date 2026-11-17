package br.com.blockready.mapper;

import br.com.blockready.dto.Requests.OrganizacaoRequest;
import br.com.blockready.dto.Responses.OrganizacaoResponse;
import br.com.blockready.model.entity.Organizacao;

public final class OrganizacaoMapper {

    private OrganizacaoMapper() { }

    public static OrganizacaoResponse toResponse(Organizacao o) {
        return new OrganizacaoResponse(o.getId(), o.getNome(), o.getDescricao(), o.getTipo(), o.getContatoEmail(),
                o.getContatoTelefone(), o.getEndereco(), o.getLatitude(), o.getLongitude(), o.getResponsavelNome(),
                o.getStatusAprovacao(), o.getCriadoEm());
    }

    /** Aplica os dados da requisição à organização (cadastro e edição). */
    public static void aplicar(Organizacao o, OrganizacaoRequest r) {
        o.atualizarDados(r.nome().trim(), r.descricao(), r.tipo(), r.contatoEmail(), r.contatoTelefone(),
                r.endereco(), r.latitude(), r.longitude(), r.responsavelNome().trim());
    }
}
