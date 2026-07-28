package com.iflash.toolkit;

import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Data
@Getter
public class TransactionResponse {
    private String ticker;
    private Long volume;
    private BigDecimal price;
    private List<TransactionInfoResponse> transactions;

    public record TransactionInfoResponse(long volume, BigDecimal price) {
    }
}
