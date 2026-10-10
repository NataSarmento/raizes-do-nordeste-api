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


@RestController @RequestMapping("/promocoes") @Tag(name = "Promoções") @SecurityRequirement(name = "bearerAuth")
public class PromocaoController {
    private final CatalogoService service;
    public PromocaoController(CatalogoService service) { this.service = service; }

    @Operation(summary = "Lista promoções (informe o código em codigoPromocao ao criar o pedido)")
    @GetMapping
    public PageResponse<PromocaoResponse> listar(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int limit) {
        return service.listarPromocoes(page, limit);
    }

    @Operation(summary = "Cria promoção (GERENTE/ADMIN)")
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public PromocaoResponse criar(@Valid @RequestBody PromocaoRequest r) { return service.criarPromocao(r); }
}
