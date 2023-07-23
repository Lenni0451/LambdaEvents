package net.lenni0451.lambdaevents;

import net.lenni0451.reflect.ClassLoaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static net.lenni0451.lambdaevents.TestManager.DATA_SOURCE;
import static net.lenni0451.reflect.wrapper.ASMWrapper.desc;
import static net.lenni0451.reflect.wrapper.ASMWrapper.slash;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClassLoaderTest {

    @ParameterizedTest
    @MethodSource(DATA_SOURCE)
    void test(final LambdaManager manager) {
        String className = slash(ClassLoaderTest.class) + "$Test" + System.nanoTime();

        ClassWriter w = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        w.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, slash(Object.class), null);
        {
            w.visitField(Opcodes.ACC_PRIVATE, "virtual", desc(Runnable.class), null, null).visitEnd();
            w.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "static", desc(Runnable.class), null, null).visitEnd();
        }
        {
            MethodVisitor mv = w.visitMethod(Opcodes.ACC_PUBLIC, "<init>", desc(new Class[]{Runnable.class, Runnable.class}, void.class), null, null);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, slash(Object.class), "<init>", desc(new Class[0], void.class), false);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitFieldInsn(Opcodes.PUTFIELD, className, "virtual", desc(Runnable.class));
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitFieldInsn(Opcodes.PUTSTATIC, className, "static", desc(Runnable.class));
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(2, 3);
            mv.visitEnd();
        }
        {
            MethodVisitor mv = w.visitMethod(Opcodes.ACC_PUBLIC, "virtualTest", desc(new Class[]{String.class}, void.class), null, null);
            mv.visitAnnotation(desc(EventHandler.class), true).visitEnd();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, className, "virtual", desc(Runnable.class));
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, slash(Runnable.class), "run", desc(new Class[]{}, void.class), true);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            MethodVisitor mv = w.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "staticTest", desc(new Class[]{String.class}, void.class), null, null);
            mv.visitAnnotation(desc(EventHandler.class), true).visitEnd();
            mv.visitFieldInsn(Opcodes.GETSTATIC, className, "static", desc(Runnable.class));
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, slash(Runnable.class), "run", desc(new Class[]{}, void.class), true);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(2, 1);
            mv.visitEnd();
        }
        w.visitEnd();
        byte[] classBytes = w.toByteArray();
        boolean[] called = new boolean[]{false, false};

        ClassLoader loader = new ClassLoader() {
        };
        Class<?> clazz = ClassLoaders.defineClass(loader, null, classBytes);
        Object instance = Assertions.assertDoesNotThrow(() -> {
            return clazz.getDeclaredConstructor(Runnable.class, Runnable.class).newInstance((Runnable) () -> called[0] = true, (Runnable) () -> called[1] = true);
        });

        manager.register(clazz);
        manager.register(instance);
        manager.call("Test");

        assertTrue(called[0]);
        assertTrue(called[1]);
    }

}
