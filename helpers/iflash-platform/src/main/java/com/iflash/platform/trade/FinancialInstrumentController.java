package com.iflash.platform.trade;

import com.iflash.core.engine.FinancialInstrumentInfo;
import com.iflash.core.engine.OrderBookOperations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/instrument")
@RequiredArgsConstructor
class FinancialInstrumentController {

    private final OrderBookOperations orderBookOperations;

    @GetMapping
    ResponseEntity<List<FinancialInstrumentInfo>> getCurrentPrices() {
        List<FinancialInstrumentInfo> financialInstrumentInfo = orderBookOperations.getFinancialInstrumentInfo();

        return ResponseEntity.ok(financialInstrumentInfo);
    }
}
