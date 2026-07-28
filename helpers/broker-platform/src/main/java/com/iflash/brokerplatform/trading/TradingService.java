package com.iflash.brokerplatform.trading;

import com.iflash.brokerplatform.market.FillDto;
import com.iflash.brokerplatform.market.IflashApiClient;
import com.iflash.brokerplatform.market.OrderRequestDto;
import com.iflash.brokerplatform.market.OrderResultDto;
import com.iflash.brokerplatform.market.PriceDto;
import com.iflash.brokerplatform.user.User;
import com.iflash.brokerplatform.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Places an order on the engine and settles it against the user's cash and positions.
 *
 * <p>The engine fills immediately and is anonymous, so we pre-validate funds/holdings
 * <em>before</em> calling it, then settle strictly on the fills it returns (it may
 * partial-fill against available liquidity).
 */
@Service
public class TradingService {

    private static final String BUY = "BID";
    private static final String SELL = "ASK";
    private static final String MARKET = "MARKET";

    private final UserRepository userRepository;
    private final PositionRepository positionRepository;
    private final TradeRepository tradeRepository;
    private final IflashApiClient api;

    public TradingService(UserRepository userRepository, PositionRepository positionRepository,
                          TradeRepository tradeRepository, IflashApiClient api) {
        this.userRepository = userRepository;
        this.positionRepository = positionRepository;
        this.tradeRepository = tradeRepository;
        this.api = api;
    }

    @Transactional
    public OrderOutcome placeOrder(Long userId, String rawTicker, String direction, String orderType,
                                   BigDecimal price, long volume) {
        String ticker = rawTicker == null ? "" : rawTicker.trim().toUpperCase();
        validateInputs(ticker, direction, orderType, price, volume);

        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        Position position = positionRepository.findByUserIdAndTicker(userId, ticker)
                                              .orElseGet(() -> new Position(userId, ticker, 0));

        preValidate(direction, orderType, price, volume, user, position, ticker);

        // Engine executes here; a thrown EngineException rolls back the whole transaction.
        OrderResultDto result = api.registerOrder(
                new OrderRequestDto(direction, orderType, ticker, isMarket(orderType) ? null : price, volume));

        long filledQuantity = totalFilled(result);
        BigDecimal cash = totalCash(result);
        settle(direction, user, position, filledQuantity, cash);

        tradeRepository.save(new Trade(userId, ticker, direction, orderType, price, filledQuantity, cash));
        return new OrderOutcome(ticker, direction, orderType, volume, filledQuantity, cash);
    }

    @Transactional(readOnly = true)
    public long heldQuantity(Long userId, String ticker) {
        return positionRepository.findByUserIdAndTicker(userId, ticker == null ? "" : ticker.trim().toUpperCase())
                                 .map(Position::getQuantity)
                                 .orElse(0L);
    }

    @Transactional(readOnly = true)
    public List<Trade> history(Long userId) {
        return tradeRepository.findTop100ByUserIdOrderByCreatedAtDesc(userId);
    }

    private void preValidate(String direction, String orderType, BigDecimal price, long volume,
                             User user, Position position, String ticker) {
        if (isBuy(direction)) {
            BigDecimal unitPrice = isMarket(orderType) ? currentPrice(ticker) : price;
            BigDecimal estimatedCost = unitPrice.multiply(BigDecimal.valueOf(volume));
            if (estimatedCost.compareTo(user.getBalance()) > 0) {
                throw new TradingException("Insufficient funds: estimated cost " + estimatedCost
                        + " exceeds balance " + user.getBalance());
            }
        } else {
            if (position.getQuantity() < volume) {
                throw new TradingException("Insufficient holdings: you own " + position.getQuantity()
                        + " " + ticker + ", cannot sell " + volume);
            }
        }
    }

    private void settle(String direction, User user, Position position, long filledQuantity, BigDecimal cash) {
        if (filledQuantity == 0) {
            return;
        }
        if (isBuy(direction)) {
            user.setBalance(user.getBalance().subtract(cash));
            position.setQuantity(position.getQuantity() + filledQuantity);
        } else {
            user.setBalance(user.getBalance().add(cash));
            position.setQuantity(position.getQuantity() - filledQuantity);
        }
        positionRepository.save(position);
    }

    private BigDecimal currentPrice(String ticker) {
        PriceDto price = api.getPrice(ticker);
        if (price == null || price.price() == null) {
            throw new TradingException("Cannot determine current price for " + ticker);
        }
        return price.price();
    }

    private void validateInputs(String ticker, String direction, String orderType, BigDecimal price, long volume) {
        if (ticker.isEmpty()) {
            throw new TradingException("Ticker is required");
        }
        if (!BUY.equals(direction) && !SELL.equals(direction)) {
            throw new TradingException("Direction must be BID (buy) or ASK (sell)");
        }
        if (orderType == null || orderType.isBlank()) {
            throw new TradingException("Order type is required");
        }
        if (volume <= 0) {
            throw new TradingException("Volume must be greater than 0");
        }
        if (!isMarket(orderType) && (price == null || price.signum() <= 0)) {
            throw new TradingException("Price is required for a " + orderType + " order");
        }
    }

    private long totalFilled(OrderResultDto result) {
        if (result == null || result.transactions() == null) {
            return 0;
        }
        return result.transactions()
                     .stream()
                     .mapToLong(FillDto::volume)
                     .sum();
    }

    private BigDecimal totalCash(OrderResultDto result) {
        if (result == null || result.transactions() == null) {
            return BigDecimal.ZERO;
        }
        return result.transactions()
                     .stream()
                     .map(fill -> fill.price().multiply(BigDecimal.valueOf(fill.volume())))
                     .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isBuy(String direction) {
        return BUY.equals(direction);
    }

    private boolean isMarket(String orderType) {
        return MARKET.equalsIgnoreCase(orderType);
    }
}
