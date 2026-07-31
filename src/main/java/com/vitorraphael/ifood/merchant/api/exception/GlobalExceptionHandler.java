package com.vitorraphael.ifood.merchant.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(VendaNaoEncontradaException.class)
    public ResponseEntity<ErroResposta> tratarVendaNaoEncontrada(VendaNaoEncontradaException e, HttpServletRequest request) {
        return construirResposta(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(TokenIndisponivelException.class)
    public ResponseEntity<ErroResposta> tratarTokenIndisponivel(TokenIndisponivelException e, HttpServletRequest request) {
        log.error("Token indisponível: {}", e.getMessage());
        return construirResposta(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), request);
    }

    @ExceptionHandler(IFoodApiException.class)
    public ResponseEntity<ErroResposta> tratarIFoodApiException(IFoodApiException e, HttpServletRequest request) {
        HttpStatus status = mapearStatusIFood(e.getStatusIfood());
        if (status.is5xxServerError()) {
            log.error("Erro de integração com o iFood [{}]: {}", e.getStatusIfood(), e.getMessage());
        }
        return construirResposta(status, e.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> tratarErroGenerico(Exception e, HttpServletRequest request) {
        log.error("Erro inesperado em {}: {}", request.getRequestURI(), e.getMessage(), e);
        return construirResposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado.", request);
    }

    /**
     * Traduz o status HTTP que o iFood devolveu pro status que faz sentido a nossa
     * própria API responder: erros de credencial/permissão da nossa integração viram
     * 502 (o problema é nosso, não de quem chamou nossa API); conflito e limite de
     * requisição são repassados como fazem sentido pro cliente da nossa API também.
     */
    private HttpStatus mapearStatusIFood(int statusIfood) {
        return switch (statusIfood) {
            case 404 -> HttpStatus.NOT_FOUND;
            case 409 -> HttpStatus.CONFLICT;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }

    private ResponseEntity<ErroResposta> construirResposta(HttpStatus status, String mensagem, HttpServletRequest request) {
        ErroResposta erro = new ErroResposta(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(erro);
    }
}
