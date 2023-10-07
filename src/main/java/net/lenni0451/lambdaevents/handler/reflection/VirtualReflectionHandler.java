package net.lenni0451.lambdaevents.handler.reflection;

import lombok.SneakyThrows;
import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.utils.EventUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * The {@link AHandler} implementation which calls the handler method using reflection.<br>
 * <b>Only for use if the method has no parameters.</b>
 */
public class VirtualReflectionHandler extends AHandler {

    private final Method method;

    /**
     * @param owner      The owner of the handler method
     * @param instance   The instance of the handler owner
     * @param annotation The {@link EventHandler} annotation of the handler method
     * @param method     The handler method
     */
    public VirtualReflectionHandler(@Nonnull Class<?> owner, @Nullable Object instance, @Nonnull EventHandler annotation, @Nonnull final Method method) {
        super(owner, instance, annotation);
        this.method = method;
    }

    @Override
    @SneakyThrows
    public void call(Object event) {
        this.method.invoke(this.getInstance());
    }

    @Override
    public String toString() {
        return "virtualReflection: " + this.getOwner().getName() + " -> " + EventUtils.toString(this.method);
    }

}
