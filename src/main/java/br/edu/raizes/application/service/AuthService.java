package br.edu.raizes.application.service;

import br.edu.raizes.api.dto.*;
import br.edu.raizes.api.dto.Dtos.*;
import br.edu.raizes.domain.exception.*;
import br.edu.raizes.domain.model.*;
import br.edu.raizes.infrastructure.persistence.*;
import br.edu.raizes.infrastructure.security.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.*;
import java.time.*;
import java.util.*;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AuthService {
    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditoriaService auditoria;

    public AuthService(UsuarioRepository repo, PasswordEncoder encoder, JwtService jwt, AuditoriaService auditoria) {
        this.repo = repo; this.encoder = encoder; this.jwt = jwt; this.auditoria = auditoria;
    }

    /** Cadastro público: sempre perfil CLIENTE; exige consentimento LGPD (validado no DTO). */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest r) {
        String email = r.email().trim().toLowerCase();
        if (repo.existsByEmail(email)) throw new ConflitoException("E-mail já cadastrado");
        Usuario u = new Usuario();
        u.setNome(r.nome().trim()); u.setEmail(email);
        u.setSenhaHash(encoder.encode(r.senha()));
        u.setPerfil(Perfil.CLIENTE);
        u.setConsentimentoLgpd(true); u.setConsentimentoLgpdEm(LocalDateTime.now());
        u.setConsentimentoFidelidade(r.consentimentoFidelidade());
        u = repo.save(u);
        auditoria.registrarComo(email, "USUARIO_CADASTRADO", "Usuario", u.getId(), "perfil=CLIENTE consentimentoLgpd=true");
        return UsuarioResponse.de(u);
    }

    public TokenResponse login(LoginRequest r) {
        String email = r.email().trim().toLowerCase();
        Usuario u = repo.findByEmail(email).filter(Usuario::isAtivo).orElse(null);
        if (u == null || !encoder.matches(r.senha(), u.getSenhaHash())) {
            auditoria.registrarComo(email, "LOGIN_FALHA", "Usuario", null, "credenciais inválidas");
            throw new BadCredentialsException("Credenciais inválidas");
        }
        auditoria.registrarComo(email, "LOGIN_SUCESSO", "Usuario", u.getId(), null);
        return new TokenResponse(jwt.gerar(u), "Bearer", jwt.getExpiracaoSegundos());
    }
}
