package com.iflash.platform;

import com.iflash.core.configuration.MatchingEngineException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ExceptionsHandler {

    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    @ExceptionHandler(exception = MatchingEngineException.class)
    public ExceptionResponse matchingEngineException(MatchingEngineException matchingEngineException) {
        return new ExceptionResponse(matchingEngineException.getMessage());
    }

    record ExceptionResponse(String message) {}
}
