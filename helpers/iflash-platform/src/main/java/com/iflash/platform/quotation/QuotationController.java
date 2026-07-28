package com.iflash.platform.quotation;

import com.iflash.commons.OrderBy;
import com.iflash.commons.Page;
import com.iflash.commons.Pagination;
import com.iflash.core.quotation.CurrentQuotation;
import com.iflash.core.quotation.QuotationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/quotation")
@RequiredArgsConstructor
class QuotationController {

    private final QuotationProvider quotationProvider;

    @GetMapping("/{ticker}/price")
    ResponseEntity<CurrentQuoteResponse> getCurrentPrice(@PathVariable String ticker) {
        CurrentQuotation currentQuotation = quotationProvider.getCurrentQuote(ticker.toUpperCase());

        return ResponseEntity.ok(CurrentQuoteResponse.create(currentQuotation, ticker.toUpperCase()));
    }

    @GetMapping("/{ticker}/quotes")
    ResponseEntity<CurrentMultiQuoteResponse> getCurrentPrices(@PathVariable String ticker,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size,
                                                               @RequestParam(defaultValue = "ASC") OrderBy orderBy) {
        Pagination pagination = new Pagination(page, size, orderBy);
        Page<CurrentQuotation> lastQuotes = quotationProvider.getLastQuotes(ticker, pagination);

        return ResponseEntity.ok(CurrentMultiQuoteResponse.create(lastQuotes, ticker.toUpperCase()));
    }
}
