package br.edu.raizes.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import br.edu.raizes.domain.exception.*;

/** Estoque próprio de cada unidade (unidade + produto). */
@Entity @Table(name = "estoques")
@Getter @Setter @NoArgsConstructor
public class Estoque {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Unidade unidade;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Produto produto;
    private int quantidade;

    public void baixar(int qtd) {
        if (qtd > quantidade) {
            throw new ConflitoException("Estoque insuficiente para o produto '" + produto.getNome()
                    + "' (disponível: " + quantidade + ", solicitado: " + qtd + ")");
        }
        quantidade -= qtd;
    }

    public void repor(int qtd) { quantidade += qtd; }

    public boolean disponivel() { return quantidade > 0; }
}
