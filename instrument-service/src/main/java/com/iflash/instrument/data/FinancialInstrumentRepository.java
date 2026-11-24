package com.iflash.instrument.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface FinancialInstrumentRepository extends JpaRepository<FinancialInstrumentEntity, Long> {

    @Query("select new com.iflash.instrument.data.FinancialInstrumentInitial(f.ticker, f.initialPrice) from FinancialInstrumentEntity f order by f.ticker")
    List<FinancialInstrumentInitial> findAllInitialOrderedByTicker();

    @Query("select f.ticker from FinancialInstrumentEntity f")
    Set<String> findAllTickers();
}
