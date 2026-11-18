package com.hannzer.cqb.common.error;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 所有錯誤一律以 { code, message } 回傳（規格書第 6 節） */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 多則驗證訊息的分隔符號，與前端一致 */
    private static final String MESSAGE_SEPARATOR = "；";

    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiError> handleApp(AppException e) {
        return respond(e.status(), e.code(), e.getMessage());
    }

    /** @RequestBody 的 Bean Validation 失敗 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException e) {
        return validationFailed(join(e.getBindingResult().getAllErrors()));
    }

    /** 路徑參數、查詢參數的 Bean Validation 失敗 */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> handleParameterValidation(HandlerMethodValidationException e) {
        return validationFailed(join(e.getAllErrors()));
    }

    /** JSON 格式錯誤、型別不符、缺少參數、Content-Type 不支援 */
    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class,
        HttpMediaTypeNotSupportedException.class
    })
    ResponseEntity<ApiError> handleMalformedRequest(Exception e) {
        log.debug("請求格式錯誤", e);
        return validationFailed("請求格式錯誤");
    }

    /** 不存在的路徑，或路徑存在但不支援該方法 */
    @ExceptionHandler({
        NoResourceFoundException.class,
        NoHandlerFoundException.class,
        HttpRequestMethodNotSupportedException.class
    })
    ResponseEntity<ApiError> handleNotFound(Exception e) {
        return respond(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "找不到此 API");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception e) {
        log.error("未預期的錯誤", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "伺服器發生錯誤");
    }

    private static ResponseEntity<ApiError> validationFailed(String message) {
        return respond(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, message);
    }

    private static ResponseEntity<ApiError> respond(HttpStatus status, ErrorCode code, String message) {
        return ResponseEntity.status(status).body(ApiError.of(code, message));
    }

    private static String join(List<? extends MessageSourceResolvable> errors) {
        return errors.stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining(MESSAGE_SEPARATOR));
    }
}
