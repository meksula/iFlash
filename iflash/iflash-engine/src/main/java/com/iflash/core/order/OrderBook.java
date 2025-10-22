package com.iflash.core.order;

import java.util.List;

public interface OrderBook {

    OrderRegistrationResult registerOrder(RegisterOrderCommand registerOrderCommand);

    void registerTicker(String ticker);

    List<String> getAllTickers();
}
