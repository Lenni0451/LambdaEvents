package net.lenni0451.lambdaevents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static net.lenni0451.lambdaevents.TestManager.DATA_SOURCE;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExceptionTest {

    private boolean calledBefore = false;
    private boolean calledExceptional = false;
    private boolean calledAfter = false;
    private boolean calledExceptionHandler = false;

    @BeforeEach
    public void reset() {
        this.calledBefore = false;
        this.calledExceptional = false;
        this.calledAfter = false;
        this.calledExceptionHandler = false;
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void test(final LambdaManager manager) {
        manager.register(this);
        manager.setExceptionHandler((handler, event, t) -> this.calledExceptionHandler = true);
        manager.call("Test");

        assertTrue(this.calledBefore);
        assertTrue(this.calledExceptional);
        assertTrue(this.calledAfter);
        assertTrue(this.calledExceptionHandler);
    }


    @EventHandler(priority = 1)
    public void before(final String event) {
        this.calledBefore = true;
    }

    @EventHandler
    public void exceptional(final String event) {
        this.calledExceptional = true;
        throw new RuntimeException();
    }

    @EventHandler(priority = -1)
    public void after(final String event) {
        this.calledAfter = true;
    }

}
