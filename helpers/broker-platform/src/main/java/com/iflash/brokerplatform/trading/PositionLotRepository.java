package com.iflash.brokerplatform.trading;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionLotRepository extends JpaRepository<PositionLot, Long> {

    List<PositionLot> findByUserIdAndTickerOrderByPurchasedAtAscIdAsc(Long userId, String ticker);

    List<PositionLot> findByUserIdOrderByPurchasedAtAscIdAsc(Long userId);

    Optional<PositionLot> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndTicker(Long userId, String ticker);
}
