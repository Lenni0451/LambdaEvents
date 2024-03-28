package net.lenni0451.lambdaevents;

import net.lenni0451.lambdaevents.generator.ReflectionGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OverriddenPrivateSuperTest {

    private int calledSuper;
    private int calledExtending;

    @BeforeEach
    public void reset() {
        this.calledSuper = 0;
        this.calledExtending = 0;
    }

    @Test
    void overriddenPrivateHandler() {
        LambdaManager manager = LambdaManager.basic(new ReflectionGenerator());
        manager.registerSuper(new OverriddenHandler());
        manager.call("Test");

        assertEquals(this.calledSuper, 1);
        assertEquals(this.calledExtending, 1);
    }


    public class BaseClass {
        @EventHandler
        private void onEvent(final String s) {
            OverriddenPrivateSuperTest.this.calledSuper++;
        }
    }

    public class OverriddenHandler extends BaseClass {
        @EventHandler
        private void onEvent(final String s) {
            OverriddenPrivateSuperTest.this.calledExtending++;
        }
    }

}
