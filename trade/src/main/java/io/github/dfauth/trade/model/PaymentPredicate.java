package io.github.dfauth.trade.model;

import java.util.function.Predicate;
import java.util.function.Supplier;

public enum PaymentPredicate implements Supplier<Predicate<io.github.dfauth.dormant.model.Balanced>> {
    RECONCILE(() -> new Predicate<>() {

        private io.github.dfauth.dormant.model.Balanced previous = null;

        @Override
        public boolean test(io.github.dfauth.dormant.model.Balanced payment) {
            if (previous == null) {
                previous = payment;
                return false;
            } else {
                boolean result = payment.reconcilesAfter(previous);
                previous = payment;
                return !result;
            }
        }
    });

    private Supplier<Predicate<io.github.dfauth.dormant.model.Balanced>> p;

    PaymentPredicate(Supplier<Predicate<io.github.dfauth.dormant.model.Balanced>> p) {
        this.p = p;
    }

    @Override
    public Predicate<io.github.dfauth.dormant.model.Balanced> get() {
        return p.get();
    }
}
