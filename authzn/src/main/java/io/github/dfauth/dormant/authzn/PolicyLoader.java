package io.github.dfauth.dormant.authzn;

import lombok.extern.slf4j.Slf4j;

import java.util.ServiceLoader;

@Slf4j
public class PolicyLoader {

    private static final ServiceLoader.Provider<Policy> PROVIDER = loadProvider();

    @SuppressWarnings("unchecked")
    private static ServiceLoader.Provider<Policy> loadProvider() {
        return ServiceLoader.load(ServiceLoader.Provider.class)
                .findFirst()
                .orElseGet(Policies.DENY_ALL::provider);
    }

    public static Policy getPolicy() {
        return PROVIDER.get();
    }
}
