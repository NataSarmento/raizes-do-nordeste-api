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

import org.slf4j.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Registro de ações sensíveis (tabela auditoria + log). Nunca grava senhas ou tokens. */
@Service
public class AuditoriaService {
    private static final Logger LOG = LoggerFactory.getLogger("AUDITORIA");
    private final AuditoriaRepository repo;

    public AuditoriaService(AuditoriaRepository repo) { this.repo = repo; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String acao, String entidade, Long entidadeId, String detalhe) {
        registrarComo(usuarioAtual(), acao, entidade, entidadeId, detalhe);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarComo(String usuario, String acao, String entidade, Long entidadeId, String detalhe) {
        Auditoria a = new Auditoria();
        a.setUsuario(usuario); a.setAcao(acao); a.setEntidade(entidade); a.setEntidadeId(entidadeId);
        a.setDetalhe(detalhe != null && detalhe.length() > 500 ? detalhe.substring(0, 500) : detalhe);
        repo.save(a);
        LOG.info("acao={} usuario={} entidade={}#{} detalhe={}", acao, usuario, entidade, entidadeId, detalhe);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditoriaResponse> listar(String acao, int page, int limit) {
        Pageable pg = PageResponse.pageable(page, limit, Sort.by(Sort.Direction.DESC, "id"));
        Page<Auditoria> r = (acao == null || acao.isBlank()) ? repo.findAll(pg) : repo.findByAcaoContainingIgnoreCase(acao, pg);
        return PageResponse.de(r, AuditoriaResponse::de);
    }

    private String usuarioAtual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a != null && a.getPrincipal() instanceof UsuarioLogado u) ? u.email() : "anonimo";
    }
}
