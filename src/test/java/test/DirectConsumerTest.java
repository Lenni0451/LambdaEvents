package test;

import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ReflectionGenerator;

import java.util.function.Consumer;

public class DirectConsumerTest {

    public static void main(String[] args) {
        LambdaManager lm = LambdaManager.threadSafe(new ReflectionGenerator());
        Consumer<String> consumer = s -> System.out.println("Hello " + s);

        lm.register(consumer, (byte) 0, String.class);
        lm.call("Test 1");
        lm.unregister(consumer);
        lm.call("Test 2");

        lm.register(consumer, (byte) 0, String.class);
        lm.call("Test 3");
        lm.unregister(consumer, String.class);
        lm.call("Test 4");
    }

}
