package net.lenni0451.lambdaevents.generator;

import lombok.SneakyThrows;
import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.IGenerator;
import net.lenni0451.lambdaevents.handler.ConsumerHandler;
import net.lenni0451.lambdaevents.handler.RunnableHandler;
import net.lenni0451.lambdaevents.utils.LookupUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * The {@link IGenerator} implementation which calls the handler method using a {@link LambdaMetafactory} generated {@link Runnable} or {@link Consumer}.
 */
@ParametersAreNonnullByDefault
public class LambdaMetaFactoryGenerator implements IGenerator {

    private final MethodHandles.Lookup lookup;

    /**
     * Use the {@link MethodHandles.Lookup} of the current {@link ClassLoader}.
     */
    public LambdaMetaFactoryGenerator() {
        this(MethodHandles.lookup());
    }

    /**
     * @param lookup The {@link MethodHandles.Lookup} to use
     */
    public LambdaMetaFactoryGenerator(@Nonnull final MethodHandles.Lookup lookup) {
        this.lookup = lookup;
    }

    @Override
    @Nonnull
    public AHandler generate(Class<?> owner, @Nullable Object instance, EventHandler annotation, Method method, Class<?> arg) {
        Consumer<?> consumer = this.generate(owner, instance, method, Consumer.class, "accept", MethodType.methodType(void.class, Object.class));
        return new ConsumerHandler(owner, instance, annotation, consumer);
    }

    @Override
    @Nonnull
    public AHandler generateVirtual(Class<?> owner, @Nullable Object instance, EventHandler annotation, Method method) {
        Runnable runnable = this.generate(owner, instance, method, Runnable.class, "run", MethodType.methodType(void.class));
        return new RunnableHandler(owner, instance, annotation, runnable);
    }

    @SneakyThrows
    private <T> T generate(final Class<?> owner, @Nullable final Object instance, final Method method, final Class<T> interfaceClass, final String interfaceMethod, final MethodType interfaceType) {
        MethodHandles.Lookup lookup = LookupUtils.resolveLookup(this.lookup, owner); //Resolve the lookup that it can access the method
        MethodHandle handle = lookup.unreflect(method); //Unreflect the method
        if (instance == null) {
            return (T) LambdaMetafactory.metafactory(
                    lookup, //The lookup to use
                    interfaceMethod, //The method name of the called interface
                    MethodType.methodType(interfaceClass), //The return type of the interface builder
                    interfaceType, //The return type and parameter types of the interface method
                    handle, //The method handle to invoke
                    handle.type() //The type of the method handle
            ).getTarget().invoke(); //Get and invoke the interface builder
        } else {
            return (T) LambdaMetafactory.metafactory(
                    lookup, //The lookup to use
                    interfaceMethod, //The method name of the called interface
                    MethodType.methodType(interfaceClass, instance.getClass()), //The return type and parameter type of the interface builder
                    interfaceType, //The return type and parameter types of the interface method
                    handle, //The method handle to invoke
                    handle.type().dropParameterTypes(0, 1) //The type of the method handle without the instance parameter
            ).getTarget().invoke(instance); //Get and invoke the interface builder
        }
    }

}
