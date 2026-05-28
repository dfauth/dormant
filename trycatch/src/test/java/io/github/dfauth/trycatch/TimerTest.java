package io.github.dfauth.trycatch;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static io.github.dfauth.trycatch.Timer.timed;
import static io.github.dfauth.trycatch.TryCatch.tryCatch;

@Slf4j
public class TimerTest {

    static {
        System.setProperty(Timer.class.getName()+".enabled", Boolean.TRUE.toString());
    }

    @Test
    public void testIt() {
        timed(() -> {
            sleepRandom();
            zero();
            return "0";
        });
    }

    private void zero() {
        timed(() -> one());
    }

    private void one() {
        timed(() -> two());
    }

    private void two() {
        timed(() -> threeA());
        sleepRandom();
        timed(() -> threeB());
        timed(() -> threeC());
    }

    @Timed
    private void threeA() {
        sleepRandom();
    }

    @Timed
    private void threeB() {
        sleepRandom();
    }

    @Timed
    private void threeC() {
        sleepRandom();
    }

    private void sleepRandom() {
        sleepRandom(100l);
    }
    private void sleepRandom(long multiplier) {
        tryCatch(() -> Thread.sleep((long) (Math.random()*multiplier)));
    }
}