package com.iflash.brokerplatform.favorite;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    public FavoriteService(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    @Transactional
    public void add(Long userId, String ticker) {
        if (!favoriteRepository.existsByUserIdAndTicker(userId, ticker)) {
            favoriteRepository.save(new Favorite(userId, ticker));
        }
    }

    @Transactional
    public void remove(Long userId, String ticker) {
        favoriteRepository.deleteByUserIdAndTicker(userId, ticker);
    }

    @Transactional(readOnly = true)
    public List<String> tickers(Long userId) {
        return favoriteRepository.findByUserIdOrderByTicker(userId)
                                 .stream()
                                 .map(Favorite::getTicker)
                                 .toList();
    }
}
