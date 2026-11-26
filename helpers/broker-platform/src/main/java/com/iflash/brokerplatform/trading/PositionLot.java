package com.iflash.brokerplatform.trading;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "position_lot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PositionLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "purchase_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal purchasePrice;

    @Column(name = "purchased_at", nullable = false)
    private Instant purchasedAt;

    PositionLot(Long userId, String ticker, long quantity, BigDecimal cashAmount, Instant purchasedAt) {
        this.userId = userId;
        this.ticker = ticker;
        this.quantity = quantity;
        this.purchasePrice = cashAmount.divide(BigDecimal.valueOf(quantity), 4, RoundingMode.HALF_UP);
        this.purchasedAt = purchasedAt;
    }

    static PositionLot of(Trade purchase) {
        return new PositionLot(purchase.getUserId(), purchase.getTicker(), purchase.getFilledQuantity(),
                purchase.getCashAmount(), purchase.getCreatedAt());
    }

    static List<PositionLot> releaseInOrder(List<PositionLot> lots, long quantity) {
        long remaining = quantity;
        List<PositionLot> closed = new ArrayList<>();
        for (PositionLot lot : lots) {
            if (remaining == 0) {
                break;
            }
            remaining -= lot.release(remaining);
            if (lot.quantity == 0) {
                closed.add(lot);
            }
        }
        return closed;
    }

    private long release(long requested) {
        long released = Math.min(quantity, requested);
        quantity -= released;
        return released;
    }

    public BigDecimal cost() {
        return purchasePrice.multiply(BigDecimal.valueOf(quantity));
    }
}
