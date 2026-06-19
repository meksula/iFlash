package com.iflash.brokerplatform.market;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/** Read/write client for the iFlash matching-engine platform (port 10023). */
@Component
public class IflashApiClient {

    private final RestClient client;

    public IflashApiClient(RestClient iflashRestClient) {
        this.client = iflashRestClient;
    }

    public List<InstrumentDto> listInstruments() {
        return client.get()
                     .uri("/api/v1/instrument")
                     .retrieve()
                     .body(new ParameterizedTypeReference<>() {
                     });
    }

    public PriceDto getPrice(String ticker) {
        return client.get()
                     .uri("/api/v1/quotation/{ticker}/price", ticker)
                     .retrieve()
                     .body(PriceDto.class);
    }

    public QuotesDto getQuotes(String ticker, int size, String orderBy) {
        return client.get()
                     .uri(builder -> builder.path("/api/v1/quotation/{ticker}/quotes")
                                            .queryParam("page", 0)
                                            .queryParam("size", size)
                                            .queryParam("orderBy", orderBy)
                                            .build(ticker))
                     .retrieve()
                     .body(QuotesDto.class);
    }

    public OrderBookDto getOrderBook(String ticker, String orderDirection, int size) {
        return client.get()
                     .uri(builder -> builder.path("/api/v1/orderbook/{ticker}")
                                            .queryParam("orderDirection", orderDirection)
                                            .queryParam("page", 0)
                                            .queryParam("size", size)
                                            .queryParam("orderBy", "ASC")
                                            .build(ticker))
                     .retrieve()
                     .body(OrderBookDto.class);
    }

    public OrderResultDto registerOrder(OrderRequestDto request) {
        return client.post()
                     .uri("/api/v1/trade/order")
                     .contentType(MediaType.APPLICATION_JSON)
                     .body(request)
                     .retrieve()
                     .onStatus(HttpStatusCode::isError, (req, response) -> {
                         throw EngineException.from(response);
                     })
                     .body(OrderResultDto.class);
    }
}
