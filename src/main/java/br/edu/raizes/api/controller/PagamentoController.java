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

import java.util.List;

@RestController @RequestMapping("/pagamentos") @Tag(name = "Pagamentos (mock)") @SecurityRequirement(name = "bearerAuth")
public class PagamentoController {
    private final PagamentoService service;
    public PagamentoController(PagamentoService service) { this.service = service; }

    @Operation(summary = "Solicita pagamento ao gateway mock. 201 aprovado, 402 recusado, 202 gateway indisponível")
    @PostMapping @PreAuthorize("hasAnyRole('CLIENTE','ATENDENTE','GERENTE','ADMIN')")
    public ResponseEntity<PagamentoResponse> pagar(@Valid @RequestBody PagamentoRequest r, @AuthenticationPrincipal UsuarioLogado u) {
        PagamentoService.Resultado res = service.pagar(r, u);
        return ResponseEntity.status(res.httpStatus()).body(res.corpo());
    }

    @Operation(summary = "Histórico de tentativas de pagamento de um pedido")
    @GetMapping
    public List<PagamentoResponse> porPedido(@RequestParam Long pedidoId, @AuthenticationPrincipal UsuarioLogado u) {
        return service.porPedido(pedidoId, u);
    }
}
