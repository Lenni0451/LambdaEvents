package net.lenni0451.lambdaevents.handler;

import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * The {@link AHandler} implementation for {@link Runnable} events without an event parameter.
 */
@ParametersAreNonnullByDefault
public class RunnableHandler extends AHandler {

    @Nonnull
    private final Runnable runnable;

    /**
     * @param owner      The owner of the handler method
     * @param instance   The instance of the handler owner
     * @param annotation The {@link EventHandler} annotation of the handler method
     * @param runnable   The handler runnable
     */
    public RunnableHandler(final Class<?> owner, @Nullable final Object instance, final EventHandler annotation, final Runnable runnable) {
        super(owner, instance, annotation);
        this.runnable = runnable;
    }

    /**
     * @return The handler runnable
     */
    @Nonnull
    public Runnable getRunnable() {
        return this.runnable;
    }

    @Override
    public void call(Object event) {
        this.runnable.run();
    }

    @Override
    public String toString() {
        return "runnable: " + this.getOwner().getName() + " -> " + this.runnable.getClass().getName();
    }

}
