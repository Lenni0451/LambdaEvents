package net.lenni0451.lambdaevents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * The generator interface for the {@link AHandler} implementations.
 */
public interface IGenerator {

    /**
     * Generate an {@link AHandler} for the given method with an event parameter.
     *
     * @param owner      The owner of the handler method
     * @param instance   The instance of the handler owner
     * @param annotation The {@link EventHandler} annotation of the handler method
     * @param method     The handler method
     * @param arg        The event class
     * @return The generated handler
     */
    @Nonnull
    AHandler generate(@Nonnull final Class<?> owner, @Nullable final Object instance, @Nonnull final EventHandler annotation, @Nonnull final Method method, @Nonnull final Class<?> arg);

    /**
     * Generate an {@link AHandler} for the given method without an event parameter.
     *
     * @param owner      The owner of the handler method
     * @param instance   The instance of the handler owner
     * @param annotation The {@link EventHandler} annotation of the handler method
     * @param method     The handler method
     * @return The generated handler
     */
    @Nonnull
    AHandler generateVirtual(@Nonnull final Class<?> owner, @Nullable final Object instance, @Nonnull final EventHandler annotation, @Nonnull final Method method);

}
