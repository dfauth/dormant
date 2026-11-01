package io.github.dfauth.dormant.actor;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static io.github.dfauth.dormant.actor.Gatherers.windowingGatherer;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class WindowingGathererTest {

    @Test
    public void testIt() {
        assertEquals(List.of(List.of(1,2,3), List.of(2,3,4)), Stream.of(1,2,3,4).gather(windowingGatherer(3)).toList());
    }
}
