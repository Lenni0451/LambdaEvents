package test;

import net.lenni0451.le.LambdaHandler;
import net.lenni0451.le.LambdaManager;

public class Test {

    public static void main(String[] args) {
        LambdaManager.global().register(Test.class);
        LambdaManager.global().call("Hi Kevin 1");
        LambdaManager.global().unregister(Test.class);
        LambdaManager.global().call("Hi Kevin 2");
        LambdaManager.global().register(Test.class);
        LambdaManager.global().call("Hi Kevin 3");
    }

    @LambdaHandler
    public static void test(final String s) {
        System.out.println(s);
    }

}
