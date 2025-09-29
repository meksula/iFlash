package utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class BigDecimalUtils {

    private static final Random RANDOM = new Random();

    public BigDecimal priceWithRandomDecimalPlaces(BigDecimal price) {
        return price.add(BigDecimal.valueOf(RANDOM.nextDouble(0.9999))).setScale(4, RoundingMode.HALF_UP);
    }
}
