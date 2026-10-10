package br.edu.raizes.infrastructure.config;

import br.edu.raizes.domain.model.*;
import br.edu.raizes.infrastructure.persistence.*;
import org.slf4j.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Carga inicial (seed) executada apenas quando o banco está vazio. */
@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(DataSeeder.class);
    private final UsuarioRepository usuarios; private final UnidadeRepository unidades; private final ProdutoRepository produtos;
    private final EstoqueRepository estoques; private final PromocaoRepository promocoes; private final PasswordEncoder encoder;

    public DataSeeder(UsuarioRepository usuarios, UnidadeRepository unidades, ProdutoRepository produtos,
                      EstoqueRepository estoques, PromocaoRepository promocoes, PasswordEncoder encoder) {
        this.usuarios = usuarios; this.unidades = unidades; this.produtos = produtos;
        this.estoques = estoques; this.promocoes = promocoes; this.encoder = encoder;
    }

    @Override @Transactional
    public void run(String... args) {
        if (usuarios.count() > 0) return;
        usuario("Administrador", "admin@raizes.com", "Admin@123", Perfil.ADMIN, false);
        usuario("Gerente Matriz", "gerente@raizes.com", "Gerente@123", Perfil.GERENTE, false);
        usuario("Atendente Balcão", "atendente@raizes.com", "Atendente@123", Perfil.ATENDENTE, false);
        usuario("Cozinha", "cozinha@raizes.com", "Cozinha@123", Perfil.COZINHA, false);
        usuario("Cliente Teste", "cliente@raizes.com", "Cliente@123", Perfil.CLIENTE, true);

        Unidade recife = unidade("Raízes Recife - Boa Viagem", "Recife");
        Unidade salvador = unidade("Raízes Salvador - Pelourinho", "Salvador");

        Produto baiao = produto("Baião de Dois", "Arroz, feijão-de-corda e queijo coalho", "Pratos", "28.90");
        Produto carne = produto("Carne de Sol com Macaxeira", "Carne de sol na manteiga de garrafa", "Pratos", "39.90");
        Produto tapioca = produto("Tapioca de Queijo Coalho", "Tapioca recheada", "Lanches", "14.50");
        Produto suco = produto("Suco de Cajá", "Polpa natural 400ml", "Bebidas", "9.90");
        Produto bolo = produto("Bolo de Rolo", "Fatia de bolo de rolo pernambucano", "Sobremesas", "12.00");

        for (Produto p : new Produto[]{baiao, carne, tapioca, suco, bolo}) estoque(recife, p, 50);
        for (Produto p : new Produto[]{baiao, carne, tapioca}) estoque(salvador, p, 30); // suco e bolo NÃO vendidos em Salvador

        Promocao promo = new Promocao();
        promo.setCodigo("BEMVINDO10"); promo.setDescricao("10% de desconto no pedido"); promo.setPercentualDesconto(new BigDecimal("10.00"));
        promocoes.save(promo);
        LOG.info("Seed concluído. Usuários: admin@raizes.com/Admin@123, gerente@raizes.com/Gerente@123, "
                + "atendente@raizes.com/Atendente@123, cozinha@raizes.com/Cozinha@123, cliente@raizes.com/Cliente@123");
    }

    private void usuario(String nome, String email, String senha, Perfil perfil, boolean fidelidade) {
        Usuario u = new Usuario();
        u.setNome(nome); u.setEmail(email); u.setSenhaHash(encoder.encode(senha)); u.setPerfil(perfil);
        u.setConsentimentoLgpd(true); u.setConsentimentoLgpdEm(LocalDateTime.now()); u.setConsentimentoFidelidade(fidelidade);
        usuarios.save(u);
    }
    private Unidade unidade(String nome, String cidade) {
        Unidade u = new Unidade(); u.setNome(nome); u.setCidade(cidade); return unidades.save(u);
    }
    private Produto produto(String nome, String desc, String cat, String preco) {
        Produto p = new Produto(); p.setNome(nome); p.setDescricao(desc); p.setCategoria(cat); p.setPreco(new BigDecimal(preco));
        return produtos.save(p);
    }
    private void estoque(Unidade u, Produto p, int qtd) {
        Estoque e = new Estoque(); e.setUnidade(u); e.setProduto(p); e.setQuantidade(qtd); estoques.save(e);
    }
}
