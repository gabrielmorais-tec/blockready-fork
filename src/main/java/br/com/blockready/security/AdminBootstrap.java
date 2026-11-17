package br.com.blockready.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.blockready.model.entity.Usuario;
import br.com.blockready.model.enums.Perfil;
import br.com.blockready.repository.UsuarioRepository;
import br.com.blockready.service.AuditoriaService;

/**
 * Cria o primeiro administrador a partir de variáveis de ambiente, somente se ainda não existir nenhum.
 * Sem as variáveis, nada é criado: não há credencial padrão no código.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;
    private final String email;
    private final String senha;

    public AdminBootstrap(UsuarioRepository usuarios, PasswordEncoder encoder, AuditoriaService auditoria,
            @Value("${blockready.bootstrap-admin.email:}") String email,
            @Value("${blockready.bootstrap-admin.password:}") String senha) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.auditoria = auditoria;
        this.email = email;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || senha.isBlank()) {
            log.info("Administrador inicial não configurado (BOOTSTRAP_ADMIN_EMAIL/BOOTSTRAP_ADMIN_PASSWORD).");
            return;
        }
        if (usuarios.existsByPerfil(Perfil.ADMIN)) {
            return;
        }
        if (senha.length() < 8 || senha.length() > 72) {
            log.warn("BOOTSTRAP_ADMIN_PASSWORD deve ter entre 8 e 72 caracteres. Administrador não criado.");
            return;
        }
        Usuario admin = usuarios.save(new Usuario("Administrador", email.trim().toLowerCase(),
                encoder.encode(senha), null, Perfil.ADMIN, null));
        auditoria.registrar(admin.getId(), "ADMIN_INICIAL_CRIADO", "Usuario", admin.getId(), "bootstrap");
        log.info("Administrador inicial criado.");
    }
}
