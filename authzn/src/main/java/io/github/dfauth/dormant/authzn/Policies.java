package io.github.dfauth.dormant.authzn;

import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.ServiceLoader;
import java.util.function.Supplier;

@RequiredArgsConstructor
public enum Policies implements Policy {

    ALLOW_ALL(() -> AuthorizationDecision.ALLOW),
    DENY_ALL(() -> AuthorizationDecision.DENY);

    private final Supplier<AuthorizationDecision> supplier;

    ServiceLoader.Provider<Policy> provider() {

        return new ServiceLoader.Provider<Policy>() {
            @Override
            public Class<? extends Policy> type() {
                return Policies.class;
            }

            @Override
            public Policy get() {
                return Policies.this;
            }
        };
    }

    @Override
    public ResourceHierarchy getResourceHierarchy() {
        return new ResourceHierarchy();
    }

    @Override
    public AuthorizationDecision decide(Collection<Principal> principals, Action action, Resource resource) {
        return supplier.get();
    }
}
