package br.edu.raizes.domain;

import br.edu.raizes.domain.exception.ConflitoException;
import br.edu.raizes.domain.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class RegrasDominioTest {

    @Test
    void estoqueInsuficienteLancaConflito() {
        Produto p = new Produto(); p.setNome("Baião");
        Estoque e = new Estoque(); e.setProduto(p); e.setQuantidade(2);
        assertThrows(ConflitoException.class, () -> e.baixar(3));
        e.baixar(2);
        assertEquals(0, e.getQuantidade());
    }

    @Test
    void transicoesDeStatusValidas() {
        Pedido p = new Pedido();
        p.transicionarPara(StatusPedido.EM_PREPARO);
        p.transicionarPara(StatusPedido.PRONTO);
        p.transicionarPara(StatusPedido.ENTREGUE);
        assertThrows(ConflitoException.class, () -> p.transicionarPara(StatusPedido.CANCELADO));
    }

    @Test
    void subtotalSomaItens() {
        Produto prod = new Produto(); prod.setPreco(new BigDecimal("10.00"));
        Pedido p = new Pedido();
        p.adicionarItem(prod, 3);
        p.calcularSubtotal();
        assertEquals(new BigDecimal("30.00"), p.getSubtotal());
    }
}
