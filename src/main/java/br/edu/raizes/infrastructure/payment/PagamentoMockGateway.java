package br.edu.raizes.infrastructure.payment;

import br.edu.raizes.application.port.PagamentoGateway;
import br.edu.raizes.domain.exception.GatewayIndisponivelException;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

/** Simula um gateway externo. Nenhum pagamento real é feito. */
@Component
public class PagamentoMockGateway implements PagamentoGateway {

    @Override
    public Resposta cobrar(Long pedidoId, BigDecimal valor, Simulacao simulacao) {
        Simulacao s = simulacao == null ? Simulacao.APROVADO : simulacao;
        if (s == Simulacao.TIMEOUT) {
            throw new GatewayIndisponivelException("Gateway de pagamento (mock) não respondeu a tempo");
        }
        String ref = "MOCK-" + UUID.randomUUID();
        boolean ok = s == Simulacao.APROVADO;
        String payload = String.format(
                "{\"transactionId\":\"%s\",\"orderId\":%d,\"amount\":%s,\"status\":\"%s\",\"reason\":%s}",
                ref, pedidoId, valor.toPlainString(), ok ? "APPROVED" : "DECLINED",
                ok ? "null" : "\"CARTAO_RECUSADO_SIMULADO\"");
        return new Resposta(ok, ref, ok ? "Pagamento aprovado (mock)" : "Pagamento recusado (mock)", payload);
    }
}
