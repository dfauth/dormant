package io.github.dfauth.trade.model;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.github.dfauth.trycatch.Predicates.orTrue;

public enum PositionPredicate implements Supplier<Predicate<Position>> {
    OPEN(Position::isOpen),
    CLOSED(Position::isClosed),
    SHORT(Position::isShort),
    LONG(Predicate.not(Position::isShort));

    private Predicate<Position> p;

    PositionPredicate(Predicate<Position> p) {
        this.p = p;
    }

    @Override
    public Predicate<Position> get() {
        return p;
    }

    public static Predicate<Position> filter(Optional<PositionPredicate> o) {
        return orTrue(o.map(Supplier::get));
    }

    public Predicate<Position> and(PositionPredicate positionPredicate) {
        return get().and(positionPredicate.get());
    }

    public Predicate<Position> or(PositionPredicate positionPredicate) {
        return get().or(positionPredicate.get());
    }
}
