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
        Long volumeFilled = sumFilledVolume(finishedTransactionInfoList);
        Long volumePending = registerOrderCommand.volume() - volumeFilled;
        OrderFillDetails orderFillDetails = new OrderFillDetails(registerOrderCommand.volume(), volumeFilled, volumePending, message);
        return new OrderRegistrationResult(OrderRegistrationState.SUCCESS, TransactionPhase.PARTIALLY_COMPLETED, finishedTransactionInfoList, null, orderFillDetails);
    }

    public static OrderRegistrationResult limitOrderPlacedSuccessfully(RegisterOrderCommand registerOrderCommand) {
        OrderFillDetails orderFillDetails = new OrderFillDetails(registerOrderCommand.volume(), 0L, registerOrderCommand.volume(), "Limit order registered successfully");
        return new OrderRegistrationResult(OrderRegistrationState.SUCCESS, TransactionPhase.IDLING_ON_QUEUE, Collections.emptyList(), null, orderFillDetails);
    }

    public static OrderRegistrationResult limitOrderFullyCompleted(List<FinishedTransactionInfo> finishedTransactionInfo, RegisterOrderCommand registerOrderCommand) {
        Long volumeFilled = sumFilledVolume(finishedTransactionInfo);
        OrderFillDetails orderFillDetails = new OrderFillDetails(registerOrderCommand.volume(), volumeFilled, 0L, "Limit order completed successfully");
        return new OrderRegistrationResult(OrderRegistrationState.SUCCESS, TransactionPhase.FULLY_COMPLETED, finishedTransactionInfo, null, orderFillDetails);
    }

    public static OrderRegistrationResult limitOrderPartiallyCompleted(List<FinishedTransactionInfo> finishedTransactionInfo, RegisterOrderCommand registerOrderCommand) {
        String message = "Could not filled complete full requested volume, partially filled transaction and another part of requested volume placed in queue";
        Long volumeFilled = sumFilledVolume(finishedTransactionInfo);
        Long volumePending = registerOrderCommand.volume() - volumeFilled;
        OrderFillDetails orderFillDetails = new OrderFillDetails(registerOrderCommand.volume(), volumeFilled, volumePending, message);
        return new OrderRegistrationResult(OrderRegistrationState.SUCCESS, TransactionPhase.PARTIALLY_COMPLETED, finishedTransactionInfo, null, orderFillDetails);
    }

    public record OrderFillDetails(Long volumeRequested, Long volumeFilled, Long volumePending, String message) {
    }

    private static Long sumFilledVolume(List<FinishedTransactionInfo> finishedTransactionInfoList) {
        return finishedTransactionInfoList.stream()
                                          .map(FinishedTransactionInfo::volume)
                                          .reduce(Long::sum)
                                          .orElse(0L);
    }
}
