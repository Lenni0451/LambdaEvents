package test;

import net.lenni0451.le.EventHandler;
import net.lenni0451.le.LambdaManager;

import java.util.function.Consumer;

public class Test {

    public static void main(String[] args) {
        System.out.println("Register");
        LambdaManager.g().register(Test.class);
        LambdaManager.g().call(new Integer(1337));
        LambdaManager.g().call("Test");
        System.out.println();

        System.out.println("Unregister Integer");
        LambdaManager.g().unregister(Integer.class, Test.class);
        LambdaManager.g().call(new Integer(1337));
        LambdaManager.g().call("Test");
        System.out.println();

        System.out.println("Unregister");
        LambdaManager.g().unregister(Test.class);
        LambdaManager.g().call(new Integer(1337));
        LambdaManager.g().call("Test");
        System.out.println();
    }

    @EventHandler(priority = 120, eventClasses = {Integer.class, String.class})
    public static Consumer<Object> testBoth = o -> System.out.println("Both Consumer: " + o.getClass().getName() + " " + o);

    @EventHandler(priority = 120, eventClasses = Integer.class)
    public static Consumer<Object> testInteger = o -> System.out.println("Integer Consumer: " + o.getClass().getName() + " " + o);

    @EventHandler(priority = 120, eventClasses = String.class)
    public static Consumer<Object> testString = o -> System.out.println("String Consumer: " + o.getClass().getName() + " " + o);

}
