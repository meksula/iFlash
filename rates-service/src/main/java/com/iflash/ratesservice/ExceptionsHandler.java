package com.iflash.ratesservice;

import com.iflash.ratesservice.quotation.QuotationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ExceptionsHandler {

    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    @ExceptionHandler(exception = QuotationException.class)
    public ExceptionResponse quotationException(QuotationException quotationException) {
        return new ExceptionResponse(quotationException.getMessage());
    }

    record ExceptionResponse(String message) {}
}
