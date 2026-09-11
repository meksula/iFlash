package com.iflash.instrument.data;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "financial_instrument")
class FinancialInstrumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false, unique = true, updatable = false)
    private String ticker;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal initialPrice;

    @Version
    private Long version;

    public static FinancialInstrumentEntity createNew(String ticker, BigDecimal initialPrice) {
        return FinancialInstrumentEntity.builder()
                .ticker(ticker)
                .initialPrice(initialPrice)
                .build();
    }
}
