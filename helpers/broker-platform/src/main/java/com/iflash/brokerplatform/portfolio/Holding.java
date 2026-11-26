package com.iflash.brokerplatform.portfolio;

import com.iflash.brokerplatform.market.PriceChange;
import com.iflash.brokerplatform.trading.PositionLot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/** A position valued at the current market price. */
public record Holding(String ticker, long quantity, BigDecimal purchasePrice, BigDecimal currentPrice,
                      BigDecimal marketValue, BigDecimal changePercent, List<Lot> lots) {

    static Holding of(String ticker, long quantity, BigDecimal currentPrice, List<PositionLot> positionLots) {
        BigDecimal purchasePrice = averagePurchasePrice(positionLots);
        List<Lot> lots = positionLots.stream()
                                     .map(lot -> Lot.of(lot, currentPrice))
                                     .toList();
        return new Holding(ticker, quantity, purchasePrice, currentPrice,
                currentPrice.multiply(BigDecimal.valueOf(quantity)), PriceChange.percent(purchasePrice, currentPrice),
                lots);
    }

    private static BigDecimal averagePurchasePrice(List<PositionLot> lots) {
        long quantity = lots.stream()
                            .mapToLong(PositionLot::getQuantity)
                            .sum();
        if (quantity == 0) {
            return null;
        }
        return lots.stream()
                   .map(PositionLot::cost)
                   .reduce(BigDecimal.ZERO, BigDecimal::add)
                   .divide(BigDecimal.valueOf(quantity), 4, RoundingMode.HALF_UP);
    }

    public record Lot(Long id, long quantity, BigDecimal purchasePrice, Instant purchasedAt,
                      BigDecimal changePercent) {

        static Lot of(PositionLot lot, BigDecimal currentPrice) {
            return new Lot(lot.getId(), lot.getQuantity(), lot.getPurchasePrice(), lot.getPurchasedAt(),
                    PriceChange.percent(lot.getPurchasePrice(), currentPrice));
        }
    }
}
