package net.lenni0451.le;

class StopCall extends RuntimeException {

    protected StopCall() {
    }

    @Override
    public String toString() {
        return "";
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
