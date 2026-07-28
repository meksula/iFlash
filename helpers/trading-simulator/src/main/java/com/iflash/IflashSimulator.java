package com.iflash;

import com.iflash.tick.TickManager;
import com.iflash.toolkit.ApiToolkit;
import com.iflash.trader.Trader;

import java.util.Set;

public class IflashSimulator {

    public static void main( String[] args ) {

        ApiToolkit apiToolkit = new ApiToolkit();
        TradersBootstrapper tradersBootstrapper = new TradersBootstrapper(apiToolkit);
        Set<Trader> traders = tradersBootstrapper.bootstrap();
        TickManager tickManager = new TickManager(traders);
        tickManager.run();
    }
}
