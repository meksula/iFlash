package com.iflash.ratesservice.quotation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/quotation")
class QuotationController {

    @GetMapping
    Mono<String> fetchQuote() {
        return Mono.just("test");
    }
}
