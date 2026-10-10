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


@RestController @RequestMapping("/unidades") @Tag(name = "Unidades")
public class UnidadeController {
    private final CatalogoService service;
    public UnidadeController(CatalogoService service) { this.service = service; }

    @Operation(summary = "Lista unidades da rede (público)")
    @GetMapping
    public PageResponse<UnidadeResponse> listar(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int limit) {
        return service.listarUnidades(page, limit);
    }

    @Operation(summary = "Cardápio de uma unidade, com disponibilidade pelo estoque (público)")
    @GetMapping("/{id}/cardapio")
    public PageResponse<CardapioItemResponse> cardapio(@PathVariable Long id, @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int limit) {
        return service.cardapio(id, page, limit);
    }

    @Operation(summary = "Cria unidade (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public UnidadeResponse criar(@Valid @RequestBody UnidadeRequest r) { return service.criarUnidade(r); }
}
