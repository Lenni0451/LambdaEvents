package net.lenni0451.lambdaevents.generator;

import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.IGenerator;
import net.lenni0451.lambdaevents.handler.ConsumerHandler;
import net.lenni0451.lambdaevents.handler.RunnableHandler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class MethodHandleGenerator implements IGenerator {

    private final MethodHandles.Lookup lookup;

    public MethodHandleGenerator(final MethodHandles.Lookup lookup) {
        this.lookup = lookup;
    }

    @Override
    public AHandler generate(Class<?> owner, Object instance, EventHandler annotation, Method method, Class<?> arg) {
        MethodHandle handle = this.getHandle(owner, instance, method);
        return new ConsumerHandler(owner, instance, annotation, handle::invoke);
    }

    @Override
    public AHandler generateVirtual(Class<?> owner, Object instance, EventHandler annotation, Method method) {
        MethodHandle handle = this.getHandle(owner, instance, method);
        return new RunnableHandler(owner, instance, annotation, handle::invoke);
    }

    private MethodHandle getHandle(final Class<?> owner, final Object instance, final Method method) {
        try {
            MethodHandle handle = this.lookup.in(owner).unreflect(method);
            if (instance != null) handle = handle.bindTo(instance);
            return handle;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
