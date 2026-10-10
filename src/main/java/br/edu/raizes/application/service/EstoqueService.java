package br.edu.raizes.application.service;

import br.edu.raizes.api.dto.*;
import br.edu.raizes.api.dto.Dtos.*;
import br.edu.raizes.domain.exception.*;
import br.edu.raizes.domain.model.*;
import br.edu.raizes.infrastructure.persistence.*;
import br.edu.raizes.infrastructure.security.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.*;
import java.time.*;
import java.util.*;


@Service
public class EstoqueService {
    private final EstoqueRepository estoques;
    private final MovimentoEstoqueRepository movimentos;
    private final UnidadeRepository unidades;
    private final ProdutoRepository produtos;
    private final AuditoriaService auditoria;

    public EstoqueService(EstoqueRepository estoques, MovimentoEstoqueRepository movimentos, UnidadeRepository unidades,
                          ProdutoRepository produtos, AuditoriaService auditoria) {
        this.estoques = estoques; this.movimentos = movimentos; this.unidades = unidades;
        this.produtos = produtos; this.auditoria = auditoria;
    }

    @Transactional
    public EstoqueResponse movimentar(MovimentoEstoqueRequest r) {
        Unidade un = unidades.findById(r.unidadeId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade não encontrada: " + r.unidadeId()));
        Produto pr = produtos.findById(r.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + r.produtoId()));
        Optional<Estoque> atual = estoques.buscarParaAtualizar(un.getId(), pr.getId());
        Estoque e;
        if (r.tipo() == TipoMovimento.ENTRADA) {
            e = atual.orElseGet(() -> { Estoque n = new Estoque(); n.setUnidade(un); n.setProduto(pr); return estoques.save(n); });
            e.repor(r.quantidade());
        } else {
            e = atual.orElseThrow(() -> new RecursoNaoEncontradoException("Produto sem estoque cadastrado nesta unidade"));
            e.baixar(r.quantidade());
        }
        MovimentoEstoque m = new MovimentoEstoque();
        m.setUnidade(un); m.setProduto(pr); m.setTipo(r.tipo()); m.setQuantidade(r.quantidade()); m.setMotivo(r.motivo());
        movimentos.save(m);
        auditoria.registrar("ESTOQUE_MOVIMENTADO", "Estoque", e.getId(), r.tipo() + " " + r.quantidade() + " un. produto=" + pr.getId());
        return EstoqueResponse.de(e);
    }

    @Transactional(readOnly = true)
    public PageResponse<EstoqueResponse> consultar(Long unidadeId, int page, int limit) {
        unidades.findById(unidadeId).orElseThrow(() -> new RecursoNaoEncontradoException("Unidade não encontrada: " + unidadeId));
        return PageResponse.de(estoques.findByUnidadeId(unidadeId, PageResponse.pageable(page, limit, Sort.by("produto.nome"))),
                EstoqueResponse::de);
    }
}
