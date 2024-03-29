package net.lenni0451.lambdaevents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The abstract handler class generated by an {@link IGenerator} implementation.
 */
public abstract class AHandler {

    @Nonnull
    protected final Class<?> owner;
    @Nullable
    protected final Object instance;
    @Nonnull
    protected final EventHandler annotation;
    private final boolean handleCancelled;

    /**
     * @param owner      The owner of the handler method
     * @param instance   The instance of the handler owner
     * @param annotation The {@link EventHandler} annotation of the handler method
     */
    public AHandler(final Class<?> owner, @Nullable final Object instance, final EventHandler annotation) {
        this.owner = owner;
        this.instance = instance;
        this.annotation = annotation;
        this.handleCancelled = annotation.handleCancelled();
    }

    /**
     * @return The owner of the handler method
     */
    @Nonnull
    public Class<?> getOwner() {
        return this.owner;
    }

    /**
     * @return The instance of the handler owner
     */
    @Nullable
    public Object getInstance() {
        return this.instance;
    }

    /**
     * @return The {@link EventHandler} annotation of the handler method
     */
    @Nonnull
    public EventHandler getAnnotation() {
        return this.annotation;
    }

    /**
     * @return If the handler is static
     */
    public boolean isStatic() {
        return this.instance == null;
    }

    /**
     * @return If the handler should handle cancelled events
     */
    public boolean shouldHandleCancelled() {
        return this.handleCancelled;
    }

    /**
     * Call the event handler with the given event instance.
     *
     * @param event The event instance
     */
    public abstract void call(@Nonnull final Object event);

    @Override
    public abstract String toString();

}
