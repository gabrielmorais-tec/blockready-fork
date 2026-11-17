package br.com.blockready.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.blockready.dto.Requests.VoluntarioRequest;
import br.com.blockready.dto.Responses.PageResponse;
import br.com.blockready.dto.Responses.VoluntarioResponse;
import br.com.blockready.exception.ResourceNotFoundException;
import br.com.blockready.model.entity.Voluntario;
import br.com.blockready.repository.UsuarioRepository;
import br.com.blockready.repository.VoluntarioRepository;
import br.com.blockready.security.UsuarioPrincipal;

@Service
public class VoluntarioService {

    private final VoluntarioRepository voluntarios;
    private final UsuarioRepository usuarios;

    public VoluntarioService(VoluntarioRepository voluntarios, UsuarioRepository usuarios) {
        this.voluntarios = voluntarios;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public VoluntarioResponse meu(UsuarioPrincipal ator) {
        return voluntarios.findByUsuarioId(ator.getId()).map(this::paraResposta)
                .orElseThrow(() -> new ResourceNotFoundException("Você ainda não cadastrou sua disponibilidade."));
    }

    @Transactional
    public VoluntarioResponse salvar(VoluntarioRequest r, UsuarioPrincipal ator) {
        Voluntario v = voluntarios.findByUsuarioId(ator.getId())
                .orElseGet(() -> new Voluntario(usuarios.getReferenceById(ator.getId())));
        v.atualizar(r.habilidades().trim(), r.disponibilidade().trim(), r.ativo());
        return paraResposta(voluntarios.save(v));
    }

    /** Lista para gestores e administradores (inclui contato). */
    @Transactional(readOnly = true)
    public PageResponse<VoluntarioResponse> listar(int pagina, int tamanho) {
        return PageResponse.de(voluntarios
                .findAll(PageResponse.pageable(pagina, tamanho, Sort.by(Sort.Direction.DESC, "atualizadoEm")))
                .map(this::paraResposta));
    }

    private VoluntarioResponse paraResposta(Voluntario v) {
        return new VoluntarioResponse(v.getId(), v.getUsuario().getNome(), v.getUsuario().getTelefone(),
                v.getHabilidades(), v.getDisponibilidade(), v.isAtivo(), v.getAtualizadoEm());
    }
}
