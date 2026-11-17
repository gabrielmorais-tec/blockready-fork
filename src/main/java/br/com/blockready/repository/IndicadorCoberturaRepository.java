package br.com.blockready.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.blockready.model.entity.IndicadorCobertura;

public interface IndicadorCoberturaRepository extends JpaRepository<IndicadorCobertura, Long> {

    List<IndicadorCobertura> findByCenarioIdOrderByCategoria(Long cenarioId);
}
