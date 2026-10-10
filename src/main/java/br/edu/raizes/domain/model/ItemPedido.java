package br.edu.raizes.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "itens_pedido")
@Getter @Setter @NoArgsConstructor
public class ItemPedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Pedido pedido;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Produto produto;
    private int quantidade;
    @Column(nullable = false) private BigDecimal precoUnitario;

    public BigDecimal subtotal() { return precoUnitario.multiply(BigDecimal.valueOf(quantidade)); }
}
