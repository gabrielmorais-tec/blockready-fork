package br.com.blockready.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.blockready.model.entity.Necessidade;

public interface NecessidadeRepository extends JpaRepository<Necessidade, Long> {
}
