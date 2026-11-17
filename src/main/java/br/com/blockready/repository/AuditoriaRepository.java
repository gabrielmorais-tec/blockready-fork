package br.com.blockready.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.blockready.model.entity.AuditoriaRegistro;

public interface AuditoriaRepository extends JpaRepository<AuditoriaRegistro, Long> {
}
