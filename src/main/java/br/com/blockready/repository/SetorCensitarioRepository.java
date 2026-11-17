package br.com.blockready.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.blockready.model.entity.SetorCensitario;

public interface SetorCensitarioRepository extends JpaRepository<SetorCensitario, Long> {

    Optional<SetorCensitario> findByCodigoSetor(String codigoSetor);

    Page<SetorCensitario> findByOrderByCodigoSetorAsc(Pageable pageable);

    long countByGeometriaIsNotNull();

    @Query("select coalesce(sum(s.populacao), 0) from SetorCensitario s")
    long populacaoTotal();

    @Query("select coalesce(sum(s.domicilios), 0) from SetorCensitario s")
    long domiciliosTotal();

    /** Consulta espacial: recursos comunitários contidos na malha do setor (mesmo SRID 4326). */
    @Query(nativeQuery = true, value = """
            select r.id from recurso_comunitario r
            join setor_censitario s on s.id = :setorId
            where s.geometria is not null and ST_Contains(s.geometria, r.geom)
            """)
    List<Long> idsRecursosNoSetor(@Param("setorId") Long setorId);

    /** Consulta espacial: recursos a até {@code metros} da malha (ou do entorno) do setor. */
    @Query(nativeQuery = true, value = """
            select r.id from recurso_comunitario r
            join setor_censitario s on s.id = :setorId
            where s.geometria is not null
              and ST_DWithin(CAST(s.geometria AS geography), CAST(r.geom AS geography), :metros)
            """)
    List<Long> idsRecursosProximosAoSetor(@Param("setorId") Long setorId, @Param("metros") double metros);

    /** População, por setor, com contagem de recursos validados dentro da malha (quando ela existe). */
    @Query(nativeQuery = true, value = """
            select s.codigo_setor, s.municipio, s.uf, s.populacao, s.domicilios,
                   case when s.geometria is null then null
                        else (select count(*) from recurso_comunitario r
                              where r.status = 'VALIDADO' and ST_Contains(s.geometria, r.geom)) end as recursos
            from setor_censitario s
            order by s.populacao desc
            limit :limite
            """)
    List<Object[]> coberturaPorSetor(@Param("limite") int limite);
}
