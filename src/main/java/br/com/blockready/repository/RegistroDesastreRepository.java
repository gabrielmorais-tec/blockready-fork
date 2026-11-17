package br.com.blockready.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.blockready.model.entity.RegistroDesastre;

public interface RegistroDesastreRepository extends JpaRepository<RegistroDesastre, Long> {

    boolean existsByMunicipioAndUfAndTipoDesastreAndOcorridoEm(String municipio, String uf,
            String tipoDesastre, LocalDate ocorridoEm);

    /** Agregado por tipo de desastre: ocorrências, afetados, desabrigados, desalojados, mortos e prejuízo. */
    @Query("""
            select d.tipoDesastre, count(d), sum(d.afetados), sum(d.desabrigados), sum(d.desalojados),
                   sum(d.mortos), sum(coalesce(d.prejuizoTotal, 0)), min(d.ocorridoEm), max(d.ocorridoEm)
            from RegistroDesastre d
            group by d.tipoDesastre
            order by count(d) desc
            """)
    List<Object[]> agregadoPorTipo();

    @Query("select max(d.desabrigados + d.desalojados) from RegistroDesastre d")
    Integer maiorDeslocamentoRegistrado();
}
