package net.lenni0451.le;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Caller {

    private final Object instance;
    private final LambdaHandler handlerInfo;
    private final BiConsumer virtualConsumer;
    private final Consumer staticConsumer;

    public Caller(final Object instance, final LambdaHandler handlerInfo, final BiConsumer virtualConsumer) {
        this.instance = instance;
        this.handlerInfo = handlerInfo;
        this.virtualConsumer = virtualConsumer;
        this.staticConsumer = null;
    }

    public Caller(final LambdaHandler handlerInfo, final Consumer staticConsumer) {
        this.instance = null;
        this.handlerInfo = handlerInfo;
        this.virtualConsumer = null;
        this.staticConsumer = staticConsumer;
    }

    public LambdaHandler getHandlerInfo() {
        return this.handlerInfo;
    }

    public void call(final Object lambda) {
        if (this.virtualConsumer != null) this.virtualConsumer.accept(this.instance, lambda);
        else this.staticConsumer.accept(lambda);
    }

}
