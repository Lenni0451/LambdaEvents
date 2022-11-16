package test;

import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ReflectionGenerator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@SuppressWarnings("rawtypes")
public class FieldTest {

    @EventHandler
    public static final Consumer<String> staticConsumer1 = s -> System.out.println("Static 1 " + s);

    @EventHandler(events = String.class)
    public static final Consumer staticConsumer2 = s -> System.out.println("Static 2 " + s);

    @EventHandler(events = String.class)
    public static final Runnable staticRunnable1 = () -> System.out.println("Static Runnable");

    @EventHandler
    public final Consumer<String> virtualConsumer1 = s -> System.out.println("Virtual 1 " + s);

    @EventHandler(events = String.class)
    public final Consumer virtualConsumer2 = s -> System.out.println("Virtual 1 " + s);

    @EventHandler(events = String.class)
    public final Runnable virtualRunnable1 = () -> System.out.println("Virtual Runnable");

    public static void main(String[] args) {
        LambdaManager lm = new LambdaManager(new ConcurrentHashMap<>(), CopyOnWriteArrayList::new, new ReflectionGenerator());
        FieldTest test = new FieldTest();

        lm.register(FieldTest.class);
        lm.register(test);
        lm.call("Test 1");

        System.out.println();
        lm.unregister(FieldTest.class);
        lm.call("Test 2");

        System.out.println();
        lm.unregister(test);
        lm.call("Test 3");
    }

}
