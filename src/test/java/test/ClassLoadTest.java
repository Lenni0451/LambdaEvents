package test;

import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClassLoadTest {

    public static void main(String[] args) throws Throwable {
        String className = "test.Test";
        InputStream is = ClassLoadTest.class.getClassLoader().getResourceAsStream(className.replace(".", "/") + ".class");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len;
        while ((len = is.read(buf)) != -1) baos.write(buf, 0, len);

        ClassLoader cl = new ClassLoader() {
            public Class<?> define(final String name, final byte[] bytes) {
                return this.defineClass(name, bytes, 0, bytes.length);
            }
        };
        Method define = cl.getClass().getDeclaredMethod("define", String.class, byte[].class);
        define.setAccessible(true);
        Class<?> clazz = (Class<?>) define.invoke(cl, className, baos.toByteArray());
        Object instance = clazz.getDeclaredConstructor().newInstance();

        LambdaManager lm = new LambdaManager(new ConcurrentHashMap<>(), CopyOnWriteArrayList::new, new LambdaMetaFactoryGenerator(MethodHandles.lookup()));
//        LambdaManager lm = new LambdaManager(new ConcurrentHashMap<>(), CopyOnWriteArrayList::new, new MethodHandleGenerator(MethodHandles.lookup()));
//        LambdaManager lm = new LambdaManager(new ConcurrentHashMap<>(), CopyOnWriteArrayList::new, new ReflectionGenerator());

//        lm.register(new Test());
        lm.register(clazz);
        lm.register(instance);
        lm.call("Test");
    }

}
