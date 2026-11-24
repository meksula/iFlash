package com.iflash.mes.order;

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
@RequestMapping("/internal/api/v1/order")
@RequiredArgsConstructor
class OrderController {

    private final TradingOperations tradingOperations;

    @PostMapping
    ResponseEntity<OrderRegistrationResult> registerOrder(@RequestBody RegisterOrderCommand registerOrderCommand) {
        log.info("Order registration command: {}", registerOrderCommand);

        OrderRegistrationResult orderRegistrationResult = tradingOperations.registerOrder(registerOrderCommand);

        log.info("Order registration end with result: {}", orderRegistrationResult);
        return ResponseEntity.ok(orderRegistrationResult);
    }
}
