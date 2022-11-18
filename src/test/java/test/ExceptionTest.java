package test;

import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;

public class ExceptionTest {

    public static void main(String[] args) {
        LambdaManager lm = LambdaManager.basic(new LambdaMetaFactoryGenerator());
        lm.register(ExceptionTest.class);
        lm.call("Test");
    }

    @EventHandler
    public static void test(String s) {
        throw new RuntimeException(s);
    }

}
