package net.lenni0451.lambdaevents;

import javax.annotation.Nonnull;

/**
 * An interface to handle all exceptions thrown by event handlers.
 */
public interface IExceptionHandler {

    /**
     * Handle the thrown exception.
     *
     * @param handler The handler that threw the exception
     * @param event   The instance of the event which caused the exception
     * @param t       The thrown exception
     */
    void handle(@Nonnull final AHandler handler, @Nonnull final Object event, @Nonnull final Throwable t);

}
