package io.github.dfauth.trade.model;

import io.github.dfauth.ta.AverageTrueRange;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Function;

@Slf4j
@AllArgsConstructor
public class ATR {

    private final Price price;
    @Getter
    private final Double atr;

    public double getPriceAsAFractionOfAtr() {
        return getAtrFraction(price.close());
    }

    public double getAtrFraction(double d) {
        return d/atr;
    }

    static Function<Price, Optional<ATR>> streamAtr(int period) {
        return p -> AverageTrueRange.atrStream(period).apply(p).map(atr -> new ATR(p, atr));
    }
}
