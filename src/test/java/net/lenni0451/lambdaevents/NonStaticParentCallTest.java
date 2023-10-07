package net.lenni0451.lambdaevents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.Serializable;

import static net.lenni0451.lambdaevents.TestManager.DATA_SOURCE;
import static net.lenni0451.lambdaevents.TestManager.throwingExceptionHandler;
import static org.junit.jupiter.api.Assertions.*;

public class NonStaticParentCallTest {

    private boolean calledObject = false;
    private boolean calledSerializable = false;
    private boolean calledThrowable = false;
    private boolean calledException = false;
    private boolean calledRuntimeException = false;

    @BeforeEach
    void reset() {
        this.calledObject = false;
        this.calledSerializable = false;
        this.calledThrowable = false;
        this.calledException = false;
        this.calledRuntimeException = false;
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void call(final LambdaManager manager) {
        assertDoesNotThrow(() -> manager.register(this));
        manager.setExceptionHandler(throwingExceptionHandler());
        manager.call(new RuntimeException());

        assertFalse(this.calledObject);
        assertFalse(this.calledSerializable);
        assertFalse(this.calledThrowable);
        assertFalse(this.calledException);
        assertTrue(this.calledRuntimeException);
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void callParents(final LambdaManager manager) {
        assertDoesNotThrow(() -> manager.register(this));
        manager.setExceptionHandler(throwingExceptionHandler());
        manager.callParents(new RuntimeException());

        assertTrue(this.calledObject);
        assertTrue(this.calledSerializable);
        assertTrue(this.calledThrowable);
        assertTrue(this.calledException);
        assertTrue(this.calledRuntimeException);
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void alwaysCallParents(final LambdaManager manager) {
        assertDoesNotThrow(() -> manager.register(this));
        manager.setExceptionHandler(throwingExceptionHandler());
        manager.setAlwaysCallParents(true);
        manager.callParents(new RuntimeException());

        assertTrue(this.calledObject);
        assertTrue(this.calledSerializable);
        assertTrue(this.calledThrowable);
        assertTrue(this.calledException);
        assertTrue(this.calledRuntimeException);
    }


    @EventHandler
    public void onObject(final Object o) {
        this.calledObject = true;
    }

    @EventHandler
    public void onSerializable(final Serializable s) {
        this.calledSerializable = true;
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
