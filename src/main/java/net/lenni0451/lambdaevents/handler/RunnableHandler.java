package net.lenni0451.lambdaevents.handler;

import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.utils.TryingRunnable;

public class RunnableHandler extends AHandler {

    private final TryingRunnable runnable;

    public RunnableHandler(final Class<?> owner, final Object instance, final EventHandler annotation, final TryingRunnable runnable) {
        super(owner, instance, annotation);
        this.runnable = runnable;
    }

    @Override
    public void call(Object event) {
        try {
            this.runnable.run();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
