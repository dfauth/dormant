package io.github.dfauth.trade.service;

public class DuplicateTradeException extends RuntimeException {

    public DuplicateTradeException(String confirmationId) {
        super("Trade with confirmationId '" + confirmationId + "' already exists");
    }
}
