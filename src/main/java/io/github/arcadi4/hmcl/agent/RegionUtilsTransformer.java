package io.github.arcadi4.hmcl.agent;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Set;

/// Rewrites the network region checks in `org.jackhuang.hmcl.util.RegionUtils`
/// so the mainland-China download gate reports "not mainland China".
///
/// Only the network gate is patched. The machine check is deliberately left
/// untouched because HMCL uses it to prefer BMCLAPI mirrors, order Terracotta
/// nodes, and decide offline-account behavior; those features are independent
/// of the download restriction.
@NotNullByDefault
final class RegionUtilsTransformer implements ClassFileTransformer {

    /// Binary name of the class that owns HMCL's region checks.
    static final String TARGET_CLASS = "org/jackhuang/hmcl/util/RegionUtils";

    /// Network gate methods that must always return `false`.
    ///
    /// `isIsChinaMainlandNetwork` is the public API used by the download pages
    /// and providers. `checkChinaMainlandNetwork` is the private implementation
    /// used by the public method, so patching it as well keeps behavior correct
    /// even if a future version bypasses the public wrapper.
    @Unmodifiable
    private static final Set<String> TARGET_METHODS = Set.of(
            "isIsChinaMainlandNetwork",
            "checkChinaMainlandNetwork"
    );

    /// Creates the transformer registered by the agent.
    RegionUtilsTransformer() {
    }

    /// Intercepts the HMCL region-check class and returns its patched bytes.
    ///
    /// @param loader the defining loader, possibly `null` for bootstrap classes
    /// @param className internal binary name supplied by the JVM, possibly `null`
    /// @param classBeingRedefined class being redefined, or `null` during class load
    /// @param protectionDomain protection domain of the class, possibly `null`
    /// @param classfileBuffer original class file bytes
    /// @return patched class bytes, or `null` when this class is not a target
    @Override
    public @Nullable byte[] transform(
            @Nullable ClassLoader loader,
            @Nullable String className,
            @Nullable Class<?> classBeingRedefined,
            @Nullable ProtectionDomain protectionDomain,
            byte[] classfileBuffer
    ) {
        if (!TARGET_CLASS.equals(className)) {
            return null;
        }

        try {
            byte[] patched = patch(classfileBuffer);
            System.err.println("[hmcl-download-agent] Patched " + className);
            return patched;
        } catch (Throwable e) {
            System.err.println("[hmcl-download-agent] Failed to patch " + className + ": " + e);
            e.printStackTrace(System.err);
            return null;
        }
    }

    /// Patches the supplied class bytes.
    ///
    /// @param classfileBuffer original class file
    /// @return transformed class file
    /// @throws IllegalStateException if the class contains none of the expected
    ///                               boolean region-check methods
    static byte[] patch(byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);

        boolean patched = false;
        for (MethodNode method : classNode.methods) {
            if (TARGET_METHODS.contains(method.name)
                    && Type.getReturnType(method.desc).getSort() == Type.BOOLEAN) {
                forceFalse(method);
                patched = true;
            }
        }

        if (!patched) {
            throw new IllegalStateException("No boolean network region-check methods found");
        }

        // COMPUTE_MAXS recalculates the stack/locals of the rewritten methods.
        // The rewritten bodies contain no branches, so no stack map frames are
        // required for them; untouched methods keep their original frames.
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    /// Replaces a method body with `return false`.
    ///
    /// @param method method whose instructions, exception table, and local
    ///               variable metadata are replaced
    private static void forceFalse(MethodNode method) {
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        method.localVariables = null;
        method.visibleLocalVariableAnnotations = null;
        method.invisibleLocalVariableAnnotations = null;
        method.maxStack = 1;
        method.maxLocals = 0;
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
    }
}
