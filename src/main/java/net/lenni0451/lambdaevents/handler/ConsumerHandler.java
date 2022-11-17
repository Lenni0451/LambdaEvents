package net.lenni0451.lambdaevents.handler;

import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class ConsumerHandler extends AHandler {

    private final Consumer<Object> consumer;

    public ConsumerHandler(@Nonnull final Class<?> owner, @Nullable final Object instance, @Nonnull final EventHandler annotation, @Nonnull final Consumer<Object> consumer) {
        super(owner, instance, annotation);
        this.consumer = consumer;
    }

    @Nonnull
    public Consumer<Object> getConsumer() {
        return this.consumer;
    }

    @Override
    public void call(Object event) {
        this.consumer.accept(event);
    }

}
