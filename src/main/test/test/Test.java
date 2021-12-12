package test;

import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.LambdaManager;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Test {

    public static final Map<String, Object> _LAMBDA_CACHE = new HashMap<>();

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

    @EventHandler(eventClasses = {Integer.class, String.class})
    public static Consumer<Object> testBoth = o -> System.out.println("Both Consumer: " + o.getClass().getName() + " " + o);

    @EventHandler(eventClasses = Integer.class)
    public static Consumer<Object> testInteger = o -> System.out.println("Integer Consumer: " + o.getClass().getName() + " " + o);

    @EventHandler(eventClasses = String.class)
    public static Consumer<Object> testString = o -> System.out.println("String Consumer: " + o.getClass().getName() + " " + o);

}
