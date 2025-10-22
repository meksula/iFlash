package com.iflash.platform.quotation;

import com.iflash.commons.OrderBy;
import com.iflash.core.quotation.CurrentQuote;
import com.iflash.core.quotation.QuotationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/quotation")
@RequiredArgsConstructor
class QuotationController {

    private final QuotationProvider quotationProvider;

    @GetMapping("/{ticker}/price")
    ResponseEntity<CurrentQuoteResponse> getCurrentPrice(@PathVariable String ticker) {
        CurrentQuote currentQuote = quotationProvider.getCurrentQuote(ticker.toUpperCase());

        return ResponseEntity.ok(CurrentQuoteResponse.create(currentQuote, ticker.toUpperCase()));
    }

    @GetMapping("/{ticker}/{limit}/{orderBy}")
    ResponseEntity<CurrentMultiQuoteResponse> getCurrentPrices(@PathVariable String ticker, @PathVariable Integer limit, @PathVariable OrderBy orderBy) {
        List<CurrentQuote> lastQuotes = quotationProvider.getLastQuotes(ticker, limit, orderBy);

        return ResponseEntity.ok(CurrentMultiQuoteResponse.create(lastQuotes, ticker.toUpperCase()));
    }
}
