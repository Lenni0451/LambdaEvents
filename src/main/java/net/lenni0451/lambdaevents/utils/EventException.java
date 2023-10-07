package net.lenni0451.lambdaevents.utils;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A util exception class to print event handler exceptions.<br>
 * The stacktrace is removed to prevent spamming the console.
 */
@ParametersAreNonnullByDefault
public class EventException extends Exception {

    public EventException(final String message, final Throwable cause) {
        super(message, cause);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return new StackTraceElement[0];
    }

}
