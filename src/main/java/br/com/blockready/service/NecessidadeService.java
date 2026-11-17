package br.com.blockready.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.blockready.dto.Requests.NecessidadeRequest;
import br.com.blockready.dto.Responses.SolicitacaoResponse;
import br.com.blockready.exception.BusinessException;
import br.com.blockready.mapper.SolicitacaoMapper;
import br.com.blockready.model.entity.Necessidade;
import br.com.blockready.model.entity.Solicitacao;
import br.com.blockready.model.entity.SolicitacaoHistorico;
import br.com.blockready.model.entity.Usuario;
import br.com.blockready.model.enums.StatusSolicitacao;
import br.com.blockready.repository.NecessidadeRepository;
import br.com.blockready.repository.SolicitacaoHistoricoRepository;
import br.com.blockready.repository.SolicitacaoRepository;
import br.com.blockready.repository.UsuarioRepository;
import br.com.blockready.security.UsuarioPrincipal;

/** Registro de necessidades. Cada necessidade abre uma solicitação (status ABERTA) para acompanhamento. */
@Service
public class NecessidadeService {

    private final NecessidadeRepository necessidades;
    private final SolicitacaoRepository solicitacoes;
    private final SolicitacaoHistoricoRepository historicos;
    private final UsuarioRepository usuarios;

    public NecessidadeService(NecessidadeRepository necessidades, SolicitacaoRepository solicitacoes,
            SolicitacaoHistoricoRepository historicos, UsuarioRepository usuarios) {
        this.necessidades = necessidades;
        this.solicitacoes = solicitacoes;
        this.historicos = historicos;
        this.usuarios = usuarios;
    }

    @Transactional
    public SolicitacaoResponse registrar(NecessidadeRequest r, UsuarioPrincipal ator) {
        if ((r.latitude() == null) != (r.longitude() == null)) {
            throw new BusinessException("Informe latitude e longitude juntas, ou nenhuma das duas.");
        }
        Usuario morador = usuarios.getReferenceById(ator.getId());
        Necessidade n = necessidades.save(new Necessidade(morador, r.categoria(), r.descricao().trim(),
                r.quantidade(), r.unidade().trim(), r.endereco().trim(), r.latitude(), r.longitude(),
                r.prioridade()));
        Solicitacao s = solicitacoes.save(new Solicitacao(n));
        historicos.save(new SolicitacaoHistorico(s, null, StatusSolicitacao.ABERTA, "Necessidade registrada.", morador));
        return SolicitacaoMapper.toResponse(s, true);
    }
}
