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
public class PagamentoService {
    private static final int TENTATIVAS_GATEWAY = 2;

    public record Resultado(PagamentoResponse corpo, int httpStatus) { }

    private final PagamentoRepository pagamentos;
    private final PagamentoGateway gateway;
    private final PedidoService pedidoService;
    private final FidelidadeService fidelidade;
    private final AuditoriaService auditoria;

    public PagamentoService(PagamentoRepository pagamentos, PagamentoGateway gateway, PedidoService pedidoService,
                            FidelidadeService fidelidade, AuditoriaService auditoria) {
        this.pagamentos = pagamentos; this.gateway = gateway; this.pedidoService = pedidoService;
        this.fidelidade = fidelidade; this.auditoria = auditoria;
    }

    /**
     * Fluxo crítico (2/3): solicita cobrança ao gateway (mock), registra o pagamento e atualiza o pedido.
     * 201 aprovado | 402 recusado (pedido CANCELADO, estoque devolvido) | 202 gateway indisponível (pedido mantido).
     */
    @Transactional
    public Resultado pagar(PagamentoRequest r, UsuarioLogado logado) {
        Pedido pedido = pedidoService.buscar(r.pedidoId());
        pedidoService.verificarDono(pedido, logado);
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new ConflitoException("Pedido não está aguardando pagamento (status atual: " + pedido.getStatus() + ")");
        }

        PagamentoGateway.Resposta resp = null;
        for (int t = 1; t <= TENTATIVAS_GATEWAY && resp == null; t++) {
            try {
                resp = gateway.cobrar(pedido.getId(), pedido.getTotal(), r.simularResultado());
            } catch (GatewayIndisponivelException e) {
                // tolerância a falhas: tenta novamente e, se persistir, mantém o pedido aguardando pagamento
            }
        }

        Pagamento pg = new Pagamento();
        pg.setPedido(pedido); pg.setValor(pedido.getTotal());

        if (resp == null) {
            pg.setStatus(StatusPagamento.PENDENTE);
            pg.setMensagem("Gateway indisponível. O pedido segue AGUARDANDO_PAGAMENTO; tente novamente.");
            pg = pagamentos.save(pg);
            auditoria.registrar("PAGAMENTO_PENDENTE", "Pagamento", pg.getId(), "gateway indisponível pedido=" + pedido.getId());
            return new Resultado(PagamentoResponse.de(pg), 202);
        }

        pg.setReferenciaExterna(resp.referencia()); pg.setMensagem(resp.mensagem()); pg.setPayloadGateway(resp.payloadJson());
        if (resp.aprovado()) {
            pg.setStatus(StatusPagamento.APROVADO);
            pg = pagamentos.save(pg);
            pedido.transicionarPara(StatusPedido.EM_PREPARO);
            fidelidade.acumular(pedido);
            auditoria.registrar("PAGAMENTO_APROVADO", "Pagamento", pg.getId(), "pedido=" + pedido.getId() + " ref=" + resp.referencia());
            return new Resultado(PagamentoResponse.de(pg), 201);
        }
        pg.setStatus(StatusPagamento.RECUSADO);
        pg = pagamentos.save(pg);
        pedidoService.cancelarInterno(pedido, "pagamento recusado");
        auditoria.registrar("PAGAMENTO_RECUSADO", "Pagamento", pg.getId(), "pedido=" + pedido.getId());
        return new Resultado(PagamentoResponse.de(pg), 402);
    }

    @Transactional(readOnly = true)
    public List<PagamentoResponse> porPedido(Long pedidoId, UsuarioLogado logado) {
        Pedido p = pedidoService.buscar(pedidoId);
        pedidoService.verificarDono(p, logado);
        return pagamentos.findByPedidoIdOrderByIdAsc(pedidoId).stream().map(PagamentoResponse::de).toList();
    }
}
