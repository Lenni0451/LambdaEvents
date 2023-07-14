package net.lenni0451.lambdaevents;

import net.lenni0451.lambdaevents.handler.ConsumerHandler;
import net.lenni0451.lambdaevents.handler.ExceptionHandler;
import net.lenni0451.lambdaevents.handler.RunnableHandler;
import net.lenni0451.lambdaevents.utils.EventException;
import net.lenni0451.lambdaevents.utils.EventUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("ALL")
public class LambdaManager {

    /**
     * Create a new {@link LambdaManager} instance using {@link HashMap} and {@link ArrayList}<br>
     * This implementation is not thread safe
     *
     * @param generator The {@link IGenerator} implementation which should be used
     * @return The new {@link LambdaManager} instance
     */
    public static LambdaManager basic(@Nonnull final IGenerator generator) {
        return new LambdaManager(new HashMap<>(), ArrayList::new, generator);
    }

    /**
     * Create a new {@link LambdaManager} instance using {@link ConcurrentHashMap} and {@link CopyOnWriteArrayList}<br>
     * This implementation is thread safe but has a performance impact
     *
     * @param generator The {@link IGenerator} implementation which should be used
     * @return The new {@link LambdaManager} instance
     */
    public static LambdaManager threadSafe(@Nonnull final IGenerator generator) {
        return new LambdaManager(new ConcurrentHashMap<>(), CopyOnWriteArrayList::new, generator);
    }


    private final Map<Class<?>, List<AHandler>> handlers;
    private final Supplier<List<AHandler>> listSupplier;
    private final IGenerator generator;

    private ExceptionHandler exceptionHandler = (handler, event, t) -> {
        new EventException("Exception occurred in '" + event.getClass().getSimpleName() + "' handler in '" + handler.getOwner().getName() + "'", t).printStackTrace();
    };
    private boolean registerSuperHandler = false;

    /**
     * @param handlers     The map which should be used to store the event to handler mappings
     * @param listSupplier The supplier for the list which should be used to store the handlers for an event
     * @param generator    The {@link IGenerator} implementation which should be used
     */
    public LambdaManager(@Nonnull final Map<Class<?>, List<AHandler>> handlers, @Nonnull final Supplier<List<AHandler>> listSupplier, @Nonnull final IGenerator generator) {
        this.handlers = handlers;
        this.listSupplier = listSupplier;
        this.generator = generator;
    }

