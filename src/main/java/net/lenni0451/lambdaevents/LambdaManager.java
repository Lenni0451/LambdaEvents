package net.lenni0451.lambdaevents;

import net.lenni0451.lambdaevents.handler.ConsumerHandler;
import net.lenni0451.lambdaevents.handler.ExceptionHandler;
import net.lenni0451.lambdaevents.handler.RunnableHandler;
import net.lenni0451.lambdaevents.utils.EventException;
import net.lenni0451.lambdaevents.utils.EventUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LambdaManager {

    public static LambdaManager basic(@Nonnull final IGenerator generator) {
        return new LambdaManager(new HashMap<>(), ArrayList::new, generator);
    }

    public static LambdaManager threadSafe(@Nonnull final IGenerator generator) {
        return new LambdaManager(new ConcurrentHashMap<>(), CopyOnWriteArrayList::new, generator);
    }


    private final Map<Class<?>, List<AHandler>> handlers;
    private final Supplier<List<AHandler>> listSupplier;
    private final IGenerator generator;

    private ExceptionHandler exceptionHandler = (handler, event, t) -> {
        new EventException("Exception occurred in '" + event.getClass().getSimpleName() + "' handler in '" + handler.getOwner().getName() + "'", t).printStackTrace();
    };

    public LambdaManager(@Nonnull final Map<Class<?>, List<AHandler>> handlers, @Nonnull final Supplier<List<AHandler>> listSupplier, @Nonnull final IGenerator generator) {
        this.handlers = handlers;
        this.listSupplier = listSupplier;
        this.generator = generator;
    }

    public void setExceptionHandler(@Nonnull final ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }


    @Nonnull
    public <T> T call(@Nonnull final T event) {
        List<AHandler> handlers = this.handlers.get(event.getClass());
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

    public void register(@Nonnull final Runnable runnable, @Nonnull final Class<?>... events) {
        this.register(runnable, (byte) 0, events);
    }

    public void register(@Nonnull final Runnable runnable, final byte priority, @Nonnull final Class<?>... events) {
        if (events.length == 0) throw new IllegalArgumentException("No events specified");
        synchronized (this.handlers) {
            for (Class<?> event : events) {
                List<AHandler> handlers = this.handlers.computeIfAbsent(event, (key) -> this.listSupplier.get());
                handlers.add(new RunnableHandler(runnable.getClass(), null, EventUtils.newEventHandler(priority), runnable));
            }
        }
    }

    public void register(@Nonnull final Consumer<?> consumer, @Nonnull final Class<?>... events) {
        this.register(consumer, (byte) 0, events);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void register(@Nonnull final Consumer consumer, final byte priority, @Nonnull final Class<?>... events) {
        if (events.length == 0) throw new IllegalArgumentException("No events specified");
        synchronized (this.handlers) {
            for (Class<?> event : events) {
                List<AHandler> handlers = this.handlers.computeIfAbsent(event, (key) -> this.listSupplier.get());
                handlers.add(new ConsumerHandler(consumer.getClass(), null, EventUtils.newEventHandler(priority), consumer));
            }
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

    public void unregister(@Nonnull final Runnable runnable) {
        synchronized (this.handlers) {
            Iterator<Map.Entry<Class<?>, List<AHandler>>> it = this.handlers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Class<?>, List<AHandler>> entry = it.next();
                List<AHandler> handlers = entry.getValue();
                handlers.removeIf(handler -> handler instanceof RunnableHandler && ((RunnableHandler) handler).getRunnable().equals(runnable));
                if (handlers.isEmpty()) it.remove();
            }
        }
    }

    public void unregister(@Nonnull final Runnable runnable, @Nonnull final Class<?>... events) {
        if (events.length == 0) {
            this.unregister(runnable);
            return;
        }
        synchronized (this.handlers) {
            for (Class<?> event : events) {
                List<AHandler> handlers = this.handlers.get(event);
                if (handlers == null) continue;
                handlers.removeIf(handler -> handler instanceof RunnableHandler && ((RunnableHandler) handler).getRunnable().equals(runnable));
                if (handlers.isEmpty()) this.handlers.remove(event);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public void unregister(@Nonnull final Consumer consumer) {
        synchronized (this.handlers) {
            Iterator<Map.Entry<Class<?>, List<AHandler>>> it = this.handlers.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Class<?>, List<AHandler>> entry = it.next();
                List<AHandler> handlers = entry.getValue();
                handlers.removeIf(handler -> handler instanceof ConsumerHandler && ((ConsumerHandler) handler).getConsumer().equals(consumer));
                if (handlers.isEmpty()) it.remove();
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public void unregister(@Nonnull final Consumer consumer, @Nonnull final Class<?>... events) {
        if (events.length == 0) {
            this.unregister(consumer);
            return;
        }
        synchronized (this.handlers) {
            for (Class<?> event : events) {
                List<AHandler> handlers = this.handlers.get(event);
                if (handlers == null) continue;
                handlers.removeIf(handler -> handler instanceof ConsumerHandler && ((ConsumerHandler) handler).getConsumer().equals(consumer));
                if (handlers.isEmpty()) this.handlers.remove(event);
            }
        }
    }


    private void resortHandlers(final List<AHandler> handlers) {
        handlers.sort(Comparator.comparingInt((AHandler o) -> o.getAnnotation().priority()).reversed());
    }

}
