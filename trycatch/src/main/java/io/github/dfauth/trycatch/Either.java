package io.github.dfauth.trycatch;

import java.util.function.Function;

public interface Either<L, R> {

    static <L, R> Either<L,R> left(L l) {
        return new Left<>(l);
    }

    static <L, R> Either<L,R> right(R r) {
        return new Right<>(r);
    }

    default L left() {
        throw new UnsupportedOperationException(this+" has no left value");
    }

    default R right() {
        throw new UnsupportedOperationException(this+" has no right value");
    }

    default boolean isLeft() {
        return false;
    }

    default boolean isRight() {
        return false;
    }

    default <S> S mapLeft(Function<L,S> f) {
        return f.apply(left());
    }

    default <S> S mapRight(Function<R,S> f) {
        return f.apply(right());
    }

    record Left<L,R>(L left) implements Either<L, R> {

        @Override
        public boolean isLeft() {
            return true;
        }
    }

    record Right<L,R>(R right) implements Either<L, R> {

        @Override
        public boolean isRight() {
            return true;
        }
    }
}
