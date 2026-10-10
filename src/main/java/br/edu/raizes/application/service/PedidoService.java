package br.edu.raizes.application.service;

import br.edu.raizes.api.dto.*;
import br.edu.raizes.api.dto.Dtos.*;
import br.edu.raizes.application.port.PagamentoGateway;
import br.edu.raizes.domain.exception.*;
import br.edu.raizes.domain.model.*;
import br.edu.raizes.infrastructure.persistence.*;
import br.edu.raizes.infrastructure.security.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.util.*;


@Service
public class PedidoService {
    private final PedidoRepository pedidos;
    private final UsuarioRepository usuarios;
    private final UnidadeRepository unidades;
    private final ProdutoRepository produtos;
    private final EstoqueRepository estoques;
    private final MovimentoEstoqueRepository movimentos;
    private final PromocaoRepository promocoes;
    private final FidelidadeService fidelidade;
    private final AuditoriaService auditoria;

    public PedidoService(PedidoRepository pedidos, UsuarioRepository usuarios, UnidadeRepository unidades,
                         ProdutoRepository produtos, EstoqueRepository estoques, MovimentoEstoqueRepository movimentos,
                         PromocaoRepository promocoes, FidelidadeService fidelidade, AuditoriaService auditoria) {
        this.pedidos = pedidos; this.usuarios = usuarios; this.unidades = unidades; this.produtos = produtos;
        this.estoques = estoques; this.movimentos = movimentos; this.promocoes = promocoes;
        this.fidelidade = fidelidade; this.auditoria = auditoria;
    }

    private record Baixa(Produto produto, Estoque estoque, int quantidade) { }

