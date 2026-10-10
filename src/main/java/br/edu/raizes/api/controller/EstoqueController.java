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


@RestController @RequestMapping("/estoque") @Tag(name = "Estoque") @SecurityRequirement(name = "bearerAuth")
public class EstoqueController {
    private final EstoqueService service;
    public EstoqueController(EstoqueService service) { this.service = service; }

    @Operation(summary = "Movimenta estoque (entrada/saída) de uma unidade (GERENTE/ADMIN)")
    @PostMapping("/movimentos") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public EstoqueResponse movimentar(@Valid @RequestBody MovimentoEstoqueRequest r) { return service.movimentar(r); }

    @Operation(summary = "Consulta saldo de estoque por unidade")
    @GetMapping @PreAuthorize("hasAnyRole('ATENDENTE','COZINHA','GERENTE','ADMIN')")
    public PageResponse<EstoqueResponse> consultar(@RequestParam Long unidadeId, @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int limit) {
        return service.consultar(unidadeId, page, limit);
    }
}
