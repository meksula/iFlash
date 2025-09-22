package com.iflash.core.engine;

public class MatchingEngineFactory {

    public MatchingEngine factorize(MatchingEngineType matchingEngineType) {
        return switch (matchingEngineType) {
            case SINGLE_THREAD_ENGINE -> SingleThreadMatchingEngine.create();
        };
    }
}
