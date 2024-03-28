package net.lenni0451.lambdaevents;

import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;
import net.lenni0451.reflect.JavaBypass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PrivateHandlerTest {

    private boolean handled = false;

    @BeforeEach
    void setUp() {
        this.handled = false;
    }

    @Test
    void callFail() {
        LambdaManager manager = LambdaManager.basic(new LambdaMetaFactoryGenerator());
        manager.setExceptionHandler(IExceptionHandler.throwing());
        assertThrows(IllegalAccessException.class, () -> manager.registerSuper(new ExtendingHandlerClass()));

        assertFalse(this.handled);
        manager.call("Test");
        assertFalse(this.handled);
    }

    @Test
    void callSuccess() {
        LambdaManager manager = LambdaManager.basic(new LambdaMetaFactoryGenerator(JavaBypass.TRUSTED_LOOKUP));
        manager.setExceptionHandler(IExceptionHandler.throwing());
        assertDoesNotThrow(() -> manager.registerSuper(new ExtendingHandlerClass()));

        assertFalse(this.handled);
        manager.call("Test");
        assertTrue(this.handled);
    }


    private class PrivateHandlerClass {
        @EventHandler(events = String.class)
        private void handle() {
            PrivateHandlerTest.this.handled = true;
        }
    }

    public class ExtendingHandlerClass extends PrivateHandlerClass {
    }

}
