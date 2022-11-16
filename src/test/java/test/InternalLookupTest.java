package test;

import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InternalLookupTest {

    public static void main(String[] args) throws Throwable {
        Unsafe unsafe;
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        unsafe = (Unsafe) f.get(null);

        MethodHandles.lookup(); //Load the class for java 8
        MethodHandles.Lookup internalLookup;
        f = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        internalLookup = (MethodHandles.Lookup) unsafe.getObject(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f));

//        LambdaManager lm = new LambdaManager(new ConcurrentHashMap<>(), CopyOnWriteArrayList::new, new LambdaMetaFactoryGenerator(MethodHandles.lookup()));
        LambdaManager lm = new LambdaManager(new ConcurrentHashMap<>(), CopyOnWriteArrayList::new, new LambdaMetaFactoryGenerator(internalLookup));
        lm.register(TestListener.class);
        lm.call("Test");
    }


    private static class TestListener {
        @EventHandler
        private static void test(final String s) {
            System.out.println(s);
        }
    }

}
