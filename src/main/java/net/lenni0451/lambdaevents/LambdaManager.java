package net.lenni0451.lambdaevents;

import net.lenni0451.lambdaevents.handler.ConsumerHandler;
import net.lenni0451.lambdaevents.handler.RunnableHandler;
import net.lenni0451.lambdaevents.types.ICancellableEvent;
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
import java.util.function.BiPredicate;
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

    @Nullable
    private IEventFilter eventFilter = null;
    private IExceptionHandler exceptionHandler = IExceptionHandler.infoPrint();
    private boolean registerSuperHandler = false;
    private boolean alwaysCallParents = false;

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
     * Set the event filter which is used to validate registered and called events.<br>
     * This can be used to only allow certain events to be registered or called.<br>
     * If an event is not allowed
     *
     * @param eventFilter The {@link Consumer} which should be used to filter events
     * @return The current {@link LambdaManager} instance
     */
    public LambdaManager setEventFilter(@Nullable final IEventFilter eventFilter) {
        this.eventFilter = eventFilter;
        return this;
    }

    /**
     * Set the {@link IExceptionHandler} which is used to handle thrown exceptions in event handlers.
     *
     * @param exceptionHandler The {@link IExceptionHandler} which should be used to handle exceptions
     * @return The current {@link LambdaManager} instance
     */
    public LambdaManager setExceptionHandler(final IExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    /**
     * Register all event handlers from super classes.<br>
     * <b>This does not include static event handlers!</b>
     *
     * @param registerSuperHandler If super classes should be scanned for event handlers
     * @return The current {@link LambdaManager} instance
     */
    public LambdaManager setRegisterSuperHandler(final boolean registerSuperHandler) {
        this.registerSuperHandler = registerSuperHandler;
        return this;
    }

    /**
     * Always call all event handlers for parent classes of the event (including interfaces).<br>
     * Basically a redirect from {@link #call(Object)} to {@link #callParents(Object)}.
     *
     * @param alwaysCallParents If all event handlers for parent classes should be called
     * @return The current {@link LambdaManager} instance
     */
    public LambdaManager setAlwaysCallParents(final boolean alwaysCallParents) {
        this.alwaysCallParents = alwaysCallParents;
        return this;
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
        if (this.alwaysCallParents) return this.callParents(event); //Redirect to callParents() if alwaysCallParents is true
        if (this.eventFilter != null && !this.eventFilter.check(event.getClass(), IEventFilter.CheckType.CALL)) return event;
        this.call(event.getClass(), event);
        return event;
    }

    /**
     * Call all handlers for the given event and all parent classes of the event (including interfaces).<br>
     * e.g. {@link RuntimeException} {@literal ->} {@link Exception} {@literal ->} {@link Throwable} {@literal ->} {@link Serializable} {@literal ->} {@link Object}
     *
     * @param event The event instance
     * @param <T>   The event type
     * @return The given event instance
     */
    @Nonnull
    public <T> T callParents(final T event) {
        if (this.eventFilter != null && !this.eventFilter.check(event.getClass(), IEventFilter.CheckType.CALL)) return event;
        for (Class<?> clazz : this.parentsCache.computeIfAbsent(event.getClass(), clazz -> {
            //Calculate all parent classes and interfaces and cache them
            Set<Class<?>> parents = new LinkedHashSet<>();
            EventUtils.getSuperClasses(parents, clazz);
            return parents.toArray(new Class[0]);
        })) {
            this.call(clazz, event);
        }
        return event;
    }

    private <T> void call(final Class<?> clazz, final T event) {
        AHandler[] handlers = this.handlerArrays.get(clazz);
        if (handlers == null) return; //No handlers registered for this event
        ICancellableEvent cancellable = event instanceof ICancellableEvent ? (ICancellableEvent) event : null;
        for (AHandler handler : handlers) {
            if (cancellable != null && !handler.shouldHandleCancelled() && cancellable.isCancelled()) {
                //Skip the handler if the event is cancelled
                continue;
            }
            try {
                handler.call(event);
            } catch (StopCall ignored) {
                return; //Stop calling the following handlers
            } catch (Throwable t) {
                this.exceptionHandler.handle(handler, event, t); //The handler threw an exception, handle it and continue
            }
        }
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
        this.register(event, owner, null, true, false);
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
        this.register(event, owner.getClass(), owner, false, this.registerSuperHandler);
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
        this.register(event, owner.getClass(), owner, false, true);
    }

    /**
     * Register a {@link Runnable} as an event handler for the given events.
     *
     * @param runnable The {@link Runnable} which should be registered
     * @param events   The events for which the {@link Runnable} should be registered
     */
    public void registerRunnable(final Runnable runnable, final Class<?>... events) {
        this.registerRunnable(runnable, 0, events);
    }

    /**
     * Register a {@link Runnable} as an event handler for the given events with the given priority.
     *
     * @param runnable The {@link Runnable} which should be registered
     * @param priority The priority of the {@link Runnable}
     * @param events   The events for which the {@link Runnable} should be registered
     */
    public void registerRunnable(final Runnable runnable, final int priority, final Class<?>... events) {
        if (events.length == 0) throw new IllegalArgumentException("No events specified");
        synchronized (this.handlers) {
            for (Class<?> event : events) {
                if (this.eventFilter != null && !this.eventFilter.check(event, IEventFilter.CheckType.EXPLICIT_REGISTER)) continue;
                //Add a new RunnableHandler for each event
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
    public void registerConsumer(final Consumer<?> consumer, final Class<?>... events) {
        this.registerConsumer(consumer, 0, events);
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
    public void registerConsumer(final Consumer<?> consumer, final int priority, final Class<?>... events) {
        if (events.length == 0) throw new IllegalArgumentException("No events specified");
        synchronized (this.handlers) {
            for (Class<?> event : events) {
                if (this.eventFilter != null && !this.eventFilter.check(event, IEventFilter.CheckType.EXPLICIT_REGISTER)) continue;
                //Add a new ConsumerHandler for each event
                List<AHandler> handlers = this.handlers.computeIfAbsent(event, (key) -> this.listSupplier.get());
                handlers.add(new ConsumerHandler(consumer.getClass(), null, EventUtils.newEventHandler(priority), consumer));
                this.checkCallChain(event, handlers);
            }
        }
    }

    /**
     * <b>Deprecated! Please use {@link #registerRunnable(Runnable, Class[])} instead</b>
     */
    @Deprecated
    public void register(final Runnable runnable, final Class<?>... events) {
        this.registerRunnable(runnable, events);
    }

    /**
     * <b>Deprecated! Please use {@link #registerRunnable(Runnable, int, Class[])} instead</b>
     */
    @Deprecated
    public void register(final Runnable runnable, final int priority, final Class<?>... events) {
        this.registerRunnable(runnable, priority, events);
    }

    /**
     * <b>Deprecated! Please use {@link #registerConsumer(Consumer, Class[])} instead</b>
     */
    @Deprecated
    public void register(final Consumer<?> consumer, final Class<?>... events) {
        this.registerConsumer(consumer, events);
    }

    /**
     * <b>Deprecated! Please use {@link #registerConsumer(Consumer, int, Class[])} instead</b>
     */
    @Deprecated
    public void register(final Consumer<?> consumer, final int priority, final Class<?>... events) {
        this.registerConsumer(consumer, priority, events);
    }

    private void register(@Nullable final Class<?> event, final Class<?> owner, @Nullable final Object instance, final boolean isStatic, final boolean registerSuperHandler) {
        Predicate<Class<?>> eventFilter;
        if (event == null) {
            //Register all events
            eventFilter = e -> this.eventFilter == null || this.eventFilter.check(e, IEventFilter.CheckType.REGISTER);
        } else {
            //Only register the given event
            if (this.eventFilter != null && !this.eventFilter.check(event, IEventFilter.CheckType.EXPLICIT_REGISTER)) return;
            eventFilter = e -> e.equals(event);
        }

        for (EventUtils.MethodHandler handler : EventUtils.getMethods(owner, method -> Modifier.isStatic(method.getModifiers()) == isStatic, registerSuperHandler)) {
            //Register all methods which handle the given event
            EventHandler annotation = handler.getAnnotation();
            Method method = handler.getMethod();
            EventUtils.verify(handler.getOwner(), annotation, method); //Check if the method is a valid event handler
            for (Class<?> eventClass : EventUtils.getEvents(annotation, method, eventFilter)) {
                //Go through all events which the method handles and register them
                //Here 'virtual' means that the method does not take the event as a parameter
                this.registerMethod(handler.getOwner(), instance, annotation, method, eventClass, method.getParameterCount() == 0);
            }
        }
        for (EventUtils.FieldHandler handler : EventUtils.getFields(owner, field -> Modifier.isStatic(field.getModifiers()) == isStatic, registerSuperHandler)) {
            //Register all fields which handle the given event
            EventHandler annotation = handler.getAnnotation();
            Field field = handler.getField();
            EventUtils.verify(handler.getOwner(), annotation, field); //Check if the field is a valid event handler
            for (Class<?> eventClass : EventUtils.getEvents(annotation, field, eventFilter)) {
                //Go through all events which the field handles and register them
                this.registerField(handler.getOwner(), instance, annotation, field, eventClass);
            }
        }
    }

    private void registerMethod(final Class<?> owner, @Nullable final Object instance, final EventHandler annotation, final Method method, final Class<?> event, final boolean virtual) {
        synchronized (this.handlers) {
            //Generate a new handler and add it to the list
            List<AHandler> handlers = this.handlers.computeIfAbsent(event, (key) -> this.listSupplier.get());
            AHandler handler;
            if (virtual) handler = this.generator.generateVirtual(owner, instance, annotation, method); //Handler without parameter
            else handler = this.generator.generate(owner, instance, annotation, method, event); //Handler with parameter
            handlers.add(handler);
            this.checkCallChain(event, handlers);
        }
    }

    private void registerField(final Class<?> owner, @Nullable final Object instance, final EventHandler annotation, final Field field, final Class<?> event) {
        synchronized (this.handlers) {
            //Get the field value and create a new handler for it
            List<AHandler> handlers = this.handlers.computeIfAbsent(event, (key) -> this.listSupplier.get());
            AHandler handler;
            try {
                if (Runnable.class.isAssignableFrom(field.getType())) handler = new RunnableHandler(owner, instance, annotation, (Runnable) field.get(instance)); //Runnable handler
                else handler = new ConsumerHandler(owner, instance, annotation, (Consumer<?>) field.get(instance)); //Consumer handler
                //The else block only receiving a Consumer is ensured by EventUtils.verify()
            } catch (Throwable t) {
                //Possible exception when getting the field value
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
                handlers.removeIf(handler -> handler.isStatic() && handler.getOwner().equals(owner)); //Only remove static handlers which belong to the given class
                checked.put(entry.getKey(), handlers);
            }
            for (Map.Entry<Class<?>, List<AHandler>> entry : checked.entrySet()) {
                //Check and redo the call chain for all events
                this.checkCallChain(entry.getKey(), entry.getValue());
            }
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
            handlers.removeIf(handler -> handler.isStatic() && handler.getOwner().equals(owner)); //Only remove static handlers which belong to the given class
            this.checkCallChain(event, handlers);
        }
    }

    /**
     * Unregister all non-static event handlers from the given object's class.
     *
     * @param owner The object from which the non-static event handlers should be unregistered
     */
    public void unregister(final Object owner) {
        synchronized (this.handlers) {
            Map<Class<?>, List<AHandler>> checked = new HashMap<>();
            for (Map.Entry<Class<?>, List<AHandler>> entry : this.handlers.entrySet()) {
                List<AHandler> handlers = entry.getValue();
                handlers.removeIf(handler -> !handler.isStatic() && owner.equals(handler.getInstance())); //Only remove non-static handlers which belong to the given object
                checked.put(entry.getKey(), handlers);
            }
            for (Map.Entry<Class<?>, List<AHandler>> entry : checked.entrySet()) {
                //Check and redo the call chain for all events
                this.checkCallChain(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Unregister all non-static event handlers for the given event from the given object's class.
     *
     * @param event The event class
     * @param owner The object from which the non-static event handlers should be unregistered
     */
    public void unregister(final Class<?> event, final Object owner) {
        synchronized (this.handlers) {
            List<AHandler> handlers = this.handlers.get(event);
            if (handlers == null) return;
            handlers.removeIf(handler -> !handler.isStatic() && owner.equals(handler.getInstance())); //Only remove non-static handlers which belong to the given object
            this.checkCallChain(event, handlers);
        }
    }

    /**
     * Unregister a {@link Runnable} from all events.
     *
     * @param runnable The {@link Runnable} to unregister
     */
    public void unregisterRunnable(final Runnable runnable) {
        synchronized (this.handlers) {
            Map<Class<?>, List<AHandler>> checked = new HashMap<>();
            for (Map.Entry<Class<?>, List<AHandler>> entry : this.handlers.entrySet()) {
                List<AHandler> handlers = entry.getValue();
                //Only remove RunnableHandlers which call the given Runnable
                handlers.removeIf(handler -> handler instanceof RunnableHandler && ((RunnableHandler) handler).getRunnable().equals(runnable));
                checked.put(entry.getKey(), handlers);
            }
            for (Map.Entry<Class<?>, List<AHandler>> entry : checked.entrySet()) {
                //Check and redo the call chain for all events
                this.checkCallChain(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Unregister a {@link Runnable} from the given events.
     *
     * @param runnable The {@link Runnable} to unregister
     * @param events   The events from which the {@link Runnable} should be unregistered
     */
    public void unregisterRunnable(final Runnable runnable, final Class<?>... events) {
        if (events.length == 0) {
            this.unregisterRunnable(runnable); //Redirect to the other method if no events are specified
            return;
        }
        synchronized (this.handlers) {
            for (Class<?> event : events) {
                List<AHandler> handlers = this.handlers.get(event);
                if (handlers == null) continue;
                //Only remove RunnableHandlers which call the given Runnable
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
    public void unregisterConsumer(final Consumer<?> consumer) {
        synchronized (this.handlers) {
            Map<Class<?>, List<AHandler>> checked = new HashMap<>();
            for (Map.Entry<Class<?>, List<AHandler>> entry : this.handlers.entrySet()) {
                List<AHandler> handlers = entry.getValue();
                //Only remove ConsumerHandlers which call the given Consumer
                handlers.removeIf(handler -> handler instanceof ConsumerHandler && ((ConsumerHandler) handler).getConsumer().equals(consumer));
                checked.put(entry.getKey(), handlers);
            }
            for (Map.Entry<Class<?>, List<AHandler>> entry : checked.entrySet()) {
                //Check and redo the call chain for all events
                this.checkCallChain(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Unregister a {@link Consumer} from the given events.
     *
     * @param consumer The {@link Consumer} to unregister
     * @param events   The events from which the {@link Consumer} should be unregistered
     */
    public void unregisterConsumer(final Consumer<?> consumer, final Class<?>... events) {
        if (events.length == 0) {
            this.unregisterConsumer(consumer); //Redirect to the other method if no events are specified
            return;
        }
        synchronized (this.handlers) {
            for (Class<?> event : events) {
                List<AHandler> handlers = this.handlers.get(event);
                if (handlers == null) continue;
                //Only remove ConsumerHandlers which call the given Consumer
                handlers.removeIf(handler -> handler instanceof ConsumerHandler && ((ConsumerHandler) handler).getConsumer().equals(consumer));
                this.checkCallChain(event, handlers);
            }
        }
    }

    /**
     * <b>Deprecated! Please use {@link #unregisterRunnable(Runnable)} instead</b>
     */
    @Deprecated
    public void unregister(final Runnable runnable) {
        this.unregisterRunnable(runnable);
    }

    /**
     * <b>Deprecated! Please use {@link #unregisterRunnable(Runnable, Class[])} instead</b>
     */
    @Deprecated
    public void unregister(final Runnable runnable, final Class<?>... events) {
        this.unregisterRunnable(runnable, events);
    }

    /**
     * <b>Deprecated! Please use {@link #unregisterConsumer(Consumer)} instead</b>
     */
    @Deprecated
    public void unregister(final Consumer<?> consumer) {
        this.unregisterConsumer(consumer);
    }

    /**
     * <b>Deprecated! Please use {@link #unregisterConsumer(Consumer, Class[])} instead</b>
     */
    @Deprecated
    public void unregister(final Consumer<?> consumer, final Class<?>... events) {
        this.unregisterConsumer(consumer, events);
    }

    /**
     * Unregister all handlers for the given event.<br>
     * This unregisters both static and non-static handlers.
     *
     * @param event The event class
     */
    public void unregisterAll(final Class<?> event) {
        synchronized (this.handlers) {
            this.checkCallChain(event, Collections.emptyList());
        }
    }

    /**
     * Unregister all handlers for the given event which match the given filter.<br>
     * This unregisters both static and non-static handlers.
     *
     * @param event  The event class
     * @param filter The filter which should be used to filter the handlers
     */
    public void unregisterAll(final Class<?> event, final Predicate<Class<?>> filter) {
        synchronized (this.handlers) {
            this.unregisterAll(event, filter, false);
            this.unregisterAll(event, filter, true);
        }
    }

    /**
     * Unregister all handlers for the given event which match the given filter.
     *
     * @param event          The event class
     * @param filter         The filter which should be used to filter the handlers
     * @param staticHandlers If static handlers should be unregistered
     */
    public void unregisterAll(final Class<?> event, final Predicate<Class<?>> filter, final boolean staticHandlers) {
        synchronized (this.handlers) {
            List<AHandler> handlers = this.handlers.get(event);
            if (handlers == null) return;
            handlers.removeIf(handler -> {
                if (handler.isStatic() != staticHandlers) return false;
                return filter.test(handler.getOwner());
            });
            this.checkCallChain(event, handlers);
        }
    }

    /**
     * Unregister all handlers for the given event which match the given filter.<br>
     * This unregisters both static and non-static handlers.<br>
     * The optional parameter is the instance of the handler or empty if the handler is static.
     *
     * @param event  The event class
     * @param filter The filter which should be used to filter the handlers
     */
    public void unregisterAll(final Class<?> event, final BiPredicate<Class<?>, Optional<Object>> filter) {
        synchronized (this.handlers) {
            List<AHandler> handlers = this.handlers.get(event);
            if (handlers == null) return;
            handlers.removeIf(handler -> filter.test(handler.getOwner(), Optional.ofNullable(handler.getInstance())));
            this.checkCallChain(event, handlers);
        }
    }


    private void checkCallChain(final Class<?> event, final List<AHandler> handlers) {
        if (handlers.isEmpty()) {
            //If the handlers list is empty remove it from the handler maps
            this.handlers.remove(event);
            this.handlerArrays.remove(event);
            return;
        } else if (handlers.size() > 1) {
            //Resort the handlers if there are more than one
            handlers.sort(Comparator.comparingInt((AHandler o) -> o.getAnnotation().priority()).reversed());
        }
        //Update the handler array
        this.handlerArrays.put(event, handlers.toArray(new AHandler[0]));
    }

    /**
     * Generate a debug output of all registered events and handlers.
     *
     * @return The debug output
     */
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("LambdaManager{\n"); //Header of the debug output
        for (Map.Entry<Class<?>, AHandler[]> entry : this.handlerArrays.entrySet()) {
            out.append("\t").append(entry.getKey().getName()).append("[\n"); //Name of the event class
            for (AHandler handler : entry.getValue()) out.append("\t\t").append(handler.toString()).append("\n"); //The handler toString() method
            out.append("\t]\n");
        }
        return out.append("}").toString();
    }

}
