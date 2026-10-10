package br.edu.raizes.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import br.edu.raizes.domain.exception.ConflitoException;

@Entity @Table(name = "pedidos")
@Getter @Setter @NoArgsConstructor
public class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Usuario cliente;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Unidade unidade;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CanalPedido canalPedido;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StatusPedido status = StatusPedido.AGUARDANDO_PAGAMENTO;
    @Column(nullable = false) private String formaPagamento;
    @Column(nullable = false) private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(nullable = false) private BigDecimal desconto = BigDecimal.ZERO;
    @Column(nullable = false) private BigDecimal total = BigDecimal.ZERO;
    private String codigoPromocao;
    private int pontosResgatados;
    @Column(unique = true) private String idempotencyKey;
    private LocalDateTime criadoEm = LocalDateTime.now();
    private LocalDateTime atualizadoEm = LocalDateTime.now();
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();

    public void adicionarItem(Produto p, int qtd) {
        ItemPedido i = new ItemPedido();
        i.setPedido(this); i.setProduto(p); i.setQuantidade(qtd); i.setPrecoUnitario(p.getPreco());
        itens.add(i);
    }

    public void calcularSubtotal() {
        subtotal = itens.stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void transicionarPara(StatusPedido novo) {
        if (!status.podeIrPara(novo)) {
            throw new ConflitoException("Transição de status inválida: " + status + " -> " + novo);
        }
        status = novo;
        atualizadoEm = LocalDateTime.now();
    }
}
