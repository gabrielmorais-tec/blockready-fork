package br.com.blockready.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.blockready.model.entity.Voluntario;

public interface VoluntarioRepository extends JpaRepository<Voluntario, Long> {

    Optional<Voluntario> findByUsuarioId(Long usuarioId);

    long countByAtivoTrue();
}
