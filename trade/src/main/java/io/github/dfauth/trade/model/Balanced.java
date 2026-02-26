package io.github.dfauth.trade.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static io.github.dfauth.trycatch.Utils.bd;

public interface Balanced {
    LocalDate getDate();
    BigDecimal getValue();
    int getDirection();
    BigDecimal getBalance();

    default BigDecimal getDirectionalValue() {
        return getValue().multiply(bd(getDirection()));
    }

    default boolean reconcilesAfter(Balanced previous) {
        return previous.reconcilesBefore(this);
    }

    default boolean reconcilesBefore(Balanced next) {
        return getBalance().add(next.getDirectionalValue()).compareTo(next.getBalance()) == 0;
    }

    class SortingBalancer implements Balanced {

        private List<Payment> unreconciled = new ArrayList<>();
        private LinkedList<Payment> balances = new LinkedList<>();

        public SortingBalancer add(Payment balance) {
            if(balances.isEmpty()) {
                balances.add(balance);
            } else {
                if(balance.reconcilesBefore(balances.getFirst())) {
                    balances.addFirst(balance);
                    tidyUp();
                } else if(balance.reconcilesAfter(balances.getLast())) {
                    balances.addLast(balance);
                    tidyUp();
                } else {
                    unreconciled.add(balance);
                }
            }
            return this;
        }

        private void tidyUp() {
            if(!unreconciled.isEmpty()) {
                add(unreconciled.removeFirst());
            }
        }

        private void assertTidy() {
            assert unreconciled.isEmpty();
        }

        @Override
        public LocalDate getDate() {
            assertTidy();
            return balances.stream().map(Balanced::getDate).distinct().findFirst().orElseThrow();
        }

        @Override
        public BigDecimal getValue() {
            assertTidy();
            return balances.stream().map(Balanced::getDirectionalValue).reduce(BigDecimal::add).orElseThrow();
        }

        @Override
        public BigDecimal getDirectionalValue() {
            return getValue();
        }

        @Override
        public int getDirection() {
            assertTidy();
            return getDirectionalValue().divide(getValue(), MathContext.DECIMAL128).intValue();
        }

        @Override
        public BigDecimal getBalance() {
            return balances.getLast().getBalance();
        }

        public Stream<Payment> stream() {
            assertTidy();
            return balances.stream();
        }
    }
}
