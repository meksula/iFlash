package com.iflash.core.order;

import java.util.Collections;
import java.util.List;

public record OrderRegistrationResult(OrderRegistrationState orderRegistrationState, List<FinishedTransactionInfo> finishedTransactionInfoList, String errorMessage,
                                      OrderFillDetails orderFillDetails) {

    public static OrderRegistrationResult success(List<FinishedTransactionInfo> finishedTransactionInfoList) {
        return new OrderRegistrationResult(OrderRegistrationState.SUCCESS, finishedTransactionInfoList, null, null);
    }

    public static OrderRegistrationResult failure(List<FinishedTransactionInfo> finishedTransactionInfoList, String errorMessage) {
        return new OrderRegistrationResult(OrderRegistrationState.SUCCESS, finishedTransactionInfoList, errorMessage, null);
    }

    public static OrderRegistrationResult partiallySuccess(List<FinishedTransactionInfo> finishedTransactionInfoList, RegisterOrderCommand registerOrderCommand) {
        String message = "Could not filled complete full requested volume, partially filled transaction";
        Long volumeFilled = finishedTransactionInfoList.stream()
                                                       .map(FinishedTransactionInfo::volume)
                                                       .reduce(Long::sum)
                                                       .orElse(0L);
        Long volumePending = registerOrderCommand.volume() - volumeFilled;
        OrderFillDetails orderFillDetails = new OrderFillDetails(registerOrderCommand.volume(), volumeFilled, volumePending, message);
        return new OrderRegistrationResult(OrderRegistrationState.PENDING, finishedTransactionInfoList, null, orderFillDetails);
    }

    public static OrderRegistrationResult limitOrderSuccess(RegisterOrderCommand registerOrderCommand) {
        OrderFillDetails orderFillDetails = new OrderFillDetails(registerOrderCommand.volume(), 0L, registerOrderCommand.volume(), "Limit order registered successfully");
        return new OrderRegistrationResult(OrderRegistrationState.PENDING, Collections.emptyList(), null, orderFillDetails);
    }

    public boolean isPartiallyFinished() {
        return OrderRegistrationState.PENDING == this.orderRegistrationState;
    }

    public record OrderFillDetails(Long volumeRequested, Long volumeFilled, Long volumePending, String message) {
    }
}
