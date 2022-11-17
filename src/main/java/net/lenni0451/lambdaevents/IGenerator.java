package net.lenni0451.lambdaevents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;

public interface IGenerator {

    @Nonnull
    AHandler generate(@Nonnull final Class<?> owner, @Nullable final Object instance, @Nonnull final EventHandler annotation, @Nonnull final Method method, @Nonnull final Class<?> arg);

    @Nonnull
    AHandler generateVirtual(@Nonnull final Class<?> owner, @Nullable final Object instance, @Nonnull final EventHandler annotation, @Nonnull final Method method);

}
