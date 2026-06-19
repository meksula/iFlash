package com.iflash.brokerplatform.market;

import java.util.List;

/** Mirrors the engine's {@code Page<T>} JSON shape ({@code {elements, pagination}}). */
public record PageDto<T>(List<T> elements) {
}
