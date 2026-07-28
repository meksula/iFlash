package com.iflash.brokerplatform.favorite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserIdOrderByTicker(Long userId);

    boolean existsByUserIdAndTicker(Long userId, String ticker);

    void deleteByUserIdAndTicker(Long userId, String ticker);
}
