package net.lenni0451.lambdaevents.generator;

import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.IGenerator;
import net.lenni0451.lambdaevents.handler.ConsumerHandler;
import net.lenni0451.lambdaevents.handler.RunnableHandler;
import net.lenni0451.lambdaevents.utils.EventUtils;

import java.lang.reflect.Method;

public class ReflectionGenerator implements IGenerator {

    @Override
    public AHandler generate(Class<?> owner, Object instance, EventHandler annotation, Method method, Class<?> arg) {
        method.setAccessible(true);
        return new ConsumerHandler(owner, instance, annotation, event -> {
            try {
                method.invoke(instance, event);
            } catch (Throwable t) {
                EventUtils.sneak(t);
            }
        });
    }

    @Override
    public AHandler generateVirtual(Class<?> owner, Object instance, EventHandler annotation, Method method) {
        method.setAccessible(true);
        return new RunnableHandler(owner, instance, annotation, () -> {
            try {
                method.invoke(instance);
            } catch (Throwable t) {
                EventUtils.sneak(t);
            }
        });
    }

}
