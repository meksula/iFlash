package com.iflash.brokerplatform.api;

import com.iflash.brokerplatform.favorite.FavoriteService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/favorites")
class FavoriteApiController {

    private final FavoriteService favoriteService;

    FavoriteApiController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/{ticker}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void add(@PathVariable String ticker, @CurrentUserId Long userId) {
        favoriteService.add(userId, ticker.toUpperCase());
    }

    @DeleteMapping("/{ticker}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable String ticker, @CurrentUserId Long userId) {
        favoriteService.remove(userId, ticker.toUpperCase());
    }
}
