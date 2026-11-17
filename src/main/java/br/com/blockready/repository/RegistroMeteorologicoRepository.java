package br.com.blockready.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.blockready.model.entity.RegistroMeteorologico;

public interface RegistroMeteorologicoRepository extends JpaRepository<RegistroMeteorologico, Long> {

    boolean existsByEstacaoCodigoAndMedidoEm(String estacaoCodigo, Instant medidoEm);

    @Query("select max(r.medidoEm) from RegistroMeteorologico r")
    Instant ultimaMedicao();

    @Query("select count(distinct r.estacaoCodigo) from RegistroMeteorologico r")
    long totalEstacoes();

    /** Resumo por estação no período: acumulado, máximo e última medição. */
    @Query("""
            select r.estacaoCodigo, max(r.estacaoNome), max(r.municipio), max(r.uf),
                   max(r.latitude), max(r.longitude),
                   sum(r.precipitacaoMm), max(r.precipitacaoMm), max(r.medidoEm)
            from RegistroMeteorologico r
            where r.medidoEm >= :desde
            group by r.estacaoCodigo
            order by sum(r.precipitacaoMm) desc
            """)
    List<Object[]> resumoPorEstacao(@Param("desde") Instant desde);

    /** Recursos a até {@code metros} de uma estação: respeita a representatividade espacial da medição. */
    @Query(nativeQuery = true, value = """
            select distinct r.id from recurso_comunitario r
            join registro_meteorologico m on m.estacao_codigo = :estacaoCodigo
            where ST_DWithin(CAST(r.geom AS geography), CAST(m.geom AS geography), :metros)
            """)
    List<Long> idsRecursosProximosAEstacao(@Param("estacaoCodigo") String estacaoCodigo,
            @Param("metros") double metros);
}
