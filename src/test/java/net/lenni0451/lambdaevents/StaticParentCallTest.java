package net.lenni0451.lambdaevents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.Serializable;

import static net.lenni0451.lambdaevents.TestManager.DATA_SOURCE;
import static net.lenni0451.lambdaevents.TestManager.throwingExceptionHandler;
import static org.junit.jupiter.api.Assertions.*;

public class StaticParentCallTest {

    private static boolean calledObject = false;
    private static boolean calledSerializable = false;
    private static boolean calledThrowable = false;
    private static boolean calledException = false;
    private static boolean calledRuntimeException = false;

    @BeforeEach
    void reset() {
        calledObject = false;
        calledSerializable = false;
        calledThrowable = false;
        calledException = false;
        calledRuntimeException = false;
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void call(final LambdaManager manager) {
        assertDoesNotThrow(() -> manager.register(StaticParentCallTest.class));
        manager.setExceptionHandler(throwingExceptionHandler());
        manager.call(new RuntimeException());

        assertFalse(calledObject);
        assertFalse(calledSerializable);
        assertFalse(calledThrowable);
        assertFalse(calledException);
        assertTrue(calledRuntimeException);
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void callParents(final LambdaManager manager) {
        assertDoesNotThrow(() -> manager.register(StaticParentCallTest.class));
        manager.setExceptionHandler(throwingExceptionHandler());
        manager.callParents(new RuntimeException());

        assertTrue(calledObject);
        assertTrue(calledSerializable);
        assertTrue(calledThrowable);
        assertTrue(calledException);
        assertTrue(calledRuntimeException);
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void alwaysCallParents(final LambdaManager manager) {
        assertDoesNotThrow(() -> manager.register(StaticParentCallTest.class));
        manager.setExceptionHandler(throwingExceptionHandler());
        manager.setAlwaysCallParents(true);
        manager.callParents(new RuntimeException());

        assertTrue(calledObject);
        assertTrue(calledSerializable);
        assertTrue(calledThrowable);
        assertTrue(calledException);
        assertTrue(calledRuntimeException);
    }


    @EventHandler
    public static void onObject(final Object o) {
        calledObject = true;
    }

    @EventHandler
    public static void onSerializable(final Serializable s) {
        calledSerializable = true;
    }

    @EventHandler
    public static void onThrowable(final Throwable t) {
        calledThrowable = true;
    }

    @EventHandler
    public static void onException(final Exception e) {
        calledException = true;
    }

    @EventHandler
    public static void onRuntimeException(final RuntimeException e) {
        calledRuntimeException = true;
    }

}
