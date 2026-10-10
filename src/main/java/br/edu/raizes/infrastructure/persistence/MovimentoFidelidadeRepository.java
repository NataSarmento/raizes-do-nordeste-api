package br.edu.raizes.infrastructure.persistence;

import br.edu.raizes.domain.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface MovimentoFidelidadeRepository extends JpaRepository<MovimentoFidelidade, Long> {
    @Query("select coalesce(sum(m.pontos), 0) from MovimentoFidelidade m where m.usuario.id = :usuarioId")
    long saldo(Long usuarioId);

    Page<MovimentoFidelidade> findByUsuarioIdOrderByIdDesc(Long usuarioId, Pageable pageable);
}
