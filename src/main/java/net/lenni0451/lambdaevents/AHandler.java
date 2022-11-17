package net.lenni0451.lambdaevents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AHandler {

    private final Class<?> owner;
    private final Object instance;
    private final EventHandler annotation;

    public AHandler(@Nonnull final Class<?> owner, @Nullable final Object instance, @Nonnull final EventHandler annotation) {
        this.owner = owner;
        this.instance = instance;
        this.annotation = annotation;
    }

    @Nonnull
    public Class<?> getOwner() {
        return this.owner;
    }

    @Nullable
    public Object getInstance() {
        return this.instance;
    }

    @Nonnull
    public EventHandler getAnnotation() {
        return this.annotation;
    }

    public boolean isStatic() {
        return this.instance == null;
    }

    public abstract void call(final Object event);

}
