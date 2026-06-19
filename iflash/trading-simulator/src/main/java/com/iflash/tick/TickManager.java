package com.iflash.tick;

import com.iflash.trader.Trader;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class TickManager implements Runnable {

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
            System.out.println("Round: " + round);
            for (Trader trader : traders) {
                trader.decide();
            }
        }, 0, TICK_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
    }
}
