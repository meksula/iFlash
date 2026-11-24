package com.iflash.tradingservice.order;

import java.math.BigDecimal;
import java.util.List;

record RegisterOrderResponse(OrderDirection orderDirection,
                             OrderType orderType,
                             String ticker,
                             BigDecimal price,
                             Long volume,
                             List<TransactionInfoResponse> transactions) {

    record TransactionInfoResponse(long volume, BigDecimal price) {
    }

    static RegisterOrderResponse response(OrderRegistrationResult orderRegistrationResult, RegisterOrderRequest registerOrderRequest) {
        return new RegisterOrderResponse(registerOrderRequest.orderDirection(), registerOrderRequest.orderType(), registerOrderRequest.ticker(),
                                         registerOrderRequest.price(), registerOrderRequest.volume(),
                                         orderRegistrationResult.finishedTransactionInfoList()
                                                                .stream()
                                                                .map(transactionInfo -> new TransactionInfoResponse(transactionInfo.volume(), transactionInfo.price()))
                                                                .toList());
    }
}
