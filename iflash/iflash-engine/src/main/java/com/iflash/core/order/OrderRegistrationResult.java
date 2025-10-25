package com.iflash.core.order;

import java.util.List;

public record OrderRegistrationResult(OrderRegistrationState orderRegistrationState, List<FinishedTransactionInfo> finishedTransactionInfoList, String errorMessage,
                                      PartialFillDetails partialFillDetails) {

    public static OrderRegistrationResult success(List<FinishedTransactionInfo> finishedTransactionInfoList) {
        return new OrderRegistrationResult(OrderRegistrationState.SUCCESS, finishedTransactionInfoList, null, null);
    }

    public static OrderRegistrationResult failure(String errorMessage) {
        return new OrderRegistrationResult(OrderRegistrationState.SUCCESS, null, errorMessage, null);
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
        PartialFillDetails partialFillDetails = new PartialFillDetails(registerOrderCommand.volume(), volumeFilled, message);
        return new OrderRegistrationResult(OrderRegistrationState.SUCCESS, finishedTransactionInfoList, null, partialFillDetails);
    }

    record PartialFillDetails(Long volumeRequested, Long volumeFilled, String message) {
    }
}
