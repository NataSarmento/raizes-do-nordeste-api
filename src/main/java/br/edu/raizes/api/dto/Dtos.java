package br.edu.raizes.api.dto;

import br.edu.raizes.application.port.PagamentoGateway;
import br.edu.raizes.domain.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

/** Contratos de request/response agrupados por recurso. */
public final class Dtos {
    private Dtos() { }

    // ---------- auth / usuários ----------
    public record RegistroRequest(
            @NotBlank @Size(max = 120) @Schema(example = "Maria Souza") String nome,
            @NotBlank @Email @Schema(example = "maria@email.com") String email,
            @NotBlank @Size(min = 8, max = 72) @Schema(example = "Senha@123") String senha,
            @AssertTrue(message = "O consentimento LGPD é obrigatório para o cadastro") boolean consentimentoLgpd,
            boolean consentimentoFidelidade) { }

    public record LoginRequest(
            @NotBlank @Email @Schema(example = "cliente@raizes.com") String email,
            @NotBlank @Schema(example = "Cliente@123") String senha) { }

    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) { }

    public record CriarUsuarioRequest(
            @NotBlank String nome, @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String senha, @NotNull Perfil perfil) { }

    public record ConsentimentoRequest(@NotNull Boolean fidelidade) { }

    public record UsuarioResponse(Long id, String nome, String email, Perfil perfil,
                                  boolean consentimentoLgpd, boolean consentimentoFidelidade, LocalDateTime criadoEm) {
        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(),
                    u.isConsentimentoLgpd(), u.isConsentimentoFidelidade(), u.getCriadoEm());
        }
    }

    // ---------- unidades / produtos / promoções ----------
    public record UnidadeRequest(@NotBlank String nome, @NotBlank String cidade) { }
    public record UnidadeResponse(Long id, String nome, String cidade, boolean ativa) {
        public static UnidadeResponse de(Unidade u) { return new UnidadeResponse(u.getId(), u.getNome(), u.getCidade(), u.isAtiva()); }
    }

    public record ProdutoRequest(
            @NotBlank String nome, String descricao, @NotBlank String categoria,
            @NotNull @DecimalMin("0.01") @Schema(example = "12.50") BigDecimal preco) { }
    public record ProdutoResponse(Long id, String nome, String descricao, String categoria, BigDecimal preco, boolean ativo) {
        public static ProdutoResponse de(Produto p) {
            return new ProdutoResponse(p.getId(), p.getNome(), p.getDescricao(), p.getCategoria(), p.getPreco(), p.isAtivo());
        }
    }

    public record CardapioItemResponse(Long produtoId, String nome, String descricao, String categoria,
                                       BigDecimal preco, boolean disponivel) {
        public static CardapioItemResponse de(Estoque e) {
            Produto p = e.getProduto();
            return new CardapioItemResponse(p.getId(), p.getNome(), p.getDescricao(), p.getCategoria(), p.getPreco(), e.disponivel());
        }
    }

    public record PromocaoRequest(
            @NotBlank String codigo, @NotBlank String descricao,
            @NotNull @DecimalMin("0.01") @DecimalMax("100") BigDecimal percentualDesconto, LocalDate validaAte) { }
    public record PromocaoResponse(Long id, String codigo, String descricao, BigDecimal percentualDesconto,
                                   LocalDate validaAte, boolean vigente) {
        public static PromocaoResponse de(Promocao p) {
            return new PromocaoResponse(p.getId(), p.getCodigo(), p.getDescricao(), p.getPercentualDesconto(), p.getValidaAte(), p.vigente());
        }
    }

    // ---------- estoque ----------
    public record MovimentoEstoqueRequest(
            @NotNull Long unidadeId, @NotNull Long produtoId, @NotNull TipoMovimento tipo,
            @NotNull @Min(1) Integer quantidade, @Size(max = 200) String motivo) { }
    public record EstoqueResponse(Long unidadeId, Long produtoId, String produto, int quantidade) {
        public static EstoqueResponse de(Estoque e) {
            return new EstoqueResponse(e.getUnidade().getId(), e.getProduto().getId(), e.getProduto().getNome(), e.getQuantidade());
        }
    }

    // ---------- pedidos ----------
    public record ItemPedidoRequest(@NotNull Long produtoId, @NotNull @Min(1) Integer quantidade) { }

    public record CriarPedidoRequest(
            @NotNull @Schema(example = "TOTEM", description = "APP, TOTEM, BALCAO, PICKUP ou WEB") CanalPedido canalPedido,
            @Schema(description = "Obrigatório para ATENDENTE/GERENTE/ADMIN; ignorado para CLIENTE") Long clienteId,
            @NotNull @Schema(example = "1") Long unidadeId,
            @NotEmpty @Valid List<ItemPedidoRequest> itens,
            @NotBlank @Schema(example = "MOCK") String formaPagamento,
            @Schema(example = "BEMVINDO10") String codigoPromocao,
            @Min(0) @Schema(description = "Pontos de fidelidade a resgatar (10 pontos = R$ 1,00)") Integer resgatarPontos) { }

    public record AtualizarStatusRequest(@NotNull StatusPedido status) { }

    public record ItemPedidoResponse(Long produtoId, String produto, int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) { }

    public record PedidoResponse(Long id, Long clienteId, Long unidadeId, CanalPedido canalPedido, StatusPedido status,
                                 String formaPagamento, BigDecimal subtotal, BigDecimal desconto, BigDecimal total,
                                 String codigoPromocao, int pontosResgatados, List<ItemPedidoResponse> itens,
                                 LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        public static PedidoResponse de(Pedido p) {
            return new PedidoResponse(p.getId(), p.getCliente().getId(), p.getUnidade().getId(), p.getCanalPedido(),
                    p.getStatus(), p.getFormaPagamento(), p.getSubtotal(), p.getDesconto(), p.getTotal(),
                    p.getCodigoPromocao(), p.getPontosResgatados(),
                    p.getItens().stream().map(i -> new ItemPedidoResponse(i.getProduto().getId(), i.getProduto().getNome(),
                            i.getQuantidade(), i.getPrecoUnitario(), i.subtotal())).toList(),
                    p.getCriadoEm(), p.getAtualizadoEm());
        }
    }

    // ---------- pagamentos ----------
    public record PagamentoRequest(
            @NotNull Long pedidoId,
            @Schema(description = "Apenas para testes do mock: APROVADO (padrão), RECUSADO ou TIMEOUT",
                    example = "APROVADO") PagamentoGateway.Simulacao simularResultado) { }

    public record PagamentoResponse(Long id, Long pedidoId, StatusPagamento status, BigDecimal valor,
                                    String referenciaExterna, String mensagem, String payloadGateway,
                                    StatusPedido statusPedido) {
        public static PagamentoResponse de(Pagamento p) {
            return new PagamentoResponse(p.getId(), p.getPedido().getId(), p.getStatus(), p.getValor(),
                    p.getReferenciaExterna(), p.getMensagem(), p.getPayloadGateway(), p.getPedido().getStatus());
        }
    }

    // ---------- fidelidade / auditoria ----------
    public record SaldoResponse(long pontos, boolean consentimentoFidelidade) { }
    public record MovimentoFidelidadeResponse(Long id, TipoFidelidade tipo, int pontos, Long pedidoId, LocalDateTime criadoEm) {
        public static MovimentoFidelidadeResponse de(MovimentoFidelidade m) {
            return new MovimentoFidelidadeResponse(m.getId(), m.getTipo(), m.getPontos(), m.getPedidoId(), m.getCriadoEm());
        }
    }
    public record AuditoriaResponse(Long id, String usuario, String acao, String entidade, Long entidadeId,
                                    String detalhe, LocalDateTime criadoEm) {
        public static AuditoriaResponse de(Auditoria a) {
            return new AuditoriaResponse(a.getId(), a.getUsuario(), a.getAcao(), a.getEntidade(), a.getEntidadeId(), a.getDetalhe(), a.getCriadoEm());
        }
    }
}
