package test;

import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;

public class SuperRegister {

    public static void main(String[] args) {
        LambdaManager lm = LambdaManager.threadSafe(new LambdaMetaFactoryGenerator());
        Test2 t = new Test2();

        lm.register(t);
        lm.call("Run 1");
        lm.unregister(t);

        System.out.println();
        lm.setRegisterSuperHandler(true);
        lm.register(t);
        lm.call("Run 2");

        System.out.println();
        lm.unregister(t);
        lm.call("Run 3");
    }

    public static class Test1 {
        @EventHandler
        public void test1(String s) {
            System.out.println("Test 1: " + s);
        }
    }

    public static class Test2 extends Test1 {
        @EventHandler
        public void test2(String s) {
            System.out.println("Test 2: " + s);
        }
    }

}
