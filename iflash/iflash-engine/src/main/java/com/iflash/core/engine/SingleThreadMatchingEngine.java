package com.iflash.core.engine;

public class SingleThreadMatchingEngine implements MatchingEngine {

    private SingleThreadMatchingEngine() {
    }

    public static SingleThreadMatchingEngine create() {
        return new SingleThreadMatchingEngine();
    }

    @Override
    public MatchingEngineState initialize() {
        return null;
    }
}
