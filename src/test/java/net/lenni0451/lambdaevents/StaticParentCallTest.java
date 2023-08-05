package net.lenni0451.lambdaevents;

import net.lenni0451.lambdaevents.utils.EventUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static net.lenni0451.lambdaevents.TestManager.DATA_SOURCE;
import static org.junit.jupiter.api.Assertions.*;

public class StaticParentCallTest {

    private static boolean calledObject = false;
    private static boolean calledThrowable = false;
    private static boolean calledException = false;
    private static boolean calledRuntimeException = false;

    @BeforeEach
    void reset() {
        calledObject = false;
        calledThrowable = false;
        calledException = false;
        calledRuntimeException = false;
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void call(final LambdaManager manager) {
        assertDoesNotThrow(() -> manager.register(StaticParentCallTest.class));
        manager.setExceptionHandler((handler, event, t) -> EventUtils.sneak(t));
        manager.call(new RuntimeException());

        assertFalse(calledObject);
        assertFalse(calledThrowable);
        assertFalse(calledException);
        assertTrue(calledRuntimeException);
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void callParents(final LambdaManager manager) {
        assertDoesNotThrow(() -> manager.register(StaticParentCallTest.class));
        manager.setExceptionHandler((handler, event, t) -> EventUtils.sneak(t));
        manager.callParents(new RuntimeException());

        assertTrue(calledObject);
        assertTrue(calledThrowable);
        assertTrue(calledException);
        assertTrue(calledRuntimeException);
    }


    @EventHandler
    public static void onObject(final Object o) {
        calledObject = true;
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
