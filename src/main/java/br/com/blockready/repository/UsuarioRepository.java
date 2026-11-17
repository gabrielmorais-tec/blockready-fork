package br.com.blockready.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.blockready.model.entity.Usuario;
import br.com.blockready.model.enums.Perfil;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPerfil(Perfil perfil);
}
