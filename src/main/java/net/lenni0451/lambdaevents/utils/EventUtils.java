package net.lenni0451.lambdaevents.utils;

import net.lenni0451.lambdaevents.EventHandler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class EventUtils {

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

    public static Class<?>[] getEvents(final EventHandler annotation, final Method method, final Predicate<Class<?>> accept) {
        if (method.getParameterCount() == 1) {
            Class<?> param = method.getParameterTypes()[0];
            if (!accept.test(param)) return new Class[0];
            return new Class<?>[]{param};
        } else {
            return Arrays.stream(annotation.events()).filter(accept).toArray(Class[]::new);
        }
    }

    public static List<MethodHandler> getMethods(final Class<?> owner, final Predicate<Method> accept) {
        List<MethodHandler> handler = new ArrayList<>();
        for (Method method : owner.getDeclaredMethods()) {
            EventHandler annotation = method.getDeclaredAnnotation(EventHandler.class);
            if (annotation == null) continue;
            if (!accept.test(method)) continue;

            handler.add(new MethodHandler(annotation, method));
        }
        return handler;
    }


    public static class MethodHandler {
        private final EventHandler annotation;
        private final Method method;

        private MethodHandler(final EventHandler annotation, final Method method) {
            this.annotation = annotation;
            this.method = method;
        }

        public EventHandler getAnnotation() {
            return this.annotation;
        }

        public Method getMethod() {
            return this.method;
        }
    }

}
