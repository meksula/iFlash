package com.iflash.toolkit;

import lombok.Getter;

import java.util.List;

@Getter
public class Page {

    List<OrderBookSnapshotResponse.OrderBookEntry> elements;
}
