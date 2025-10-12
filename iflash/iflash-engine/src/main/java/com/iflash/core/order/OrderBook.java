package com.iflash.core.order;

public interface OrderBook {

    OrderRegistrationResult registerOrder(RegisterOrderCommand registerOrderCommand);

    void registerTicker(String ticker);
}
