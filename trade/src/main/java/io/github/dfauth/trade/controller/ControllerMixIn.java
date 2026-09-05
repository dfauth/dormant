package io.github.dfauth.trade.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    default  <T> Stream<Map.Entry<String, Try<T>>> streamCodes(List<List<String>> codes, Function<String, Optional<T>> f) {
        return codes.stream()
                .flatMap(List::stream)
                .filter(not(StringUtils::isBlank))
                .map(c -> Map.entry(c, tryWrap(c, f)));
    }

    default <T> Try<T> tryWrap(String code, Function<String, Optional<T>> f) {
        return tryCatch(() -> f.apply(code)
                        .map(SuccessWrapper::new)
                        .orElseThrow(() -> new IllegalStateException("No value found for code "+code)),
                FailureWrapper::new
        );
    }

    default  <T> Map<String, Try<T>> mapCodes(List<List<String>> codes, Function<String, Optional<T>> f) {
        return streamCodes(codes, f).collect(Collectors.mapEntryCollector());
    }

    class SuccessWrapper<T> extends Success<T> {

        public SuccessWrapper(T t) {
            super(t);
        }

        @JsonIgnore
        @Override
        public boolean isSuccess() {
            return super.isSuccess();
        }

        @JsonIgnore
        @Override
        public boolean isFailure() {
            return super.isFailure();
        }
    }

    class FailureWrapper<T> extends Failure<T> {

        public FailureWrapper(Exception exception) {
            super(exception);
        }

        @JsonIgnore
        @Override
        public boolean isSuccess() {
            return super.isSuccess();
        }

        @JsonIgnore
        @Override
        public boolean isFailure() {
            return super.isFailure();
        }

        public String getError() {
            return super.exception.getMessage();
        }
    }
}
