package com.iflash.brokerplatform.market;

import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Wraps an error response from the iFlash engine, surfacing its {@code message} field. */
public class EngineException extends RuntimeException {

    private static final Pattern MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    public EngineException(String message) {
        super(message);
    }

    static EngineException from(ClientHttpResponse response) throws IOException {
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        return new EngineException(extractMessage(body));
    }

    private static String extractMessage(String body) {
        if (body == null || body.isBlank()) {
            return "Engine rejected the request";
        }
        Matcher matcher = MESSAGE.matcher(body);
        if (matcher.find()) {
            return matcher.group(1).replace("\\\"", "\"");
        }
        return body;
    }
}
