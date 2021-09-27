package test;

import net.lenni0451.le.LambdaHandler;
import net.lenni0451.le.LambdaManager;

public class Test {

    public static void main(String[] args) {
        LambdaManager.global().register(Test.class);
        LambdaManager.global().call("1337");
        LambdaManager.global().call(1337);
    }

    @LambdaHandler(priority = 1)
    public static void test1(final String s) {
        System.out.println("String1: " + s);
        throw LambdaManager.STOP;
    }

    @LambdaHandler
    public static void test2(final String s) {
        System.out.println("String2: " + s);
    }

    @LambdaHandler
    public static void test(final Integer i) {
        System.out.println("Integer: " + i);
    }

}
