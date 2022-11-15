package net.lenni0451.lambdaevents.handler;

import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.utils.TryingConsumer;

public class ConsumerHandler extends AHandler {

    private final TryingConsumer consumer;

    public ConsumerHandler(final Class<?> owner, final Object instance, final EventHandler annotation, final TryingConsumer consumer) {
        super(owner, instance, annotation);
        this.consumer = consumer;
    }

    @Override
    public void call(Object event) {
        try {
            this.consumer.accept(event);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
