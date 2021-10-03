package net.lenni0451.le;

import java.lang.annotation.Annotation;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LambdaManager {

    public static final StopCall STOP = new StopCall();
    private static LambdaManager GLOBAL;

    /**
     * Get the global instance of the {@link LambdaManager}<br>
     * This method has a short name to prevent call lines getting too long
     */
    public static LambdaManager g() {
        if (GLOBAL == null) GLOBAL = new LambdaManager();
        return GLOBAL;
    }


    private final Map<Class<?>, List<Caller>> invoker = new ConcurrentHashMap<>();
    private Consumer<Throwable> exceptionHandler = Throwable::printStackTrace;

    /**
     * Register all event listener in a class
     *
     * @param instanceOrClass Instance if virtual events/Class if static events
     */
    public void register(final Object instanceOrClass) {
        this.register(null, instanceOrClass);
    }

    /**
     * Register all event listener in a class
     *
     * @param eventClass      The class of the event to register
     * @param instanceOrClass Instance if virtual events/Class if static events
     */
    public void register(final Class<?> eventClass, final Object instanceOrClass) {
        Objects.requireNonNull(instanceOrClass, "Instance or class can not be null");

        final boolean isStatic = instanceOrClass instanceof Class<?>;
        final Class<?> clazz = isStatic ? (Class<?>) instanceOrClass : instanceOrClass.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            EventHandler handlerInfo = method.getDeclaredAnnotation(EventHandler.class);
            if (handlerInfo == null) continue;
            if (isStatic != Modifier.isStatic(method.getModifiers())) continue;
            if (method.getParameterCount() != 1) continue;
            if (eventClass != null && !method.getParameterTypes()[0].equals(eventClass)) continue;
            try {
                List<Caller> list = this.invoker.computeIfAbsent(method.getParameterTypes()[0], c -> new CopyOnWriteArrayList<>());
                list.add(this.generate(isStatic ? null : instanceOrClass, method, handlerInfo));
                list.sort(Caller.COMPARATOR);
            } catch (Throwable e) {
                throw new IllegalStateException("Unable to create Consumer for method '" + method.getName() + "' in class '" + method.getDeclaringClass().getName() + "'", e);
            }
        }
    }

    /**
     * Register a {@link Consumer} directly as a event handler<br>
     * Inline event handling is possible with this
     *
     * @param eventClass The class of the event to register
     * @param consumer   The {@link Consumer} you want to register
     * @param priority   The priority of the handler
     * @param <T>        The event type
     */
    public <T> void register(final Class<T> eventClass, final Consumer<T> consumer, final byte priority) {
        Objects.requireNonNull(eventClass, "Event class can not be null");
        Objects.requireNonNull(consumer, "Consumer can not be null");

        Caller directCaller = new Caller(consumer.getClass(), new EventHandler() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return EventHandler.class;
            }

            @Override
            public byte priority() {
                return priority;
            }
        }, consumer);
        List<Caller> list = this.invoker.computeIfAbsent(eventClass, c -> new CopyOnWriteArrayList<>());
        list.add(directCaller);
        list.sort(Caller.COMPARATOR);
    }


    /**
     * Unregister all event listener in a class
     *
     * @param instanceOrClass Instance if virtual events/Class if static events
     */
    public void unregister(final Object instanceOrClass) {
        this.unregister(null, instanceOrClass);
    }

    /**
     * Unregister all event listener in a class
     *
     * @param eventClass      The class of the event to register
     * @param instanceOrClass Instance if virtual events/Class if static events
     */
    public void unregister(final Class<?> eventClass, final Object instanceOrClass) {
        Objects.requireNonNull(instanceOrClass, "Instance or class can not be null");

        final boolean isStatic = instanceOrClass instanceof Class<?>;
        final Class<?> clazz = isStatic ? (Class<?>) instanceOrClass : instanceOrClass.getClass();
        List<Class<?>> toRemove = new ArrayList<>();
        if (eventClass == null) {
            for (Map.Entry<Class<?>, List<Caller>> entry : this.invoker.entrySet()) {
                List<Caller> list = entry.getValue();
                list.removeIf(caller -> caller.isStatic() == isStatic && caller.getOwnerClass().equals(clazz));
                if (list.isEmpty()) toRemove.add(entry.getKey());
            }
        } else {
            List<Caller> list = this.invoker.get(clazz);
            if (list == null) return;
            list.removeIf(caller -> caller.isStatic() == isStatic && caller.getOwnerClass().equals(clazz));
        }
        for (Class<?> clazzToRemove : toRemove) this.invoker.remove(clazzToRemove);
    }

    /**
     * Unregister a {@link Consumer} directly
     *
     * @param consumer The {@link Consumer} to unregister
     */
    public void unregister(final Consumer<?> consumer) {
        for (Map.Entry<Class<?>, List<Caller>> entry : this.invoker.entrySet()) {
            entry.getValue().removeIf(caller -> caller.getOwnerClass().equals(consumer.getClass()));
        }
    }


    /**
     * Call all event listener
     *
     * @param event The event to call with
     */
    public <T> T call(final T event) {
        Objects.requireNonNull(event, "Event can not be null");

        List<Caller> list = this.invoker.get(event.getClass());
        if (list == null) return event;
        try {
            for (Caller caller : list) caller.call(event);
        } catch (StopCall ignored) {
        } catch (Throwable t) {
            this.exceptionHandler.accept(t);
        }
        return event;
    }

    /**
     * Set the event call exception handler
     */
    public void setExceptionHandler(final Consumer<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }


    /**
     * Generate a {@link Caller} for the given method
     *
     * @param instance    The instance of the method owner
     * @param method      The method to call
     * @param handlerInfo The {@link EventHandler} annotation of the method
     * @return The new Caller instance for virtual and static calls
     * @throws Throwable Because {@link java.lang.invoke.MethodHandle#invokeExact(Object...)} can throw a throwable
     */
    private Caller generate(final Object instance, final Method method, final EventHandler handlerInfo) throws Throwable {
        final boolean isStatic = instance == null;
        if (isStatic) {
            CallSite callSite = LambdaMetafactory.metafactory(
                    MethodHandles.lookup(),
                    "accept",
                    MethodType.methodType(Consumer.class),
                    MethodType.methodType(void.class, Object.class),
                    MethodHandles.lookup().findStatic(method.getDeclaringClass(), method.getName(), MethodType.methodType(void.class, method.getParameterTypes()[0])),
                    MethodType.methodType(void.class, method.getParameterTypes()[0])
            );
            Consumer consumer = (Consumer) callSite.getTarget().invokeExact();
            return new Caller(method.getDeclaringClass(), handlerInfo, consumer);
        } else {
            CallSite callSite = LambdaMetafactory.metafactory(
                    MethodHandles.lookup(),
                    "accept",
                    MethodType.methodType(BiConsumer.class),
                    MethodType.methodType(void.class, Object.class, Object.class),
                    MethodHandles.lookup().findVirtual(method.getDeclaringClass(), method.getName(), MethodType.methodType(void.class, method.getParameterTypes()[0])),
                    MethodType.methodType(void.class, instance.getClass(), method.getParameterTypes()[0])
            );
            BiConsumer consumer = (BiConsumer) callSite.getTarget().invokeExact();
            return new Caller(method.getDeclaringClass(), instance, handlerInfo, consumer);
        }
    }

}
