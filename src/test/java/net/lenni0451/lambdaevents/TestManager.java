package net.lenni0451.lambdaevents;

import lombok.SneakyThrows;
import net.lenni0451.lambdaevents.generator.ASMGenerator;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;
import net.lenni0451.lambdaevents.generator.MethodHandleGenerator;
import net.lenni0451.lambdaevents.generator.ReflectionGenerator;

public class TestManager {

    public static final String DATA_SOURCE = "net.lenni0451.lambdaevents.TestManager#make";

    public static LambdaManager[] make() {
        return new LambdaManager[]{
                LambdaManager.basic(new ReflectionGenerator()),
                LambdaManager.basic(new MethodHandleGenerator()),
                LambdaManager.basic(new LambdaMetaFactoryGenerator()),
                LambdaManager.basic(new ASMGenerator()),
        };
    }

    public static IExceptionHandler throwingExceptionHandler() {
        return new ThrowingExceptionHandler();
    }


    private static class ThrowingExceptionHandler implements IExceptionHandler {
        @Override
        @SneakyThrows
        public void handle(AHandler handler, Object event, Throwable t) {
            throw t;
        }
    }

}
