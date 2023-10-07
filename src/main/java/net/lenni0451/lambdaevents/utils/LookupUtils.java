package net.lenni0451.lambdaevents.utils;

import lombok.SneakyThrows;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Various utils for {@link MethodHandles.Lookup}.
 */
public class LookupUtils {

    private static final Map<ClassLoader, LookupGetterLoader> loaders = new WeakHashMap<>();

    /**
     * Get a {@link MethodHandles.Lookup} in the given {@link ClassLoader}.<br>
     * This method defines a new class in the given {@link ClassLoader} to get the {@link MethodHandles.Lookup}.
     *
     * @param classLoader The class loader to get the lookup in
     * @return The lookup
     */
    @Nonnull
    @SneakyThrows
    public static MethodHandles.Lookup getIn(@Nonnull final ClassLoader classLoader) {
        synchronized (loaders) {
            LookupGetterLoader loader = loaders.computeIfAbsent(classLoader, LookupGetterLoader::new);
            Class<?> lookupGetter;
            if (!loader.isDefined(LookupGetter.class.getName())) {
                try (InputStream is = LookupGetter.class.getClassLoader().getResourceAsStream(LookupGetter.class.getName().replace('.', '/') + ".class")) {
                    if (is == null) throw new ClassNotFoundException(LookupGetter.class.getName());
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = is.read(buf)) != -1) baos.write(buf, 0, len);
                    lookupGetter = loader.define(LookupGetter.class.getName(), baos.toByteArray());
                }
            } else {
                lookupGetter = loader.loadClass(LookupGetter.class.getName());
            }
            return (MethodHandles.Lookup) lookupGetter.getDeclaredMethod("get").invoke(null);
        }
    }

    /**
     * Resolve a lookup to access a given class.
     *
     * @param lookup   The lookup to resolve
     * @param accessed The class to access
     * @return The resolved lookup
     * @throws IllegalStateException If the lookup can't be resolved to access the given class
     */
    @Nonnull
    public static MethodHandles.Lookup resolveLookup(@Nonnull MethodHandles.Lookup lookup, @Nonnull final Class<?> accessed) {
        if (canAccess(lookup, accessed)) return lookup;
        lookup = lookup.in(accessed);
        if (canAccess(lookup, accessed)) return lookup;
        lookup = getIn(accessed.getClassLoader());
        if (canAccess(lookup, accessed)) return lookup;
        lookup = lookup.in(accessed);
        if (canAccess(lookup, accessed)) return lookup;
        throw new IllegalStateException("Could not resolve lookup for " + accessed.getName());
    }

    /**
     * Check if the given lookup can access the given class.
     *
     * @param lookup The lookup to check
     * @param clazz  The class to check
     * @return If the lookup can access the given class
     */
    public static boolean canAccess(@Nonnull final MethodHandles.Lookup lookup, @Nonnull final Class<?> clazz) {
        return canAccess(clazz, lookup.lookupClass()) && (lookup.lookupModes() & Modifier.PRIVATE) != 0;
    }

    /**
     * Reimplementation of {@link sun.invoke.util.VerifyAccess#isTypeVisible(Class, Class)}.
     *
     * @param wanted The class that should be visible
     * @param clazz  The class that should be able to see the wanted class
     * @return If the wanted class is accessible
     */
    public static boolean canAccess(@Nonnull Class<?> wanted, @Nonnull final Class<?> clazz) {
        if (wanted == clazz) return true;
        while (wanted.isArray()) wanted = wanted.getComponentType();
        if (wanted.isPrimitive()) return true;
        if (wanted == Object.class) return true;

        ClassLoader wantedLoader = wanted.getClassLoader();
        ClassLoader clazzLoader = clazz.getClassLoader();
        if (wantedLoader == clazzLoader) return true;
        if (wantedLoader != null && clazzLoader == null) return false;
        if (wantedLoader == null && wanted.getName().startsWith("java.")) return true;
        try {
            return Class.forName(wanted.getName(), false, clazzLoader) == wanted;
        } catch (Throwable t) {
            return false;
        }
    }


    private static class LookupGetterLoader extends ClassLoader {
        static {
            ClassLoader.registerAsParallelCapable();
        }

        protected LookupGetterLoader(final ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Object getClassLoadingLock(final String name) {
            return super.getClassLoadingLock(name);
        }

        protected Class<?> define(final String name, final byte[] bytes) {
            synchronized (this.getClassLoadingLock(name)) {
                Class<?> clazz = this.defineClass(name, bytes, 0, bytes.length);
                this.resolveClass(clazz);
                return clazz;
            }
        }

        protected boolean isDefined(final String name) {
            synchronized (this.getClassLoadingLock(name)) {
                return this.findLoadedClass(name) != null;
            }
        }
    }

}
