package net.lenni0451.lambdaevents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static net.lenni0451.lambdaevents.TestManager.DATA_SOURCE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnregisterAllTest {

    private boolean calledObject = false;
    private boolean calledString = false;
    private boolean calledInteger = false;

    @BeforeEach
    void reset() {
        this.calledObject = false;
        this.calledString = false;
        this.calledInteger = false;
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void test(final LambdaManager manager) {
        manager.register(this);
        manager.call(new Object());
        manager.call("Test");
        manager.call(1);
        assertTrue(this.calledObject);
        assertTrue(this.calledString);
        assertTrue(this.calledInteger);

        this.reset();
        manager.unregisterAll(String.class);
        manager.call(new Object());
        manager.call("Test");
        manager.call(1);
        assertTrue(this.calledObject);
        assertFalse(this.calledString);
        assertTrue(this.calledInteger);

        this.reset();
        manager.unregisterAll(Object.class, UnregisterAllTest.class::equals);
        manager.call(new Object());
        manager.call("Test");
        manager.call(1);
        assertFalse(this.calledObject);
        assertFalse(this.calledString);
        assertTrue(this.calledInteger);
    }


    @EventHandler
    public void onObject(final Object event) {
        this.calledObject = true;
    }

    @EventHandler
    public void onString(final String event) {
        this.calledString = true;
    }

    @EventHandler
    public void onInteger(final Integer event) {
        this.calledInteger = true;
    }

}
