package net.lenni0451.lambdaevents;

import java.lang.reflect.Method;

public interface IGenerator {

    AHandler generate(final Class<?> owner, final Object instance, final EventHandler annotation, final Method method, final Class<?> arg);

    AHandler generateVirtual(final Class<?> owner, final Object instance, final EventHandler annotation, final Method method);

}
