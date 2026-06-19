package com.iflash.brokerplatform.trading;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** How many units of a ticker a user currently holds. */
@Entity
@Table(name = "position", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "ticker"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String ticker;

    @Setter
    @Column(nullable = false)
    private long quantity;

    public Position(Long userId, String ticker, long quantity) {
        this.userId = userId;
        this.ticker = ticker;
        this.quantity = quantity;
    }
}
