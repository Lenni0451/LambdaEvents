package net.lenni0451.lambdaevents.utils;

/**
 * A util exception class to print event handler exceptions.<br>
 * The stacktrace is removed to prevent spamming the console.
 */
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
