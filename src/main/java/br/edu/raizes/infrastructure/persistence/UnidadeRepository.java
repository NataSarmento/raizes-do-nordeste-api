package br.edu.raizes.infrastructure.persistence;

import br.edu.raizes.domain.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {
    Page<Unidade> findByAtivaTrue(Pageable pageable);
}
