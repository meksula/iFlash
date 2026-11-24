package com.iflash.instrument.api;

import com.iflash.instrument.data.FinancialInstrumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/v1/instrument")
@RequiredArgsConstructor
class InstrumentController {

    private final FinancialInstrumentRepository financialInstrumentRepository;

    @GetMapping
    Flux<FinancialInstrumentInfo> getInstruments() {
        return Flux.defer(() -> Flux.fromIterable(financialInstrumentRepository.findAllInitialOrderedByTicker()))
                   .map(financialInstrumentInitial -> new FinancialInstrumentInfo(financialInstrumentInitial.ticker(), financialInstrumentInitial.price()))
                   .subscribeOn(Schedulers.boundedElastic());
    }
}
