package com.iflash.platform.trade;

import com.iflash.core.order.OrderDirection;
import com.iflash.core.order.OrderType;
import com.iflash.core.order.RegisterOrderCommand;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
class RegisterOrderRequest {

    private OrderDirection orderDirection;
    private OrderType orderType;
    private String ticker;
    private BigDecimal price;
    private Long volume;

    RegisterOrderCommand command() {
        return new RegisterOrderCommand(orderDirection, orderType, ticker, price, volume);
    }
}
