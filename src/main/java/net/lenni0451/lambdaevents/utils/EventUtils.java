package net.lenni0451.lambdaevents.utils;

import lombok.Data;
import net.lenni0451.lambdaevents.EventHandler;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
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

@ParametersAreNonnullByDefault
public class EventUtils {

    /**
     * Get all methods of the given class which are annotated with {@link EventHandler}.<br>
     * This method does not verify if the methods are valid.
     *
     * @param owner                The class to get the methods from
     * @param accept               If the method should be accepted
     * @param registerSuperHandler If the super methods should be registered
     * @return The list of all found methods
     */
    @Nonnull
    public static List<MethodHandler> getMethods(final Class<?> owner, final Predicate<Method> accept, final boolean registerSuperHandler) {
        List<MethodHandler> handler = new ArrayList<>();
        Class<?> current = owner;
        do {
            for (Method method : current.getDeclaredMethods()) {
                EventHandler annotation = method.getDeclaredAnnotation(EventHandler.class);
                if (annotation == null) continue;
                if (!accept.test(method)) continue;

                handler.add(new MethodHandler(current, annotation, method));
            }
        } while ((current = current.getSuperclass()) != null && registerSuperHandler);
        return handler;
    }

    /**
     * Get all fields of the given class which are annotated with {@link EventHandler}.<br>
     * This method does not verify if the fields are valid.
     *
     * @param owner                The class to get the fields from
     * @param accept               If the field should be accepted
     * @param registerSuperHandler If the super fields should be registered
     * @return The list of all found fields
     */
    @Nonnull
    public static List<FieldHandler> getFields(final Class<?> owner, final Predicate<Field> accept, final boolean registerSuperHandler) {
        List<FieldHandler> handler = new ArrayList<>();
        Class<?> current = owner;
        do {
            for (Field field : current.getDeclaredFields()) {
                EventHandler annotation = field.getDeclaredAnnotation(EventHandler.class);
                if (annotation == null) continue;
                if (!accept.test(field)) continue;

                handler.add(new FieldHandler(current, annotation, field));
            }
        } while ((current = current.getSuperclass()) != null && registerSuperHandler);
        return handler;
    }

    /**
     * Check if the given method is a valid event handler method.
     *
     * @param owner      The owner of the method
     * @param annotation The {@link EventHandler} annotation of the method
     * @param method     The method to check
     * @throws IllegalStateException If the method is not valid
     */
    public static void verify(final Class<?> owner, final EventHandler annotation, final Method method) {
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
     * Check if the given field is a valid event handler field.
     *
     * @param owner      The owner of the field
     * @param annotation The {@link EventHandler} annotation of the field
     * @param field      The field to check
     */
    public static void verify(final Class<?> owner, final EventHandler annotation, final Field field) {
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
     * Get all events handled by the given method.
     *
     * @param annotation The {@link EventHandler} annotation of the method
     * @param method     The method to get the events from
     * @param accept     If the event should be accepted
     * @return The list of all found events
     */
    @Nonnull
    public static Class<?>[] getEvents(final EventHandler annotation, final Method method, final Predicate<Class<?>> accept) {
        if (method.getParameterCount() == 1) {
            Class<?> param = method.getParameterTypes()[0];
            if (!accept.test(param)) return new Class[0];
            return new Class<?>[]{param};
        } else {
            return Arrays.stream(annotation.events()).filter(accept).toArray(Class[]::new);
        }
    }

    /**
     * Get all events handled by the given field.
     *
     * @param annotation The {@link EventHandler} annotation of the field
     * @param field      The field to get the events from
     * @param accept     If the event should be accepted
     * @return The list of all found events
     */
    @Nonnull
    public static Class<?>[] getEvents(final EventHandler annotation, final Field field, final Predicate<Class<?>> accept) {
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
     * Create a new {@link EventHandler} instance with the given priority.
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
     * Convert a method to a string.
     *
     * @param method The method to convert
     * @return The string representation of the method
     */
    public static String toString(final Method method) {
        Class<?> returnType = method.getReturnType();
        Class<?>[] params = method.getParameterTypes();
        StringBuilder out = new StringBuilder().append(method.getName()).append('(');
        for (int i = 0; i < params.length; i++) {
            out.append(params[i].getSimpleName());
            if (i != params.length - 1) out.append(", ");
        }
        return out.append(")").append(returnType.getSimpleName()).toString();
    }

    /**
     * Convert a method handle to a string.
     *
     * @param methodHandle The method to convert
     * @return The string representation of the method
     */
    public static String toString(final MethodHandle methodHandle) {
        MethodType type = methodHandle.type();
        StringBuilder out = new StringBuilder().append('(');
        for (int i = 0; i < type.parameterCount(); i++) {
            out.append(type.parameterType(i).getSimpleName());
            if (i != type.parameterCount() - 1) out.append(", ");
        }
        return out.append(")").append(type.returnType().getSimpleName()).toString();
    }


    /**
     * A wrapper class for an event handler method with it's {@link EventHandler} annotation.
     */
    @Data
    @ParametersAreNonnullByDefault
    public static class MethodHandler {
        @Nonnull
        private final Class<?> owner;
        @Nonnull
        private final EventHandler annotation;
        @Nonnull
        private final Method method;
    }

    /**
     * A wrapper class for an event handler field with it's {@link EventHandler} annotation.
     */
    @Data
    @ParametersAreNonnullByDefault
    public static class FieldHandler {
        @Nonnull
        private final Class<?> owner;
        @Nonnull
        private final EventHandler annotation;
        @Nonnull
        private final Field field;
    }

}
