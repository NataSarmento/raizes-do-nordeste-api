package br.edu.raizes.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "produtos")
@Getter @Setter @NoArgsConstructor
public class Produto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String nome;
    private String descricao;
    @Column(nullable = false) private String categoria;
    @Column(nullable = false) private BigDecimal preco;
    private boolean ativo = true;
}
