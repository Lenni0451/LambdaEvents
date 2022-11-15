package net.lenni0451.lambdaevents.utils;

public interface TryingConsumer {

    void accept(Object o) throws Throwable;

}
