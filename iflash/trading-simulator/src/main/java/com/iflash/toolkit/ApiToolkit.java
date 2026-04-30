package com.iflash.toolkit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflash.core.engine.FinancialInstrumentInfo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ApiToolkit {

    private final String baseUrl = "http://localhost:10023";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiToolkit() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<FinancialInstrumentInfo> getInstruments() {
        String data = get("/api/v1/instrument");
        try {
            return objectMapper.readValue(data, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public OrderBookSnapshotResponse getOrderBook(String ticker, int page, int size, String orderBy, String orderDirection) {
        String uri = String.format("/api/v1/orderbook/%s?page=%d&size=%d&orderBy=%s&orderDirection=%s", ticker, page, size, orderBy, orderDirection);
        String data = get(uri);
        try {
            return objectMapper.readValue(data, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String getCurrentQuotation(String ticker) {
        return get("/api/v1/quotation/" + ticker + "/price");
    }

    public String getQuotationHistory(String ticker, int amount, String order) {
        String uri = String.format("/api/v1/quotation/%s/%d/%s", ticker, amount, order);
        return get(uri);
    }

    public String placeOrder(String orderDirection, String orderType, String ticker, int volume) {
        String body = String.format("""
                {
                  "orderDirection": "%s",
                  "orderType": "%s",
                  "ticker": "%s",
                  "volume": %d
                }""", orderDirection, orderType, ticker, volume);
        return post("/api/v1/trade/order", body);
    }

    public String placeOrder(String orderDirection, String orderType, String ticker, int volume, double price) {
        String body = String.format("""
                {
                  "orderDirection": "%s",
                  "orderType": "%s",
                  "ticker": "%s",
                  "volume": %d,
                  "price": %.2f
                }""", orderDirection, orderType, ticker, volume, price);
        return post("/api/v1/trade/order", body);
    }

    private String get(String path) {
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(baseUrl + path))
                                         .GET()
                                         .build();
        return send(request);
    }

    private String post(String path, String body) {
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(baseUrl + path))
                                         .header("Content-Type", "application/json")
                                         .POST(HttpRequest.BodyPublishers.ofString(body))
                                         .build();
        return send(request);
    }

    private String send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
        }
    }
}