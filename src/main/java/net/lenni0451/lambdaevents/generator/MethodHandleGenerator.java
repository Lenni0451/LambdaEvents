package net.lenni0451.lambdaevents.generator;

import lombok.SneakyThrows;
import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.IGenerator;
import net.lenni0451.lambdaevents.handler.methodhandle.MethodHandleHandler;
import net.lenni0451.lambdaevents.handler.methodhandle.VirtualMethodHandleHandler;
import net.lenni0451.lambdaevents.utils.LookupUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * The {@link IGenerator} implementation which calls the handler method using method handles.
 */
public class MethodHandleGenerator implements IGenerator {

    private final MethodHandles.Lookup lookup;

    /**
     * Use the {@link MethodHandles.Lookup} of the current {@link ClassLoader}.
     */
    public MethodHandleGenerator() {
        this(MethodHandles.lookup());
    }

    /**
     * @param lookup The {@link MethodHandles.Lookup} to use
     */
    public MethodHandleGenerator(@Nonnull final MethodHandles.Lookup lookup) {
        this.lookup = lookup;
    }

    @Override
    @Nonnull
    public AHandler generate(@Nonnull Class<?> owner, @Nullable Object instance, @Nonnull EventHandler annotation, @Nonnull Method method, @Nonnull Class<?> arg) {
        MethodHandle handle = this.getHandle(owner, instance, method);
        return new MethodHandleHandler(owner, instance, annotation, handle);
    }

    @Override
    @Nonnull
    public AHandler generateVirtual(@Nonnull Class<?> owner, @Nullable Object instance, @Nonnull EventHandler annotation, @Nonnull Method method) {
        MethodHandle handle = this.getHandle(owner, instance, method);
        return new VirtualMethodHandleHandler(owner, instance, annotation, handle);
    }

    @SneakyThrows
    private MethodHandle getHandle(final Class<?> owner, final Object instance, final Method method) {
        MethodHandle handle = LookupUtils.resolveLookup(this.lookup, owner).unreflect(method);
        if (instance != null) handle = handle.bindTo(instance);
        return handle;
    }

}
