package io.github.dfauth.trade.model;

import java.util.function.Predicate;

public enum PositionPredicate implements Predicate<Position> {
    OPEN(Position::isOpen),
    CLOSED(Position::isClosed),
    SHORT(Position::isShort),
    LONG(Predicate.not(Position::isShort));

    private Predicate<Position> p;

    PositionPredicate(Predicate<Position> p) {
        this.p = p;
    }

    @Override
    public boolean test(Position position) {
        return p.test(position);
    }
}
