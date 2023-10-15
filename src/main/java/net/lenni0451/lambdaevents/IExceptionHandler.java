package net.lenni0451.lambdaevents;

import net.lenni0451.lambdaevents.utils.EventException;
import net.lenni0451.lambdaevents.utils.ThrowingExceptionHandler;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * An interface to handle all exceptions thrown by event handlers.
 */
@ParametersAreNonnullByDefault
public interface IExceptionHandler {

    /**
     * @return A simple exception handler which just prints the stacktrace
     */
    static IExceptionHandler simplePrint() {
        return (handler, event, t) -> t.printStackTrace();
    }

    /**
     * @return An exception handler which prints the stacktrace with some additional information about the event and handler
     */
    static IExceptionHandler infoPrint() {
        return (handler, event, t) -> {
            new EventException("Exception occurred in '" + event.getClass().getSimpleName() + "' handler in '" + handler.getOwner().getName() + "'", t).printStackTrace();
        };
    }

    /**
     * @return A simple exception handler which just rethrows the exception
     */
    static IExceptionHandler throwing() {
        return new ThrowingExceptionHandler();
    }

    /**
     * @return A simple exception handler which ignores all exceptions
     */
    static IExceptionHandler ignore() {
        return (handler, event, t) -> {};
    }


    /**
     * Handle the thrown exception.
     *
     * @param handler The handler that threw the exception
     * @param event   The instance of the event which caused the exception
     * @param t       The thrown exception
     */
    void handle(final AHandler handler, final Object event, final Throwable t);

}
