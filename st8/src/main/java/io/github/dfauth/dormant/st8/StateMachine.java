package io.github.dfauth.dormant.st8;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static io.github.dfauth.trycatch.Predicates.always;
import static java.util.function.Predicate.not;

@Slf4j
@Builder
@AllArgsConstructor
@Getter
public class StateMachine<STATE, CTX> {

    private CTX context;
    private State<STATE, CTX> state;

    public <EVENT> StateMachine<STATE, CTX> onEvent(EVENT e) {
        this.state = state.onEvent(e, context).filter(not(state::equals)).map(next -> {
            state.onExit().accept(context);
            return next.onEntry(context);
        }).orElse(state);
        return this;
    }

    public static class StateMachineBuilder<STATE, CTX> {

        private State<STATE, CTX> initial;
        private CTX ctx;
        Map<STATE, State<STATE, CTX>> states = new HashMap<>();

        public StateMachineBuilder<STATE, CTX> initial(STATE s, Consumer<CTX> onEntry, Consumer<CTX> onExit) {
            this.initial = State.state(s, onEntry, onExit);
            return state(this.initial);
        }

        public StateMachineBuilder<STATE, CTX> state(STATE s, Consumer<CTX> onEntry, Consumer<CTX> onExit) {
            return state(State.state(s, onEntry, onExit));
        }

        public StateMachineBuilder<STATE, CTX> state(State<STATE, CTX> s) {
            states.put(s.payload(), s);
            return this;
        }

        public StateMachine.IntermediateGrammar<STATE, CTX> whenInState(STATE s) {
            return new IntermediateGrammar<>(this, s);
        }

        public StateMachineBuilder<STATE, CTX> context(CTX ctx) {
            this.ctx = ctx;
            return this;
        }

        public StateMachine<STATE, CTX> build() {
            return new StateMachine<>(ctx, initial);
        }
    }

    @RequiredArgsConstructor
    public static class IntermediateGrammar<STATE, CTX> {

        private final StateMachineBuilder<STATE, CTX> builder;
        private final STATE state;

        public <EVENT> IntermediateGrammar2<STATE, CTX, EVENT> onEvent(EVENT e) {
            return onEvent(e, always());
        }

        public <EVENT> IntermediateGrammar2<STATE, CTX, EVENT> onEvent(EVENT e, Predicate<CTX> guard) {
            return new IntermediateGrammar2<>(builder, state, e, guard);
        }
    }

    @RequiredArgsConstructor
    public static class IntermediateGrammar2<STATE, CTX, EVENT> {

        private final StateMachineBuilder<STATE, CTX> builder;
        private final STATE state;
        private final EVENT event;
        private final Predicate<CTX> guard;

        public StateMachineBuilder<STATE, CTX> transitionTo(STATE next) {
            return Optional.ofNullable(builder.states.get(state)).map(src ->
                Optional.ofNullable(builder.states.get(next)).map(dest -> {
                    src.transitions().put(event, new Transition<>(guard, dest));
                    return builder;
                }).orElseThrow(() -> new IllegalArgumentException("destination state not found: "+next))
            ).orElseThrow(() -> new IllegalArgumentException("source state not found: "+state));
        }

        public IntermediateGrammar2<STATE, CTX, EVENT> guard(Predicate<CTX> p) {
            return new IntermediateGrammar2<>(builder, state, event, p);
        }
    }
}
