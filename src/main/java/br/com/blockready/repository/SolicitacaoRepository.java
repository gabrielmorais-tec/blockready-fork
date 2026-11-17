package br.com.blockready.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import br.com.blockready.model.entity.Solicitacao;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long>, JpaSpecificationExecutor<Solicitacao> {

    @Query("select s.status, count(s) from Solicitacao s group by s.status")
    List<Object[]> contarPorStatus();
}
