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


/** Unidades da rede, produtos, cardápio por unidade e promoções. */
@Service
public class CatalogoService {
    private final UnidadeRepository unidades;
    private final ProdutoRepository produtos;
    private final EstoqueRepository estoques;
    private final PromocaoRepository promocoes;
    private final AuditoriaService auditoria;

    public CatalogoService(UnidadeRepository unidades, ProdutoRepository produtos, EstoqueRepository estoques,
                           PromocaoRepository promocoes, AuditoriaService auditoria) {
        this.unidades = unidades; this.produtos = produtos; this.estoques = estoques;
        this.promocoes = promocoes; this.auditoria = auditoria;
    }

    // ----- unidades -----
    @Transactional(readOnly = true)
    public PageResponse<UnidadeResponse> listarUnidades(int page, int limit) {
        return PageResponse.de(unidades.findByAtivaTrue(PageResponse.pageable(page, limit, Sort.by("nome"))), UnidadeResponse::de);
    }

    @Transactional
    public UnidadeResponse criarUnidade(UnidadeRequest r) {
        Unidade u = new Unidade(); u.setNome(r.nome()); u.setCidade(r.cidade());
        u = unidades.save(u);
        auditoria.registrar("UNIDADE_CRIADA", "Unidade", u.getId(), u.getNome());
        return UnidadeResponse.de(u);
    }

    @Transactional(readOnly = true)
    public PageResponse<CardapioItemResponse> cardapio(Long unidadeId, int page, int limit) {
        unidades.findById(unidadeId).filter(Unidade::isAtiva)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade não encontrada: " + unidadeId));
        return PageResponse.de(estoques.findByUnidadeIdAndProdutoAtivoTrue(unidadeId,
                PageResponse.pageable(page, limit, Sort.by("produto.nome"))), CardapioItemResponse::de);
    }

    // ----- produtos -----
    @Transactional(readOnly = true)
    public PageResponse<ProdutoResponse> listarProdutos(int page, int limit) {
        return PageResponse.de(produtos.findByAtivoTrue(PageResponse.pageable(page, limit, Sort.by("nome"))), ProdutoResponse::de);
    }

    @Transactional(readOnly = true)
    public ProdutoResponse obterProduto(Long id) { return ProdutoResponse.de(produto(id)); }

    @Transactional
    public ProdutoResponse criarProduto(ProdutoRequest r) {
        Produto p = new Produto(); preencher(p, r);
        p = produtos.save(p);
        auditoria.registrar("PRODUTO_CRIADO", "Produto", p.getId(), p.getNome());
        return ProdutoResponse.de(p);
    }

    @Transactional
    public ProdutoResponse atualizarProduto(Long id, ProdutoRequest r) {
        Produto p = produto(id); preencher(p, r);
        auditoria.registrar("PRODUTO_ATUALIZADO", "Produto", id, "preco=" + r.preco());
        return ProdutoResponse.de(p);
    }

    @Transactional
    public void desativarProduto(Long id) {
        produto(id).setAtivo(false);
        auditoria.registrar("PRODUTO_DESATIVADO", "Produto", id, null);
    }

    // ----- promoções (regra de aplicação: código informado no pedido -> % sobre o subtotal) -----
    @Transactional(readOnly = true)
    public PageResponse<PromocaoResponse> listarPromocoes(int page, int limit) {
        return PageResponse.de(promocoes.findAll(PageResponse.pageable(page, limit, Sort.by("id"))), PromocaoResponse::de);
    }

    @Transactional
    public PromocaoResponse criarPromocao(PromocaoRequest r) {
        String codigo = r.codigo().trim().toUpperCase();
        if (promocoes.findByCodigoIgnoreCase(codigo).isPresent()) throw new ConflitoException("Código promocional já existe");
        Promocao p = new Promocao();
        p.setCodigo(codigo); p.setDescricao(r.descricao()); p.setPercentualDesconto(r.percentualDesconto()); p.setValidaAte(r.validaAte());
        p = promocoes.save(p);
        auditoria.registrar("PROMOCAO_CRIADA", "Promocao", p.getId(), codigo + " " + r.percentualDesconto() + "%");
        return PromocaoResponse.de(p);
    }

    private Produto produto(Long id) {
        return produtos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + id));
    }

    private void preencher(Produto p, ProdutoRequest r) {
        p.setNome(r.nome()); p.setDescricao(r.descricao()); p.setCategoria(r.categoria()); p.setPreco(r.preco());
    }
}
