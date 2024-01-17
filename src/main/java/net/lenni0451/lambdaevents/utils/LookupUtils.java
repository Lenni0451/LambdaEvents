package net.lenni0451.lambdaevents.utils;

import lombok.SneakyThrows;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Various utils for {@link MethodHandles.Lookup}.
 */
public class LookupUtils {

    private static final Map<ClassLoader, LookupGetterLoader> loaders = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Get a {@link MethodHandles.Lookup} in the given {@link ClassLoader}.<br>
     * This method defines a new class in the given {@link ClassLoader} to get the {@link MethodHandles.Lookup}.
     *
     * @param classLoader The class loader to get the lookup in
     * @return The lookup
     */
    @Nonnull
    @SneakyThrows
    public static MethodHandles.Lookup getIn(final ClassLoader classLoader) {
        LookupGetterLoader loader = loaders.computeIfAbsent(classLoader, LookupGetterLoader::new);
        Class<?> lookupGetter;
        if (!loader.isDefined(LookupGetter.class.getName())) {
            //If the class is not defined we need to get its bytes and define it
            try (InputStream is = LookupGetter.class.getClassLoader().getResourceAsStream(LookupGetter.class.getName().replace('.', '/') + ".class")) {
                if (is == null) throw new ClassNotFoundException(LookupGetter.class.getName()); //This should hopefully never happen
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int len;
                while ((len = is.read(buf)) != -1) baos.write(buf, 0, len);
                lookupGetter = loader.define(LookupGetter.class.getName(), baos.toByteArray()); //Define the class
            }
        } else {
            //If the class is already defined we can just load it
            lookupGetter = loader.loadClass(LookupGetter.class.getName());
        }
        return (MethodHandles.Lookup) lookupGetter.getDeclaredMethod("get").invoke(null); //Invoke the get() method and return the lookup
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
    public static MethodHandles.Lookup resolveLookup(MethodHandles.Lookup lookup, final Class<?> accessed) {
        if (canAccess(lookup, accessed)) return lookup;
        lookup = lookup.in(accessed); //Try to get a lookup in the accessed class
        if (canAccess(lookup, accessed)) return lookup;
        lookup = getIn(accessed.getClassLoader()); //Try to get a lookup in the accessed class's class loader
        if (canAccess(lookup, accessed)) return lookup;
        lookup = lookup.in(accessed); //Again try to get a lookup in the accessed class
        if (canAccess(lookup, accessed)) return lookup;
        throw new IllegalStateException("Could not resolve lookup for " + accessed.getName()); //If it still doesn't work give up
    }

    /**
     * Check if the given lookup can access the given class.
     *
     * @param lookup The lookup to check
     * @param clazz  The class to check
     * @return If the lookup can access the given class
     */
    public static boolean canAccess(final MethodHandles.Lookup lookup, final Class<?> clazz) {
        return canAccess(clazz, lookup.lookupClass()) && (lookup.lookupModes() & Modifier.PRIVATE) != 0;
    }

    /**
     * Reimplementation of {@link sun.invoke.util.VerifyAccess#isTypeVisible(Class, Class)}.
     *
     * @param wanted The class that should be visible
     * @param clazz  The class that should be able to see the wanted class
     * @return If the wanted class is accessible
     */
    public static boolean canAccess(Class<?> wanted, final Class<?> clazz) {
        if (wanted == clazz) return true; //Same class
        while (wanted.isArray()) wanted = wanted.getComponentType(); //Get the component type of the array (including multidimensional arrays)
        if (wanted.isPrimitive()) return true; //Primitive classes are always accessible
        if (wanted == Object.class) return true; //Object is always accessible

        ClassLoader wantedLoader = wanted.getClassLoader();
        ClassLoader clazzLoader = clazz.getClassLoader();
        if (wantedLoader == clazzLoader) return true; //Same class loader so should be accessible
        if (wantedLoader != null && clazzLoader == null) return false; //The clazz is a bootstrap class, so it can't access other class loaders
        if (wantedLoader == null && wanted.getName().startsWith("java.")) return true; //The wanted class is in the java. package and can be accessed by everything
        try {
            //Try to load the wanted class in the clazz class loader and check if it is the same class
            //If it is not the same class the clazz can only access its own version of the wanted class
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
