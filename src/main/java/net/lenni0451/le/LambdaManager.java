package net.lenni0451.le;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LambdaManager {

    private static LambdaManager GLOBAL;

    /**
     * Get the global instance of the {@link LambdaManager}
     */
    public static LambdaManager global() {
        if (GLOBAL == null) GLOBAL = new LambdaManager();
        return GLOBAL;
    }


    private final Map<Class<?>, List<Caller>> invoker = new ConcurrentHashMap<>();

    /**
     * Register all lambda listener in a class
     *
     * @param instanceOrClass Instance if virtual events/Class if static events
     */
    public void register(final Object instanceOrClass) {
        this.register(null, instanceOrClass);
    }

    /**
     * Register all lambda listener in a class
     *
     * @param lambdaClass     The class of the event to register
     * @param instanceOrClass Instance if virtual events/Class if static events
     */
    public void register(final Class<?> lambdaClass, final Object instanceOrClass) {
        Objects.requireNonNull(instanceOrClass, "Instance or class can not be null");

        final boolean isStatic = instanceOrClass instanceof Class<?>;
        final Class<?> clazz = isStatic ? (Class<?>) instanceOrClass : instanceOrClass.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            LambdaHandler handlerInfo = method.getDeclaredAnnotation(LambdaHandler.class);
            if (handlerInfo == null) continue;
            if (isStatic != Modifier.isStatic(method.getModifiers())) continue;
            if (method.getParameterCount() != 1) continue;
            if (lambdaClass != null && !method.getParameterTypes()[0].equals(lambdaClass)) continue;
            try {
                List<Caller> list = this.invoker.computeIfAbsent(method.getParameterTypes()[0], c -> new CopyOnWriteArrayList<>());
                list.add(this.generate(isStatic ? null : instanceOrClass, method, handlerInfo));
                list.sort(Caller.COMPARATOR);
            } catch (Throwable e) {
                throw new IllegalStateException("Unable to create Consumer for method '" + method.getName() + "' in class '" + method.getDeclaringClass().getName() + "'", e);
            }
        }
    }

    /**
     * Unregister all lambda listener in a class
     *
     * @param instanceOrClass Instance if virtual events/Class if static events
     */
    public void unregister(final Object instanceOrClass) {
        this.unregister(null, instanceOrClass);
    }

    /**
     * Unregister all lambda listener in a class
     *
     * @param lambdaClass     The class of the event to register
     * @param instanceOrClass Instance if virtual events/Class if static events
     */
    public void unregister(final Class<?> lambdaClass, final Object instanceOrClass) {
        Objects.requireNonNull(instanceOrClass, "Instance or class can not be null");

        final boolean isStatic = instanceOrClass instanceof Class<?>;
        final Class<?> clazz = isStatic ? (Class<?>) instanceOrClass : instanceOrClass.getClass();
        List<Class<?>> toRemove = new ArrayList<>();
        if (lambdaClass == null) {
            for (Map.Entry<Class<?>, List<Caller>> entry : this.invoker.entrySet()) {
                List<Caller> list = entry.getValue();
                list.removeIf(caller -> caller.isStatic() == isStatic && caller.getOwnerClass().equals(clazz));
                if (list.isEmpty()) toRemove.add(entry.getKey());
            }
        } else {
            List<Caller> list = this.invoker.get(clazz);
            if (list == null) return;
            list.removeIf(caller -> caller.isStatic() == isStatic && caller.getOwnerClass().equals(clazz));
        }
        for (Class<?> clazzToRemove : toRemove) this.invoker.remove(clazzToRemove);
    }

    /**
     * Call all lambda listener
     *
     * @param lambda The lambda to call with
     */
    public <T> T call(final T lambda) {
        Objects.requireNonNull(lambda, "Lambda can not be null");

        List<Caller> list = this.invoker.get(lambda.getClass());
        if (list == null) return lambda;
        for (Caller caller : list) caller.call(lambda);
        return lambda;
    }


    /**
     * Generate a {@link Caller} for the given method
     *
     * @param instance    The instance of the method owner
     * @param method      The method to call
     * @param handlerInfo The {@link LambdaHandler} annotation of the method
     * @return The new Caller instance for virtual and static calls
     * @throws Throwable Because {@link java.lang.invoke.MethodHandle#invokeExact(Object...)} can throw a throwable
     */
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
            return new Caller(method.getDeclaringClass(), handlerInfo, consumer);
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
            return new Caller(method.getDeclaringClass(), instance, handlerInfo, consumer);
        }
    }

}
