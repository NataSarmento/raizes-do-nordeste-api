package br.edu.raizes.application.port;

import java.math.BigDecimal;

/** Porta de saída: o domínio/aplicação dependem desta interface, não do provedor real. */
public interface PagamentoGateway {
    enum Simulacao { APROVADO, RECUSADO, TIMEOUT }

    record Resposta(boolean aprovado, String referencia, String mensagem, String payloadJson) { }

    Resposta cobrar(Long pedidoId, BigDecimal valor, Simulacao simulacao);
}
