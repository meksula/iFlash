package com.iflash.platform.trade;

import com.iflash.core.engine.TradingOperations;
import com.iflash.core.order.OrderRegistrationResult;
import com.iflash.core.order.RegisterOrderCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/trade")
@RequiredArgsConstructor
class TradeController {

    private final TradingOperations tradingOperations;

    @PostMapping("/order")
    ResponseEntity<RegisterOrderResponse> registerOrder(@RequestBody RegisterOrderRequest registerOrderRequest) {
        log.info("Order registration request: {}", registerOrderRequest);

        RegisterOrderCommand registerOrderCommand = registerOrderRequest.command();
        OrderRegistrationResult orderRegistrationResult = tradingOperations.registerOrder(registerOrderCommand);
        RegisterOrderResponse registerOrderResponse = RegisterOrderResponse.response(orderRegistrationResult, registerOrderRequest);

        log.info("Order registration end with result: {}", orderRegistrationResult);
        return ResponseEntity.ok(registerOrderResponse);
    }
}
