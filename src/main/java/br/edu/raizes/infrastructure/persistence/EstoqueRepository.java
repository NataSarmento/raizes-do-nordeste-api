package br.edu.raizes.infrastructure.persistence;

import br.edu.raizes.domain.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {
    Optional<Estoque> findByUnidadeIdAndProdutoId(Long unidadeId, Long produtoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Estoque e where e.unidade.id = :unidadeId and e.produto.id = :produtoId")
    Optional<Estoque> buscarParaAtualizar(Long unidadeId, Long produtoId);

    Page<Estoque> findByUnidadeId(Long unidadeId, Pageable pageable);
    Page<Estoque> findByUnidadeIdAndProdutoAtivoTrue(Long unidadeId, Pageable pageable);
}
