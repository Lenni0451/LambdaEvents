package test;

import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public class InternalLookupTest {

    public static void main(String[] args) throws Throwable {
//        LambdaManager lm = LambdaManager.threadSafe(new LambdaMetaFactoryGenerator(MethodHandles.lookup()));
        LambdaManager lm = LambdaManager.threadSafe(new LambdaMetaFactoryGenerator(getInternalLookup()));
        lm.register(TestListener.class);
        lm.call("Test");
    }

    public static MethodHandles.Lookup getInternalLookup() throws NoSuchFieldException, IllegalAccessException {
        Unsafe unsafe;
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        MethodHandles.lookup(); //Load the class for java 8
        f = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        return (MethodHandles.Lookup) unsafe.getObject(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f));
    }


    private static class TestListener {
        @EventHandler
        private static void test(final String s) {
            System.out.println(s);
        }
    }

}
