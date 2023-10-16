package net.lenni0451.lambdaevents;

import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;
import net.lenni0451.lambdaevents.test_classes.ExtendingHandlerClass;
import net.lenni0451.reflect.JavaBypass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PrivateHandlerTest {

    private ExtendingHandlerClass handler;

    @BeforeEach
    void setUp() {
        handler = new ExtendingHandlerClass();
    }

    @Test
    void callFail() {
        LambdaManager manager = LambdaManager.basic(new LambdaMetaFactoryGenerator());
        manager.setExceptionHandler(IExceptionHandler.throwing());
        assertThrows(IllegalAccessException.class, () -> manager.registerSuper(this.handler));

        assertFalse(this.handler.handled);
        manager.call("Test");
        assertFalse(this.handler.handled);
    }

    @Test
    void callSuccess() {
        LambdaManager manager = LambdaManager.basic(new LambdaMetaFactoryGenerator(JavaBypass.TRUSTED_LOOKUP));
        manager.setExceptionHandler(IExceptionHandler.throwing());
        assertDoesNotThrow(() -> manager.registerSuper(this.handler));

        assertFalse(this.handler.handled);
        manager.call("Test");
        assertTrue(this.handler.handled);
    }

}
