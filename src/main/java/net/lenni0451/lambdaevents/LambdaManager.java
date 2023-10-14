package net.lenni0451.lambdaevents;

import net.lenni0451.lambdaevents.handler.ConsumerHandler;
import net.lenni0451.lambdaevents.handler.RunnableHandler;
import net.lenni0451.lambdaevents.utils.EventException;
import net.lenni0451.lambdaevents.utils.EventUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class LambdaManager {

    /**
     * Create a new {@link LambdaManager} instance using a {@link HashMap} and {@link ArrayList}.<br>
     * This implementation is not thread safe.
     *
     * @param generator The {@link IGenerator} implementation which should be used
     * @return The new {@link LambdaManager} instance
     */
    public static LambdaManager basic(final IGenerator generator) {
        return new LambdaManager(HashMap::new, ArrayList::new, generator);
    }

    /**
     * Create a new {@link LambdaManager} instance using a {@link ConcurrentHashMap} and {@link CopyOnWriteArrayList}.<br>
     * This implementation is thread safe but has a performance impact.
     *
     * @param generator The {@link IGenerator} implementation which should be used
     * @return The new {@link LambdaManager} instance
     */
    public static LambdaManager threadSafe(final IGenerator generator) {
        return new LambdaManager(ConcurrentHashMap::new, CopyOnWriteArrayList::new, generator);
    }


    private final Map<Class<?>, List<AHandler>> handlers;
    private final Map<Class<?>, AHandler[]> handlerArrays;
    private final Map<Class<?>, Class<?>[]> parentsCache;
    private final Supplier<List<AHandler>> listSupplier;
    private final IGenerator generator;

    private IExceptionHandler exceptionHandler = (handler, event, t) -> {
        new EventException("Exception occurred in '" + event.getClass().getSimpleName() + "' handler in '" + handler.getOwner().getName() + "'", t).printStackTrace();
    };
    private boolean registerSuperHandler = false;
    private boolean alwaysCallParents = false;

    /**
     * <b>Deprecated constructor, use {@link #LambdaManager(Supplier, Supplier, IGenerator)}.</b>
     */
    @Deprecated
    public LambdaManager(final Map<Class<?>, List<AHandler>> handlers, final Supplier<List<AHandler>> listSupplier, final IGenerator generator) {
        this(handlers instanceof HashMap ? HashMap::new : ConcurrentHashMap::new, listSupplier, generator);
    }

    /**
     * @param mapSupplier  The supplier for the maps used to store the event to handler mappings
     * @param listSupplier The supplier for the list used to store the handlers for an event
     * @param generator    The {@link IGenerator} implementation which should be used
     */
    public LambdaManager(final Supplier<Map> mapSupplier, final Supplier<List<AHandler>> listSupplier, final IGenerator generator) {
        this.handlers = mapSupplier.get();
        this.handlerArrays = mapSupplier.get();
        this.parentsCache = mapSupplier.get();
        this.listSupplier = listSupplier;
        this.generator = generator;
    }

    /**
     * Set the {@link IExceptionHandler} which is used to handle thrown exceptions in event handlers.
     *
     * @param exceptionHandler The {@link IExceptionHandler} which should be used to handle exceptions
     */
    public void setExceptionHandler(final IExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * Register all event handlers from super classes.<br>
     * <b>This does not include static event handlers!</b>
     *
     * @param registerSuperHandler If super classes should be scanned for event handlers
     */
    public void setRegisterSuperHandler(final boolean registerSuperHandler) {
        this.registerSuperHandler = registerSuperHandler;
    }

    /**
     * Always call all event handlers for parent classes of the event (including interfaces).<br>
     * Basically a redirect from {@link #call(Object)} to {@link #callParents(Object)}.
     *
     * @param alwaysCallParents If all event handlers for parent classes should be called
     */
    public void setAlwaysCallParents(final boolean alwaysCallParents) {
        this.alwaysCallParents = alwaysCallParents;
    }


    /**
     * Call all handlers for the given event.
     *
     * @param event The event instance
     * @param <T>   The event type
     * @return The given event instance
     */
    @Nonnull
    public <T> T call(final T event) {
        if (this.alwaysCallParents) return this.callParents(event);
        AHandler[] handlers = this.handlerArrays.get(event.getClass());
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
     * Call all handlers for the given event and all parent classes of the event (including interfaces).<br>
     * {@link RuntimeException} -{@literal >} {@link Exception} -{@literal >} {@link Throwable} -{@literal >} {@link Serializable} -{@literal >} {@link Object}
     *
     * @param event The event instance
     * @param <T>   The event type
     * @return The given event instance
     */
    @Nonnull
    public <T> T callParents(final T event) {
        for (Class<?> clazz : this.parentsCache.computeIfAbsent(event.getClass(), clazz -> {
            Set<Class<?>> parents = new LinkedHashSet<>();
            Class<?> current = clazz;
            while (current != null) {
                parents.add(current);
                Collections.addAll(parents, current.getInterfaces());
                current = current.getSuperclass();
            }
            return parents.toArray(new Class[0]);
        })) {
            AHandler[] handlers = this.handlerArrays.get(clazz);
            if (handlers == null) continue;

            for (AHandler handler : handlers) {
                try {
                    handler.call(event);
                } catch (StopCall ignored) {
                    break;
                } catch (Throwable t) {
                    this.exceptionHandler.handle(handler, event, t);
                }
            }
        }
        return event;
    }


    /**
     * Register all static event handlers in the given class.
     *
     * @param owner The class which should be scanned
     */
    public void register(final Class<?> owner) {
        this.register(null, owner);
    }

    /**
     * Register all static event handlers for the given event in the given class.
     *
     * @param event The event class
     * @param owner The class which should be scanned
     */
    public void register(@Nullable final Class<?> event, final Class<?> owner) {
        for (EventUtils.MethodHandler handler : EventUtils.getMethods(owner, method -> Modifier.isStatic(method.getModifiers()), false)) {
            if (event == null) this.registerMethod(handler.getOwner(), null, handler.getAnnotation(), handler.getMethod(), e -> true);
            else this.registerMethod(handler.getOwner(), null, handler.getAnnotation(), handler.getMethod(), e -> e.equals(event));
        }
        for (EventUtils.FieldHandler handler : EventUtils.getFields(owner, field -> Modifier.isStatic(field.getModifiers()), false)) {
            if (event == null) this.registerField(handler.getOwner(), null, handler.getAnnotation(), handler.getField(), e -> true);
            else this.registerField(handler.getOwner(), null, handler.getAnnotation(), handler.getField(), e -> e.equals(event));
        }
    }

    /**
     * Register all non-static event handlers in the given object's class.
     *
     * @param owner The object which should be scanned
     */
    public void register(final Object owner) {
        this.register(null, owner);
    }

    /**
     * Register all non-static event handlers for the given event in the given object's class.
     *
     * @param event The event class
     * @param owner The object which should be scanned
     */
    public void register(@Nullable final Class<?> event, final Object owner) {
        this.registerNonStatic(event, owner, this.registerSuperHandler);
    }

    /**
     * Register all non-static event handlers for the given object's class and all its super classes.<br>
     * This does not include static event handlers!
     *
     * @param owner The object which should be scanned
     */
    public void registerSuper(final Object owner) {
        this.registerSuper(null, owner);
    }

    /**
     * Register all non-static event handlers for the given event in the given object's class and all its super classes.<br>
     * This does not include static event handlers!
     *
     * @param event The event class
     * @param owner The object which should be scanned
     */
    public void registerSuper(@Nullable final Class<?> event, final Object owner) {
        this.registerNonStatic(event, owner, true);
    }

    /**
     * Register a {@link Runnable} as an event handler for the given events.
     *
     * @param runnable The {@link Runnable} which should be registered
     * @param events   The events for which the {@link Runnable} should be registered
     */
    public void register(final Runnable runnable, final Class<?>... events) {
        this.register(runnable, 0, events);
    }

    /**
     * Register a {@link Runnable} as an event handler for the given events with the given priority.
     *
     * @param runnable The {@link Runnable} which should be registered
     * @param priority The priority of the {@link Runnable}
     * @param events   The events for which the {@link Runnable} should be registered
     */
    public void register(final Runnable runnable, final int priority, final Class<?>... events) {
        if (events.length == 0) throw new IllegalArgumentException("No events specified");
        synchronized (this.handlers) {
            for (Class<?> event : events) {
                List<AHandler> handlers = this.handlers.computeIfAbsent(event, (key) -> this.listSupplier.get());
                handlers.add(new RunnableHandler(runnable.getClass(), null, EventUtils.newEventHandler(priority), runnable));
                this.checkCallChain(event, handlers);
            }
        }
    }

    /**
     * Register a {@link Consumer} as an event handler for the given events.<br>
     * Be aware that the {@link Consumer} will be called with the event instance as parameter.
     * When using wrong type args this will result in a {@link ClassCastException}.
     *
     * @param consumer The {@link Consumer} which should be registered
     * @param events   The events for which the {@link Consumer} should be registered
     */
    public void register(final Consumer<?> consumer, final Class<?>... events) {
        this.register(consumer, 0, events);
    }

    /**
     * Register a {@link Consumer} as an event handler for the given events with the given priority.<br>
     * Be aware that the {@link Consumer} will be called with the event instance as parameter.
     * When using wrong type args this will result in a {@link ClassCastException}.
     *
     * @param consumer The {@link Consumer} which should be registered
     * @param priority The priority of the {@link Consumer}
     * @param events   The events for which the {@link Consumer} should be registered
     */
    public void register(final Consumer<?> consumer, final int priority, final Class<?>... events) {
        if (events.length == 0) throw new IllegalArgumentException("No events specified");
        synchronized (this.handlers) {
            for (Class<?> event : events) {
                List<AHandler> handlers = this.handlers.computeIfAbsent(event, (key) -> this.listSupplier.get());
                handlers.add(new ConsumerHandler(consumer.getClass(), null, EventUtils.newEventHandler(priority), consumer));
                this.checkCallChain(event, handlers);
            }
        }
    }

    private void registerNonStatic(@Nullable final Class<?> event, final Object owner, final boolean registerSuperHandler) {
        for (EventUtils.MethodHandler handler : EventUtils.getMethods(owner.getClass(), method -> !Modifier.isStatic(method.getModifiers()), registerSuperHandler)) {
            if (event == null) this.registerMethod(handler.getOwner(), owner, handler.getAnnotation(), handler.getMethod(), e -> true);
            else this.registerMethod(handler.getOwner(), owner, handler.getAnnotation(), handler.getMethod(), e -> e.equals(event));
        }
        for (EventUtils.FieldHandler handler : EventUtils.getFields(owner.getClass(), field -> !Modifier.isStatic(field.getModifiers()), registerSuperHandler)) {
            if (event == null) this.registerField(handler.getOwner(), owner, handler.getAnnotation(), handler.getField(), e -> true);
            else this.registerField(handler.getOwner(), owner, handler.getAnnotation(), handler.getField(), e -> e.equals(event));
        }
    }

    private void registerMethod(final Class<?> owner, @Nullable final Object instance, final EventHandler annotation, final Method method, final Predicate<Class<?>> accept) {
        EventUtils.verify(owner, annotation, method);
        for (Class<?> event : EventUtils.getEvents(annotation, method, accept)) this.registerMethod(owner, instance, annotation, method, event, method.getParameterCount() == 0);
    }

    private void registerMethod(final Class<?> owner, @Nullable final Object instance, final EventHandler annotation, final Method method, final Class<?> event, final boolean virtual) {
        synchronized (this.handlers) {
            List<AHandler> handlers = this.handlers.computeIfAbsent(event, (key) -> this.listSupplier.get());
            AHandler handler;
            if (virtual) handler = this.generator.generateVirtual(owner, instance, annotation, method);
            else handler = this.generator.generate(owner, instance, annotation, method, event);
            handlers.add(handler);
            this.checkCallChain(event, handlers);
        }
    }

    private void registerField(final Class<?> owner, @Nullable final Object instance, final EventHandler annotation, final Field field, final Predicate<Class<?>> accept) {
        EventUtils.verify(owner, annotation, field);
        for (Class<?> event : EventUtils.getEvents(annotation, field, accept)) this.registerField(owner, instance, annotation, field, event);
    }

    private void registerField(final Class<?> owner, @Nullable final Object instance, final EventHandler annotation, final Field field, final Class<?> event) {
        synchronized (this.handlers) {
            List<AHandler> handlers = this.handlers.computeIfAbsent(event, (key) -> this.listSupplier.get());
            AHandler handler;
            try {
                if (Runnable.class.isAssignableFrom(field.getType())) handler = new RunnableHandler(owner, instance, annotation, (Runnable) field.get(instance));
                else handler = new ConsumerHandler(owner, instance, annotation, (Consumer<?>) field.get(instance));
            } catch (Throwable t) {
                throw new RuntimeException("Failed to register field '" + field.getName() + "' in class '" + owner.getName() + "'", t);
            }
            handlers.add(handler);
            this.checkCallChain(event, handlers);
        }
    }


    /**
     * Unregister all static event handlers from the given class.
     *
     * @param owner The class from which the static event handlers should be unregistered
     */
    public void unregister(final Class<?> owner) {
        synchronized (this.handlers) {
            Map<Class<?>, List<AHandler>> checked = new HashMap<>();
            for (Map.Entry<Class<?>, List<AHandler>> entry : this.handlers.entrySet()) {
                List<AHandler> handlers = entry.getValue();
                handlers.removeIf(handler -> handler.isStatic() && handler.getOwner().equals(owner));
                checked.put(entry.getKey(), handlers);
            }
            for (Map.Entry<Class<?>, List<AHandler>> entry : checked.entrySet()) this.checkCallChain(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Unregister all static event handlers for the given event from the given class.
     *
     * @param event The event class
     * @param owner The class from which the static event handlers should be unregistered
     */
    public void unregister(final Class<?> event, final Class<?> owner) {
        synchronized (this.handlers) {
            List<AHandler> handlers = this.handlers.get(event);
            if (handlers == null) return;
            handlers.removeIf(handler -> handler.isStatic() && handler.getOwner().equals(owner));
            this.checkCallChain(event, handlers);
        }
    }

    /**
     * Unregister all non-static event handlers from the given object's class.
     *
     * @param owner The object from which the non-static event handlers should be unregistered
     */
    @SuppressWarnings("DataFlowIssue")
    public void unregister(final Object owner) {
        synchronized (this.handlers) {
            Map<Class<?>, List<AHandler>> checked = new HashMap<>();
            for (Map.Entry<Class<?>, List<AHandler>> entry : this.handlers.entrySet()) {
                List<AHandler> handlers = entry.getValue();
                handlers.removeIf(handler -> !handler.isStatic() && handler.getInstance().equals(owner));
                checked.put(entry.getKey(), handlers);
            }
            for (Map.Entry<Class<?>, List<AHandler>> entry : checked.entrySet()) this.checkCallChain(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Unregister all non-static event handlers for the given event from the given object's class.
     *
     * @param event The event class
     * @param owner The object from which the non-static event handlers should be unregistered
     */
    @SuppressWarnings("DataFlowIssue")
    public void unregister(final Class<?> event, final Object owner) {
        synchronized (this.handlers) {
            List<AHandler> handlers = this.handlers.get(event);
            if (handlers == null) return;
            handlers.removeIf(handler -> !handler.isStatic() && handler.getInstance().equals(owner));
            this.checkCallChain(event, handlers);
        }
    }

    /**
     * Unregister a {@link Runnable} from all events.
     *
     * @param runnable The {@link Runnable} to unregister
     */
    public void unregister(final Runnable runnable) {
        synchronized (this.handlers) {
            Map<Class<?>, List<AHandler>> checked = new HashMap<>();
            for (Map.Entry<Class<?>, List<AHandler>> entry : this.handlers.entrySet()) {
                List<AHandler> handlers = entry.getValue();
                handlers.removeIf(handler -> handler instanceof RunnableHandler && ((RunnableHandler) handler).getRunnable().equals(runnable));
                checked.put(entry.getKey(), handlers);
            }
            for (Map.Entry<Class<?>, List<AHandler>> entry : checked.entrySet()) this.checkCallChain(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Unregister a {@link Runnable} from the given events.
     *
     * @param runnable The {@link Runnable} to unregister
     * @param events   The events from which the {@link Runnable} should be unregistered
     */
    public void unregister(final Runnable runnable, final Class<?>... events) {
        if (events.length == 0) {
            this.unregister(runnable);
            return;
        }
        synchronized (this.handlers) {
            for (Class<?> event : events) {
                List<AHandler> handlers = this.handlers.get(event);
                if (handlers == null) continue;
                handlers.removeIf(handler -> handler instanceof RunnableHandler && ((RunnableHandler) handler).getRunnable().equals(runnable));
                this.checkCallChain(event, handlers);
            }
        }
    }

    /**
     * Unregister a {@link Consumer} from all events.
     *
     * @param consumer The {@link Consumer} to unregister
     */
    public void unregister(final Consumer<?> consumer) {
        synchronized (this.handlers) {
            Map<Class<?>, List<AHandler>> checked = new HashMap<>();
            for (Map.Entry<Class<?>, List<AHandler>> entry : this.handlers.entrySet()) {
                List<AHandler> handlers = entry.getValue();
                handlers.removeIf(handler -> handler instanceof ConsumerHandler && ((ConsumerHandler) handler).getConsumer().equals(consumer));
                checked.put(entry.getKey(), handlers);
            }
            for (Map.Entry<Class<?>, List<AHandler>> entry : checked.entrySet()) this.checkCallChain(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Unregister a {@link Consumer} from the given events.
     *
     * @param consumer The {@link Consumer} to unregister
     * @param events   The events from which the {@link Consumer} should be unregistered
     */
    public void unregister(final Consumer<?> consumer, final Class<?>... events) {
        if (events.length == 0) {
            this.unregister(consumer);
            return;
        }
        synchronized (this.handlers) {
            for (Class<?> event : events) {
                List<AHandler> handlers = this.handlers.get(event);
                if (handlers == null) continue;
                handlers.removeIf(handler -> handler instanceof ConsumerHandler && ((ConsumerHandler) handler).getConsumer().equals(consumer));
                this.checkCallChain(event, handlers);
            }
        }
    }


    private void checkCallChain(final Class<?> event, final List<AHandler> handlers) {
        if (handlers.isEmpty()) {
            this.handlers.remove(event);
            this.handlerArrays.remove(event);
            return;
        } else if (handlers.size() > 1) {
            handlers.sort(Comparator.comparingInt((AHandler o) -> o.getAnnotation().priority()).reversed());
        }
        this.handlerArrays.put(event, handlers.toArray(new AHandler[0]));
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("LambdaManager{\n");
        for (Map.Entry<Class<?>, AHandler[]> entry : this.handlerArrays.entrySet()) {
            out.append("\t").append(entry.getKey().getName()).append("[\n");
            for (AHandler handler : entry.getValue()) out.append("\t\t").append(handler.toString()).append("\n");
            out.append("\t]\n");
        }
        return out.append("}").toString();
    }

}
