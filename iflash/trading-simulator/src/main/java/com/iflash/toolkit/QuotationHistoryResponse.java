package com.iflash.toolkit;

import java.util.List;

public record QuotationHistoryResponse(String ticker, List<FinancialInstrumentInfo> quotations) {
}
