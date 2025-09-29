package com.iflash.commons;

public class ValidateUtils {

    public static <T> T requireNonNullOrThrow(T object, RuntimeException exception) {
        if (object == null) {
            throw exception;
        }
        return object;
    }

    public static <T extends Number> T mustBePositive(T number, RuntimeException exception) {
        if (number.doubleValue() <= 0) {
            throw exception;
        }
        return number;
    }
}
