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


@RestController @RequestMapping("/produtos") @Tag(name = "Produtos") @SecurityRequirement(name = "bearerAuth")
public class ProdutoController {
    private final CatalogoService service;
    public ProdutoController(CatalogoService service) { this.service = service; }

    @Operation(summary = "Lista produtos ativos")
    @GetMapping
    public PageResponse<ProdutoResponse> listar(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int limit) {
        return service.listarProdutos(page, limit);
    }

    @Operation(summary = "Consulta produto por id")
    @GetMapping("/{id}")
    public ProdutoResponse obter(@PathVariable Long id) { return service.obterProduto(id); }

    @Operation(summary = "Cria produto (GERENTE/ADMIN)")
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ProdutoResponse criar(@Valid @RequestBody ProdutoRequest r) { return service.criarProduto(r); }

    @Operation(summary = "Atualiza produto (GERENTE/ADMIN)")
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public ProdutoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest r) { return service.atualizarProduto(id, r); }

    @Operation(summary = "Desativa produto (GERENTE/ADMIN)")
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasAnyRole('GERENTE','ADMIN')")
    public void desativar(@PathVariable Long id) { service.desativarProduto(id); }
}
