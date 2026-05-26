package io.astraebio.qtrace;

import qupath.lib.gui.QuPathGUI;

/**
 * Service interface implemented by the Enterprise module.
 * Discovered at runtime via ServiceLoader — if no enterprise JAR is present,
 * QTracePluginManager.get() returns null and premium features are silently absent.
 *
 * Premium features: Dashboard analytics, Batch processing.
 * Cryptographic certification (Ed25519 + OpenTimestamps) will be added here in v0.7.0.
 */
public interface QTracePlugin {
    void showDashboard(QuPathGUI qupath);
    void startBatch(QuPathGUI qupath, QTraceController controller);
}
