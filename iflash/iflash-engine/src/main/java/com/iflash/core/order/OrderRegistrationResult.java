package com.iflash.core.order;

import java.util.List;

public record OrderRegistrationResult(List<FinishedTransactionInfo> finishedTransactionInfoList) {
}
