package br.edu.raizes.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "auditoria")
@Getter @Setter @NoArgsConstructor
public class Auditoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String usuario;
    @Column(nullable = false) private String acao;
    private String entidade;
    private Long entidadeId;
    @Column(length = 500) private String detalhe;
    private LocalDateTime criadoEm = LocalDateTime.now();
}
