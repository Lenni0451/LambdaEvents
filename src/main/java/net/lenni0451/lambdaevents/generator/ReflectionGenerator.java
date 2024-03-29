package net.lenni0451.lambdaevents.generator;

import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.IGenerator;
import net.lenni0451.lambdaevents.handler.reflection.ReflectionHandler;
import net.lenni0451.lambdaevents.handler.reflection.VirtualReflectionHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * The {@link IGenerator} implementation which calls the handler method using reflection.
 */
public class ReflectionGenerator implements IGenerator {

    @Override
    @Nonnull
    public AHandler generate(Class<?> owner, @Nullable Object instance, EventHandler annotation, Method method, Class<?> arg) {
        method.setAccessible(true); //Make sure the method is accessible
        return new ReflectionHandler(owner, instance, annotation, method);
    }

    @Override
    @Nonnull
    public AHandler generateVirtual(Class<?> owner, @Nullable Object instance, EventHandler annotation, Method method) {
        method.setAccessible(true); //Make sure the method is accessible
        return new VirtualReflectionHandler(owner, instance, annotation, method);
    }

}
