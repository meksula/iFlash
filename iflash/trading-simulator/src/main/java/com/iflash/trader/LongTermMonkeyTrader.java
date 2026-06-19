package com.iflash.trader;

import com.iflash.toolkit.ApiToolkit;
import com.iflash.toolkit.FinancialInstrumentInfo;
import com.iflash.toolkit.TransactionResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LongTermMonkeyTrader implements Trader {

    private static final Random random = new Random();

    private final String brokerName;
    private final String firstName;
    private final String lastName;
    private final ApiToolkit apiToolkit;

    private final Map<String, List<Position>> portfolio;
    private BigDecimal availableCash;

    public LongTermMonkeyTrader(String brokerName,
                                String firstName,
                                String lastName,
                                ApiToolkit apiToolkit,
                                BigDecimal initialCash,
                                Currency currency,
                                Map<String, List<Position>> portfolio) {
        this.brokerName = brokerName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.apiToolkit = apiToolkit;
        this.portfolio = portfolio;
        this.availableCash = initialCash;
    }

    @Override
    public void decide() {
        // buying part
        System.out.println("---------------------------------------------------\n" +
                          stringifyPortfolio() + "\n" +
                           "Trader [broker: " + brokerName + "] " + firstName + " " + lastName + " is making a decision...");

        ApiToolkit.ApiResponse<List<FinancialInstrumentInfo>> instrumentsResponse = apiToolkit.getInstruments();
        if (instrumentsResponse.getHttpStatus() != 200) {
            System.out.println("Could not fetch instruments. HTTP status: " + instrumentsResponse.getHttpStatus());
            return;
        }

        List<FinancialInstrumentInfo> instruments = instrumentsResponse.getResponseBody();
        if (instruments == null || instruments.isEmpty()) {
            System.out.println("No available instruments for trading decision.");
            return;
        }

        int selectedInstrumentIndex = random.nextInt(instruments.size());
        FinancialInstrumentInfo selectedInstrument = instruments.get(selectedInstrumentIndex);
        System.out.println("Selected instrument: " + selectedInstrument);

        int volumeToBuy = random.nextInt(10) + 1;
        BigDecimal estimatedBuyCost = selectedInstrument.currentPrice()
                                                        .multiply(BigDecimal.valueOf(volumeToBuy));

        if (availableCash.compareTo(estimatedBuyCost) < 0) {
            System.out.println("Not enough cash to buy " + volumeToBuy + " of " + selectedInstrument.ticker());
            return;
        }

        ApiToolkit.ApiResponse<TransactionResponse> buyOrderResponse = apiToolkit.placeMarketOrder("BID", selectedInstrument.ticker(), volumeToBuy);
        if (buyOrderResponse.getHttpStatus() != 200) {
            System.out.println("Failed to place buy order for " + selectedInstrument.ticker() + " http status: " + buyOrderResponse.getHttpStatus());
            return;
        } else {
            System.out.println("Buy order placed successfully for " + selectedInstrument.ticker());
            if (buyOrderResponse.getResponseBody().getTransactions().isEmpty()) {
                System.out.println("Market is sold out and has no executed transactions for " + selectedInstrument.ticker());
                return;
            }
        }

        TransactionResponse buyTransaction = buyOrderResponse.getResponseBody();
        if (buyTransaction.getVolume() == null || buyTransaction.getVolume() <= 0) {
            System.out.println("Buy order response has no executed volume for " + selectedInstrument.ticker());
            return;
        }

        long boughtVolume = buyTransaction.getVolume();
        BigDecimal totalBuyCost = calculateTotalTransactionValue(buyTransaction.getTransactions());
        if (totalBuyCost.compareTo(BigDecimal.ZERO) <= 0) {
            totalBuyCost = estimatedBuyCost;
        }

        BigDecimal averageBoughtPrice = totalBuyCost.divide(BigDecimal.valueOf(boughtVolume), RoundingMode.HALF_UP);
        availableCash = availableCash.subtract(totalBuyCost);

        Position boughtPosition = new Position(ZonedDateTime.now(), boughtVolume, averageBoughtPrice);
        portfolio.computeIfAbsent(buyTransaction.getTicker(), ignored -> new ArrayList<>())
                 .add(boughtPosition);

        // selling part
        if (portfolio.isEmpty()) {
            return;
        }

        System.out.println("Hmmm maybe should I sell something? Let's check the portfolio");

        for (String tickerToEvaluate : new ArrayList<>(portfolio.keySet())) {
            List<Position> openPositions = portfolio.get(tickerToEvaluate);
            if (openPositions == null || openPositions.isEmpty()) {
                continue;
            }

            ApiToolkit.ApiResponse<FinancialInstrumentInfo> quotationResponse = apiToolkit.getCurrentQuotation(tickerToEvaluate);
            if (quotationResponse.getHttpStatus() != 200 || quotationResponse.getResponseBody() == null) {
                System.out.println("Could not retrieve current market price for " + tickerToEvaluate + ". Skipping evaluation.");
                continue;
            }

            BigDecimal currentMarketPrice = quotationResponse.getResponseBody()
                                                             .currentPrice();
            if (currentMarketPrice == null) {
                System.out.println("Current market price is null for " + tickerToEvaluate + ". Skipping evaluation.");
                continue;
            }

            Iterator<Position> positionIterator = openPositions.iterator();
            while (positionIterator.hasNext()) {
                Position position = positionIterator.next();
                if (position.getPrice() == null) {
                    System.out.println("Position price is null for " + tickerToEvaluate + ". Skipping evaluation.");
                    continue;
                }

                BigDecimal priceDelta = currentMarketPrice.subtract(position.getPrice());
                if (priceDelta.compareTo(BigDecimal.ZERO) < 0) {
                    System.out.println("Price went down for " + tickerToEvaluate + ". Let's wait.");
                    continue;
                }

                System.out.println("Price went up for " + tickerToEvaluate + ". Let's consider selling. But I want to minimal 3% growth");
                BigDecimal targetSellPrice = position.getPrice()
                                                     .multiply(BigDecimal.valueOf(1.03));
                if (targetSellPrice.compareTo(currentMarketPrice) >= 0) {
                    System.out.println("Price is not good enough for selling " + tickerToEvaluate);
                    continue;
                }

                System.out.println("Price is good for selling " + tickerToEvaluate + " because I bought it at " + position.getPrice() + " and now it's " + currentMarketPrice +
                                   ". Target sell price was " + targetSellPrice);
                System.out.println("Let's sell " + position.getAmount() + " of " + tickerToEvaluate);

                ApiToolkit.ApiResponse<TransactionResponse> sellOrderResponse = apiToolkit.placeMarketOrder("ASK", tickerToEvaluate, position.getAmount());
                if (sellOrderResponse.getHttpStatus() != 200 || sellOrderResponse.getResponseBody() == null) {
                    System.out.println("Sell order failed for " + tickerToEvaluate + ". HTTP status: " + sellOrderResponse.getHttpStatus());
                    continue;
                }

                BigDecimal totalSellValue = calculateTotalTransactionValue(sellOrderResponse.getResponseBody()
                                                                                            .getTransactions());
                if (totalSellValue.compareTo(BigDecimal.ZERO) <= 0) {
                    totalSellValue = currentMarketPrice.multiply(BigDecimal.valueOf(position.getAmount()));
                }

                availableCash = availableCash.add(totalSellValue);
                positionIterator.remove();
            }

            if (openPositions.isEmpty()) {
                portfolio.remove(tickerToEvaluate);
            }
        }
    }

    private BigDecimal calculateTotalTransactionValue(List<TransactionResponse.TransactionInfoResponse> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return transactions.stream()
                           .map(transaction -> transaction.price()
                                                          .multiply(BigDecimal.valueOf(transaction.volume())))
                           .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String stringifyPortfolio() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Portfolio of [").append(brokerName).append("] ").append(firstName).append(" ").append(lastName).append(": ");
        portfolio.forEach((ticker, positions) -> {
           stringBuilder.append(ticker)
                        .append("@")
                        .append(positions.stream()
                                         .map(Position::getAmount)
                                         .reduce(Long::sum)
                                         .orElse(0L))
                        .append(" ");
        });
        return stringBuilder.toString();
    }
}
