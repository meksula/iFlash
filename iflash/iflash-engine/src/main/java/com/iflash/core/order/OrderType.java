package com.iflash.core.order;

enum OrderType {
    MARKET,        // market order - executed immediately at the best available price
    LIMIT,         // limit order - executed only at the specified price or better
    STOP,          // stop order - triggered when the stop price is reached
    STOP_LIMIT,    // stop-limit order - becomes a limit order once the stop price is reached
    ICEBERG,       // iceberg order - only part of the volume is visible, rest is hidden
    FOK,           // Fill or Kill - must be executed immediately in full or cancelled
    IOC,           // Immediate or Cancel - execute as much as possible immediately, cancel the rest
    GTC,           // Good Till Cancelled - remains active until explicitly cancelled
    GTD,           // Good Till Date - remains active until a specified date
    AON            // All or None - executed only if the entire order can be filled
}