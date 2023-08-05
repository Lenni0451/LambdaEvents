package net.lenni0451.lambdaevents;

import net.lenni0451.lambdaevents.utils.EventUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static net.lenni0451.lambdaevents.TestManager.DATA_SOURCE;
import static org.junit.jupiter.api.Assertions.*;

public class NonStaticParentCallTest {

    private boolean calledObject = false;
    private boolean calledThrowable = false;
    private boolean calledException = false;
    private boolean calledRuntimeException = false;

    @BeforeEach
    void reset() {
        this.calledObject = false;
        this.calledThrowable = false;
        this.calledException = false;
        this.calledRuntimeException = false;
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void call(final LambdaManager manager) {
        assertDoesNotThrow(() -> manager.register(this));
        manager.setExceptionHandler((handler, event, t) -> EventUtils.sneak(t));
        manager.call(new RuntimeException());

        assertFalse(this.calledObject);
        assertFalse(this.calledThrowable);
        assertFalse(this.calledException);
        assertTrue(this.calledRuntimeException);
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void callParents(final LambdaManager manager) {
        assertDoesNotThrow(() -> manager.register(this));
        manager.setExceptionHandler((handler, event, t) -> EventUtils.sneak(t));
        manager.callParents(new RuntimeException());

        assertTrue(this.calledObject);
        assertTrue(this.calledThrowable);
        assertTrue(this.calledException);
        assertTrue(this.calledRuntimeException);
    }


    @EventHandler
    public void onObject(final Object o) {
        this.calledObject = true;
    }

    @EventHandler
    public void onThrowable(final Throwable t) {
        this.calledThrowable = true;
    }

    @EventHandler
    public void onException(final Exception e) {
        this.calledException = true;
    }

    @EventHandler
    public void onRuntimeException(final RuntimeException e) {
        this.calledRuntimeException = true;
    }

}
