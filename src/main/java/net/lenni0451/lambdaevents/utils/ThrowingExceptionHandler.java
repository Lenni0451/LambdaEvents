package net.lenni0451.lambdaevents.utils;

import lombok.SneakyThrows;
import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.IExceptionHandler;

/**
 * An {@link IExceptionHandler} which rethrows the exception
 */
public class ThrowingExceptionHandler implements IExceptionHandler {

    @Override
    @SneakyThrows
    public void handle(AHandler handler, Object event, Throwable t) {
        throw t;
    }

}
