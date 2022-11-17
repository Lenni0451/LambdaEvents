package test;

import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ReflectionGenerator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DirectRunnableTest {

    public static void main(String[] args) {
        LambdaManager lm = new LambdaManager(new ConcurrentHashMap<>(), CopyOnWriteArrayList::new, new ReflectionGenerator());
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
