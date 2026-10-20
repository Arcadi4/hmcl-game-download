package io.github.arcadi4.hmcl.agent;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.instrument.Instrumentation;

/// Java agent entry point.
///
/// HMCL 3.17.0.362 (upstream PR #7017) gates game-content downloads behind
/// `org.jackhuang.hmcl.util.RegionUtils.isIsChinaMainlandNetwork`. The agent
/// installs a transformer that makes the network check report "not mainland
/// China", restoring the download UI and download providers on machines where
/// the restriction would otherwise apply.
///
/// The machine check is left intact: HMCL uses it for mirror preference,
/// Terracotta node selection, and offline-account behavior, none of which are
/// part of the download restriction.
///
/// The HMCL jar itself is never modified; the patch exists only in memory for
/// the lifetime of the JVM.
@NotNullByDefault
public final class HmclDownloadAgent {

    /// Prevents instantiation of this static entry-point class.
    private HmclDownloadAgent() {
    }

    /// JVM entry point when the jar is passed with `-javaagent:hmcl-download-agent.jar`.
    ///
    /// @param agentArgs unused agent arguments, possibly `null`
    /// @param instrumentation instrumentation service used to register the transformer
    public static void premain(@Nullable String agentArgs, Instrumentation instrumentation) {
        System.err.println("[hmcl-download-agent] Disabling HMCL mainland-China download restrictions");
        instrumentation.addTransformer(new RegionUtilsTransformer(), false);
    }
}
