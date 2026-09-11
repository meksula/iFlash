package com.iflash.instrument.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialInstrumentRepository extends JpaRepository<FinancialInstrumentEntity, Long> {
}
