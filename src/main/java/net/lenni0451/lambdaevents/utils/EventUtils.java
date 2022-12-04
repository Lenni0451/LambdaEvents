package net.lenni0451.lambdaevents.utils;

import net.lenni0451.lambdaevents.EventHandler;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EventUtils {

    /**
     * Get all methods of the given class which are annotated with {@link EventHandler}<br>
     * This method does not verify if the methods are valid
     *
     * @param owner  The class to get the methods from
     * @param accept If the method should be accepted
     * @return The list of all found methods
     */
    @Nonnull
    public static List<MethodHandler> getMethods(@Nonnull final Class<?> owner, @Nonnull final Predicate<Method> accept) {
        List<MethodHandler> handler = new ArrayList<>();
        for (Method method : owner.getDeclaredMethods()) {
            EventHandler annotation = method.getDeclaredAnnotation(EventHandler.class);
            if (annotation == null) continue;
            if (!accept.test(method)) continue;

            handler.add(new MethodHandler(annotation, method));
        }
        return handler;
    }

    /**
     * Get all fields of the given class which are annotated with {@link EventHandler}<br>
     * This method does not verify if the fields are valid
     *
     * @param owner  The class to get the fields from
     * @param accept If the field should be accepted
     * @return The list of all found fields
     */
    @Nonnull
    public static List<FieldHandler> getFields(@Nonnull final Class<?> owner, @Nonnull final Predicate<Field> accept) {
        List<FieldHandler> handler = new ArrayList<>();
        for (Field field : owner.getDeclaredFields()) {
            EventHandler annotation = field.getDeclaredAnnotation(EventHandler.class);
            if (annotation == null) continue;
            if (!accept.test(field)) continue;

            handler.add(new FieldHandler(annotation, field));
        }
        return handler;
    }

    /**
     * Check if the given method is a valid event handler method
     *
     * @param owner      The owner of the method
     * @param annotation The {@link EventHandler} annotation of the method
     * @param method     The method to check
     * @throws IllegalStateException If the method is not valid
     */
    public static void verify(@Nonnull final Class<?> owner, @Nonnull final EventHandler annotation, @Nonnull final Method method) {
        if (Modifier.isAbstract(method.getModifiers())) throw new IllegalStateException("Method '" + method.getName() + "' in class '" + owner.getName() + "' is abstract");
        if (Modifier.isNative(method.getModifiers())) throw new IllegalStateException("Method '" + method.getName() + "' in class '" + owner.getName() + "' is native");
        if (annotation.events().length == 0 && method.getParameterCount() != 1) {
            throw new IllegalStateException("Method '" + method.getName() + "' in class '" + owner.getName() + "' has no virtual events and more than 1 parameter");
        } else if (annotation.events().length > 0 && method.getParameterCount() != 0) {
            throw new IllegalStateException("Method '" + method.getName() + "' in class '" + owner.getName() + "' has virtual events and more than 0 parameters");
        }
        if (!method.getReturnType().equals(void.class)) throw new IllegalStateException("Method '" + method.getName() + "' in class '" + owner.getName() + "' has a return type");
    }

    /**
     * Check if the given field is a valid event handler field
     *
     * @param owner      The owner of the field
     * @param annotation The {@link EventHandler} annotation of the field
     * @param field      The field to check
     */
    public static void verify(@Nonnull final Class<?> owner, @Nonnull final EventHandler annotation, @Nonnull final Field field) {
        if (Runnable.class.isAssignableFrom(field.getType())) {
            if (annotation.events().length == 0) throw new IllegalStateException("Field '" + field.getName() + "' in class '" + owner.getName() + "' has no virtual events");
        } else if (Consumer.class.isAssignableFrom(field.getType())) {
            if (annotation.events().length == 0) {
                if (field.getGenericType() instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
                    if (parameterizedType.getActualTypeArguments().length != 1) {
                        throw new IllegalStateException("Field '" + field.getName() + "' in class '" + owner.getName() + "' has no virtual events and more than 1 generic type");
                    }
                } else {
                    throw new IllegalStateException("Field '" + field.getName() + "' in class '" + owner.getName() + "' has no virtual events and no generic type");
                }
            }
        } else {
            throw new IllegalStateException("Field '" + field.getName() + "' in class '" + owner.getName() + "' is not a Runnable or Consumer");
        }
    }

    /**
     * Get all events handled by the given method
     *
     * @param annotation The {@link EventHandler} annotation of the method
     * @param method     The method to get the events from
     * @param accept     If the event should be accepted
     * @return The list of all found events
     */
    @Nonnull
    public static Class<?>[] getEvents(@Nonnull final EventHandler annotation, @Nonnull final Method method, @Nonnull final Predicate<Class<?>> accept) {
        if (method.getParameterCount() == 1) {
            Class<?> param = method.getParameterTypes()[0];
            if (!accept.test(param)) return new Class[0];
            return new Class<?>[]{param};
        } else {
            return Arrays.stream(annotation.events()).filter(accept).toArray(Class[]::new);
        }
    }

    /**
     * Get all events handled by the given field
     *
     * @param annotation The {@link EventHandler} annotation of the field
     * @param field      The field to get the events from
     * @param accept     If the event should be accepted
     * @return The list of all found events
     */
    @Nonnull
    public static Class<?>[] getEvents(@Nonnull final EventHandler annotation, @Nonnull final Field field, @Nonnull final Predicate<Class<?>> accept) {
        List<Class<?>> events = new ArrayList<>();
        Collections.addAll(events, annotation.events());
        if (Consumer.class.isAssignableFrom(field.getType()) && events.isEmpty()) {
            if (field.getGenericType() instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
                events.add((Class<?>) parameterizedType.getActualTypeArguments()[0]);
            }
        }
        return events.stream().filter(accept).toArray(Class[]::new);
    }

    /**
     * Sneaky throw the given exception without declaring it
     *
     * @param t   The exception to throw
     * @param <T> The type to confuse the compiler
     * @throws T The given exception
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void sneak(@Nonnull final Throwable t) throws T {
        throw (T) t;
    }

    /**
     * Create a new {@link EventHandler} instance with the given priority
     *
     * @param priority The priority of the handler
     * @return The new {@link EventHandler} instance
     */
    @Nonnull
    public static EventHandler newEventHandler(final int priority) {
        return new EventHandler() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return EventHandler.class;
            }

            @Override
            public int priority() {
                return priority;
            }

            @Override
            public Class<?>[] events() {
                return new Class[0];
            }
        };
    }


    /**
     * A wrapper class for an event handler method with it's {@link EventHandler} annotation
     */
    public static class MethodHandler {
        private final EventHandler annotation;
        private final Method method;

        private MethodHandler(@Nonnull final EventHandler annotation, @Nonnull final Method method) {
            this.annotation = annotation;
            this.method = method;
        }

        @Nonnull
        public EventHandler getAnnotation() {
            return this.annotation;
        }

        @Nonnull
        public Method getMethod() {
            return this.method;
        }
    }

    /**
     * A wrapper class for an event handler field with it's {@link EventHandler} annotation
     */
    public static class FieldHandler {
        private final EventHandler annotation;
        private final Field field;

        private FieldHandler(@Nonnull final EventHandler annotation, @Nonnull final Field field) {
            this.annotation = annotation;
            this.field = field;
        }

        @Nonnull
        public EventHandler getAnnotation() {
            return this.annotation;
        }

        @Nonnull
        public Field getField() {
            return this.field;
        }
    }

}
