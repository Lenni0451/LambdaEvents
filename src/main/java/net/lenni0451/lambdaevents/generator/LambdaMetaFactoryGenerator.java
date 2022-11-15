package net.lenni0451.lambdaevents.generator;

import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.IGenerator;
import net.lenni0451.lambdaevents.handler.ConsumerHandler;
import net.lenni0451.lambdaevents.handler.RunnableHandler;
import net.lenni0451.lambdaevents.utils.TryingConsumer;
import net.lenni0451.lambdaevents.utils.TryingRunnable;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class LambdaMetaFactoryGenerator implements IGenerator {

    private final MethodHandles.Lookup lookup;

    public LambdaMetaFactoryGenerator(final MethodHandles.Lookup lookup) {
        this.lookup = lookup;
    }

    @Override
    public AHandler generate(Class<?> owner, Object instance, EventHandler annotation, Method method, Class<?> arg) {
        MethodHandles.Lookup lookup = this.lookup.in(owner);
        TryingConsumer consumer = this.generate(lookup, instance, method, TryingConsumer.class, "accept", MethodType.methodType(void.class, Object.class));
        return new ConsumerHandler(owner, instance, annotation, consumer);
    }

    @Override
    public AHandler generateVirtual(Class<?> owner, Object instance, EventHandler annotation, Method method) {
        MethodHandles.Lookup lookup = this.lookup.in(owner);
        TryingRunnable runnable = this.generate(lookup, instance, method, TryingRunnable.class, "run", MethodType.methodType(void.class));
        return new RunnableHandler(owner, instance, annotation, runnable);
    }

    private MethodHandle getHandle(final MethodHandles.Lookup lookup, final Method method) {
        try {
            return lookup.unreflect(method);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T generate(MethodHandles.Lookup lookup, final Object instance, final Method method, final Class<T> interfaceClass, final String interfaceMethod, final MethodType interfaceType) {
        try {
            MethodHandle handle = this.getHandle(lookup, method);
            MethodType type = handle.type();
            if (instance == null) {
                return (T) LambdaMetafactory.metafactory(
                        lookup,
                        interfaceMethod,
                        MethodType.methodType(interfaceClass),
                        interfaceType,
                        handle,
                        type
                ).getTarget().invoke();
            } else {
                type = type.dropParameterTypes(0, 1);
                return (T) LambdaMetafactory.metafactory(
                        lookup,
                        interfaceMethod,
                        MethodType.methodType(interfaceClass, instance.getClass()),
                        interfaceType,
                        handle,
                        type
                ).getTarget().invoke(instance);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
