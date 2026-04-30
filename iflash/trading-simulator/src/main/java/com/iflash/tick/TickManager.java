package com.iflash.tick;

import com.iflash.trader.Trader;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
public class TickManager implements Runnable {

    private final static int TICK_INTERVAL_IN_MILLISECONDS = 5000;
    private final Set<Trader> traders;

    @Override
    public void run() {
            while (true) {
                System.out.println("Tick started working");
                for (Trader trader : traders) {
                    trader.decide();
                }
                System.out.println("Tick finished, waiting for next tick");
                try {
                    Thread.sleep(TICK_INTERVAL_IN_MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
    }
}
