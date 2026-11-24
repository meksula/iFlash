package com.iflash.trader;

import com.iflash.toolkit.ApiToolkit;
import com.iflash.toolkit.TransactionResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class TradingAccount {

    private static final Logger log = LogManager.getLogger(TradingAccount.class);

    private final String owner;
    private final ApiToolkit apiToolkit;
    private final Map<String, Long> holdings = new HashMap<>();
    private BigDecimal cash;

    TradingAccount(String owner, ApiToolkit apiToolkit, BigDecimal initialCash) {
        this.owner = owner;
        this.apiToolkit = apiToolkit;
        this.cash = initialCash;
    }

    boolean holds(String ticker) {
        return holdings.containsKey(ticker);
    }

    Set<String> heldTickers() {
        return Set.copyOf(holdings.keySet());
    }

    void buy(String ticker, long volume, BigDecimal estimatedPrice) {
        BigDecimal estimatedCost = estimatedPrice.multiply(BigDecimal.valueOf(volume));
        if (cash.compareTo(estimatedCost) < 0) {
            log.info("[{}] not enough cash ({}) to buy {} of {}", owner, cash, volume, ticker);
            return;
        }
        TransactionResponse transaction = placeMarketOrder("BID", ticker, volume);
        long filled = filledVolume(transaction);
        if (filled <= 0) {
            return;
        }
        cash = cash.subtract(filledValue(transaction));
        holdings.merge(ticker, filled, Long::sum);
        log.info("[{}] BOUGHT {} {} — holdings now {}, cash {}", owner, filled, ticker, holdings.get(ticker), cash);
    }

    void sellAll(String ticker) {
        long held = holdings.getOrDefault(ticker, 0L);
        if (held <= 0) {
            return;
        }
        TransactionResponse transaction = placeMarketOrder("ASK", ticker, held);
        long filled = filledVolume(transaction);
        if (filled <= 0) {
            return;
        }
        cash = cash.add(filledValue(transaction));
        long remaining = held - filled;
        if (remaining > 0) {
            holdings.put(ticker, remaining);
        }
        else {
            holdings.remove(ticker);
        }
        log.info("[{}] SOLD {} {} — holdings now {}, cash {}", owner, filled, ticker, remaining, cash);
    }

    private TransactionResponse placeMarketOrder(String orderDirection, String ticker, long volume) {
        ApiToolkit.ApiResponse<TransactionResponse> response = apiToolkit.placeMarketOrder(orderDirection, ticker, volume);
        if (response.getHttpStatus() != 200 || response.getResponseBody() == null) {
            log.warn("[{}] {} order failed for {}. HTTP status: {}", owner, orderDirection, ticker, response.getHttpStatus());
            return null;
        }
        return response.getResponseBody();
    }

    private long filledVolume(TransactionResponse transaction) {
        if (transaction == null || transaction.getTransactions() == null) {
            return 0L;
        }
        return transaction.getTransactions()
                          .stream()
                          .mapToLong(TransactionResponse.TransactionInfoResponse::volume)
                          .sum();
    }

    private BigDecimal filledValue(TransactionResponse transaction) {
        List<TransactionResponse.TransactionInfoResponse> transactions = transaction.getTransactions();
        return transactions.stream()
                           .map(transactionInfo -> transactionInfo.price().multiply(BigDecimal.valueOf(transactionInfo.volume())))
                           .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
