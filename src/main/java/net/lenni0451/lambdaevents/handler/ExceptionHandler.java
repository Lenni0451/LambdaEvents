package net.lenni0451.lambdaevents.handler;

import net.lenni0451.lambdaevents.AHandler;

public interface ExceptionHandler {

    void handle(final AHandler handler, final Object event, final Throwable t);

}
