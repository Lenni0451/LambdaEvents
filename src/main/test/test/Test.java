package test;

import net.lenni0451.le.LambdaHandler;
import net.lenni0451.le.LambdaManager;

public class Test {

    public static void main(String[] args) {
        LambdaManager.global().register(Test.class);
        LambdaManager.global().call("1337");
        LambdaManager.global().call(1337);
    }

    @LambdaHandler
    public static void test(final String s) {
        System.out.println("String: " + s);
    }

    @LambdaHandler
    public static void test(final Integer i) {
        System.out.println("Integer: " + i);
    }

}
