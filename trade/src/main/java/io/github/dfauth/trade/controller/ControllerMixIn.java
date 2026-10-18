package io.github.dfauth.trade.controller;

import io.github.dfauth.trycatch.Collectors;
import io.github.dfauth.trycatch.Failure;
import io.github.dfauth.trycatch.Success;
import io.github.dfauth.trycatch.Try;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.github.dfauth.trycatch.TryCatch.tryCatch;
import static java.util.function.Predicate.not;

public interface ControllerMixIn {

    default  <T> Stream<T> streamCodes(List<List<String>> codes, Function<String, T> f) {
        return codes.stream()
                .flatMap(List::stream)
                .filter(not(StringUtils::isBlank))
                .map(f);
    }

    default  <T> Stream<Map.Entry<String, Try<T>>> streamCodestoMapEntries(List<List<String>> codes, Function<String, Optional<T>> f) {
        return streamCodes(codes, c -> Map.entry(c, tryWrap(c, f)));
    }

    default <T> Try<T> tryWrap(String code, Function<String, Optional<T>> f) {
        return tryCatch(() -> f.apply(code)
                        .map(Success::new)
                        .orElseThrow(() -> new IllegalStateException("No value found for code "+code)),
                Failure::new
        );
    }

    default  <T> Map<String, Try<T>> mapCodes(List<List<String>> codes, Function<String, Optional<T>> f) {
        return streamCodestoMapEntries(codes, f).collect(Collectors.mapEntryCollector());
    }

}
