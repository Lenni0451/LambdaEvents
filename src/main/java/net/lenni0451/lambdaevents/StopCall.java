package net.lenni0451.lambdaevents;

class StopCall extends RuntimeException {

    protected StopCall() {
    }

    @Override
    public String toString() {
        return "Throw this to stop the current event pipeline";
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
