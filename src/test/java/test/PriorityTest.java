package test;

import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.LambdaManager;

public class PriorityTest {

    public static void main(String[] args) {
        LambdaManager.g().register(PriorityTest.class);
        LambdaManager.g().call("");
    }

    @EventHandler(priority = -1)
    private static void last(String s) {
        System.out.println("I should be last");
    }

    @EventHandler(priority = 0)
    private static void middle(String s) {
        System.out.println("I should be second");
    }

    @EventHandler(priority = 1)
    private static void first(String s) {
        System.out.println("I should be first");
    }

}
