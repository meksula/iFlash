package com.iflash.core.engine;

import com.iflash.core.order.OrderRegistrationResult;
import com.iflash.core.order.RegisterOrderCommand;

public interface TradingOperations {

    OrderRegistrationResult registerOrder(RegisterOrderCommand registerOrderCommand);
}
