package com.iflash.commons;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.function.Function;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Page<T> {

    private final List<T> elements;
    private final Pagination pagination;

    public static <T> Page<T> of(List<T> elements, Pagination pagination) {
        return new Page<>(elements, pagination);
    }

    public <R> Page<R> map(Function<T, R> mappingFunction) {
        List<R> elementsMapped = elements.stream()
                                         .map(mappingFunction)
                                         .toList();
        return Page.of(elementsMapped, pagination);
    }
}
