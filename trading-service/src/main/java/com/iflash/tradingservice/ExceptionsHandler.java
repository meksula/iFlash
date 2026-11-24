package com.iflash.tradingservice;

import com.iflash.tradingservice.order.MatchingEngineRejectionException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ExceptionsHandler {

    @ExceptionHandler(exception = MatchingEngineRejectionException.class)
    public ResponseEntity<String> matchingEngineRejection(MatchingEngineRejectionException matchingEngineRejectionException) {
        return ResponseEntity.status(matchingEngineRejectionException.getStatusCode())
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(matchingEngineRejectionException.getResponseBody());
    }
}
