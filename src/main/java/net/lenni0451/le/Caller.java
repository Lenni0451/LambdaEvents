package net.lenni0451.le;

import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class Caller {

    public static final Comparator<Caller> COMPARATOR = (o1, o2) -> Integer.compare(o2.getHandlerInfo().priority(), o1.getHandlerInfo().priority());

    /**
     * For internal use only!<br>
     * Needed for {@link Consumer} field handler in classes
     */
    public static void _setStatic(final Caller caller, final boolean newStaticState) {
        caller.isStatic = newStaticState;
    }


    private final Class<?> ownerClass;
    private final Object instance;
    private final EventHandler handlerInfo;
    private final BiConsumer virtualConsumer;
    private final Consumer staticConsumer;
    private boolean isStatic;

    protected Caller(final Class<?> ownerClass, final Object instance, final EventHandler handlerInfo, final BiConsumer virtualConsumer) {
        this.ownerClass = ownerClass;
        this.instance = instance;
        this.handlerInfo = handlerInfo;
        this.virtualConsumer = virtualConsumer;
        this.staticConsumer = null;
        this.isStatic = false;
    }

    protected Caller(final Class<?> ownerClass, final EventHandler handlerInfo, final Consumer staticConsumer) {
        this.ownerClass = ownerClass;
        this.instance = null;
        this.handlerInfo = handlerInfo;
        this.virtualConsumer = null;
        this.staticConsumer = staticConsumer;
        this.isStatic = true;
    }

    public Class<?> getOwnerClass() {
        return this.ownerClass;
    }

    public boolean isStatic() {
        return this.isStatic;
    }

    public EventHandler getHandlerInfo() {
        return this.handlerInfo;
    }

    public void call(final Object event) {
        if (this.isStatic) this.staticConsumer.accept(event);
        else this.virtualConsumer.accept(this.instance, event);
    }

}
