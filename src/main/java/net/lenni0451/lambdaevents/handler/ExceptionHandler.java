package net.lenni0451.lambdaevents.handler;

import net.lenni0451.lambdaevents.AHandler;

import javax.annotation.Nonnull;

public interface ExceptionHandler {

    void handle(@Nonnull final AHandler handler, @Nonnull final Object event, @Nonnull final Throwable t);

}
