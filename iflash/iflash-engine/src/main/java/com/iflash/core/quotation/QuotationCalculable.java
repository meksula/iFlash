package com.iflash.core.quotation;

import java.util.List;

public interface QuotationCalculable {

    Quotation calculate(String ticker, List<BoughtTransactionInfo> boughtTransactionInfos);
}
