package net.lenni0451.lambdaevents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;

import static net.lenni0451.lambdaevents.TestManager.DATA_SOURCE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DirectFieldTest {

    private final Runnable runnable = () -> this.calledRunnable = true;
    private final Consumer<String> consumer = s -> this.calledConsumer = true;
    private boolean calledRunnable = false;
    private boolean calledConsumer = false;

    @BeforeEach
    void reset() {
        this.calledRunnable = false;
        this.calledConsumer = false;
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void registered(final LambdaManager manager) {
        manager.register(this.runnable, String.class);
        manager.register(this.consumer, String.class);
        manager.call("Test");

        assertTrue(this.calledRunnable);
        assertTrue(this.calledConsumer);
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void unregister(final LambdaManager manager) {
        manager.register(this.runnable, String.class);
        manager.register(this.consumer, String.class);
        manager.unregister(this.runnable);
        manager.unregister(this.consumer);
        manager.call("Test");

        assertFalse(this.calledRunnable);
        assertFalse(this.calledConsumer);
    }

}
