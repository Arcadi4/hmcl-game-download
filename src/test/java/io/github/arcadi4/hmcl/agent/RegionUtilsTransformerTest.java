package io.github.arcadi4.hmcl.agent;

import org.jackhuang.hmcl.util.RegionUtils;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies that [#RegionUtilsTransformer] rewrites the network region checks
/// while leaving the machine check intact.
@NotNullByDefault
final class RegionUtilsTransformerTest {

    /// Checks that the unmodified fixture reports a mainland-China machine and
    /// network, including the value stored in the static field.
    @Test
    void unmodifiedRegionChecksReportChina() throws Exception {
        Class<?> type = define(readClass(RegionUtils.class));

        assertEquals(Boolean.TRUE, invoke(type, "isIsChinaMainlandMachine"));
        assertEquals(Boolean.TRUE, invoke(type, "isIsChinaMainlandNetwork"));
        assertEquals(Boolean.TRUE, invoke(type, "checkMachineIsChinaMainland"));
        assertEquals(Boolean.TRUE, invoke(type, "checkChinaMainlandNetwork"));
        assertEquals(Boolean.TRUE, type.getField("IS_CHINA_MAINLAND_MACHINE").get(null));
    }

    /// Checks that patching disables only the network gate; the machine check
    /// and its static field must keep their original values because HMCL uses
    /// them for mirrors and offline-account behavior.
    @Test
    void patchedNetworkChecksReportNotChina() throws Exception {
        Class<?> type = define(RegionUtilsTransformer.patch(readClass(RegionUtils.class)));

        assertEquals(Boolean.TRUE, invoke(type, "isIsChinaMainlandMachine"));
        assertEquals(Boolean.FALSE, invoke(type, "isIsChinaMainlandNetwork"));
        assertEquals(Boolean.TRUE, invoke(type, "checkMachineIsChinaMainland"));
        assertEquals(Boolean.FALSE, invoke(type, "checkChinaMainlandNetwork"));
        assertEquals(Boolean.TRUE, type.getField("IS_CHINA_MAINLAND_MACHINE").get(null));
    }

    /// Checks that classes without region-check methods are rejected instead of
    /// being silently returned unchanged.
    @Test
    void rejectsUnrelatedClasses() throws Exception {
        assertThrows(IllegalStateException.class,
                () -> RegionUtilsTransformer.patch(readClass(String.class)));
    }

    /// Reads the class file bytes for a loaded class.
    ///
    /// @param type class to read
    /// @return class file bytes
    private static byte[] readClass(Class<?> type) throws Exception {
        InputStream input = Objects.requireNonNull(
                type.getResourceAsStream(type.getSimpleName() + ".class"),
                () -> "Missing class resource for " + type.getName());
        try (InputStream in = input) {
            return in.readAllBytes();
        }
    }

    /// Defines a class from raw bytes in an isolated loader.
    ///
    /// @param bytes class file bytes
    /// @return class defined by the isolated loader
    private static Class<?> define(byte[] bytes) {
        return new DefiningLoader().define(bytes);
    }

    /// Invokes a static boolean method on the given class.
    ///
    /// @param type class that declares the method
    /// @param name method name
    /// @return method result
    private static Boolean invoke(Class<?> type, String name) throws Exception {
        Method method = type.getDeclaredMethod(name);
        method.setAccessible(true);
        return (Boolean) method.invoke(null);
    }

    /// Class loader that defines patched fixture bytes without parent-first
    /// resolution of the fixture's package.
    @NotNullByDefault
    private static final class DefiningLoader extends ClassLoader {

        /// Creates a loader whose parent is the platform loader, so the fixture
        /// class is not inherited from the test class path.
        private DefiningLoader() {
            super(ClassLoader.getPlatformClassLoader());
        }

        /// Defines the fixture class from raw bytes.
        ///
        /// @param bytes class file bytes
        /// @return defined class
        private Class<?> define(byte[] bytes) {
            return defineClass(RegionUtils.class.getName(), bytes, 0, bytes.length);
        }
    }
}
