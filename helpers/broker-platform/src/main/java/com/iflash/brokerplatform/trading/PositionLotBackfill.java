package com.iflash.brokerplatform.trading;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
class PositionLotBackfill implements ApplicationRunner {

    private static final String BUY = "BID";

    private final PositionRepository positionRepository;
    private final TradeRepository tradeRepository;
    private final PositionLotRepository positionLotRepository;

    PositionLotBackfill(PositionRepository positionRepository, TradeRepository tradeRepository,
                        PositionLotRepository positionLotRepository) {
        this.positionRepository = positionRepository;
        this.tradeRepository = tradeRepository;
        this.positionLotRepository = positionLotRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        positionRepository.findByQuantityGreaterThan(0)
                          .stream()
                          .filter(position -> !positionLotRepository.existsByUserIdAndTicker(position.getUserId(),
                                  position.getTicker()))
                          .forEach(position -> positionLotRepository.saveAll(replay(position)));
    }

    private List<PositionLot> replay(Position position) {
        List<PositionLot> lots = new ArrayList<>();
        tradeRepository.findByUserIdAndTickerAndFilledQuantityGreaterThanOrderByCreatedAtAscIdAsc(
                               position.getUserId(), position.getTicker(), 0)
                       .forEach(trade -> {
                           if (BUY.equals(trade.getDirection())) {
                               lots.add(PositionLot.of(trade));
                           } else {
                               lots.removeAll(PositionLot.releaseInOrder(lots, trade.getFilledQuantity()));
                           }
                       });
        return lots;
    }
}
