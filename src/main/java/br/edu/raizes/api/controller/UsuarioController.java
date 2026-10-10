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


@RestController @RequestMapping("/usuarios") @Tag(name = "Usuários") @SecurityRequirement(name = "bearerAuth")
public class UsuarioController {
    private final UsuarioService service;
    public UsuarioController(UsuarioService service) { this.service = service; }

    @Operation(summary = "ADMIN cria usuário de funcionário (ATENDENTE, COZINHA, GERENTE, ADMIN)")
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse criar(@Valid @RequestBody CriarUsuarioRequest r) { return service.criarFuncionario(r); }

    @Operation(summary = "Meus dados (acesso é auditado)")
    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal UsuarioLogado u) { return service.meusDados(u); }

    @Operation(summary = "LGPD: anonimiza a própria conta")
    @DeleteMapping("/me") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void anonimizar(@AuthenticationPrincipal UsuarioLogado u) { service.anonimizar(u); }
}
