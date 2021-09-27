package test;

import net.lenni0451.le.LambdaHandler;
import net.lenni0451.le.LambdaManager;

public class PriorityTest {

    public static void main(String[] args) {
        LambdaManager.global().register(PriorityTest.class);
        LambdaManager.global().call("");
    }

    @LambdaHandler(priority = -1)
    public static void last(String s) {
        System.out.println("I should be last");
    }

    @LambdaHandler(priority = 0)
    public static void middle(String s) {
        System.out.println("I should be second");
    }

    @LambdaHandler(priority = 1)
    public static void first(String s) {
        System.out.println("I should be first");
    }

}
