package io.github.dfauth.trade.model;

public enum TransactionType {
    DIV(1),
    INT(1),
    DEP(1),
    PAYMENT(-1),
    CREDIT(1),
    OTHER(-1);

    private int direction;

    TransactionType(int direction) {
        this.direction = direction;
    }

    public int multiplier() {
        return direction;
    }
}