package io.github.dfauth.trycatch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.github.dfauth.trycatch.TryCatch.tryCatch;

@Slf4j
public class Timer {

    private static boolean enabled;
    public static Consumer<Elapsed> consumer = elapsed -> {
        List<String> msgs = new ArrayList<>();
        elapsed.visit(e -> {
            String spacer = IntStream.range(0, e.depth()).mapToObj(_ -> "  ").collect(Collectors.joining());
            msgs.add(String.format("%d. %s %s elapsed %.1f msec accumulated: %.1f", e.depth(), spacer, e.label, e.elapsedMsec(),
                    e.nested.stream().mapToDouble(Elapsed::elapsedMsec).sum()));
        });
        log.info(msgs.stream().collect(Collectors.joining("\n\t", "\n\t", "")));
    };

    static {
        enabled = Boolean.valueOf(System.getProperty(Timer.class.getName()+".enabled", "false"));
    }

    private static final ThreadLocal<Timer> tLocal = ThreadLocal.withInitial(Timer::new);

    public static void timed(ExceptionalRunnable execution) {
        timed(generateLabel(3), (Callable<Void>) () -> {
            execution.run();
            return null;
        });
    }

    public static <T> T timed(Callable<T> execution) {
        return timed(generateLabel(3), execution);
    }

    public static void timed(String label, ExceptionalRunnable execution) {
        timed(label, (Callable<Void>) () -> {
            execution.run();
            return null;
        });
    }

    public static <T> T timed(String label, Callable<T> execution) {
        return enabled ? tLocal.get().invoke(label, execution) : tryCatch(execution);
    }

    private static String generateLabel(int i) {
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[i];
        return String.format("%s.%s:%d",stackTraceElement.getClassName(), stackTraceElement.getMethodName(), stackTraceElement.getLineNumber());
    }

    private Elapsed root;
    private Elapsed current;

    protected  <T> T invoke(String label, Callable<T> execution) {
        var tmp = new Elapsed(this.current, label);
        if(this.root == null) {
            this.root = tmp;
        } else {
            this.current.nested.add(tmp);
        }
        this.current = tmp;
        try(tmp) {
            return execution.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            this.current = this.current.parent;
            if(this.current == null) {
                tLocal.remove();
            }
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    public static class Elapsed implements AutoCloseable {

        private final Elapsed parent;
        private final String label;
        private final long start;
        private long stop;
        private final List<Elapsed> nested;

        public Elapsed(Elapsed parent, String label) {
            this(parent, label, System.nanoTime(), new ArrayList<>());
        }

        @Override
        public void close() {
            stop = System.nanoTime();
            if(this.parent == null) {
                consumer.accept(this);
            }
        }

        private void visit(Consumer<Elapsed> consumer) {
            consumer.accept(this);
            nested.stream().forEach(e -> {
                e.visit(consumer);
            });
        }

        public int depth() {
            return parent == null ? 0 : parent.depth()+1;
        }

        public double elapsedMsec() {
            return (stop - start) / 1_000_000;
        }
    }
}