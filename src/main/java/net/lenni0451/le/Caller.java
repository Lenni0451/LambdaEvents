package net.lenni0451.le;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Caller {

    private final Class<?> ownerClass;
    private final Object instance;
    private final LambdaHandler handlerInfo;
    private final BiConsumer virtualConsumer;
    private final Consumer staticConsumer;

    public Caller(final Class<?> ownerClass, final Object instance, final LambdaHandler handlerInfo, final BiConsumer virtualConsumer) {
        this.ownerClass = ownerClass;
        this.instance = instance;
        this.handlerInfo = handlerInfo;
        this.virtualConsumer = virtualConsumer;
        this.staticConsumer = null;
    }

    public Caller(final Class<?> ownerClass, final LambdaHandler handlerInfo, final Consumer staticConsumer) {
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

    public LambdaHandler getHandlerInfo() {
        return this.handlerInfo;
    }

    public void call(final Object lambda) {
        if (this.virtualConsumer != null) this.virtualConsumer.accept(this.instance, lambda);
        else this.staticConsumer.accept(lambda);
    }

}
