package net.lenni0451.lambdaevents;

import net.lenni0451.lambdaevents.utils.EventUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LambdaManager {

    private final Map<Class<?>, List<AHandler>> handler;
    private final Supplier<List<AHandler>> listSupplier;
    private final IGenerator generator;

    private Consumer<Throwable> exceptionHandler = Throwable::printStackTrace;

    public LambdaManager(final Map<Class<?>, List<AHandler>> handler, final Supplier<List<AHandler>> listSupplier, final IGenerator generator) {
        this.handler = handler;
        this.listSupplier = listSupplier;
        this.generator = generator;
    }

    public void setExceptionHandler(final Consumer<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }


    public <T> T call(@Nonnull final T event) {
        List<AHandler> handlers = this.handler.get(event.getClass());
        if (handlers == null) return event;

        try {
            for (AHandler handler : handlers) handler.call(event);
        } catch (StopCall ignored) {
        } catch (Throwable t) {
            this.exceptionHandler.accept(t);
        }
        return event;
    }


    public void register(@Nonnull final Class<?> owner) {
        this.register(null, owner);
    }

    public void register(@Nullable final Class<?> event, @Nonnull final Class<?> owner) {
        for (EventUtils.MethodHandler handler : EventUtils.getMethods(owner, method -> Modifier.isStatic(method.getModifiers()))) {
            if (event == null) this.registerMethod(owner, null, handler.getAnnotation(), handler.getMethod(), e -> true);
            else this.registerMethod(owner, null, handler.getAnnotation(), handler.getMethod(), e -> e.equals(event));
        }
    }

    public void register(@Nonnull final Object owner) {
        this.register(null, owner);
    }

    public void register(@Nullable final Class<?> event, @Nonnull final Object owner) {
        for (EventUtils.MethodHandler handler : EventUtils.getMethods(owner.getClass(), method -> !Modifier.isStatic(method.getModifiers()))) {
            if (event == null) this.registerMethod(owner.getClass(), owner, handler.getAnnotation(), handler.getMethod(), e -> true);
            else this.registerMethod(owner.getClass(), owner, handler.getAnnotation(), handler.getMethod(), e -> e.equals(event));
        }
    }

    private void registerMethod(final Class<?> owner, final Object instance, final EventHandler annotation, final Method method, final Predicate<Class<?>> accept) {
        EventUtils.verify(owner, annotation, method);
        for (Class<?> event : EventUtils.getEvents(annotation, method, accept)) this.registerMethod(owner, instance, annotation, method, event, method.getParameterCount() == 0);
    }

    private void registerMethod(final Class<?> owner, final Object instance, final EventHandler annotation, final Method method, final Class<?> event, final boolean virtual) {
        synchronized (this.handler) {
            List<AHandler> handlers = this.handler.computeIfAbsent(event, (key) -> this.listSupplier.get());
            AHandler handler;
            if (virtual) handler = this.generator.generateVirtual(owner, instance, annotation, method);
            else handler = this.generator.generate(owner, instance, annotation, method, event);
            handlers.add(handler);
        }
    }


    public void unregister(@Nonnull final Class<?> owner) {
        for (EventUtils.MethodHandler handler : EventUtils.getMethods(owner, method -> Modifier.isStatic(method.getModifiers()))) {
            try {
                EventUtils.verify(owner, handler.getAnnotation(), handler.getMethod());
                for (Class<?> event : EventUtils.getEvents(handler.getAnnotation(), handler.getMethod(), e -> true)) this.unregister(event, owner);
            } catch (Throwable ignored) {
            }
        }
    }

    public void unregister(@Nullable final Class<?> event, @Nonnull final Class<?> owner) {
        synchronized (this.handler) {
            List<AHandler> handlers = this.handler.get(event);
            if (handlers == null) return;
            handlers.removeIf(handler -> handler.isStatic() && handler.getOwner().equals(owner));
            if (handlers.isEmpty()) this.handler.remove(event);
        }
    }

    public void unregister(@Nonnull final Object owner) {
        for (EventUtils.MethodHandler handler : EventUtils.getMethods(owner.getClass(), method -> !Modifier.isStatic(method.getModifiers()))) {
            try {
                EventUtils.verify(owner.getClass(), handler.getAnnotation(), handler.getMethod());
                for (Class<?> event : EventUtils.getEvents(handler.getAnnotation(), handler.getMethod(), e -> true)) this.unregister(event, owner);
            } catch (Throwable ignored) {
            }
        }
    }

    public void unregister(@Nullable final Class<?> event, @Nonnull final Object owner) {
        synchronized (this.handler) {
            List<AHandler> handlers = this.handler.get(event);
            if (handlers == null) return;
            handlers.removeIf(handler -> !handler.isStatic() && handler.getInstance().equals(owner));
            if (handlers.isEmpty()) this.handler.remove(event);
        }
    }

}
