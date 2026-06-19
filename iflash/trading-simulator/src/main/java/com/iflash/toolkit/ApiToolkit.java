package com.iflash.toolkit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ApiToolkit {

    private final String baseUrl = "http://localhost:10023";
    private final HttpClient httpClient = HttpClient.newHttpClient();


    public ApiToolkit() {

    }

    public ApiResponse<List<FinancialInstrumentInfo>> getInstruments() {
        return get("/api/v1/instrument", new TypeReference<>() {});
    }

    public ApiResponse<OrderBookSnapshotResponse> getOrderBook(String ticker, int page, int size, String orderBy, String orderDirection) {
        String uri = String.format("/api/v1/orderbook/%s?page=%d&size=%d&orderBy=%s&orderDirection=%s", ticker, page, size, orderBy, orderDirection);
        return get(uri, new TypeReference<>() {});
    }

    public ApiResponse<FinancialInstrumentInfo> getCurrentQuotation(String ticker) {
        return get("/api/v1/quotation/" + ticker + "/price", new TypeReference<>() {});
    }

    public ApiResponse<QuotationHistoryResponse> getQuotationHistory(String ticker, int amount, String order) {
        String uri = String.format("/api/v1/quotation/%s/%d/%s", ticker, amount, order);
        return get(uri, new TypeReference<>() {});
    }

    public ApiResponse<TransactionResponse> placeMarketOrder(String orderDirection, String ticker, long volume) {
        String body = String.format("""
                {
                  "orderDirection": "%s",
                  "orderType": "MARKET",
                  "ticker": "%s",
                  "volume": %d
                }""", orderDirection, ticker, volume);
        return post("/api/v1/trade/order", body, new TypeReference<>() {});
    }

    public ApiResponse<TransactionResponse> placeLimitOrder(String orderDirection, String ticker, int volume, double price) {
        String body = String.format("""
                {
                  "orderDirection": "%s",
                  "orderType": "LIMIT",
                  "ticker": "%s",
                  "volume": %d,
                  "price": %.2f
                }""", orderDirection, ticker, volume, price);
        return post("/api/v1/trade/order", body, new TypeReference<>() {});
    }

    private <T> ApiResponse<T> get(String path, TypeReference<T> bodyType) {
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(baseUrl + path))
                                         .GET()
                                         .build();
        return send(request, bodyType);
    }

    private <T> ApiResponse<T> post(String path, String body, TypeReference<T> bodyType) {
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(URI.create(baseUrl + path))
                                         .header("Content-Type", "application/json")
                                         .POST(HttpRequest.BodyPublishers.ofString(body))
                                         .build();
        return send(request, bodyType);
    }

    private <T> ApiResponse<T> send(HttpRequest request, TypeReference<T> bodyType) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.out.println("HTTP request failed with status code: " + response.statusCode() + " and body: " + response.body());
                return ApiResponse.empty(response.statusCode());
            }
            return ApiResponse.readResponse(response.statusCode(), response.body(),bodyType);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class ApiResponse<T> {
        private static final ObjectMapper objectMapper = new ObjectMapper();
        private final int httpStatus;
        private final T responseBody;

        static {
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        public static <T> ApiResponse<T> readResponse(int httpStatus, String responseBodyAsString, TypeReference<T> responseType) {
            try {
                T responseBody = objectMapper.readValue(responseBodyAsString, responseType);
                return new ApiResponse<>(httpStatus, responseBody);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        public static <T> ApiResponse<T> empty(int httpStatus) {
            return new ApiResponse<>(httpStatus, null);
        }
    }
}