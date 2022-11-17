package net.lenni0451.lambdaevents.generator;

import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.IGenerator;
import net.lenni0451.lambdaevents.handler.ConsumerHandler;
import net.lenni0451.lambdaevents.handler.RunnableHandler;
import net.lenni0451.lambdaevents.utils.EventUtils;
import net.lenni0451.lambdaevents.utils.LookupUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public class LambdaMetaFactoryGenerator implements IGenerator {

    private final MethodHandles.Lookup lookup;

    public LambdaMetaFactoryGenerator(@Nonnull final MethodHandles.Lookup lookup) {
        this.lookup = lookup;
    }

    @Override
    @Nonnull
    public AHandler generate(@Nonnull Class<?> owner, @Nullable Object instance, @Nonnull EventHandler annotation, @Nonnull Method method, @Nonnull Class<?> arg) {
        Consumer<Object> consumer = this.generate(owner, instance, method, Consumer.class, "accept", MethodType.methodType(void.class, Object.class));
        return new ConsumerHandler(owner, instance, annotation, consumer);
    }

    @Override
    @Nonnull
    public AHandler generateVirtual(@Nonnull Class<?> owner, @Nullable Object instance, @Nonnull EventHandler annotation, @Nonnull Method method) {
        Runnable runnable = this.generate(owner, instance, method, Runnable.class, "run", MethodType.methodType(void.class));
        return new RunnableHandler(owner, instance, annotation, runnable);
    }

    private MethodHandle getHandle(final MethodHandles.Lookup lookup, final Method method) {
        try {
            return lookup.unreflect(method);
        } catch (Throwable t) {
            EventUtils.sneak(t);
            throw new RuntimeException();
        }
    }

    private <T> T generate(final Class<?> owner, final Object instance, final Method method, final Class<T> interfaceClass, final String interfaceMethod, final MethodType interfaceType) {
        try {
            MethodHandles.Lookup lookup = LookupUtils.resolveLookup(this.lookup, owner);
            MethodHandle handle = this.getHandle(lookup, method);
            if (instance == null) {
                return (T) LambdaMetafactory.metafactory(
                        lookup,
                        interfaceMethod,
                        MethodType.methodType(interfaceClass),
                        interfaceType,
                        handle,
                        handle.type()
                ).getTarget().invoke();
            } else {
                return (T) LambdaMetafactory.metafactory(
                        lookup,
                        interfaceMethod,
                        MethodType.methodType(interfaceClass, instance.getClass()),
                        interfaceType,
                        handle,
                        handle.type().dropParameterTypes(0, 1)
                ).getTarget().invoke(instance);
            }
        } catch (Throwable t) {
            EventUtils.sneak(t);
            throw new RuntimeException();
        }
    }

}
