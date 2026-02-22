package io.github.dfauth.trycatch;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;

@Slf4j
public class Optionals {

    public static <T> Optional<T> or(BinaryOperator<T> combiner, Optional<T>... optionals) {
        return or(combiner, Arrays.asList(optionals));
    }

    public static <T> Optional<T> or(BinaryOperator<T> combiner, List<Optional<T>> optionals) {
        switch(optionals.size()) {
            case 1:
                return optionals.get(0);
            case 2:
                Optional<T> l = optionals.get(0);
                Optional<T> r = optionals.get(1);
                return l.map(_l -> r.map(_r -> combiner.apply(_l, _r)).orElse(_l)).or(() -> r);
            default:
                Optional<T> l1 = optionals.get(0);
                Optional<T> r1 = or(combiner, optionals.subList(1, optionals.size()));
                return l1.map(_l -> r1.map(_r -> combiner.apply(_l, _r)).orElse(_l)).or(() -> r1);
        }
    }
    public static <T> Optional<T> and(BinaryOperator<T> combiner, Optional<T>... optionals) {
        return and(combiner, Arrays.asList(optionals));
    }

    public static <T> Optional<T> and(BinaryOperator<T> combiner, List<Optional<T>> optionals) {
        switch(optionals.size()) {
            case 1:
                return optionals.get(0);
            case 2:
                return optionals.get(0).flatMap(_l -> optionals.get(1).flatMap(_r -> Optional.of(combiner.apply(_l, _r))));
            default:
                return optionals.get(0).flatMap(_l -> and(combiner, optionals.subList(1, optionals.size())));
        }
    }
}