    /**
     * @param exceptionHandler The {@link ExceptionHandler} which should be used to handle exceptions
     */
    public void setExceptionHandler(@Nonnull final ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Register all event handlers from super classes.
     *
     * @param registerSuperHandler If super classes should be scanned for event handlers
     */
    public void setRegisterSuperHandler(final boolean registerSuperHandler) {
        this.registerSuperHandler = registerSuperHandler;
    }


    /**
     * Call all handlers for the given event
     *
     * @param event The event instance
     * @param <T>   The event type
     * @return The given event instance
     */
    @Nonnull
    public <T> T call(@Nonnull final T event) {
        List<AHandler> handlers = this.handlers.get(event.getClass());
        if (handlers == null) return event;

        for (AHandler handler : handlers) {
            try {
                handler.call(event);
            } catch (StopCall ignored) {
                break;
            } catch (Throwable t) {
                this.exceptionHandler.handle(handler, event, t);
            }
        }
        return event;
    }


    /**
     * Register all static event handlers in the given class
     *
     * @param owner The class which should be scanned for event handlers
     */
    public void register(@Nonnull final Class<?> owner) {
        this.register(null, owner);
    }

    /**
     * Register all static event handlers for the given event in the given class
     *
     * @param event The event class
     * @param owner The class which should be scanned for event handlers
     */
    public void register(@Nullable final Class<?> event, @Nonnull final Class<?> owner) {
        for (EventUtils.MethodHandler handler : EventUtils.getMethods(owner, method -> Modifier.isStatic(method.getModifiers()), this.registerSuperHandler)) {
            if (event == null) this.registerMethod(owner, null, handler.getAnnotation(), handler.getMethod(), e -> true);
            else this.registerMethod(owner, null, handler.getAnnotation(), handler.getMethod(), e -> e.equals(event));
        }
        for (EventUtils.FieldHandler handler : EventUtils.getFields(owner, field -> Modifier.isStatic(field.getModifiers()), this.registerSuperHandler)) {
            if (event == null) this.registerField(owner, null, handler.getAnnotation(), handler.getField(), e -> true);
            else this.registerField(owner, null, handler.getAnnotation(), handler.getField(), e -> e.equals(event));
        }
    }

    /**
     * Register all non-static event handlers in the given object class
     *
     * @param owner The object which should be scanned for event handlers
     */
    public void register(@Nonnull final Object owner) {
        this.register(null, owner);
    }

    /**
     * Register all non-static event handlers for the given event in the given object class
     *
     * @param event The event class
     * @param owner The object which should be scanned for event handlers
     */
    public void register(@Nullable final Class<?> event, @Nonnull final Object owner) {
        for (EventUtils.MethodHandler handler : EventUtils.getMethods(owner.getClass(), method -> !Modifier.isStatic(method.getModifiers()), this.registerSuperHandler)) {
            if (event == null) this.registerMethod(owner.getClass(), owner, handler.getAnnotation(), handler.getMethod(), e -> true);
            else this.registerMethod(owner.getClass(), owner, handler.getAnnotation(), handler.getMethod(), e -> e.equals(event));
        }
        for (EventUtils.FieldHandler handler : EventUtils.getFields(owner.getClass(), field -> !Modifier.isStatic(field.getModifiers()), this.registerSuperHandler)) {
            if (event == null) this.registerField(owner.getClass(), owner, handler.getAnnotation(), handler.getField(), e -> true);
            else this.registerField(owner.getClass(), owner, handler.getAnnotation(), handler.getField(), e -> e.equals(event));
        }
    }

    /**
     * Register a {@link Runnable} as an event handler for the given events
     *
     * @param runnable The {@link Runnable} which should be registered
     * @param events   The events for which the {@link Runnable} should be registered
     */
    public void register(@Nonnull final Runnable runnable, @Nonnull final Class<?>... events) {
        this.register(runnable, 0, events);
    }

    /**
     * Register a {@link Runnable} as an event handler for the given events with the given priority
     *
     * @param runnable The {@link Runnable} which should be registered
     * @param priority The priority of the {@link Runnable}
     * @param events   The events for which the {@link Runnable} should be registered
     */
    public void register(@Nonnull final Runnable runnable, final int priority, @Nonnull final Class<?>... events) {
        if (events.length == 0) throw new IllegalArgumentException("No events specified");
        synchronized (this.handlers) {
            for (Class<?> event : events) {
                List<AHandler> handlers = this.handlers.computeIfAbsent(event, (key) -> this.listSupplier.get());
                handlers.add(new RunnableHandler(runnable.getClass(), null, EventUtils.newEventHandler(priority), runnable));
            }
        }
    }

    /**
     * Register a {@link Consumer} as an event handler for the given events
     *
     * @param consumer The {@link Consumer} which should be registered
     * @param events   The events for which the {@link Consumer} should be registered
     */
    public void register(@Nonnull final Consumer<?> consumer, @Nonnull final Class<?>... events) {
        this.register(consumer, 0, events);
    }

    /**
     * Register a {@link Consumer} as an event handler for the given events with the given priority
     *
     * @param consumer The {@link Consumer} which should be registered
     * @param priority The priority of the {@link Consumer}
     * @param events   The events for which the {@link Consumer} should be registered
     */
    public void register(@Nonnull final Consumer consumer, final int priority, @Nonnull final Class<?>... events) {
        if (events.length == 0) throw new IllegalArgumentException("No events specified");
        synchronized (this.handlers) {
            for (Class<?> event : events) {
                List<AHandler> handlers = this.handlers.computeIfAbsent(event, (key) -> this.listSupplier.get());
                handlers.add(new ConsumerHandler(consumer.getClass(), null, EventUtils.newEventHandler(priority), consumer));
            }
        }
    }

    private void registerMethod(final Class<?> owner, final Object instance, final EventHandler annotation, final Method method, final Predicate<Class<?>> accept) {
        EventUtils.verify(owner, annotation, method);
        for (Class<?> event : EventUtils.getEvents(annotation, method, accept)) this.registerMethod(owner, instance, annotation, method, event, method.getParameterCount() == 0);
    }

    private void registerField(final Class<?> owner, final Object instance, final EventHandler annotation, final Field field, final Predicate<Class<?>> accept) {
        EventUtils.verify(owner, annotation, field);
        for (Class<?> event : EventUtils.getEvents(annotation, field, accept)) this.registerField(owner, instance, annotation, field, event);
    }

    private void registerMethod(final Class<?> owner, final Object instance, final EventHandler annotation, final Method method, final Class<?> event, final boolean virtual) {
        synchronized (this.handlers) {
            List<AHandler> handlers = this.handlers.computeIfAbsent(event, (key) -> this.listSupplier.get());
            AHandler handler;
            if (virtual) handler = this.generator.generateVirtual(owner, instance, annotation, method);
            else handler = this.generator.generate(owner, instance, annotation, method, event);
            handlers.add(handler);
            this.resortHandlers(handlers);
        }
    }

    private void registerField(final Class<?> owner, final Object instance, final EventHandler annotation, final Field field, final Class<?> event) {
        synchronized (this.handlers) {
            List<AHandler> handlers = this.handlers.computeIfAbsent(event, (key) -> this.listSupplier.get());
            AHandler handler;
            try {
                if (Runnable.class.isAssignableFrom(field.getType())) handler = new RunnableHandler(owner, instance, annotation, (Runnable) field.get(instance));
                else handler = new ConsumerHandler(owner, instance, annotation, (Consumer<Object>) field.get(instance));
            } catch (Throwable t) {
                throw new RuntimeException("Failed to register field '" + field.getName() + "' in class '" + owner.getName() + "'", t);
            }
            handlers.add(handler);
            this.resortHandlers(handlers);
        }
    }


    /**
     * Unregister all static event handlers from the given class
     *
     * @param owner The class from which the static event handlers should be unregistered
     */
    public void unregister(@Nonnull final Class<?> owner) {
        synchronized (this.handlers) {
            Iterator<Map.Entry<Class<?>, List<AHandler>>> it = this.handlers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Class<?>, List<AHandler>> entry = it.next();
                List<AHandler> handlers = entry.getValue();
                handlers.removeIf(handler -> handler.isStatic() && handler.getOwner().equals(owner));
                if (handlers.isEmpty()) it.remove();
            }
        }
    }

