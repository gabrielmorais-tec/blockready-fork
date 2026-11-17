package br.com.blockready.mapper;

import br.com.blockready.dto.Responses.UsuarioResponse;
import br.com.blockready.model.entity.Usuario;

public final class UsuarioMapper {

    private UsuarioMapper() { }

    public static UsuarioResponse toResponse(Usuario u) {
        Long orgId = u.getOrganizacao() != null ? u.getOrganizacao().getId() : null;
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.isAtivo(), orgId,
                u.getCriadoEm());
    }
}
