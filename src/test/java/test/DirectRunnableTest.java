package test;

import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ReflectionGenerator;

public class DirectRunnableTest {

    public static void main(String[] args) {
        LambdaManager lm = LambdaManager.threadSafe(new ReflectionGenerator());
        Runnable runnable = () -> System.out.println("Hello World");

        lm.register(runnable, (byte) 0, String.class);
        lm.call("Test");
        lm.unregister(runnable);
        lm.call("Test");

        lm.register(runnable, (byte) 0, String.class);
        lm.call("Test");
        lm.unregister(runnable, String.class);
        lm.call("Test");
    }

}
