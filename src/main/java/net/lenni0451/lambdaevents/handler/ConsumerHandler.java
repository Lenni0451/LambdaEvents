package net.lenni0451.lambdaevents.handler;

import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * The {@link AHandler} implementation for {@link Consumer} events with an event parameter.
 */
public class ConsumerHandler extends AHandler {

    private final Consumer<Object> consumer;

    /**
     * @param owner      The owner of the handler method
     * @param instance   The instance of the handler owner
     * @param annotation The {@link EventHandler} annotation of the handler method
     * @param consumer   The handler consumer
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ConsumerHandler(@Nonnull final Class<?> owner, @Nullable final Object instance, @Nonnull final EventHandler annotation, @Nonnull final Consumer consumer) {
        super(owner, instance, annotation);
        this.consumer = consumer;
    }

    /**
     * @return The handler consumer
     */
    @Nonnull
    public Consumer<Object> getConsumer() {
        return this.consumer;
    }

    @Override
    public void call(Object event) {
        this.consumer.accept(event);
    }

}
