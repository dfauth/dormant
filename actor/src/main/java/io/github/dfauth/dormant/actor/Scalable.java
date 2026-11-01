package io.github.dfauth.dormant.actor;

import lombok.AllArgsConstructor;

public interface Scalable<R extends Scalable<R>> {

    R aggregate(R r);

    R scale(double multiplier);

    static DoubleWrapper scalable(double d) {
        return new DoubleWrapper(d);
    }

    @AllArgsConstructor
    class DoubleWrapper implements Scalable<DoubleWrapper> {

        private double d;

        @Override
        public DoubleWrapper aggregate(DoubleWrapper other) {
            this.d += other.d;
            return this;
        }

        @Override
        public DoubleWrapper scale(double multiplier) {
            this.d *= multiplier;
            return this;
        }
    }
}
