package br.edu.raizes.infrastructure.security;

import br.edu.raizes.domain.model.Perfil;

/** Principal extraído do token JWT. */
public record UsuarioLogado(Long id, String email, Perfil perfil) { }
