package test;

import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ReflectionGenerator;

public class PriorityTest {

    public static void main(String[] args) {
        LambdaManager lm = LambdaManager.threadSafe(new ReflectionGenerator());
        lm.register(Class3.class);
        lm.register(Class2.class);
        lm.register(Class1.class);
        lm.call("Test");
    }

    private static class Class1 {
        @EventHandler(priority = 1)
        public static void onEvent(final String s) {
            System.out.println("Event 1");
        }
    }

    private static class Class2 {
        @EventHandler
        public static void onEvent(final String s) {
            System.out.println("Event 2");
        }
    }

    private static class Class3 {
        @EventHandler(priority = -1)
        public static void onEvent(final String s) {
            System.out.println("Event 3");
        }
    }

}
