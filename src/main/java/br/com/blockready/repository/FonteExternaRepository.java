package br.com.blockready.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.blockready.model.entity.FonteExterna;

public interface FonteExternaRepository extends JpaRepository<FonteExterna, Long> {

    Optional<FonteExterna> findByCodigo(String codigo);
}
