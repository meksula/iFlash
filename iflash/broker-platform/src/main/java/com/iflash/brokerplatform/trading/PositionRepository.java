package com.iflash.brokerplatform.trading;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByUserIdAndTicker(Long userId, String ticker);

    List<Position> findByUserIdAndQuantityGreaterThanOrderByTicker(Long userId, long quantity);
}
