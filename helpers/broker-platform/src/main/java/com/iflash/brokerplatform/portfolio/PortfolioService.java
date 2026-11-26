package com.iflash.brokerplatform.portfolio;

import com.iflash.brokerplatform.market.IflashApiClient;
import com.iflash.brokerplatform.market.PriceDto;
import com.iflash.brokerplatform.trading.Position;
import com.iflash.brokerplatform.trading.PositionLot;
import com.iflash.brokerplatform.trading.PositionLotRepository;
import com.iflash.brokerplatform.trading.PositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    private final PositionRepository positionRepository;
    private final PositionLotRepository positionLotRepository;
    private final IflashApiClient api;

    public PortfolioService(PositionRepository positionRepository, PositionLotRepository positionLotRepository,
                            IflashApiClient api) {
        this.positionRepository = positionRepository;
        this.positionLotRepository = positionLotRepository;
        this.api = api;
    }

    @Transactional(readOnly = true)
    public List<Holding> holdings(Long userId) {
        Map<String, List<PositionLot>> lotsByTicker = positionLotRepository.findByUserIdOrderByPurchasedAtAscIdAsc(userId)
                                                                           .stream()
                                                                           .collect(Collectors.groupingBy(PositionLot::getTicker));
        return positionRepository.findByUserIdAndQuantityGreaterThanOrderByTicker(userId, 0)
                                 .stream()
                                 .map(position -> toHolding(position, lotsByTicker.getOrDefault(position.getTicker(), List.of())))
                                 .toList();
    }

    public BigDecimal totalMarketValue(List<Holding> holdings) {
        return holdings.stream()
                       .map(Holding::marketValue)
                       .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Holding toHolding(Position position, List<PositionLot> lots) {
        return Holding.of(position.getTicker(), position.getQuantity(), currentPrice(position.getTicker()), lots);
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
