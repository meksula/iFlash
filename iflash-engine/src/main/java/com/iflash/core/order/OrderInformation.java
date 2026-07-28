package com.iflash.core.order;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public record OrderInformation(ZonedDateTime orderCreationDate, BigDecimal price, Long volume) {
}
