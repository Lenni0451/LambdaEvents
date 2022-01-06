package net.lenni0451.lambdaevents;

import java.lang.annotation.Annotation;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class LambdaManager {

    private static LambdaManager GLOBAL;

    /**
     * Get the global instance of the {@link LambdaManager}<br>
     * This method has a short name to prevent call lines getting too long
     */
    public static LambdaManager g() {
        if (GLOBAL == null) GLOBAL = new LambdaManager();
        return GLOBAL;
    }

    /**
     * Call this method in an event handler to stop the current event pipeline from going further
     */
    public static void stop() {
        throw new StopCall();
    }


    private final Map<Class<?>, List<Caller>> invoker = new ConcurrentHashMap<>();
    private Consumer<Throwable> exceptionHandler = Throwable::printStackTrace;

    /**
     * Register all event listener in a class
     *
     * @param handler Instance if virtual events/Class if static events
     * @throws IllegalStateException If there is something wrong with your listener
     */
    public void register(final Object handler) {
        this.register(null, handler);
    }

    /**
     * Register all event listener in a class
     *
     * @param eventClass The class of the event to register
     * @param handler    Instance if virtual events/Class if static events
     * @throws IllegalStateException If there is something wrong with your listener
     */
    public void register(final Class<?> eventClass, final Object handler) {
        Objects.requireNonNull(handler, "Instance or class can not be null");

        final boolean isStatic = handler instanceof Class<?>;
        final Class<?> clazz = isStatic ? (Class<?>) handler : handler.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            EventHandler handlerInfo = method.getDeclaredAnnotation(EventHandler.class);
            if (handlerInfo == null) continue;
            if (handlerInfo.eventClasses().length != 0) {
                //The listener would get skipped. This may save someone from a lot of headache because of event listener not behaving as expected
                throw new IllegalStateException("Method '" + method.getName() + "' in class '" + method.getDeclaringClass().getName() + "' has event classes specified in annotation which is not supported");
            }
            if (isStatic != Modifier.isStatic(method.getModifiers())) continue;
            if (method.getParameterCount() != 1) {
                //The listener would get skipped. This may save someone from a lot of headache because of not working event listener
                throw new IllegalStateException("Method '" + method.getName() + "' in class '" + method.getDeclaringClass().getName() + "' has multiple arguments specified which is not supported");
            }
            if (eventClass != null && !method.getParameterTypes()[0].equals(eventClass)) continue;
            if (!Modifier.isPublic(method.getModifiers())) {
                throw new IllegalStateException("Method '" + method.getName() + "' in class '" + method.getDeclaringClass().getName() + "' is not public which is not supported");
            }
            try {
                List<Caller> list = this.invoker.computeIfAbsent(method.getParameterTypes()[0], c -> new CopyOnWriteArrayList<>());
                list.add(this.generate(isStatic ? null : handler, method, handlerInfo));
                list.sort(Caller.COMPARATOR);
            } catch (Throwable t) {
                throw new IllegalStateException("Unable to create Consumer for method '" + method.getName() + "' in class '" + method.getDeclaringClass().getName() + "'", t);
            }
        }
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            EventHandler handlerInfo = field.getDeclaredAnnotation(EventHandler.class);
            if (!field.getType().equals(Consumer.class)) continue;
            if (handlerInfo == null) continue;
            if (handlerInfo.eventClasses().length == 0) {
                throw new IllegalStateException("Consumer '" + field.getName() + "' in class '" + field.getDeclaringClass().getName() + "' does not have any event types set");
            }
            if (isStatic != Modifier.isStatic(field.getModifiers())) continue;
            if (eventClass != null && !Arrays.asList(handlerInfo.eventClasses()).contains(eventClass)) continue;
            try {
                Consumer consumer = (Consumer) field.get(isStatic ? null : handler);
                for (Class<?> eventType : handlerInfo.eventClasses()) {
                    List<Caller> list = this.invoker.computeIfAbsent(eventType, c -> new CopyOnWriteArrayList<>());
                    Caller caller = new Caller(field.getDeclaringClass(), handlerInfo, consumer);
                    Caller._setStatic(caller, isStatic);
                    list.add(caller);
                    list.sort(Caller.COMPARATOR);
                }
            } catch (Throwable t) {
                throw new IllegalStateException("Unable to get Consumer '" + field.getName() + "' in class '" + field.getDeclaringClass().getName() + "'", t);
            }
        }
    }

    /**
     * Register a {@link Consumer} directly as an event handler<br>
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

            @Override
            public Class<?>[] eventClasses() {
                return new Class<?>[0];
            }
        }, consumer);
        List<Caller> list = this.invoker.computeIfAbsent(eventClass, c -> new CopyOnWriteArrayList<>());
        list.add(directCaller);
        list.sort(Caller.COMPARATOR);
    }


    /**
     * Unregister all event listener in a class
     *
     * @param handler Instance if virtual events/Class if static events
     */
    public void unregister(final Object handler) {
        this.unregister(null, handler);
    }

    /**
     * Unregister all event listener in a class
     *
     * @param eventClass The class of the event to register
     * @param handler    Instance if virtual events/Class if static events
     */
    public void unregister(final Class<?> eventClass, final Object handler) {
        Objects.requireNonNull(handler, "Instance or class can not be null");

        final boolean isStatic = handler instanceof Class<?>;
        final Class<?> clazz = isStatic ? (Class<?>) handler : handler.getClass();
        List<Class<?>> toRemove = new ArrayList<>();
        if (eventClass == null) {
            for (Map.Entry<Class<?>, List<Caller>> entry : this.invoker.entrySet()) {
                List<Caller> list = entry.getValue();
                list.removeIf(caller -> caller.isStatic() == isStatic && caller.getOwnerClass().equals(clazz));
                if (list.isEmpty()) toRemove.add(entry.getKey());
            }
        } else {
            List<Caller> list = this.invoker.get(eventClass);
            if (list == null) return;
            list.removeIf(caller -> caller.isStatic() == isStatic && caller.getOwnerClass().equals(clazz));
        }
        for (Class<?> clazzToRemove : toRemove) this.invoker.remove(clazzToRemove);
    }

    /**
     * Unregister a {@link Consumer} directly
     *
     * @param eventClass The class of the event to unregister
     * @param consumer   The {@link Consumer} to unregister
     * @param <T>        The event type
     */
    public <T> void unregister(final Class<T> eventClass, final Consumer<?> consumer) {
        this.invoker.get(eventClass).removeIf(caller -> caller.getOwnerClass().equals(consumer.getClass()));
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
        Map<String, Caller> eventCache = null;
        try {
            Field f = method.getDeclaringClass().getField("_LAMBDA_CACHE");
            f.setAccessible(true);
            eventCache = (Map<String, Caller>) f.get(instance);
        } catch (Throwable ignored) {
        }
        if (isStatic) {
            String cacheKey = "static " + method.getDeclaringClass().getName() + " " + method;
            if (eventCache != null && eventCache.containsKey(cacheKey)) {
                return eventCache.get(cacheKey);
            } else {
                CallSite callSite = LambdaMetafactory.metafactory(
                        MethodHandles.lookup(),
                        "accept",
                        MethodType.methodType(Consumer.class),
                        MethodType.methodType(void.class, Object.class),
                        MethodHandles.lookup().findStatic(method.getDeclaringClass(), method.getName(), MethodType.methodType(void.class, method.getParameterTypes()[0])),
                        MethodType.methodType(void.class, method.getParameterTypes()[0])
                );
                Caller caller = new Caller(method.getDeclaringClass(), handlerInfo, (Consumer) callSite.getTarget().invokeExact());
                if (eventCache != null) eventCache.put(cacheKey, caller);
                return caller;
            }
        } else {
            String cacheKey = "virtual " + method.getDeclaringClass().getName() + " " + method;
            if (eventCache != null && eventCache.containsKey(cacheKey)) {
                return eventCache.get(cacheKey);
            } else {
                CallSite callSite = LambdaMetafactory.metafactory(
                        MethodHandles.lookup(),
                        "accept",
                        MethodType.methodType(BiConsumer.class),
                        MethodType.methodType(void.class, Object.class, Object.class),
                        MethodHandles.lookup().findVirtual(method.getDeclaringClass(), method.getName(), MethodType.methodType(void.class, method.getParameterTypes()[0])),
                        MethodType.methodType(void.class, instance.getClass(), method.getParameterTypes()[0])
                );
                Caller caller = new Caller(method.getDeclaringClass(), instance, handlerInfo, (BiConsumer) callSite.getTarget().invokeExact());
                if (eventCache != null) eventCache.put(cacheKey, caller);
                return caller;
            }
        }
    }

}
