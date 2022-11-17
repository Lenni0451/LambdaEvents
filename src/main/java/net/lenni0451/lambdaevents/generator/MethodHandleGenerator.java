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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class MethodHandleGenerator implements IGenerator {

    private final MethodHandles.Lookup lookup;

    public MethodHandleGenerator(@Nonnull final MethodHandles.Lookup lookup) {
        this.lookup = lookup;
    }

    @Override
    @Nonnull
    public AHandler generate(@Nonnull Class<?> owner, @Nullable Object instance, @Nonnull EventHandler annotation, @Nonnull Method method, @Nonnull Class<?> arg) {
        MethodHandle handle = this.getHandle(owner, instance, method);
        return new ConsumerHandler(owner, instance, annotation, event -> {
            try {
                handle.invoke(event);
            } catch (Throwable t) {
                EventUtils.sneak(t);
            }
        });
    }

    @Override
    @Nonnull
    public AHandler generateVirtual(@Nonnull Class<?> owner, @Nullable Object instance, @Nonnull EventHandler annotation, @Nonnull Method method) {
        MethodHandle handle = this.getHandle(owner, instance, method);
        return new RunnableHandler(owner, instance, annotation, () -> {
            try {
                handle.invokeExact();
            } catch (Throwable t) {
                EventUtils.sneak(t);
            }
        });
    }

    private MethodHandle getHandle(final Class<?> owner, final Object instance, final Method method) {
        try {
            MethodHandle handle = LookupUtils.resolveLookup(this.lookup, owner).unreflect(method);
            if (instance != null) handle = handle.bindTo(instance);
            return handle;
        } catch (Throwable t) {
            EventUtils.sneak(t);
            throw new RuntimeException();
        }
    }

}
