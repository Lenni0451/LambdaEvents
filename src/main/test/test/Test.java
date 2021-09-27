package test;

import net.lenni0451.le.LambdaHandler;
import net.lenni0451.le.LambdaManager;

public class Test {

    public static void main(String[] args) {
        LambdaManager.global().register(Test.class);
//        LambdaManager.global().register(new Test());
        LambdaManager.global().call("Hi Kevin"/**/);
    }

    @LambdaHandler
    public static void test(final String s) {
        System.out.println(s);
    }

}
