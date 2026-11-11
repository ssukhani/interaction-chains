package com.interactionchains.app.analysis;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UnknownCatalogItemException.class)
    ResponseEntity<ApiError> unknown(UnknownCatalogItemException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(InvalidAnalysisRequestException.class)
    ResponseEntity<ApiError> invalid(InvalidAnalysisRequestException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(IntentServiceException.class)
    ResponseEntity<ApiError> intent(IntentServiceException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ApiError(exception.getMessage()));
    }
}
