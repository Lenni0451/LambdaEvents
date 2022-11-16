package net.lenni0451.lambdaevents.handler;

import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;

import java.util.function.Consumer;

public class ConsumerHandler extends AHandler {

    private final Consumer<Object> consumer;

    public ConsumerHandler(final Class<?> owner, final Object instance, final EventHandler annotation, final Consumer<Object> consumer) {
        super(owner, instance, annotation);
        this.consumer = consumer;
    }

    @Override
    public void call(Object event) {
        this.consumer.accept(event);
    }

}
