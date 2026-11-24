package com.iflash.ratesservice.quotation;

import java.util.List;
import java.util.function.Function;

record Page<T>(List<T> elements, Pagination pagination) {

    static <T> Page<T> empty(Pagination pagination) {
        return new Page<>(List.of(), pagination);
    }

    <R> Page<R> map(Function<T, R> mappingFunction) {
        return new Page<>(elements.stream()
                                  .map(mappingFunction)
                                  .toList(), pagination);
    }
}
