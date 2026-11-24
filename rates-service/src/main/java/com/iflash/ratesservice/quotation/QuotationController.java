package com.iflash.ratesservice.quotation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quotation")
@RequiredArgsConstructor
class QuotationController {

    private final QuotationStore quotationStore;

    @GetMapping("/{ticker}/price")
    ResponseEntity<CurrentQuoteResponse> getCurrentPrice(@PathVariable String ticker) {
        Quotation currentQuote = quotationStore.currentQuote(ticker.toUpperCase());

        return ResponseEntity.ok(CurrentQuoteResponse.create(currentQuote, ticker.toUpperCase()));
    }

    @GetMapping("/{ticker}/quotes")
    ResponseEntity<CurrentMultiQuoteResponse> getCurrentPrices(@PathVariable String ticker,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size,
                                                               @RequestParam(defaultValue = "ASC") OrderBy orderBy) {
        Pagination pagination = new Pagination(page, size, orderBy);
        Page<Quotation> lastQuotes = quotationStore.lastQuotes(ticker, pagination);

        return ResponseEntity.ok(CurrentMultiQuoteResponse.create(lastQuotes, ticker.toUpperCase()));
    }
}
