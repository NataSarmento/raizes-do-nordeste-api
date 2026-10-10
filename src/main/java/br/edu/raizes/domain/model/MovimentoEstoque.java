package br.edu.raizes.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "movimentos_estoque")
@Getter @Setter @NoArgsConstructor
public class MovimentoEstoque {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Unidade unidade;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Produto produto;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TipoMovimento tipo;
    private int quantidade;
    private String motivo;
    private Long pedidoId;
    private LocalDateTime criadoEm = LocalDateTime.now();
}
