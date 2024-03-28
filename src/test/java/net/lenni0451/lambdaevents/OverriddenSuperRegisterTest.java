package net.lenni0451.lambdaevents;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static net.lenni0451.lambdaevents.TestManager.DATA_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OverriddenSuperRegisterTest {

    private int calledSuper;
    private int calledExtending;

    @BeforeEach
    public void reset() {
        this.calledSuper = 0;
        this.calledExtending = 0;
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void overriddenHandler(final LambdaManager manager) {
        manager.registerSuper(new OverriddenHandler());
        manager.call("Test");

        assertEquals(this.calledSuper, 0); //should be 1 if it was possible to call the actual super method
        assertEquals(this.calledExtending, 1); //would be 2 if the super method wasn't filtered out
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void overriddenMethod(final LambdaManager manager) {
        manager.registerSuper(new OverriddenMethod());
        manager.call("Test");

        assertEquals(this.calledSuper, 0);
        assertEquals(this.calledExtending, 1);
    }

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void overriddenMethodWithoutSuper(final LambdaManager manager) {
        manager.register(new OverriddenMethod());
        manager.call("Test");

        assertEquals(this.calledSuper, 0);
        assertEquals(this.calledExtending, 0);
    }


    public class BaseClass {
        @EventHandler
        public void onEvent(final String s) {
            OverriddenSuperRegisterTest.this.calledSuper++;
        }
    }

    public class OverriddenHandler extends BaseClass {
        @Override
        @EventHandler
        public void onEvent(final String s) {
            OverriddenSuperRegisterTest.this.calledExtending++;
        }
    }

    public class OverriddenMethod extends BaseClass {
        @Override
        public void onEvent(final String s) {
            OverriddenSuperRegisterTest.this.calledExtending++;
        }
    }

}
