package br.edu.raizes.api.controller;

import br.edu.raizes.api.dto.*;
import br.edu.raizes.api.dto.Dtos.*;
import br.edu.raizes.application.service.*;
import br.edu.raizes.domain.model.*;
import br.edu.raizes.infrastructure.security.UsuarioLogado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController @RequestMapping("/pedidos") @Tag(name = "Pedidos") @SecurityRequirement(name = "bearerAuth")
public class PedidoController {
    private final PedidoService service;
    public PedidoController(PedidoService service) { this.service = service; }

    @Operation(summary = "Cria pedido (canalPedido obrigatório). Header opcional Idempotency-Key evita duplicidade")
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('CLIENTE','ATENDENTE','GERENTE','ADMIN')")
    public PedidoResponse criar(@Valid @RequestBody CriarPedidoRequest r,
                                @RequestHeader(value = "Idempotency-Key", required = false) String chave,
                                @AuthenticationPrincipal UsuarioLogado u) {
        return service.criar(r, chave, u);
    }

    @Operation(summary = "Lista pedidos com filtros (?canalPedido=TOTEM&status=...&unidadeId=...). Cliente vê só os seus")
    @GetMapping
    public PageResponse<PedidoResponse> listar(@RequestParam(required = false) CanalPedido canalPedido,
                                               @RequestParam(required = false) StatusPedido status,
                                               @RequestParam(required = false) Long unidadeId,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int limit,
                                               @AuthenticationPrincipal UsuarioLogado u) {
        return service.listar(canalPedido, status, unidadeId, page, limit, u);
    }

    @Operation(summary = "Consulta pedido por id")
    @GetMapping("/{id}")
    public PedidoResponse obter(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado u) { return service.obter(id, u); }

    @Operation(summary = "Atualiza status (COZINHA: PRONTO; ATENDENTE/GERENTE/ADMIN: PRONTO, ENTREGUE, CANCELADO)")
    @PatchMapping("/{id}/status") @PreAuthorize("hasAnyRole('COZINHA','ATENDENTE','GERENTE','ADMIN')")
    public PedidoResponse status(@PathVariable Long id, @Valid @RequestBody AtualizarStatusRequest r, @AuthenticationPrincipal UsuarioLogado u) {
        return service.atualizarStatus(id, r.status(), u);
    }

    @Operation(summary = "Cancela pedido (devolve estoque e estorna pontos)")
    @PostMapping("/{id}/cancelar") @PreAuthorize("hasAnyRole('CLIENTE','ATENDENTE','GERENTE','ADMIN')")
    public PedidoResponse cancelar(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado u) { return service.cancelar(id, u); }
}
