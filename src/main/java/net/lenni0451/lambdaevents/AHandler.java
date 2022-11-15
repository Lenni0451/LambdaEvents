package net.lenni0451.lambdaevents;

public abstract class AHandler {

    private final Class<?> owner;
    private final Object instance;
    private final EventHandler annotation;

    public AHandler(final Class<?> owner, final Object instance, final EventHandler annotation) {
        this.owner = owner;
        this.instance = instance;
        this.annotation = annotation;
    }

    public Class<?> getOwner() {
        return this.owner;
    }

    public Object getInstance() {
        return this.instance;
    }

    public EventHandler getAnnotation() {
        return this.annotation;
    }

    public boolean isStatic() {
        return this.instance == null;
    }

    public abstract void call(final Object event);

}
