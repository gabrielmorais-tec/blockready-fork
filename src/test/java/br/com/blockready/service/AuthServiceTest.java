package br.com.blockready.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.blockready.dto.Requests.RegistroRequest;
import br.com.blockready.exception.BusinessException;
import br.com.blockready.exception.ConflictException;
import br.com.blockready.model.entity.Usuario;
import br.com.blockready.model.enums.Perfil;
import br.com.blockready.repository.OrganizacaoRepository;
import br.com.blockready.repository.UsuarioRepository;

class AuthServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final OrganizacaoRepository organizacoes = mock(OrganizacaoRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AuditoriaService auditoria = mock(AuditoriaService.class);
    private final AuthService service = new AuthService(usuarios, organizacoes, encoder, auditoria);

    @Test
    void cadastroGuardaApenasOHashDaSenha() {
        when(encoder.encode("senha-forte-1")).thenReturn("HASH");
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        service.registrar(new RegistroRequest("Maria", "Maria@Exemplo.org", "senha-forte-1", null, Perfil.MORADOR, null));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        assertThat(captor.getValue().getSenhaHash()).isEqualTo("HASH");
        assertThat(captor.getValue().getEmail()).isEqualTo("maria@exemplo.org");
    }

    @Test
    void emailDuplicadoGeraConflito() {
        when(usuarios.existsByEmailIgnoreCase("maria@exemplo.org")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(
                new RegistroRequest("Maria", "maria@exemplo.org", "senha-forte-1", null, Perfil.MORADOR, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cadastroPublicoNaoCriaGestorNemAdministrador() {
        assertThatThrownBy(() -> service.registrar(
                new RegistroRequest("X", "x@exemplo.org", "senha-forte-1", null, Perfil.ADMIN, null)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.registrar(
                new RegistroRequest("X", "x@exemplo.org", "senha-forte-1", null, Perfil.GESTOR, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void organizacaoExigeDadosInstitucionais() {
        assertThatThrownBy(() -> service.registrar(
                new RegistroRequest("Ana", "ana@ong.org", "senha-forte-1", null, Perfil.ORGANIZACAO, null)))
                .isInstanceOf(BusinessException.class);
    }
}
