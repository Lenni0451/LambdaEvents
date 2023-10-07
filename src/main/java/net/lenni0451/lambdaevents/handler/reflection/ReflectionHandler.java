package net.lenni0451.lambdaevents.handler.reflection;

import lombok.SneakyThrows;
import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.utils.EventUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Method;

/**
 * The {@link AHandler} implementation which calls the handler method using reflection.<br>
 * <b>Only for use if the method has the event as the only parameter.</b>
 */
@ParametersAreNonnullByDefault
public class ReflectionHandler extends AHandler {

    private final Method method;

    /**
     * @param owner      The owner of the handler method
     * @param instance   The instance of the handler owner
     * @param annotation The {@link EventHandler} annotation of the handler method
     * @param method     The handler method
     */
    public ReflectionHandler(Class<?> owner, @Nullable Object instance, EventHandler annotation, final Method method) {
        super(owner, instance, annotation);
        this.method = method;
    }

    @Override
    @SneakyThrows
    public void call(Object event) {
        this.method.invoke(this.getInstance(), event);
    }

    @Override
    public String toString() {
        return "reflection: " + this.getOwner().getName() + " -> " + EventUtils.toString(this.method);
    }

}
