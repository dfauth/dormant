package io.github.dfauth.dormant.actor;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Function;

public interface StatefulCalculation<T, R> extends Function<T, Optional<R>> {
}
