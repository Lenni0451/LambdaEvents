package net.lenni0451.lambdaevents.handler.methodhandle;

import lombok.SneakyThrows;
import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.utils.EventUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.invoke.MethodHandle;

/**
 * The {@link AHandler} implementation which calls the handler method using method handles.<br>
 * <b>Only for use if the method has the event as the only parameter.</b>
 */
@ParametersAreNonnullByDefault
public class MethodHandleHandler extends AHandler {

    private final MethodHandle methodHandle;

    /**
     * @param owner        The owner of the handler method
     * @param instance     The instance of the handler owner
     * @param annotation   The {@link EventHandler} annotation of the handler method
     * @param methodHandle The handler method handle
     */
    public MethodHandleHandler(Class<?> owner, @Nullable Object instance, EventHandler annotation, final MethodHandle methodHandle) {
        super(owner, instance, annotation);
        this.methodHandle = methodHandle;
    }

    @Override
    @SneakyThrows
    public void call(Object event) {
        this.methodHandle.invoke(event);
    }

    @Override
    public String toString() {
        return "methodHandle: " + this.getOwner().getName() + " -> " + EventUtils.toString(this.methodHandle);
    }

}
