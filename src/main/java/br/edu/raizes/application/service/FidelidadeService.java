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


/** Regra: 1 ponto por R$ 1,00 pago (apenas com consentimento); 10 pontos = R$ 1,00 de desconto. */
@Service
public class FidelidadeService {
    public static final int PONTOS_POR_REAL_RESGATE = 10;
    private final MovimentoFidelidadeRepository repo;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;

    public FidelidadeService(MovimentoFidelidadeRepository repo, UsuarioRepository usuarios, AuditoriaService auditoria) {
        this.repo = repo; this.usuarios = usuarios; this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public SaldoResponse saldo(UsuarioLogado logado) {
        Usuario u = usuarios.findById(logado.id()).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        return new SaldoResponse(repo.saldo(u.getId()), u.isConsentimentoFidelidade());
    }

    @Transactional(readOnly = true)
    public PageResponse<MovimentoFidelidadeResponse> historico(UsuarioLogado logado, int page, int limit) {
        return PageResponse.de(repo.findByUsuarioIdOrderByIdDesc(logado.id(), PageResponse.pageable(page, limit, Sort.unsorted())),
                MovimentoFidelidadeResponse::de);
    }

    @Transactional
    public SaldoResponse atualizarConsentimento(UsuarioLogado logado, boolean aceito) {
        Usuario u = usuarios.findById(logado.id()).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        u.setConsentimentoFidelidade(aceito);
        auditoria.registrar("CONSENTIMENTO_FIDELIDADE_ALTERADO", "Usuario", u.getId(), "aceito=" + aceito);
        return new SaldoResponse(repo.saldo(u.getId()), aceito);
    }

    public long saldoDe(Long usuarioId) { return repo.saldo(usuarioId); }

    public void acumular(Pedido p) {
        Usuario u = p.getCliente();
        int pontos = p.getTotal().intValue();
        if (u.isConsentimentoFidelidade() && pontos > 0) registrar(u, TipoFidelidade.ACUMULO, pontos, p.getId());
    }

    public void resgatar(Usuario u, int pontos, Long pedidoId) { registrar(u, TipoFidelidade.RESGATE, -pontos, pedidoId); }

    public void estornar(Usuario u, int pontos, Long pedidoId) { registrar(u, TipoFidelidade.ESTORNO, pontos, pedidoId); }

    private void registrar(Usuario u, TipoFidelidade tipo, int pontos, Long pedidoId) {
        MovimentoFidelidade m = new MovimentoFidelidade();
        m.setUsuario(u); m.setTipo(tipo); m.setPontos(pontos); m.setPedidoId(pedidoId);
        repo.save(m);
    }
}
