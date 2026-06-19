package com.iflash.brokerplatform.portfolio;

import com.iflash.brokerplatform.market.IflashApiClient;
import com.iflash.brokerplatform.market.PriceDto;
import com.iflash.brokerplatform.trading.Position;
import com.iflash.brokerplatform.trading.PositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PortfolioService {

    private final PositionRepository positionRepository;
    private final IflashApiClient api;

    public PortfolioService(PositionRepository positionRepository, IflashApiClient api) {
        this.positionRepository = positionRepository;
        this.api = api;
    }

    @Transactional(readOnly = true)
    public List<Holding> holdings(Long userId) {
        return positionRepository.findByUserIdAndQuantityGreaterThanOrderByTicker(userId, 0)
                                 .stream()
                                 .map(this::toHolding)
                                 .toList();
    }

    public BigDecimal totalMarketValue(List<Holding> holdings) {
        return holdings.stream()
                       .map(Holding::marketValue)
                       .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Holding toHolding(Position position) {
        BigDecimal price = currentPrice(position.getTicker());
        BigDecimal marketValue = price.multiply(BigDecimal.valueOf(position.getQuantity()));
        return new Holding(position.getTicker(), position.getQuantity(), price, marketValue);
    }

    private BigDecimal currentPrice(String ticker) {
        try {
            PriceDto price = api.getPrice(ticker);
            return price != null && price.price() != null ? price.price() : BigDecimal.ZERO;
        } catch (RuntimeException e) {
            return BigDecimal.ZERO;
        }
    }
}
