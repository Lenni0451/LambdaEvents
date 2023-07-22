package test;

import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;

import java.lang.invoke.MethodHandles;

public class Test {

    public static void main(String[] args) throws Throwable {
//        LambdaManager lm = LambdaManager.threadSafe(new ReflectionGenerator());
//        LambdaManager lm = LambdaManager.threadSafe(new MethodHandleGenerator(MethodHandles.lookup()));
        LambdaManager lm = LambdaManager.threadSafe(new LambdaMetaFactoryGenerator(MethodHandles.lookup()));
        Test test = new Test();

        lm.register(test);
        lm.register(Test.class);

        lm.call("Test 1");
        System.out.println();

        lm.unregister(test);
        lm.call("Test 2");
        System.out.println();

        lm.unregister(Test.class);
        lm.call("Test 3");
        System.out.println();
    }

    @EventHandler
    public static void staticTest(final String s) {
        System.out.println("Static 1: " + s);
    }

    @EventHandler(events = String.class)
    public static void staticTest() {
        System.out.println("Static 2");
    }

    @EventHandler
    public void test(final String s) {
        System.out.println("Instance 1: " + s);
    }

    @EventHandler(events = String.class)
    public void test() {
        System.out.println("Instance 2");
    }

}
