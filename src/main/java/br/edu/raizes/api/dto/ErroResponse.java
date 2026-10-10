package br.edu.raizes.api.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Padrão único de erro para toda a API. */
public record ErroResponse(LocalDateTime timestamp, int status, String codigo, String mensagem,
                           String path, List<String> detalhes) { }
