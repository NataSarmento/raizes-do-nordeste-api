package br.edu.raizes.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "movimentos_fidelidade")
@Getter @Setter @NoArgsConstructor
public class MovimentoFidelidade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Usuario usuario;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TipoFidelidade tipo;
    private int pontos;
    private Long pedidoId;
    private LocalDateTime criadoEm = LocalDateTime.now();
}
