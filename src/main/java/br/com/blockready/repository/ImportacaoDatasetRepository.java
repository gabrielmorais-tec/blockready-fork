package br.com.blockready.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.blockready.model.entity.ImportacaoDataset;
import br.com.blockready.model.enums.StatusImportacao;

public interface ImportacaoDatasetRepository extends JpaRepository<ImportacaoDataset, Long> {

    Page<ImportacaoDataset> findByOrderByCriadoEmDesc(Pageable pageable);

    Page<ImportacaoDataset> findByFonteIdOrderByCriadoEmDesc(Long fonteId, Pageable pageable);

    /** Controle de duplicidade: o mesmo arquivo (hash) só volta a entrar se a tentativa anterior falhou. */
    boolean existsByFonteIdAndHashArquivoAndStatusNot(Long fonteId, String hashArquivo, StatusImportacao status);
}
