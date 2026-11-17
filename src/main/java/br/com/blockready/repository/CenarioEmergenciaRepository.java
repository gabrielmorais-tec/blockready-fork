package br.com.blockready.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.blockready.model.entity.CenarioEmergencia;

public interface CenarioEmergenciaRepository extends JpaRepository<CenarioEmergencia, Long> {

    Page<CenarioEmergencia> findByOrderByCriadoEmDesc(Pageable pageable);
}
