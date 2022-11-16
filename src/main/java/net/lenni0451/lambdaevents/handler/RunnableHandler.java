package net.lenni0451.lambdaevents.handler;

import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;

public class RunnableHandler extends AHandler {

    private final Runnable runnable;

    public RunnableHandler(final Class<?> owner, final Object instance, final EventHandler annotation, final Runnable runnable) {
        super(owner, instance, annotation);
        this.runnable = runnable;
    }

    @Override
    public void call(Object event) {
        this.runnable.run();
    }

}
