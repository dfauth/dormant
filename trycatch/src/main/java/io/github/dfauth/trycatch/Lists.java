package io.github.dfauth.trycatch;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.stream;

@Slf4j
public class Lists {

    public static <T> List<T> append(List<T>... lists) {
        return java.util.stream.Stream.of(lists)
                .flatMap(List::stream)
                .toList();
    }

    public static <T> List<T> modifiableList(T... elements) {
        return stream(elements).reduce(new ArrayList<>(), (l, r) -> {
            l.add(r);
            return l;
        }, (l, r) -> {
            l.addAll(r);
            return l;
        });
    }
}
