package br.com.blockready.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.blockready.model.entity.RegistroDisponibilidade;

public interface RegistroDisponibilidadeRepository extends JpaRepository<RegistroDisponibilidade, Long> {

    Page<RegistroDisponibilidade> findByRecursoIdOrderByRegistradoEmDesc(Long recursoId, Pageable pageable);
}
