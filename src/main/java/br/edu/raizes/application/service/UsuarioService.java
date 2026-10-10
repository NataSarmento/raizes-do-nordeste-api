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

import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UsuarioService {
    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final AuditoriaService auditoria;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder, AuditoriaService auditoria) {
        this.repo = repo; this.encoder = encoder; this.auditoria = auditoria;
    }

    @Transactional
    public UsuarioResponse criarFuncionario(CriarUsuarioRequest r) {
        String email = r.email().trim().toLowerCase();
        if (repo.existsByEmail(email)) throw new ConflitoException("E-mail já cadastrado");
        Usuario u = new Usuario();
        u.setNome(r.nome().trim()); u.setEmail(email); u.setPerfil(r.perfil());
        u.setSenhaHash(encoder.encode(r.senha()));
        u.setConsentimentoLgpd(true); u.setConsentimentoLgpdEm(LocalDateTime.now());
        u = repo.save(u);
        auditoria.registrar("USUARIO_CRIADO_POR_ADMIN", "Usuario", u.getId(), "perfil=" + r.perfil());
        return UsuarioResponse.de(u);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse meusDados(UsuarioLogado logado) {
        Usuario u = buscar(logado.id());
        auditoria.registrar("ACESSO_DADOS_PESSOAIS", "Usuario", u.getId(), "consulta ao próprio perfil");
        return UsuarioResponse.de(u);
    }

    /** LGPD: direito de eliminação/anonimização. */
    @Transactional
    public void anonimizar(UsuarioLogado logado) {
        Usuario u = buscar(logado.id());
        u.anonimizar();
        auditoria.registrar("USUARIO_ANONIMIZADO", "Usuario", u.getId(), "solicitação do titular (LGPD art. 18)");
    }

    private Usuario buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }
}
