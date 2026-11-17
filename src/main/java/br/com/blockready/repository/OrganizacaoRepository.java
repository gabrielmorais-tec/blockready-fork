package br.com.blockready.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.blockready.model.entity.Organizacao;
import br.com.blockready.model.enums.StatusAprovacao;

public interface OrganizacaoRepository extends JpaRepository<Organizacao, Long> {

    Page<Organizacao> findByStatusAprovacao(StatusAprovacao status, Pageable pageable);

    long countByStatusAprovacao(StatusAprovacao status);
}
