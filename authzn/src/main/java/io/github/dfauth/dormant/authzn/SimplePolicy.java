package io.github.dfauth.dormant.authzn;

import lombok.extern.slf4j.Slf4j;

import java.util.ServiceLoader;

@Slf4j
public class SimplePolicy implements Policy {

    private ResourceHierarchy resourceHierarchy = new ResourceHierarchy();

    @Override
    public ResourceHierarchy getResourceHierarchy() {
        return resourceHierarchy;
    }

    public void add(Directive directive) {
        resourceHierarchy.addDirective(directive);
    }

    public static class Provider implements ServiceLoader.Provider<Policy> {

        @Override
        public Class<? extends Policy> type() {
            return SimplePolicy.class;
        }

        @Override
        public Policy get() {
            return new SimplePolicy();
        }
    }
}
