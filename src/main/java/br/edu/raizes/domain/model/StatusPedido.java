package br.edu.raizes.domain.model;

/** Máquina de estados do pedido (regra de negócio no domínio). */
public enum StatusPedido {
    AGUARDANDO_PAGAMENTO, EM_PREPARO, PRONTO, ENTREGUE, CANCELADO;

    public boolean podeIrPara(StatusPedido destino) {
        return switch (this) {
            case AGUARDANDO_PAGAMENTO -> destino == EM_PREPARO || destino == CANCELADO;
            case EM_PREPARO -> destino == PRONTO || destino == CANCELADO;
            case PRONTO -> destino == ENTREGUE;
            case ENTREGUE, CANCELADO -> false;
        };
    }
}
