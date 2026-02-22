package io.github.dfauth.trade.model;

import java.math.BigDecimal;

import static io.github.dfauth.trycatch.Utils.bd;

public enum Side {
    BUY(-1), SELL(1);

    private final int multiplier;

    Side(int multiplier) {
        this.multiplier = multiplier;
    }

    public int signed(int i) {
        return multiplier * i;
    }

    public BigDecimal signed(BigDecimal bd) {
        return bd(multiplier).multiply(bd);
    }

    public Side flip() {
        return isBuy() ? SELL : BUY;
    }

    public boolean isBuy() {
        return this == BUY;
    }

    public boolean isSell() {
        return this == SELL;
    }
}
