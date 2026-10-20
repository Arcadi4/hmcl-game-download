/*
 * Test fixture that mimics HMCL's region-check surface.
 *
 * HMCL Download Agent
 */

package org.jackhuang.hmcl.util;

import org.jetbrains.annotations.NotNullByDefault;

/// Minimal stand-in for HMCL's `RegionUtils`, used to verify the transformer
/// without requiring an HMCL installation.
@NotNullByDefault
public final class RegionUtils {

    /// Field initialized from the private helper, mirroring HMCL's layout.
    public static final boolean IS_CHINA_MAINLAND_MACHINE = checkMachineIsChinaMainland();

    /// Prevents instantiation.
    private RegionUtils() {
    }

    /// Always reports a mainland-China machine in the unmodified fixture.
    ///
    /// @return always `true`
    public static boolean isIsChinaMainlandMachine() {
        return true;
    }

    /// Always reports a mainland-China network in the unmodified fixture.
    ///
    /// @return always `true`
    public static boolean isIsChinaMainlandNetwork() {
        return true;
    }

    /// Private machine check used by the static initializer.
    ///
    /// @return always `true`
    private static boolean checkMachineIsChinaMainland() {
        return true;
    }

    /// Private network check used by the public wrapper.
    ///
    /// @return always `true`
    private static boolean checkChinaMainlandNetwork() {
        return true;
    }
}
