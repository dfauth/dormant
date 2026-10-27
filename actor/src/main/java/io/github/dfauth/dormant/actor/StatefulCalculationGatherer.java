package io.github.dfauth.dormant.actor;

import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;
import java.util.stream.Gatherer;

import static java.util.function.Predicate.not;

@RequiredArgsConstructor
public class StatefulCalculationGatherer<T, R> implements Gatherer<T, StatefulCalculation<T, R>, R> {

    private final StatefulCalculation<T, R> statefulCalculation;

    @Override
    public Supplier<StatefulCalculation<T, R>> initializer() {
        return () -> statefulCalculation;
    }

    @Override
    public Integrator<StatefulCalculation<T, R>, T, R> integrator() {
        return (state, element, downstream) ->
                state.apply(element).stream().filter(not(downstream::push)).findFirst().isEmpty();
    }
}
