package net.lenni0451.lambdaevents.utils;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandles;

/**
 * A wrapper class to get the {@link MethodHandles.Lookup} for the current {@link ClassLoader}
 */
public class LookupGetter {

    @Nonnull
    public static MethodHandles.Lookup get() {
        return MethodHandles.lookup();
    }

}