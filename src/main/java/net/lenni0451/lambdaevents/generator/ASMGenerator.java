package net.lenni0451.lambdaevents.generator;

import net.lenni0451.lambdaevents.AHandler;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.lambdaevents.IGenerator;
import net.lenni0451.reflect.stream.RStream;
import net.lenni0451.reflect.wrapper.ASMWrapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static net.lenni0451.reflect.wrapper.ASMWrapper.*;

/**
 * The {@link IGenerator} implementation which calls the handler method using an ASM generated handler.<br>
 * <b>This requires the <a href="https://github.com/Lenni0451/Reflect">Reflect library</a> to work (<a href="https://mvnrepository.com/artifact/net.lenni0451/Reflect">maven</a>)!</b><br>
 * <b>This also requires <a href="https://asm.ow2.io/">ASM</a> to work (<a href="https://mvnrepository.com/artifact/org.ow2.asm/asm">maven</a>)!</b>
 */
@ParametersAreNonnullByDefault
public class ASMGenerator implements IGenerator {

    @Nonnull
    @Override
    public AHandler generate(Class<?> owner, @Nullable Object instance, EventHandler annotation, Method method, Class<?> arg) {
        return this.define(owner, instance, annotation, method, arg);
    }

    @Nonnull
    @Override
    public AHandler generateVirtual(Class<?> owner, @Nullable Object instance, EventHandler annotation, Method method) {
        return this.define(owner, instance, annotation, method, null);
    }

    private AHandler define(Class<?> owner, @Nullable Object instance, EventHandler annotation, Method method, @Nullable Class<?> arg) {
        String handlerName = slash(owner.getPackage().getName()) + "/LambdaEvents$ASMHandler";
        ASMWrapper w = ASMWrapper.create(opcode("ACC_PUBLIC"), handlerName, null, slash(AHandler.class), null);
        this.makeConstructor(handlerName, w, instance);
        this.makeCaller(handlerName, w, owner, instance, method, arg);

        Class<?> handlerClazz = w.defineMetafactory(owner);
        return RStream.of(handlerClazz).constructors().by(0).newInstance(owner, instance, annotation);
    }

    private void makeConstructor(final String handlerName, final ASMWrapper w, @Nullable final Object instance) {
        String desc = desc(new Class[]{Class.class, Object.class, EventHandler.class}, void.class);
        boolean isStatic = instance == null;
        if (!isStatic) w.visitField(opcode("ACC_PRIVATE"), "instance", desc(instance.getClass()), null, null);

        ASMWrapper.MethodVisitorAccess mv = w.visitMethod(opcode("ACC_PUBLIC"), "<init>", desc, null, null);
        mv.visitVarInsn(opcode("ALOAD"), 0);
        mv.visitVarInsn(opcode("ALOAD"), 1);
        mv.visitVarInsn(opcode("ALOAD"), 2);
        mv.visitVarInsn(opcode("ALOAD"), 3);
        mv.visitMethodInsn(opcode("INVOKESPECIAL"), slash(AHandler.class), "<init>", desc, false);
        if (!isStatic) {
            mv.visitVarInsn(opcode("ALOAD"), 0);
            mv.visitVarInsn(opcode("ALOAD"), 2);
            mv.visitTypeInsn(opcode("CHECKCAST"), slash(instance.getClass()));
            mv.visitFieldInsn(opcode("PUTFIELD"), handlerName, "instance", desc(instance.getClass()));
        }
        mv.visitInsn(opcode("RETURN"));
        if (isStatic) mv.visitMaxs(4, 4);
        else mv.visitMaxs(5, 5);
        mv.visitEnd();
    }

    private void makeCaller(final String handlerName, final ASMWrapper w, final Class<?> owner, @Nullable final Object instance, final Method method, @Nullable final Class<?> arg) {
        boolean isStatic = instance == null;
        boolean isInterface = Modifier.isInterface(owner.getModifiers());

        ASMWrapper.MethodVisitorAccess mv = w.visitMethod(opcode("ACC_PUBLIC"), "call", desc(new Class[]{Object.class}, void.class), null, null);
        if (!isStatic) {
            mv.visitVarInsn(opcode("ALOAD"), 0);
            mv.visitFieldInsn(opcode("GETFIELD"), handlerName, "instance", desc(instance.getClass()));
        }
        if (arg != null) {
            mv.visitVarInsn(opcode("ALOAD"), 1);
            mv.visitTypeInsn(opcode("CHECKCAST"), slash(arg));
        }
        if (isStatic) mv.visitMethodInsn(opcode("INVOKESTATIC"), slash(owner), method.getName(), desc(method), isInterface);
        else mv.visitMethodInsn(opcode(isInterface ? "INVOKEINTERFACE" : "INVOKEVIRTUAL"), slash(owner), method.getName(), desc(method), isInterface);
        mv.visitInsn(opcode("RETURN"));
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }

}
