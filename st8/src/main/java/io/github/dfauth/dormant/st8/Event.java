package io.github.dfauth.dormant.st8;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record Event<EVENT>(EVENT payload) {

    public static <EVENT> Event<EVENT> onEvent(EVENT e) {
        return new Event<>(e);
    }
}
