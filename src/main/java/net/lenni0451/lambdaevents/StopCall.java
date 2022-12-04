package net.lenni0451.lambdaevents;

/**
 * Stop the execution of the event call and prevent the following handlers from being invoked
 */
public class StopCall extends RuntimeException {

    /**
     * The instance of this exception without a stacktrace
     */
    public static final StopCall INSTANCE = new StopCall();


    private StopCall() {
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
