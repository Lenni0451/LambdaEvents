package net.lenni0451.le;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LambdaManager {

    private static LambdaManager GLOBAL;

    public static LambdaManager global() {
        if (GLOBAL == null) GLOBAL = new LambdaManager();
        return GLOBAL;
    }


    private final Map<Class<?>, List<Caller>> invoker = new ConcurrentHashMap<>();

    public void register(final Object instanceOrClass) {
        Objects.requireNonNull(instanceOrClass, "Instance or class can not be null");

        final boolean isStatic = instanceOrClass instanceof Class<?>;
        final Class<?> clazz = isStatic ? (Class<?>) instanceOrClass : instanceOrClass.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            LambdaHandler handlerInfo = method.getDeclaredAnnotation(LambdaHandler.class);
            if (handlerInfo == null) continue;
            if (isStatic != Modifier.isStatic(method.getModifiers())) continue;
            if (method.getParameterCount() != 1) continue;
            try {
                this.invoker.computeIfAbsent(method.getParameterTypes()[0], c -> new CopyOnWriteArrayList<>()).add(this.generate(isStatic ? null : instanceOrClass, method, handlerInfo));
            } catch (Throwable e) {
                throw new IllegalStateException("Unable to create Consumer for method '" + method.getName() + "' in class '" + method.getDeclaringClass().getName() + "'", e);
            }
        }
    }

    public void call(final Object lambda) {
        List<Caller> list = this.invoker.get(lambda.getClass());
        if (list == null) return;
        for (Caller caller : list) caller.call(lambda);
    }


    private Caller generate(final Object instance, final Method method, final LambdaHandler handlerInfo) throws Throwable {
        final boolean isStatic = instance == null;
        if (isStatic) {
            CallSite callSite = LambdaMetafactory.metafactory(
                    MethodHandles.lookup(),
                    "accept",
                    MethodType.methodType(Consumer.class),
                    MethodType.methodType(void.class, Object.class),
                    MethodHandles.lookup().findStatic(method.getDeclaringClass(), method.getName(), MethodType.methodType(void.class, method.getParameterTypes()[0])),
                    MethodType.methodType(void.class, method.getParameterTypes()[0])
            );
            Consumer consumer = (Consumer) callSite.getTarget().invokeExact();
            return new Caller(handlerInfo, consumer);
        } else {
            CallSite callSite = LambdaMetafactory.metafactory(
                    MethodHandles.lookup(),
                    "accept",
                    MethodType.methodType(BiConsumer.class),
                    MethodType.methodType(void.class, Object.class, Object.class),
                    MethodHandles.lookup().findVirtual(method.getDeclaringClass(), method.getName(), MethodType.methodType(void.class, method.getParameterTypes()[0])),
                    MethodType.methodType(void.class, instance.getClass(), method.getParameterTypes()[0])
            );
            BiConsumer consumer = (BiConsumer) callSite.getTarget().invokeExact();
            return new Caller(instance, handlerInfo, consumer);
        }
    }

}
