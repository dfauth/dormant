package io.github.dfauth.trade.model;

import java.util.function.Predicate;
import java.util.function.Supplier;

public enum PaymentPredicate implements Supplier<Predicate<Balanced>> {
    RECONCILE(() -> new Predicate<>() {

        private Balanced previous = null;

        @Override
        public boolean test(Balanced payment) {
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

    private Supplier<Predicate<Balanced>> p;

    PaymentPredicate(Supplier<Predicate<Balanced>> p) {
        this.p = p;
    }

    @Override
    public Predicate<Balanced> get() {
        return p.get();
    }
}
