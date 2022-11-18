package net.lenni0451.lambdaevents.utils;

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
