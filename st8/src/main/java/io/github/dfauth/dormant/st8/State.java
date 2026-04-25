package io.github.dfauth.dormant.st8;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public record State<STATE, CTX>(STATE payload, Consumer<CTX> onEntry, Consumer<CTX> onExit, Map<Object, Transition<STATE, CTX>> transitions) {

    public static <STATE, CTX> State<STATE, CTX> state(STATE s, Consumer<CTX> onEntry, Consumer<CTX> onExit) {
        return new State<>(s, onEntry, onExit);
    }

    public static <STATE, CTX> StateLookup<STATE, CTX> transitionTo(STATE s) {
        return builder -> builder.states.get(s);
    }

    public State(STATE payload, Consumer<CTX> onEntry, Consumer<CTX> onExit) {
        this(payload, onEntry, onExit, new HashMap<>());
    }

    public <EVENT> Optional<State<STATE, CTX>> onEvent(EVENT e, CTX context) {
        return Optional.ofNullable(transitions.get(e))
                .filter(t -> t.test(context))
                .map(Transition::next);
    }

    public State<STATE, CTX> onEntry(CTX context) {
        onEntry.accept(context);
        return this;
    }

    public interface StateLookup<STATE, CTX> extends Function<StateMachine.StateMachineBuilder<STATE, CTX>, State<STATE, CTX>> {}
}
