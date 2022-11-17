package net.lenni0451.lambdaevents;

public class StopCall extends RuntimeException {

    public static final StopCall INSTANCE = new StopCall();


    private StopCall() {
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
