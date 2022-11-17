package net.lenni0451.lambdaevents.utils;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandles;

public class LookupGetter {

    @Nonnull
    public static MethodHandles.Lookup get() {
        return MethodHandles.lookup();
    }

}