package net.lenni0451.lambdaevents.generator;

import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.IGenerator;
import net.lenni0451.lambdaevents.handler.ConsumerHandler;
import net.lenni0451.lambdaevents.handler.RunnableHandler;

import java.lang.reflect.Method;

public class ReflectionGenerator implements IGenerator {

    @Override
    public AHandler generate(Class<?> owner, Object instance, EventHandler annotation, Method method, Class<?> arg) {
        method.setAccessible(true);
        return new ConsumerHandler(owner, instance, annotation, o -> method.invoke(instance, o));
    }

    @Override
    public AHandler generateVirtual(Class<?> owner, Object instance, EventHandler annotation, Method method) {
        method.setAccessible(true);
        return new RunnableHandler(owner, instance, annotation, () -> method.invoke(instance));
    }

}
