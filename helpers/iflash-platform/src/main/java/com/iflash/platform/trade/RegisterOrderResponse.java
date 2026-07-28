package com.iflash.platform.trade;

import com.iflash.core.order.OrderDirection;
import com.iflash.core.order.OrderRegistrationResult;
import com.iflash.core.order.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
class RegisterOrderResponse {
    private OrderDirection orderDirection;
    private OrderType orderType;
    private String ticker;
    private BigDecimal price;
    private Long volume;
    private List<TransactionInfoResponse> transactions;

    record TransactionInfoResponse(long volume, BigDecimal price) {
    }

    public static RegisterOrderResponse response(OrderRegistrationResult orderRegistrationResult, RegisterOrderRequest registerOrderRequest) {
        return new RegisterOrderResponse(registerOrderRequest.getOrderDirection(), registerOrderRequest.getOrderType(), registerOrderRequest.getTicker(),
                                         registerOrderRequest.getPrice(), registerOrderRequest.getVolume(),
                                         orderRegistrationResult.finishedTransactionInfoList()
                                                                .stream()
                                                                .map(transactionInfo -> new TransactionInfoResponse(transactionInfo.volume(), transactionInfo.price()))
                                                                .collect(Collectors.toList()));
    }
}