    /** Fluxo crítico (1/3): valida regras, baixa estoque, aplica promoção/pontos e persiste o pedido. */
    @Transactional
    public PedidoResponse criar(CriarPedidoRequest r, String idempotencyKey, UsuarioLogado logado) {
        String chave = (idempotencyKey == null || idempotencyKey.isBlank()) ? null : idempotencyKey.trim();
        if (chave != null) {
            Optional<Pedido> existente = pedidos.findByIdempotencyKey(chave);
            if (existente.isPresent()) return PedidoResponse.de(existente.get()); // repetição segura
        }
        if (!"MOCK".equalsIgnoreCase(r.formaPagamento())) {
            throw new RegraInvalidaException("formaPagamento não suportada. Use MOCK");
        }
        Usuario cliente = resolverCliente(r, logado);
        Unidade unidade = unidades.findById(r.unidadeId()).filter(Unidade::isAtiva)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade não encontrada: " + r.unidadeId()));

        Pedido p = new Pedido();
        p.setCliente(cliente); p.setUnidade(unidade); p.setCanalPedido(r.canalPedido());
        p.setFormaPagamento(r.formaPagamento().toUpperCase()); p.setIdempotencyKey(chave);

        Map<Long, Integer> agregados = new LinkedHashMap<>();
        for (ItemPedidoRequest i : r.itens()) agregados.merge(i.produtoId(), i.quantidade(), Integer::sum);

        List<Baixa> baixas = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : agregados.entrySet()) {
            Produto prod = produtos.findById(e.getKey()).filter(Produto::isAtivo)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + e.getKey()));
            Estoque est = estoques.buscarParaAtualizar(unidade.getId(), prod.getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Produto '" + prod.getNome() + "' não é vendido na unidade " + unidade.getId()));
            est.baixar(e.getValue()); // lança 409 se insuficiente
            p.adicionarItem(prod, e.getValue());
            baixas.add(new Baixa(prod, est, e.getValue()));
        }
        p.calcularSubtotal();

        BigDecimal descontoPromo = BigDecimal.ZERO;
        if (r.codigoPromocao() != null && !r.codigoPromocao().isBlank()) {
            Promocao promo = promocoes.findByCodigoIgnoreCase(r.codigoPromocao().trim()).filter(Promocao::vigente)
                    .orElseThrow(() -> new RegraInvalidaException("Código promocional inválido ou expirado"));
            descontoPromo = p.getSubtotal().multiply(promo.getPercentualDesconto())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            p.setCodigoPromocao(promo.getCodigo());
        }

        int pontos = r.resgatarPontos() == null ? 0 : r.resgatarPontos();
        BigDecimal descontoPontos = BigDecimal.ZERO;
        if (pontos > 0) {
            if (!cliente.isConsentimentoFidelidade()) {
                throw new RegraInvalidaException("O resgate de pontos exige consentimento ao programa de fidelidade");
            }
            if (fidelidade.saldoDe(cliente.getId()) < pontos) throw new ConflitoException("Saldo de pontos insuficiente");
            descontoPontos = BigDecimal.valueOf(pontos)
                    .divide(BigDecimal.valueOf(FidelidadeService.PONTOS_POR_REAL_RESGATE), 2, RoundingMode.HALF_UP);
        }
        BigDecimal desconto = descontoPromo.add(descontoPontos);
        if (desconto.compareTo(p.getSubtotal()) > 0) throw new RegraInvalidaException("Desconto excede o valor do pedido");
        p.setDesconto(desconto);
        p.setTotal(p.getSubtotal().subtract(desconto));
        p.setPontosResgatados(pontos);

        p = pedidos.save(p);
        for (Baixa b : baixas) {
            MovimentoEstoque m = new MovimentoEstoque();
            m.setUnidade(unidade); m.setProduto(b.produto()); m.setTipo(TipoMovimento.SAIDA);
            m.setQuantidade(b.quantidade()); m.setMotivo("Pedido #" + p.getId()); m.setPedidoId(p.getId());
            movimentos.save(m);
        }
        if (pontos > 0) fidelidade.resgatar(cliente, pontos, p.getId());
        auditoria.registrar("PEDIDO_CRIADO", "Pedido", p.getId(), "canal=" + p.getCanalPedido() + " total=" + p.getTotal());
        return PedidoResponse.de(p);
    }

    @Transactional(readOnly = true)
    public PedidoResponse obter(Long id, UsuarioLogado logado) {
        Pedido p = buscar(id);
        verificarDono(p, logado);
        return PedidoResponse.de(p);
    }

    @Transactional(readOnly = true)
    public PageResponse<PedidoResponse> listar(CanalPedido canal, StatusPedido status, Long unidadeId,
                                               int page, int limit, UsuarioLogado logado) {
        Specification<Pedido> spec = (root, q, cb) -> cb.conjunction();
        if (canal != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("canalPedido"), canal));
        if (status != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        if (unidadeId != null) spec = spec.and((root, q, cb) -> cb.equal(root.get("unidade").get("id"), unidadeId));
        if (logado.perfil() == Perfil.CLIENTE) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("cliente").get("id"), logado.id()));
        }
        Pageable pg = PageResponse.pageable(page, limit, Sort.by(Sort.Direction.DESC, "id"));
        return PageResponse.de(pedidos.findAll(spec, pg), PedidoResponse::de);
    }

    @Transactional
    public PedidoResponse atualizarStatus(Long id, StatusPedido novo, UsuarioLogado logado) {
        Pedido p = buscar(id);
        if (logado.perfil() == Perfil.COZINHA && novo != StatusPedido.PRONTO) {
            throw new AccessDeniedException("Cozinha só pode marcar pedidos como PRONTO");
        }
        if (novo == StatusPedido.CANCELADO) {
            cancelarInterno(p, "cancelamento via atualização de status");
        } else if (novo == StatusPedido.EM_PREPARO) {
            throw new ConflitoException("EM_PREPARO é definido automaticamente pela aprovação do pagamento");
        } else {
            StatusPedido anterior = p.getStatus();
            p.transicionarPara(novo);
            auditoria.registrar("PEDIDO_STATUS_ALTERADO", "Pedido", p.getId(), anterior + " -> " + novo);
        }
        return PedidoResponse.de(p);
    }

    @Transactional
    public PedidoResponse cancelar(Long id, UsuarioLogado logado) {
        Pedido p = buscar(id);
        verificarDono(p, logado);
        cancelarInterno(p, "cancelamento solicitado por " + logado.perfil());
        return PedidoResponse.de(p);
    }

    /** Usado também pelo PagamentoService quando o pagamento é recusado. Devolve estoque e estorna pontos. */
    public void cancelarInterno(Pedido p, String motivo) {
        StatusPedido anterior = p.getStatus();
        p.transicionarPara(StatusPedido.CANCELADO); // 409 se a transição for inválida
        for (ItemPedido i : p.getItens()) {
            Estoque est = estoques.buscarParaAtualizar(p.getUnidade().getId(), i.getProduto().getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Estoque não encontrado para devolução"));
            est.repor(i.getQuantidade());
            MovimentoEstoque m = new MovimentoEstoque();
            m.setUnidade(p.getUnidade()); m.setProduto(i.getProduto()); m.setTipo(TipoMovimento.ENTRADA);
            m.setQuantidade(i.getQuantidade()); m.setMotivo("Cancelamento do pedido #" + p.getId()); m.setPedidoId(p.getId());
            movimentos.save(m);
        }
        if (p.getPontosResgatados() > 0) fidelidade.estornar(p.getCliente(), p.getPontosResgatados(), p.getId());
        auditoria.registrar("PEDIDO_CANCELADO", "Pedido", p.getId(), anterior + " -> CANCELADO; " + motivo);
    }

    public Pedido buscar(Long id) {
        return pedidos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado: " + id));
    }

    public void verificarDono(Pedido p, UsuarioLogado logado) {
        if (logado.perfil() == Perfil.CLIENTE && !p.getCliente().getId().equals(logado.id())) {
            throw new AccessDeniedException("Pedido pertence a outro cliente");
        }
    }

    private Usuario resolverCliente(CriarPedidoRequest r, UsuarioLogado logado) {
        if (logado.perfil() == Perfil.CLIENTE) {
            if (r.clienteId() != null && !r.clienteId().equals(logado.id())) {
                throw new AccessDeniedException("Cliente só pode criar pedidos para si mesmo");
            }
            return usuarios.findById(logado.id()).orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
        }
        if (r.clienteId() == null) throw new RegraInvalidaException("clienteId é obrigatório para pedidos feitos por funcionários");
        return usuarios.findById(r.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + r.clienteId()));
    }
}
