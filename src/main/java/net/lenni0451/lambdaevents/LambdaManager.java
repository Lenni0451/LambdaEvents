package net.lenni0451.lambdaevents;

import net.lenni0451.lambdaevents.handler.ConsumerHandler;
import net.lenni0451.lambdaevents.handler.RunnableHandler;
import net.lenni0451.lambdaevents.utils.EventUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LambdaManager {

    private final Map<Class<?>, List<AHandler>> handlers;
    private final Supplier<List<AHandler>> listSupplier;
    private final IGenerator generator;

    private Consumer<Throwable> exceptionHandler = Throwable::printStackTrace;

    public LambdaManager(@Nonnull final Map<Class<?>, List<AHandler>> handlers, @Nonnull final Supplier<List<AHandler>> listSupplier, @Nonnull final IGenerator generator) {
        this.handlers = handlers;
        this.listSupplier = listSupplier;
        this.generator = generator;
    }

    public void setExceptionHandler(@Nonnull final Consumer<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }


    @Nonnull
    public <T> T call(@Nonnull final T event) {
        List<AHandler> handlers = this.handlers.get(event.getClass());
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
        for (EventUtils.FieldHandler handler : EventUtils.getFields(owner, field -> Modifier.isStatic(field.getModifiers()))) {
            if (event == null) this.registerField(owner, null, handler.getAnnotation(), handler.getField(), e -> true);
            else this.registerField(owner, null, handler.getAnnotation(), handler.getField(), e -> e.equals(event));
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
        for (EventUtils.FieldHandler handler : EventUtils.getFields(owner.getClass(), field -> !Modifier.isStatic(field.getModifiers()))) {
            if (event == null) this.registerField(owner.getClass(), owner, handler.getAnnotation(), handler.getField(), e -> true);
            else this.registerField(owner.getClass(), owner, handler.getAnnotation(), handler.getField(), e -> e.equals(event));
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

    @SuppressWarnings("unchecked")
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


    public void unregister(@Nonnull final Class<?> owner) {
        for (EventUtils.MethodHandler handler : EventUtils.getMethods(owner, method -> Modifier.isStatic(method.getModifiers()))) {
            try {
                EventUtils.verify(owner, handler.getAnnotation(), handler.getMethod());
                for (Class<?> event : EventUtils.getEvents(handler.getAnnotation(), handler.getMethod(), e -> true)) this.unregister(event, owner);
            } catch (Throwable ignored) {
            }
        }
        for (EventUtils.FieldHandler handler : EventUtils.getFields(owner, field -> Modifier.isStatic(field.getModifiers()))) {
            try {
                EventUtils.verify(owner, handler.getAnnotation(), handler.getField());
                for (Class<?> event : EventUtils.getEvents(handler.getAnnotation(), handler.getField(), e -> true)) this.unregister(event, owner);
            } catch (Throwable ignored) {
            }
        }
    }

    public void unregister(@Nullable final Class<?> event, @Nonnull final Class<?> owner) {
        synchronized (this.handlers) {
            List<AHandler> handlers = this.handlers.get(event);
            if (handlers == null) return;
            handlers.removeIf(handler -> handler.isStatic() && handler.getOwner().equals(owner));
            if (handlers.isEmpty()) this.handlers.remove(event);
            else this.resortHandlers(handlers);
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
        for (EventUtils.FieldHandler handler : EventUtils.getFields(owner.getClass(), field -> !Modifier.isStatic(field.getModifiers()))) {
            try {
                EventUtils.verify(owner.getClass(), handler.getAnnotation(), handler.getField());
                for (Class<?> event : EventUtils.getEvents(handler.getAnnotation(), handler.getField(), e -> true)) this.unregister(event, owner);
            } catch (Throwable ignored) {
            }
        }
    }

    public void unregister(@Nullable final Class<?> event, @Nonnull final Object owner) {
        synchronized (this.handlers) {
            List<AHandler> handlers = this.handlers.get(event);
            if (handlers == null) return;
            handlers.removeIf(handler -> !handler.isStatic() && handler.getInstance().equals(owner));
            if (handlers.isEmpty()) this.handlers.remove(event);
            else this.resortHandlers(handlers);
        }
    }


    private void resortHandlers(final List<AHandler> handlers) {
        handlers.sort(Comparator.comparingInt((AHandler o) -> o.getAnnotation().priority()).reversed());
    }

}
