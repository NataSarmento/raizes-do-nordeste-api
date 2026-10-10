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


@RestController @RequestMapping("/auth") @Tag(name = "Auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @Operation(summary = "Cadastro público de cliente (exige consentimento LGPD)")
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest r) { return service.registrar(r); }

    @Operation(summary = "Login: retorna token JWT")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest r) { return service.login(r); }
}
