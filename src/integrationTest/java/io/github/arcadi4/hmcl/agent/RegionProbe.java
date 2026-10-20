package io.github.arcadi4.hmcl.agent;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.TimeZone;

/// Standalone probe used by `scripts/test-against-hmcl.sh`.
///
/// The probe forces a China-like environment:
///
/// - the default time zone is `Asia/Shanghai`, so HMCL classifies the machine as
///   mainland China;
/// - the default proxy points at a closed local port, so the network lookup
///   fails and HMCL treats the network as mainland China.
///
/// Without the agent the two checks must report `true`; with the agent the
/// machine check must stay `true` while the network gate reports `false`.
@NotNullByDefault
public final class RegionProbe {

    /// Prevents instantiation.
    private RegionProbe() {
    }

    /// Runs the probe and prints `machine=<bool> network=<bool>`.
    ///
    /// @param args unused
    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        ProxySelector.setDefault(new ClosedProxySelector());

        Class<?> regionUtils = Class.forName("org.jackhuang.hmcl.util.RegionUtils");
        boolean machine = invoke(regionUtils, "isIsChinaMainlandMachine");
        boolean network = invoke(regionUtils, "isIsChinaMainlandNetwork");
        System.out.println("machine=" + machine + " network=" + network);
    }

    /// Invokes a public static boolean method.
    ///
    /// @param type class that declares the method
    /// @param name method name
    /// @return method result
    private static boolean invoke(Class<?> type, String name) throws Exception {
        return (boolean) type.getMethod(name).invoke(null);
    }

    /// Proxy selector that sends every request to a closed local port, forcing
    /// HMCL's network lookup to fail.
    @NotNullByDefault
    private static final class ClosedProxySelector extends ProxySelector {

        /// Creates the closed-port selector.
        private ClosedProxySelector() {
        }

        /// Returns an HTTP proxy on port 1 for every URI.
        ///
        /// @param uri request URI
        /// @return immutable single-element proxy list
        @Override
        public @Unmodifiable List<Proxy> select(URI uri) {
            return List.of(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 1)));
        }

        /// Ignores connection failures; the failed connection is the point.
        ///
        /// @param uri request URI
        /// @param sa address that failed
        /// @param ioe failure
        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        }
    }
}
