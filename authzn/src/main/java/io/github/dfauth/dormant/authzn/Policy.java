package io.github.dfauth.dormant.authzn;

import io.github.dfauth.trycatch.ExceptionalRunnable;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static io.github.dfauth.trycatch.TryCatch.tryCatch;
import static io.github.dfauth.trycatch.TryCatch.tryCatchRunnable;

public interface Policy {

    static void authorize(User user, Action action, Resource resource, ExceptionalRunnable runnable) {
        tryCatchRunnable(() -> authorize(user).toPerform(action).on(resource).execute(runnable));
    }

    static <V> V authorize(User user, Action action, Resource resource, Callable<V> callable) {
        return tryCatch(() -> authorize(user).toPerform(action).on(resource).execute(callable));
    }

    static PolicyGrammar.Stage1 authorize(User user) {
        return authorize(PolicyLoader.getPolicy(), user);
    }

    static PolicyGrammar.Stage1 authorize(Policy policy, User user) {
        return new PolicyGrammar.Stage1(policy, Stream.concat(Stream.<Principal>of(user), user.roles().stream()).toList());
    }

    default PolicyGrammar.Stage1 _authorize(User user) {
        return authorize(this, user);
    }

    default PolicyGrammar.Stage2 _authorize(User user, Action action) {
        return authorize(this, user).toPerform(action);
    }

    default AuthorizationDecision _authorize(User user, Action action, Resource resource) {
        return _authorize(user, action).on(resource);
    }

    default <V> V _authorize(User user, Action action, Resource resource, Callable<V> callable) {
        return tryCatch(() -> _authorize(user, action, resource).execute(callable));
    }

    ResourceHierarchy getResourceHierarchy();

    class PolicyGrammar {

        @RequiredArgsConstructor
        static class Stage1 {

            private final Policy policy;
            private final Collection<Principal> principals;

            public Stage2 toPerform(Action action) {
                return new Stage2(policy, principals, action);
            }
        }

        @RequiredArgsConstructor
        static class Stage2 {
            private final Policy policy;
            private final Collection<Principal> principals;
            private final Action action;

            public AuthorizationDecision on(Resource resource) {
                return policy.decide(principals, action, resource);
            }
        }
    }

    default AuthorizationDecision decide(Collection<Principal> principals, Action action, Resource resource) {
        List<String> names = principals.stream().map(Principal::name).toList();
        List<Directive> directives = getResourceHierarchy().getDirectives(resource);
        return directives.stream()
                .filter(d -> names.contains(d.principal().name()) && action.implies(d.action()))
                .findFirst()
                .map(Directive::decision)
                .orElse(AuthorizationDecision.DENY);
    }
}
