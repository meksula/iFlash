package com.iflash.core.order;

import java.util.Collections;
import java.util.List;

public record OrderRegistrationResult(OrderRegistrationState orderRegistrationState,
                                      TransactionPhase transactionPhase,
                                      List<FinishedTransactionInfo> finishedTransactionInfoList,
                                      String errorMessage,
                                      OrderFillDetails orderFillDetails) {

    public static OrderRegistrationResult transactionPartiallyCompleted(List<FinishedTransactionInfo> finishedTransactionInfoList) {
        return new OrderRegistrationResult(OrderRegistrationState.SUCCESS, TransactionPhase.FULLY_COMPLETED, finishedTransactionInfoList, null, null);
    }

    public static OrderRegistrationResult failure(List<FinishedTransactionInfo> finishedTransactionInfoList, String errorMessage) {
        return new OrderRegistrationResult(OrderRegistrationState.SUCCESS, TransactionPhase.REJECTED, finishedTransactionInfoList, errorMessage, null);
    }

    public static OrderRegistrationResult transactionPartiallyCompleted(List<FinishedTransactionInfo> finishedTransactionInfoList, RegisterOrderCommand registerOrderCommand) {
        String message = "Could not filled complete full requested volume, partially filled transaction";
        Long volumeFilled = finishedTransactionInfoList.stream()
                                                       .map(FinishedTransactionInfo::volume)
                                                       .reduce(Long::sum)
                                                       .orElse(0L);
        Long volumePending = registerOrderCommand.volume() - volumeFilled;
        OrderFillDetails orderFillDetails = new OrderFillDetails(registerOrderCommand.volume(), volumeFilled, volumePending, message);
        return new OrderRegistrationResult(OrderRegistrationState.SUCCESS, TransactionPhase.PARTIALLY_COMPLETED, finishedTransactionInfoList, null, orderFillDetails);
    }

    public static OrderRegistrationResult limitOrderPlacedSuccessfully(RegisterOrderCommand registerOrderCommand) {
        OrderFillDetails orderFillDetails = new OrderFillDetails(registerOrderCommand.volume(), 0L, registerOrderCommand.volume(), "Limit order registered successfully");
        return new OrderRegistrationResult(OrderRegistrationState.SUCCESS, TransactionPhase.IDLING_ON_QUEUE, Collections.emptyList(), null, orderFillDetails);
    }

    public record OrderFillDetails(Long volumeRequested, Long volumeFilled, Long volumePending, String message) {
    }
}
