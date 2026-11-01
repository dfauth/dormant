package io.github.dfauth.dormant.actor;

import io.github.dfauth.ta.RingBuffer;
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
        assertEquals(List.of(List.of(1.0,2.0,3.0), List.of(2.0,3.0,4.0)), Stream.of(1.0,2.0,3.0,4.0).gather(windowingGatherer(RingBuffer.create(new double[3]))).toList());
    }
}
