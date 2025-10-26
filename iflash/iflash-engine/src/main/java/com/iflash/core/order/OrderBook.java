package com.iflash.core.order;

import com.iflash.commons.Page;
import com.iflash.commons.Pagination;

import java.util.List;

public interface OrderBook {

    OrderRegistrationResult registerOrder(RegisterOrderCommand registerOrderCommand);

    void registerTicker(String ticker);

    List<String> getAllTickers();

    Page<OrderInformation> getOrderBookSnapshot(String ticker, OrderDirection orderDirection, Pagination pagination);
}
