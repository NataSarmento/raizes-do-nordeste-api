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


@RestController @RequestMapping("/fidelidade") @Tag(name = "Fidelidade") @SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('CLIENTE')")
public class FidelidadeController {
    private final FidelidadeService service;
    public FidelidadeController(FidelidadeService service) { this.service = service; }

    @Operation(summary = "Saldo de pontos")
    @GetMapping("/saldo")
    public SaldoResponse saldo(@AuthenticationPrincipal UsuarioLogado u) { return service.saldo(u); }

    @Operation(summary = "Histórico de pontos (acúmulo, resgate, estorno)")
    @GetMapping("/historico")
    public PageResponse<MovimentoFidelidadeResponse> historico(@AuthenticationPrincipal UsuarioLogado u,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int limit) {
        return service.historico(u, page, limit);
    }

    @Operation(summary = "Registra/revoga consentimento ao programa de fidelidade (LGPD)")
    @PutMapping("/consentimento")
    public SaldoResponse consentimento(@Valid @RequestBody ConsentimentoRequest r, @AuthenticationPrincipal UsuarioLogado u) {
        return service.atualizarConsentimento(u, r.fidelidade());
    }
}
