package io.github.dfauth.trycatch;

import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.dfauth.trycatch.Utils.oops;

public class Maps {

    public static <K,V> BinaryOperator<Map<K,V>> merge() {
        return merge(oops("Merge not supported"));
    }

    public static <K,V> BinaryOperator<Map<K,V>> merge(BinaryOperator<V> mergeFunction) {
        return (l, r) -> Stream.concat(l.entrySet().stream(), r.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, mergeFunction));
    }
}
