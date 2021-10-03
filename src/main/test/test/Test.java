package test;

import net.lenni0451.le.EventHandler;
import net.lenni0451.le.LambdaManager;

public class Test {

    public static void main(String[] args) {
        System.out.println(LambdaManager.STOP);

        LambdaManager.g().register(Test.class);
        LambdaManager.g().call("1337");
        LambdaManager.g().call(1337);
    }

    @EventHandler(priority = 1)
    public static void test1(final String s) {
        System.out.println("String1: " + s);
        throw LambdaManager.STOP;
    }

    @EventHandler
    public static void test2(final String s) {
        System.out.println("String2: " + s);
    }

    @EventHandler
    public static void test(final Integer i) {
        System.out.println("Integer: " + i);
    }

}
