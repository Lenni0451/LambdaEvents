package net.lenni0451.le;

import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class Caller {

    public static final Comparator<Caller> COMPARATOR = (o1, o2) -> Integer.compare(o2.getHandlerInfo().priority(), o1.getHandlerInfo().priority());

    private final Class<?> ownerClass;
    private final Object instance;
    private final EventHandler handlerInfo;
    private final BiConsumer virtualConsumer;
    private final Consumer staticConsumer;

    protected Caller(final Class<?> ownerClass, final Object instance, final EventHandler handlerInfo, final BiConsumer virtualConsumer) {
        this.ownerClass = ownerClass;
        this.instance = instance;
        this.handlerInfo = handlerInfo;
        this.virtualConsumer = virtualConsumer;
        this.staticConsumer = null;
    }

    protected Caller(final Class<?> ownerClass, final EventHandler handlerInfo, final Consumer staticConsumer) {
        this.ownerClass = ownerClass;
        this.instance = null;
        this.handlerInfo = handlerInfo;
        this.virtualConsumer = null;
        this.staticConsumer = staticConsumer;
    }

    public Class<?> getOwnerClass() {
        return this.ownerClass;
    }

    public boolean isStatic() {
        return this.staticConsumer != null;
    }

    public EventHandler getHandlerInfo() {
        return this.handlerInfo;
    }

    public void call(final Object event) {
        if (this.virtualConsumer != null) this.virtualConsumer.accept(this.instance, event);
        else this.staticConsumer.accept(event);
    }

}