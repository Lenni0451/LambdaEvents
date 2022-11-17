package net.lenni0451.lambdaevents.handler;

import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RunnableHandler extends AHandler {

    private final Runnable runnable;

    public RunnableHandler(@Nonnull final Class<?> owner, @Nullable final Object instance, @Nonnull final EventHandler annotation, @Nonnull final Runnable runnable) {
        super(owner, instance, annotation);
        this.runnable = runnable;
    }

    @Override
    public void call(Object event) {
        this.runnable.run();
    }

}
