package net.lenni0451.lambdaevents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;

import static net.lenni0451.lambdaevents.TestManager.DATA_SOURCE;
import static org.junit.jupiter.api.Assertions.*;

public class StaticCallTest {

    private static boolean calledVirtualRunnable = false;
    private static boolean calledDirectConsumer = false;
    private static boolean calledVirtualConsumer = false;
    private static boolean calledDirectMethod = false;
    private static boolean calledVirtualMethod = false;

    @BeforeEach
    void reset() {
        calledVirtualRunnable = false;
        calledDirectConsumer = false;
        calledVirtualConsumer = false;
        calledDirectMethod = false;
        calledVirtualMethod = false;
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void registered(final LambdaManager manager) {
        assertDoesNotThrow(() -> manager.register(StaticCallTest.class));
        manager.call("Test");

        assertTrue(calledVirtualRunnable);
        assertTrue(calledDirectConsumer);
        assertTrue(calledVirtualConsumer);
        assertTrue(calledDirectMethod);
        assertTrue(calledVirtualMethod);
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void notRegistered(final LambdaManager manager) {
        manager.call("Test");

        assertFalse(calledVirtualRunnable);
        assertFalse(calledDirectConsumer);
        assertFalse(calledVirtualConsumer);
        assertFalse(calledDirectMethod);
        assertFalse(calledVirtualMethod);
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void unregister(final LambdaManager manager) {
        assertDoesNotThrow(() -> manager.register(StaticCallTest.class));
        manager.unregister(StaticCallTest.class);
        manager.call("Test");

        assertFalse(calledVirtualRunnable);
        assertFalse(calledDirectConsumer);
        assertFalse(calledVirtualConsumer);
        assertFalse(calledDirectMethod);
        assertFalse(calledVirtualMethod);
    }


    @EventHandler(events = String.class)
    public static final Runnable virtualRunnable = () -> calledVirtualRunnable = true;
    @EventHandler
    public static final Consumer<String> directConsumer = event -> calledDirectConsumer = true;
    @EventHandler(events = String.class)
    public static final Consumer<Object> virtualConsumer = event -> calledVirtualConsumer = true;

    @EventHandler
    public static void directMethod(final String event) {
        calledDirectMethod = true;
    }

    @EventHandler(events = String.class)
    public static void virtualMethod() {
        calledVirtualMethod = true;
    }

}
