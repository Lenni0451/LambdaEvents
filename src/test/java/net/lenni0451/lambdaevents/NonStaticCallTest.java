package net.lenni0451.lambdaevents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;

import static net.lenni0451.lambdaevents.TestManager.DATA_SOURCE;
import static org.junit.jupiter.api.Assertions.*;

public class NonStaticCallTest {

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
        manager.call("Test");

        assertTrue(this.calledVirtualRunnable);
        assertTrue(this.calledDirectConsumer);
        assertTrue(this.calledVirtualConsumer);
        assertTrue(this.calledDirectMethod);
        assertTrue(this.calledVirtualMethod);
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void notRegistered(final LambdaManager manager) {
        manager.call("Test");

        assertFalse(this.calledVirtualRunnable);
        assertFalse(this.calledDirectConsumer);
        assertFalse(this.calledVirtualConsumer);
        assertFalse(this.calledDirectMethod);
        assertFalse(this.calledVirtualMethod);
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void unregister(final LambdaManager manager) {
        assertDoesNotThrow(() -> manager.register(this));
        manager.unregister(this);
        manager.call("Test");

        assertFalse(this.calledVirtualRunnable);
        assertFalse(this.calledDirectConsumer);
        assertFalse(this.calledVirtualConsumer);
        assertFalse(this.calledDirectMethod);
        assertFalse(this.calledVirtualMethod);
    }


    @EventHandler(events = String.class)
    public final Runnable virtualRunnable = () -> this.calledVirtualRunnable = true;
    @EventHandler
    public final Consumer<String> directConsumer = event -> this.calledDirectConsumer = true;
    @EventHandler(events = String.class)
    public final Consumer<Object> virtualConsumer = event -> this.calledVirtualConsumer = true;

    @EventHandler
    public void directMethod(final String event) {
        this.calledDirectMethod = true;
    }

    @EventHandler(events = String.class)
    public void virtualMethod() {
        this.calledVirtualMethod = true;
    }

}
