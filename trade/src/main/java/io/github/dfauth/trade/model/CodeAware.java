package io.github.dfauth.trade.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor
public class CodeAware<T> {
    private final String mkt;
    private final String code;
    @Getter
    private final T payload;

    public String getCode() {
        return mkt+":"+code;
    }

    public <R> CodeAware<R> map(Function<T, R> f) {
        return new CodeAware<>(mkt, code, f.apply(payload));
    }
}
