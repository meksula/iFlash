package com.iflash.tradingservice.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
class MatchingEngineClient {

    private final WebClient webClient;

    MatchingEngineClient(WebClient.Builder webClientBuilder, @Value("${matching-engine-service.url}") String matchingEngineServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(matchingEngineServiceUrl)
                                         .build();
    }

    Mono<OrderRegistrationResult> registerOrder(RegisterOrderRequest registerOrderRequest) {
        return webClient.post()
                        .uri("/internal/api/v1/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(registerOrderRequest)
                        .exchangeToMono(response -> response.statusCode().is2xxSuccessful()
                                                    ? response.bodyToMono(OrderRegistrationResult.class)
                                                    : response.bodyToMono(String.class)
                                                              .defaultIfEmpty("")
                                                              .flatMap(body -> Mono.error(new MatchingEngineRejectionException(response.statusCode(), body))));
    }
}
