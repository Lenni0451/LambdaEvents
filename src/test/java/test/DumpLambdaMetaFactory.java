package test;

import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.LambdaMetaFactoryGenerator;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class DumpLambdaMetaFactory {

    public static void main(String[] args) throws Throwable {
        MethodHandles.Lookup internalLookup = InternalLookupTest.getInternalLookup();
        Class<?> innerClassLambdaMetafactory = Class.forName("java.lang.invoke.InnerClassLambdaMetafactory");
        Class<?> proxyClassesDumper = Class.forName("java.lang.invoke.ProxyClassesDumper");

        Object newDumper = internalLookup.findStatic(proxyClassesDumper, "getInstance", MethodType.methodType(proxyClassesDumper, String.class)).invoke("dump");
        internalLookup.findStaticSetter(innerClassLambdaMetafactory, "dumper", proxyClassesDumper).invoke(newDumper);

        LambdaManager lm = LambdaManager.threadSafe(new LambdaMetaFactoryGenerator(internalLookup));
        Class<?> privateClass = Class.forName("test.InternalLookupTest$TestListener");
        lm.register(privateClass);

        lm.call("Test");
    }

}
