package io.github.dfauth.ta;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public interface Dated<T> {
    @JsonProperty("d")
    LocalDate date();

    @JsonProperty("p")
    T payload();

    static <T> Dated<T> withPayload(LocalDate date, T payload) {
        return new Dated<T>() {
            @Override
            public LocalDate date() {
                return date;
            }

            @Override
            public T payload() {
                return payload;
            }
        };
    }
}
