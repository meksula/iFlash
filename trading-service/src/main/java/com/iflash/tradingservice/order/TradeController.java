package com.iflash.tradingservice.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/trade")
@RequiredArgsConstructor
class TradeController {

    private final MatchingEngineClient matchingEngineClient;

    @PostMapping("/order")
    Mono<ResponseEntity<RegisterOrderResponse>> registerOrder(@RequestBody RegisterOrderRequest registerOrderRequest) {
        log.debug("Order registration request: {}", registerOrderRequest);

        return matchingEngineClient.registerOrder(registerOrderRequest)
                                   .doOnNext(orderRegistrationResult -> log.debug("Order registration end with result: {}", orderRegistrationResult))
                                   .map(orderRegistrationResult -> ResponseEntity.ok(RegisterOrderResponse.response(orderRegistrationResult, registerOrderRequest)));
    }
}
