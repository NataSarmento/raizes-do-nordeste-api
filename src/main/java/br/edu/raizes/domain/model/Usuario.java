package br.edu.raizes.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String nome;
    @Column(nullable = false, unique = true) private String email;
    @Column(name = "senha_hash", nullable = false) private String senhaHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Perfil perfil;
    private boolean ativo = true;
    private boolean consentimentoLgpd;
    private LocalDateTime consentimentoLgpdEm;
    private boolean consentimentoFidelidade;
    private LocalDateTime criadoEm = LocalDateTime.now();

    /** LGPD art. 18: anonimização a pedido do titular. Mantém integridade referencial dos pedidos. */
    public void anonimizar() {
        this.nome = "Usuário anonimizado";
        this.email = "anonimo-" + id + "@anonimizado.local";
        this.ativo = false;
        this.consentimentoFidelidade = false;
    }
}
