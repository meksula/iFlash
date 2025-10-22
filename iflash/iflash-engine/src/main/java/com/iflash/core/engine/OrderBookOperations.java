package com.iflash.core.engine;

import com.iflash.commons.Page;
import com.iflash.commons.Pagination;
import com.iflash.core.order.OrderInformation;

import java.util.List;

public interface OrderBookOperations {

    List<FinancialInstrumentInfo> getFinancialInstrumentInfo();

    Page<OrderInformation> getOrderBookSnapshot(String ticker, Pagination pagination);
}
