package com.iflash.platform.orderbook;

import com.iflash.commons.OrderBy;
import com.iflash.commons.Page;
import com.iflash.commons.Pagination;
import com.iflash.core.engine.OrderBookOperations;
import com.iflash.core.order.OrderInformation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/orderbook")
@RequiredArgsConstructor
public class OrderBookController {

    private final OrderBookOperations orderBookOperations;

    @GetMapping("/{ticker}")
    ResponseEntity<OrderBookSnapshotResponse> getCurrentPrice(@PathVariable String ticker,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size,
                                                              @RequestParam(defaultValue = "ASC") OrderBy orderBy) {
        Pagination pagination = new Pagination(page, size, orderBy);
        Page<OrderInformation> orderBookSnapshot = orderBookOperations.getOrderBookSnapshot(ticker, pagination);
        Page<OrderBookSnapshotResponse.OrderBookEntry> orderBookEntryPage = orderBookSnapshot.map(orderInfo -> new OrderBookSnapshotResponse.OrderBookEntry(orderInfo.orderCreationDate(), orderInfo.price(), orderInfo.volume()));

        OrderBookSnapshotResponse orderBookSnapshotResponse = new OrderBookSnapshotResponse(ZonedDateTime.now(), ticker, orderBookEntryPage);

        return ResponseEntity.ok(orderBookSnapshotResponse);
    }
}
