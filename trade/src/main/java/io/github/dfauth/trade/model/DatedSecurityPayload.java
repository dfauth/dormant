package io.github.dfauth.trade.model;

import java.time.LocalDate;

public record DatedSecurityPayload<T>(String code, LocalDate date, T payload) {
}
