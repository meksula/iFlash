package com.iflash.platform.maintenance;

import com.iflash.core.engine.MatchingEngine;
import com.iflash.core.engine.TickerRegistrationCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class CompanyDataLoader {

    @Autowired
    private MatchingEngine matchingEngine;

    @Value("${engine.initial-data-path}")
    private String initialDataPath;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeCompanies() {
        log.info("Starting initialize companies information to order book from file: {}", initialDataPath);
        CsvCompanyReader csvCompanyReader = new CsvCompanyReader();
        List<Company> companies = csvCompanyReader.read(initialDataPath);
        List<TickerRegistrationCommand> tickerRegistrationCommands = companies.stream()
                                                                              .map(company -> new TickerRegistrationCommand(company.ticker(), company.price()))
                                                                              .toList();
        matchingEngine.initialize(tickerRegistrationCommands);
        log.info("Companies initialization finished");
    }
}
