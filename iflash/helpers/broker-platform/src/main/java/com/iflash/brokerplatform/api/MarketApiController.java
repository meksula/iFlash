package com.iflash.brokerplatform.api;

import com.iflash.brokerplatform.market.ChartPoint;
import com.iflash.brokerplatform.market.InstrumentDto;
import com.iflash.brokerplatform.market.MarketQueryService;
import com.iflash.brokerplatform.market.OrderBookView;
import com.iflash.brokerplatform.market.PriceDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Live market data for the Angular client (JWT-guarded mirror of {@code /app/*}). */
@RestController
@RequestMapping("/api/market")
class MarketApiController {

    private final MarketQueryService market;

    MarketApiController(MarketQueryService market) {
        this.market = market;
    }

    @GetMapping("/instruments")
    List<InstrumentDto> instruments() {
        return market.instruments();
    }

    @GetMapping("/price/{ticker}")
    PriceDto price(@PathVariable String ticker) {
        return market.price(ticker);
    }

    @GetMapping("/quotes/{ticker}")
    List<ChartPoint> quotes(@PathVariable String ticker) {
        return market.quotes(ticker);
    }

    @GetMapping("/orderbook/{ticker}")
    OrderBookView orderBook(@PathVariable String ticker) {
        return market.orderBook(ticker);
    }
}
