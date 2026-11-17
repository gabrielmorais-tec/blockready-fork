package br.com.blockready.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.blockready.model.entity.RecursoComunitario;
import br.com.blockready.model.enums.CategoriaRecurso;
import br.com.blockready.model.enums.StatusRecurso;

public interface RecursoRepository
        extends JpaRepository<RecursoComunitario, Long>, JpaSpecificationExecutor<RecursoComunitario> {

    long countByStatus(StatusRecurso status);

    @Query("select r.categoria, count(r) from RecursoComunitario r where r.status = :status group by r.categoria")
    List<Object[]> contarPorCategoria(@Param("status") StatusRecurso status);

    @Query("select distinct r.categoria from RecursoComunitario r where r.organizacao.id = :organizacaoId")
    List<CategoriaRecurso> categoriasDaOrganizacao(@Param("organizacaoId") Long organizacaoId);

    /** Filtro geográfico: ids dos recursos dentro do raio (em metros), usando a coluna geom do PostGIS. */
    @Query(nativeQuery = true, value = """
            select r.id from recurso_comunitario r
            where ST_DWithin(CAST(r.geom AS geography),
                             CAST(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) AS geography), :metros)
            """)
    List<Long> idsNoRaio(@Param("lat") double lat, @Param("lon") double lon, @Param("metros") double metros);
}
