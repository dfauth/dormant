package io.github.dfauth.dormant.model;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum MarketStatus {
    CLOSED, EARLY_CLOSE;

    public boolean isClosed() {
        return CLOSED == this;
    }

    public boolean isEarlyClose() {
        return EARLY_CLOSE == this;
    }
}
