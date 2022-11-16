package net.lenni0451.lambdaevents.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.WeakHashMap;

public class LookupUtils {

    private static final Map<ClassLoader, LookupGetterLoader> loaders = new WeakHashMap<>();

    public static MethodHandles.Lookup getIn(final ClassLoader classLoader) {
        synchronized (loaders) {
            try {
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
            } catch (Throwable t) {
                EventUtils.sneak(t);
                throw new RuntimeException();
            }
        }
    }

    public static MethodHandles.Lookup resolveLookup(MethodHandles.Lookup lookup, final Class<?> accessed) {
        if (canAccess(lookup, accessed)) return lookup;
        lookup = lookup.in(accessed);
        if (canAccess(lookup, accessed)) return lookup;
        lookup = getIn(accessed.getClassLoader());
        if (canAccess(lookup, accessed)) return lookup;
        lookup = lookup.in(accessed);
        if (canAccess(lookup, accessed)) return lookup;
        throw new IllegalStateException("Could not resolve lookup for " + accessed.getName());
    }

    public static boolean canAccess(final MethodHandles.Lookup lookup, final Class<?> clazz) {
        return canAccess(clazz, lookup.lookupClass()) && (lookup.lookupModes() & Modifier.PRIVATE) != 0;
    }

    /**
     * Reimplementation of {@link sun.invoke.util.VerifyAccess#isTypeVisible(Class, Class)}
     *
     * @param wanted The class that should be visible
     * @param clazz  The class that should be able to see the wanted class
     * @return If the wanted class is accessible
     */
    public static boolean canAccess(Class<?> wanted, final Class<?> clazz) {
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
