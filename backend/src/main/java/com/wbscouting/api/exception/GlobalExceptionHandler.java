package com.wbscouting.api.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
// ✅ PACOTES CORRETOS (com .annotation no final - ORIGINALMENTE FUNCIONAVA!
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Recurso Não Encontrado");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new java.util.LinkedHashMap<>();
        java.util.List<String> errorMessages = new java.util.ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
            errorMessages.add(error.getField() + ": " + error.getDefaultMessage());
        }

        String detailMsg = errorMessages.isEmpty()
                ? "Falha na validação dos campos da requisição."
                : "Falha na validação: " + String.join(" | ", errorMessages);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detailMsg);
        problemDetail.setTitle("Erro de Validação");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/validation"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errors", fieldErrors);

        return problemDetail;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentialsException(BadCredentialsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenciais inválidas. Verifique seu e-mail e senha.");
        problemDetail.setTitle("Credenciais Inválidas");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/unauthorized"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Acesso negado para este recurso.");
        problemDetail.setTitle("Acesso Proibido");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/forbidden"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(DuplicatePromotionException.class)
    public ProblemDetail handleDuplicatePromotionException(DuplicatePromotionException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Candidatura Já Promovida");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/conflict"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Regra de Negócio Violada");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/business-rule"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(InvalidApplicationException.class)
    public ProblemDetail handleInvalidApplicationException(InvalidApplicationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Candidatura Inválida");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/invalid-application"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(InvalidFileException.class)
    public ProblemDetail handleInvalidFileException(InvalidFileException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Arquivo Inválido");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/invalid-file"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(FileSizeExceededException.class)
    public ProblemDetail handleFileSizeExceededException(FileSizeExceededException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Tamanho de Arquivo Excedido");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/file-size-exceeded"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(StorageException.class)
    public ProblemDetail handleStorageException(StorageException ex) {
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_GATEWAY;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problemDetail.setTitle(ex.getTitle() != null ? ex.getTitle() : "Erro de Integração com Armazenamento");
        String typeSlug = ex.getErrorCode() != null ? ex.getErrorCode().toLowerCase().replace('_', '-') : "storage-error";
        problemDetail.setType(URI.create("https://wbscouting.com/errors/" + typeSlug));
        problemDetail.setProperty("timestamp", Instant.now());
        if (ex.getErrorCode() != null) {
            problemDetail.setProperty("storageErrorCode", ex.getErrorCode());
        }
        return problemDetail;
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSizeExceededException(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "O arquivo enviado excede o limite máximo permitido pelo servidor.");
        problemDetail.setTitle("Tamanho Máximo de Requisição Excedido");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/max-upload-size"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidTokenException(InvalidTokenException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Link ou Token Inválido ou Expirado");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/invalid-token"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Requisição Inválida");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/bad-request"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    // ============================================================
    // 🔥 CORREÇÃO DA MORTE (500 genéricos no upload composite!)
    // O envelope anti-exceção do uploadOrReplaceComposite converte
    // NPE / DataIntegrity / Lock etc em IllegalStateException.
    // ANTES: NÃO TINHA HANDLER → caía no Exception generico da linha 177
    //         → HTTP 500 genérico "Ocorreu um erro interno..." sem detalhe.
    // AGORA: Tem handler específico → HTTP 400 + MENSAGEM AMIGÁVEL NO DETAIL!
    // ============================================================
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalStateException(IllegalStateException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Não foi possível concluir a operação");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/illegal-state"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    // ============================================================
    // 🔥 Extra: Captura excecoes de TRANSACTION / ROLLBACK (muito comum
    //    em modelMediaRepository.save/delete/flush quando violacao FK ou
    //    ConstraintViolation NOT NULL). Antes virava 500 generico tambem.
    // ============================================================
    @ExceptionHandler(org.springframework.transaction.TransactionSystemException.class)
    public ProblemDetail handleTransactionSystemException(org.springframework.transaction.TransactionSystemException ex) {
        String msg = "Falha de transação ao salvar os dados no banco de dados.";
        if (ex.getRootCause() != null && ex.getRootCause().getMessage() != null && !ex.getRootCause().getMessage().isBlank()) {
            msg = msg + " Detalhe: " + ex.getRootCause().getMessage();
        } else if (ex.getMessage() != null) {
            msg = msg + " Motivo: " + ex.getMessage();
        }
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, msg);
        problemDetail.setTitle("Erro ao Persistir Dados");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/transaction-failed"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    // ============================================================
    // 🔥 Extra: Captura DataIntegrityViolationException (NOT NULL, UNIQUE, FK)
    //    antes de chegar no generico. Ex: coluna created_at NULL violacao.
    // ============================================================
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex) {
        String msg = "Um campo obrigatório está vazio ou existe um conflito de dados no banco.";
        if (ex.getMostSpecificCause() != null && ex.getMostSpecificCause().getMessage() != null) {
            msg = msg + " Detalhe: " + ex.getMostSpecificCause().getMessage();
        } else if (ex.getMessage() != null) {
            msg = msg + " Motivo: " + ex.getMessage();
        }
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, msg);
        problemDetail.setTitle("Erro de Integridade dos Dados");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/data-integrity"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFoundException(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Recurso não encontrado: " + ex.getResourcePath());
        problemDetail.setTitle("Recurso Não Encontrado");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupportedException(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não suportado para esta rota: " + ex.getMethod());
        problemDetail.setTitle("Método Não Permitido");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/method-not-allowed"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(InvalidSortPropertyException.class)
    public ProblemDetail handleInvalidSortPropertyException(InvalidSortPropertyException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Propriedade de Ordenação Inválida");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/invalid-sort"));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("invalidProperty", ex.getProperty());
        return problemDetail;
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(org.springframework.web.server.ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String reason = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, reason);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setType(URI.create("https://wbscouting.com/errors/" + status.value()));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    // ============================================================
    // 🛡️ Handler CATCH-ALL genérico: (qualquer exceção SEM handler específico).
    // ANTES (antigo bug de 500 generico):
    //   ProblemDetail.forStatusAndDetail(500, "Ocorreu um erro interno no servidor.");
    //   = SEMPRE apagava a mensagem REAL do erro.
    // HOJE (corrigido):
    //   1) Usa ex.getMessage() SE existir (mensagem real!)
    //   2) Usa ex.getCause().getMessage() SE existir.
    //   3) Só usa fallback genérico se AMBOS forem nulos.
    //   4) Mensagem fica no CAMPO DETAIL (nao debug_message) = FRONT EXIBE!
    // ============================================================
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        String msg = ex.getMessage();
        boolean msgVazia = (msg == null || msg.isBlank());
        if (msgVazia && ex.getCause() != null && ex.getCause().getMessage() != null && !ex.getCause().getMessage().isBlank()) {
            msg = ex.getCause().getMessage();
            msgVazia = false;
        }
        String detailFinal;
        if (msgVazia) {
            detailFinal = "Ocorreu um erro interno no servidor.";
        } else {
            detailFinal = msg;
        }
        // Garantia: se comecar com org.springframework / stack / java... -> user friendly
        if (detailFinal.startsWith("org.") || detailFinal.startsWith("java.") || detailFinal.length() > 300) {
            detailFinal = "Ocorreu um erro interno inesperado no servidor. A equipe técnica foi notificada.";
        }
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, detailFinal);
        problemDetail.setTitle("Erro Interno do Servidor");
        problemDetail.setType(URI.create("https://wbscouting.com/errors/internal"));
        problemDetail.setProperty("timestamp", Instant.now());
        // Debug message p/ equipe (nao exibido pro user, mas disponivel no JSON)
        try {
            String stack = ex.getClass().getSimpleName() + ": " + (ex.getMessage() != null ? ex.getMessage() : "sem mensagem");
            if (ex.getCause() != null) stack = stack + " | CAUSE: " + ex.getCause().getClass().getSimpleName() + " = " + ex.getCause().getMessage();
            problemDetail.setProperty("debug_message", stack);
        } catch (Exception ignore) {}
        return problemDetail;
    }
}
