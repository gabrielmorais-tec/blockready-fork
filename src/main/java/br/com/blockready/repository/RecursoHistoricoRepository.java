package br.com.blockready.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.blockready.model.entity.RecursoHistorico;

public interface RecursoHistoricoRepository extends JpaRepository<RecursoHistorico, Long> {

    List<RecursoHistorico> findByRecursoIdOrderByCriadoEmDesc(Long recursoId);
}
