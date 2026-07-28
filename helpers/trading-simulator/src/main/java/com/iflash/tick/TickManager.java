package com.iflash.tick;

import com.iflash.trader.Trader;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class TickManager implements Runnable {

    private static final Logger log = LogManager.getLogger(TickManager.class);

    private final static int TICK_INTERVAL_IN_SECONDS = 10;
    private final Set<Trader> traders;

    private final ScheduledExecutorService scheduledExecutorService;

    private long round = 0;

    public TickManager(Set<Trader> traders) {
        this.traders = new HashSet<>(traders);
        this.scheduledExecutorService = Executors.newScheduledThreadPool(2);
    }

    @Override
    public void run() {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            this.round++;
            log.info("===== Round {} ({} traders) =====", round, traders.size());
            for (Trader trader : traders) {
                try {
                    trader.decide();
                } catch (RuntimeException e) {
                    log.error("Trader failed during decide(), skipping this tick: {}", e.getMessage(), e);
                }
            }
        }, 0, TICK_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
    }
}
