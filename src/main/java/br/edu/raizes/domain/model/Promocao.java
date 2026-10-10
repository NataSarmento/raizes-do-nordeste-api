package br.edu.raizes.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "promocoes")
@Getter @Setter @NoArgsConstructor
public class Promocao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String codigo;
    @Column(nullable = false) private String descricao;
    @Column(nullable = false) private BigDecimal percentualDesconto;
    private LocalDate validaAte;
    private boolean ativa = true;

    public boolean vigente() {
        return ativa && (validaAte == null || !validaAte.isBefore(LocalDate.now()));
    }
}
