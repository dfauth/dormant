package io.github.dfauth.dormant.st8;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.dfauth.dormant.st8.Event.onEvent;
import static io.github.dfauth.dormant.st8.State.transitionTo;
import static io.github.dfauth.dormant.st8.TestEvent.*;
import static io.github.dfauth.dormant.st8.TestState.*;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class StateMachineTest {

    private Queue<String> q = new ArrayDeque<>();
    private AtomicBoolean toggle = new AtomicBoolean(true);

    @Test
    public void testIt() {
        try {
            /**
             *        A
             *      /  \
             *    A1   A2
             *   /      \
             *  B---B1---C
             *   \      /
             *    B2  C1
             *     \ /
             *      D
             */
            StateMachine.StateMachineBuilder<TestState, StateMachineTest> builder = StateMachine.<TestState, StateMachineTest>builder();
            assertNotNull(builder);

            builder.context(this)
                    .initial(A, t -> t.q.offer("onEntry(A)"), t -> t.q.offer("onExit(A)"))
                    .state(B, t -> t.q.offer("onEntry(B)"), t -> t.q.offer("onExit(B)"))
                    .state(C, t -> t.q.offer("onEntry(C)"), t -> t.q.offer("onExit(C)"))
                    .state(D, t -> t.q.offer("onEntry(D)"), t -> t.q.offer("onExit(D)"))
                    .whenInState(A, onEvent(A1), ctx -> ctx.toggle.get(), transitionTo(B))
                    .whenInState(A, onEvent(A2), transitionTo(C))
                    .whenInState(B, onEvent(B1), transitionTo(C))
                    .whenInState(B, onEvent(B2), transitionTo(D))
                    .whenInState(C, onEvent(C1), transitionTo(D))
            ;

            {
                StateMachine<TestState, StateMachineTest> stateMachine = builder.build();
                assertEquals(A, stateMachine.getState().payload());
            }

            {
                StateMachine<TestState, StateMachineTest> stateMachine = builder.build();
                assertEquals(B, stateMachine.onEvent(A1).getState().payload());
                assertEquals("onExit(A)", q.poll());
                assertEquals("onEntry(B)", q.poll());
                assertTrue(q.isEmpty());
            }

            {
                StateMachine<TestState, StateMachineTest> stateMachine = builder.build();
                assertEquals(B, stateMachine.onEvent(A1).getState().payload());
                assertEquals("onExit(A)", q.poll());
                assertEquals("onEntry(B)", q.poll());
                assertTrue(q.isEmpty());
                assertEquals(C, stateMachine.onEvent(B1).getState().payload());
                assertEquals("onExit(B)", q.poll());
                assertEquals("onEntry(C)", q.poll());
                assertTrue(q.isEmpty());
                assertEquals(D, stateMachine.onEvent(C1).getState().payload());
                assertEquals("onExit(C)", q.poll());
                assertEquals("onEntry(D)", q.poll());
                assertTrue(q.isEmpty());
            }
            {
                StateMachine<TestState, StateMachineTest> stateMachine = builder.build();
                assertEquals(B, stateMachine.onEvent(A1).getState().payload());
                assertEquals("onExit(A)", q.poll());
                assertEquals("onEntry(B)", q.poll());
                assertTrue(q.isEmpty());
                assertEquals(D, stateMachine.onEvent(B2).getState().payload());
                assertEquals("onExit(B)", q.poll());
                assertEquals("onEntry(D)", q.poll());
                assertTrue(q.isEmpty());
            }
            {
                StateMachine<TestState, StateMachineTest> stateMachine = builder.build();
                assertEquals(C, stateMachine.onEvent(A2).getState().payload());
                assertEquals("onExit(A)", q.poll());
                assertEquals("onEntry(C)", q.poll());
                assertTrue(q.isEmpty());
                assertEquals(D, stateMachine.onEvent(C1).getState().payload());
                assertEquals("onExit(C)", q.poll());
                assertEquals("onEntry(D)", q.poll());
                assertTrue(q.isEmpty());
            }

            {
                toggle.set(false);
                StateMachine<TestState, StateMachineTest> stateMachine = builder.build();
                assertEquals(A, stateMachine.onEvent(A1).getState().payload());
                assertTrue(q.isEmpty());
                toggle.set(true);
                assertEquals(B, stateMachine.onEvent(A1).getState().payload());
                assertEquals("onExit(A)", q.poll());
                assertEquals("onEntry(B)", q.poll());
                assertTrue(q.isEmpty());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

}