    /**
     * Unregister all static event handlers for the given event from the given class
     *
     * @param event The event class
     * @param owner The class from which the static event handlers should be unregistered
     */
    public void unregister(@Nullable final Class<?> event, @Nonnull final Class<?> owner) {
        synchronized (this.handlers) {
            List<AHandler> handlers = this.handlers.get(event);
            if (handlers == null) return;
            handlers.removeIf(handler -> handler.isStatic() && handler.getOwner().equals(owner));
            if (handlers.isEmpty()) this.handlers.remove(event);
            else this.resortHandlers(handlers);
        }
    }

    /**
     * Unregister all non-static event handlers from the given object class
     *
     * @param owner The object from which the non-static event handlers should be unregistered
     */
    public void unregister(@Nonnull final Object owner) {
        synchronized (this.handlers) {
            Iterator<Map.Entry<Class<?>, List<AHandler>>> it = this.handlers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Class<?>, List<AHandler>> entry = it.next();
                List<AHandler> handlers = entry.getValue();
                handlers.removeIf(handler -> !handler.isStatic() && handler.getInstance().equals(owner));
                if (handlers.isEmpty()) it.remove();
            }
        }
    }

    /**
     * Unregister all non-static event handlers for the given event from the given object class
     *
     * @param event The event class
     * @param owner The object from which the non-static event handlers should be unregistered
     */
    public void unregister(@Nullable final Class<?> event, @Nonnull final Object owner) {
        synchronized (this.handlers) {
            List<AHandler> handlers = this.handlers.get(event);
            if (handlers == null) return;
            handlers.removeIf(handler -> !handler.isStatic() && handler.getInstance().equals(owner));
            if (handlers.isEmpty()) this.handlers.remove(event);
            else this.resortHandlers(handlers);
        }
    }

    /**
     * Unregister a {@link Runnable} from all events
     *
     * @param runnable The {@link Runnable} to unregister
     */
    public void unregister(@Nonnull final Runnable runnable) {
        synchronized (this.handlers) {
            Iterator<Map.Entry<Class<?>, List<AHandler>>> it = this.handlers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Class<?>, List<AHandler>> entry = it.next();
                List<AHandler> handlers = entry.getValue();
                handlers.removeIf(handler -> handler instanceof RunnableHandler && ((RunnableHandler) handler).getRunnable().equals(runnable));
                if (handlers.isEmpty()) it.remove();
            }
        }
    }

    /**
     * Unregister a {@link Runnable} from the given events
     *
     * @param runnable The {@link Runnable} to unregister
     * @param events   The events from which the {@link Runnable} should be unregistered
     */
    public void unregister(@Nonnull final Runnable runnable, @Nonnull final Class<?>... events) {
        if (events.length == 0) {
            this.unregister(runnable);
            return;
        }
        synchronized (this.handlers) {
            for (Class<?> event : events) {
                List<AHandler> handlers = this.handlers.get(event);
                if (handlers == null) continue;
                handlers.removeIf(handler -> handler instanceof RunnableHandler && ((RunnableHandler) handler).getRunnable().equals(runnable));
                if (handlers.isEmpty()) this.handlers.remove(event);
            }
        }
    }

    /**
     * Unregister a {@link Consumer} from all events
     *
     * @param consumer The {@link Consumer} to unregister
     */
    public void unregister(@Nonnull final Consumer consumer) {
        synchronized (this.handlers) {
            Iterator<Map.Entry<Class<?>, List<AHandler>>> it = this.handlers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Class<?>, List<AHandler>> entry = it.next();
                List<AHandler> handlers = entry.getValue();
                handlers.removeIf(handler -> handler instanceof ConsumerHandler && ((ConsumerHandler) handler).getConsumer().equals(consumer));
                if (handlers.isEmpty()) it.remove();
            }
        }
    }

    /**
     * Unregister a {@link Consumer} from the given events
     *
     * @param consumer The {@link Consumer} to unregister
     * @param events   The events from which the {@link Consumer} should be unregistered
     */
    public void unregister(@Nonnull final Consumer consumer, @Nonnull final Class<?>... events) {
        if (events.length == 0) {
            this.unregister(consumer);
            return;
        }
        synchronized (this.handlers) {
            for (Class<?> event : events) {
                List<AHandler> handlers = this.handlers.get(event);
                if (handlers == null) continue;
                handlers.removeIf(handler -> handler instanceof ConsumerHandler && ((ConsumerHandler) handler).getConsumer().equals(consumer));
                if (handlers.isEmpty()) this.handlers.remove(event);
            }
        }
    }


    private void resortHandlers(final List<AHandler> handlers) {
        handlers.sort(Comparator.comparingInt((AHandler o) -> o.getAnnotation().priority()).reversed());
    }

}
