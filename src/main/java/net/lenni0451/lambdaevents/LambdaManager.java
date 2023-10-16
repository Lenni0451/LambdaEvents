package net.lenni0451.lambdaevents;

import net.lenni0451.lambdaevents.handler.ConsumerHandler;
import net.lenni0451.lambdaevents.handler.RunnableHandler;
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

    private IExceptionHandler exceptionHandler = IExceptionHandler.infoPrint();
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
        if (this.alwaysCallParents) return this.callParents(event); //Redirect to callParents() if alwaysCallParents is true
        AHandler[] handlers = this.handlerArrays.get(event.getClass());
        if (handlers == null) return event; //No handlers registered for this event

        for (AHandler handler : handlers) {
            try {
                handler.call(event);
            } catch (StopCall ignored) {
                break; //Stop calling the following handlers
            } catch (Throwable t) {
                this.exceptionHandler.handle(handler, event, t); //The handler threw an exception, handle it and continue
            }
        }
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
        for (Class<?> clazz : this.parentsCache.computeIfAbsent(event.getClass(), clazz -> {
            //Calculate all parent classes and interfaces and cache them
            Set<Class<?>> parents = new LinkedHashSet<>();
            EventUtils.getSuperClasses(parents, clazz);
            return parents.toArray(new Class[0]);
        })) {
            AHandler[] handlers = this.handlerArrays.get(clazz);
            if (handlers == null) continue; //No handlers registered for this event class

            for (AHandler handler : handlers) {
                try {
                    handler.call(event);
                } catch (StopCall ignored) {
                    break; //Stop calling the following handlers
                } catch (Throwable t) {
                    this.exceptionHandler.handle(handler, event, t); //The handler threw an exception, handle it and continue
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
                //Add a new ConsumerHandler for each event
                List<AHandler> handlers = this.handlers.computeIfAbsent(event, (key) -> this.listSupplier.get());
                handlers.add(new ConsumerHandler(consumer.getClass(), null, EventUtils.newEventHandler(priority), consumer));
                this.checkCallChain(event, handlers);
            }
        }
    }

    private void register(@Nullable final Class<?> event, final Class<?> owner, @Nullable final Object instance, final boolean isStatic, final boolean registerSuperHandler) {
        Predicate<Class<?>> eventFilter;
        if (event == null) eventFilter = e -> true; //Register all events
        else eventFilter = e -> e.equals(event); //Only register the given event

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
    public void unregister(final Runnable runnable) {
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
    public void unregister(final Runnable runnable, final Class<?>... events) {
        if (events.length == 0) {
            this.unregister(runnable); //Redirect to the other method if no events are specified
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
    public void unregister(final Consumer<?> consumer) {
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
    public void unregister(final Consumer<?> consumer, final Class<?>... events) {
        if (events.length == 0) {
            this.unregister(consumer); //Redirect to the other method if no events are specified
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
