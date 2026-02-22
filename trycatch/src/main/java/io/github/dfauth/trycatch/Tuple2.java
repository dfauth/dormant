package io.github.dfauth.trycatch;

import java.util.function.BiFunction;

public interface Tuple2<T,R> {

    T _1();
    R _2();
    default <S> S map(BiFunction<T,R,S> f2) {
        return f2.apply(_1(), _2());
    }

    static <T, R> Tuple2<T, R> tuple2(T t, R r) {
        return new SimpleTuple2<>(t, r);
    }

    record SimpleTuple2<T,R>(T t, R r) implements Tuple2<T, R> {

        @Override
        public T _1() {
            return t();
        }

        @Override
        public R _2() {
            return r();
        }
    }
}
