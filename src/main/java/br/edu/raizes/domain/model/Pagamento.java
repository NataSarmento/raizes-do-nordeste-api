package br.edu.raizes.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "pagamentos")
@Getter @Setter @NoArgsConstructor
public class Pagamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** Pagamento é desacoplado: referencia o pedido apenas por FK, sem cascata. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Pedido pedido;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StatusPagamento status;
    @Column(nullable = false) private BigDecimal valor;
    private String referenciaExterna;
    private String mensagem;
    @Column(length = 1000) private String payloadGateway;
    private LocalDateTime criadoEm = LocalDateTime.now();
}
