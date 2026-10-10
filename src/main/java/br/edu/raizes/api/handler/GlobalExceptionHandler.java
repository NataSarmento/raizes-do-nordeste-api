package br.edu.raizes.api.handler;

import br.edu.raizes.api.dto.ErroResponse;
import br.edu.raizes.domain.exception.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Converte qualquer falha no padrão único ErroResponse. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ErroResponse> erro(HttpStatus s, String codigo, String msg, HttpServletRequest req, List<String> det) {
        return ResponseEntity.status(s).body(new ErroResponse(LocalDateTime.now(), s.value(), codigo, msg, req.getRequestURI(), det));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> det = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).sorted().collect(Collectors.toList());
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDACAO", "Dados inválidos", req, det);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException ex, HttpServletRequest req) {
        if (ex.getCause() instanceof InvalidFormatException f) {
            String campo = f.getPath().isEmpty() ? "?" : f.getPath().get(f.getPath().size() - 1).getFieldName();
            String aceitos = f.getTargetType() != null && f.getTargetType().isEnum()
                    ? " Valores aceitos: " + Arrays.toString(f.getTargetType().getEnumConstants()) : "";
            return erro(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDACAO", "Valor inválido para o campo '" + campo + "'." + aceitos, req, null);
        }
        return erro(HttpStatus.BAD_REQUEST, "CORPO_INVALIDO", "Corpo da requisição ausente ou JSON malformado", req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErroResponse> tipoParametro(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return erro(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO", "Parâmetro '" + ex.getName() + "' com valor inválido: " + ex.getValue(), req, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErroResponse> parametroAusente(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return erro(HttpStatus.BAD_REQUEST, "PARAMETRO_AUSENTE", "Parâmetro obrigatório ausente: " + ex.getParameterName(), req, null);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return erro(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", ex.getMessage(), req, null);
    }

    @ExceptionHandler({ConflitoException.class, DataIntegrityViolationException.class})
    ResponseEntity<ErroResponse> conflito(Exception ex, HttpServletRequest req) {
        String msg = ex instanceof ConflitoException ? ex.getMessage() : "Conflito de dados (registro duplicado ou violação de integridade)";
        return erro(HttpStatus.CONFLICT, "CONFLITO", msg, req, null);
    }

    @ExceptionHandler(RegraInvalidaException.class)
    ResponseEntity<ErroResponse> regraInvalida(RegraInvalidaException ex, HttpServletRequest req) {
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, "REGRA_INVALIDA", ex.getMessage(), req, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErroResponse> naoAutenticado(AuthenticationException ex, HttpServletRequest req) {
        return erro(HttpStatus.UNAUTHORIZED, "NAO_AUTENTICADO", ex.getMessage(), req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErroResponse> acessoNegado(AccessDeniedException ex, HttpServletRequest req) {
        return erro(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", "Seu perfil não tem permissão para esta operação", req, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErroResponse> metodo(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return erro(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NAO_PERMITIDO", ex.getMessage(), req, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErroResponse> rota(NoResourceFoundException ex, HttpServletRequest req) {
        return erro(HttpStatus.NOT_FOUND, "ROTA_INEXISTENTE", "Rota inexistente", req, null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroResponse> inesperado(Exception ex, HttpServletRequest req) {
        LOG.error("Erro inesperado em {}", req.getRequestURI(), ex);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", "Erro interno. Tente novamente mais tarde", req, null);
    }
}
