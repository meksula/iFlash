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
import java.time.Instant;

/** A settled order: one row per submission, summarizing the engine's fills. */
@Entity
@Table(name = "trade")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private String direction;

    @Column(name = "order_type", nullable = false)
    private String orderType;

    @Column(precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "filled_quantity", nullable = false)
    private long filledQuantity;

    @Column(name = "cash_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal cashAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Trade(Long userId, String ticker, String direction, String orderType, BigDecimal price,
                 long filledQuantity, BigDecimal cashAmount) {
        this.userId = userId;
        this.ticker = ticker;
        this.direction = direction;
        this.orderType = orderType;
        this.price = price;
        this.filledQuantity = filledQuantity;
        this.cashAmount = cashAmount;
        this.createdAt = Instant.now();
    }
}
