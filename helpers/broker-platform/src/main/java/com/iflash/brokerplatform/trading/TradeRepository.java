package com.iflash.brokerplatform.trading;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);
}
