package io.github.dfauth.dormant.authzn;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

public interface Action {

    String WILDCARD = "*";

    static String format(Action action) {
        return action.type() + ":" + action.label();
    }

    static Action parse(String str) {
        return Optional.of(str.split(":"))
                .filter(arr -> arr.length > 1)
                .map(arr -> new ActionRecord(arr[0],arr[1])
                )
                .orElseThrow();
    }

    String type();
    String label();
    default boolean implies(Action action) {
        return action.type().equals(type()) && (action.label().equals(WILDCARD) || action.label().equals(label()));
    }

    record ActionRecord(String type, String label) implements Action {

        @NonNull
        @Override
        public String toString() {
            return format(this);
        }
    }

    @RequiredArgsConstructor
    enum CRUDActions implements Action {
        CREATE, READ, UPDATE, DELETE, ALL("*");

        private final String label;

        CRUDActions() {
            label = null;
        }

        @Override
        public String type() {
            return "CRUD";
        }

        @Override
        public String label() {
            return Optional.ofNullable(label).orElse(name());
        }
    }
}
