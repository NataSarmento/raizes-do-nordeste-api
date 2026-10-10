package br.edu.raizes.api.dto;

import org.springframework.data.domain.*;
import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> conteudo, int page, int limit, long totalElementos, int totalPaginas) {

    public static <E, T> PageResponse<T> de(Page<E> p, Function<E, T> mapper) {
        return new PageResponse<>(p.getContent().stream().map(mapper).toList(),
                p.getNumber() + 1, p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    /** page é 1-based na API; limit entre 1 e 100. */
    public static Pageable pageable(int page, int limit, Sort sort) {
        if (page < 1 || limit < 1 || limit > 100) {
            throw new br.edu.raizes.domain.exception.RegraInvalidaException(
                    "Paginação inválida: page >= 1 e 1 <= limit <= 100");
        }
        return PageRequest.of(page - 1, limit, sort);
    }
}
