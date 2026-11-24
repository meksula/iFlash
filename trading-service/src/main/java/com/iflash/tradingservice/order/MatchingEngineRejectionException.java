package com.iflash.tradingservice.order;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class MatchingEngineRejectionException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final String responseBody;

    MatchingEngineRejectionException(HttpStatusCode statusCode, String responseBody) {
        super(String.format("Matching engine rejected order with status %s: %s", statusCode, responseBody));
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
}
