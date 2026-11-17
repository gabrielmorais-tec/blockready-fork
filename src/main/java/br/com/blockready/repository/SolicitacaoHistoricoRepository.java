package br.com.blockready.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.blockready.model.entity.SolicitacaoHistorico;

public interface SolicitacaoHistoricoRepository extends JpaRepository<SolicitacaoHistorico, Long> {

    List<SolicitacaoHistorico> findBySolicitacaoIdOrderByCriadoEmAsc(Long solicitacaoId);
}
