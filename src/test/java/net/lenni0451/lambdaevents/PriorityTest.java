package net.lenni0451.lambdaevents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;

import static net.lenni0451.lambdaevents.TestManager.DATA_SOURCE;
import static org.junit.jupiter.api.Assertions.*;

public class PriorityTest {

    private boolean calledVirtualRunnable = false;
    private boolean calledDirectConsumer = false;
    private boolean calledVirtualConsumer = false;
    private boolean calledDirectMethod = false;
    private boolean calledVirtualMethod = false;

    @BeforeEach
    void reset() {
        this.calledVirtualRunnable = false;
        this.calledDirectConsumer = false;
        this.calledVirtualConsumer = false;
        this.calledDirectMethod = false;
        this.calledVirtualMethod = false;
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void registered(final LambdaManager manager) {
        assertDoesNotThrow(() -> manager.register(this));
        manager.setExceptionHandler(IExceptionHandler.throwing());
        manager.call("Test");

        assertTrue(this.calledVirtualRunnable);
        assertTrue(this.calledDirectConsumer);
        assertTrue(this.calledVirtualConsumer);
        assertTrue(this.calledDirectMethod);
        assertTrue(this.calledVirtualMethod);
    }


    @EventHandler(priority = 1, events = String.class)
    public final Runnable virtualRunnable = () -> {
        this.calledVirtualRunnable = true;

        assertFalse(this.calledDirectConsumer);
        assertFalse(this.calledVirtualConsumer);
        assertFalse(this.calledDirectMethod);
        assertTrue(this.calledVirtualMethod);
    };
    @EventHandler(priority = -1)
    public final Consumer<String> directConsumer = event -> {
        this.calledDirectConsumer = true;

        assertTrue(this.calledVirtualRunnable);
        assertTrue(this.calledVirtualConsumer);
        assertFalse(this.calledDirectMethod);
        assertTrue(this.calledVirtualMethod);
    };
    @EventHandler(events = String.class)
    public final Consumer<Object> virtualConsumer = event -> {
        this.calledVirtualConsumer = true;

        assertTrue(this.calledVirtualRunnable);
        assertFalse(this.calledDirectConsumer);
        assertFalse(this.calledDirectMethod);
        assertTrue(this.calledVirtualMethod);
    };

    @EventHandler(priority = Integer.MIN_VALUE)
    public void directMethod(final String event) {
        this.calledDirectMethod = true;

        assertTrue(this.calledVirtualRunnable);
        assertTrue(this.calledDirectConsumer);
        assertTrue(this.calledVirtualConsumer);
        assertTrue(this.calledVirtualMethod);
    }

    @EventHandler(priority = Integer.MAX_VALUE, events = String.class)
    public void virtualMethod() {
        this.calledVirtualMethod = true;

        assertFalse(this.calledVirtualRunnable);
        assertFalse(this.calledDirectConsumer);
        assertFalse(this.calledVirtualConsumer);
        assertFalse(this.calledDirectMethod);
    }

}
