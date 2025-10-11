package com.iflash.platform.trade;

import com.iflash.core.quotation.CurrentQuote;
import com.iflash.core.quotation.QuotationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class TradeController {

    private final QuotationProvider quotationProvider;

    @GetMapping("/{ticker}")
    ResponseEntity<CurrentQuoteResponse> getCurrentPrice(@PathVariable String ticker) {
        CurrentQuote currentQuote = quotationProvider.getCurrentQuote(ticker);

        return ResponseEntity.ok(CurrentQuoteResponse.create(currentQuote, ticker));
    }
}
