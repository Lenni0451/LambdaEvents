package net.lenni0451.lambdaevents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static net.lenni0451.lambdaevents.TestManager.DATA_SOURCE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SuperRegisterTest {

    private boolean calledThis;
    private boolean calledSuper;

    @BeforeEach
    public void reset() {
        this.calledThis = false;
        this.calledSuper = false;
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void withoutSuper(final LambdaManager manager) {
        manager.register(new ThisClass());
        manager.call("Test");

        assertTrue(this.calledThis);
        assertFalse(this.calledSuper);
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void withSuper(final LambdaManager manager) {
        manager.setRegisterSuperHandler(true);
        manager.register(new ThisClass());
        manager.call("Test");

        assertTrue(this.calledThis);
        assertTrue(this.calledSuper);
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void withSuperUnregister(final LambdaManager manager) {
        ThisClass instance = new ThisClass();

        manager.setRegisterSuperHandler(true);
        manager.register(instance);
        manager.call("Test");

        assertTrue(this.calledThis);
        assertTrue(this.calledSuper);

        this.reset();
        manager.unregister(instance);
        manager.call("Test");

        assertFalse(this.calledThis);
        assertFalse(this.calledSuper);
    }


    public class ThisClass extends SuperClass {
        @EventHandler
        public void testThis(final String event) {
            SuperRegisterTest.this.calledThis = true;
        }
    }

    public class SuperClass {
        @EventHandler
        public void testSuper(final String event) {
            SuperRegisterTest.this.calledSuper = true;
        }
    }

}
