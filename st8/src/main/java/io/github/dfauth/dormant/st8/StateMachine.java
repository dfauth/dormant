package io.github.dfauth.dormant.st8;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.function.Predicate.not;

@Slf4j
@Builder
@AllArgsConstructor
@Getter
public class StateMachine<STATE, CTX> {

    public static <T> Consumer<T> noOp() {
        return t -> {};
    }

    public static <T> Predicate<T> alwaysTrue() {
        return t -> true;
    }

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

        public <EVENT> StateMachineBuilder<STATE, CTX> whenInState(STATE s, Event<EVENT> e, State.StateLookup<STATE, CTX> stateLookup) {
            return whenInState(s, e, alwaysTrue(), stateLookup);
        }

        public <EVENT> StateMachineBuilder<STATE, CTX> whenInState(STATE s, Event<EVENT> e, Predicate<CTX> guard, State.StateLookup<STATE, CTX> stateLookup) {
            Optional.ofNullable(states.get(s)).ifPresent(state -> {
                state.transitions().put(e.payload(), new Transition<>(guard, stateLookup.apply(this)));
            });
            return this;
        }

        public StateMachineBuilder<STATE, CTX> context(CTX ctx) {
            this.ctx = ctx;
            return this;
        }

        public StateMachine<STATE, CTX> build() {
            return new StateMachine<>(ctx, initial);
        }
    }
}
