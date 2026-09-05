package io.qtrace.screenshots;

import io.qtrace.ClassifierFidelity;
import io.qtrace.LicenseInfo;
import io.qtrace.QTraceBatchExporter;
import io.qtrace.QTraceController;
import io.qtrace.QTraceDashboard;
import io.qtrace.QTracePanel;
import io.qtrace.QTracePlugin;
import io.qtrace.QTracePluginManager;
import io.qtrace.QTraceSettingsDialog;
import io.qtrace.ValidationStamper;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.stage.Window;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.projects.Project;
import qupath.lib.projects.ProjectImageEntry;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Generates docs/user-guide/screenshots/*.png without any human interaction.
 *
 * Boots a plain JavaFX toolkit (no QuPath, no real QuPathGUI — that class can't be
 * instantiated outside a real QuPath session, so it's Mockito-mocked instead), opens
 * each qTrace dialog/panel with plausible demo data, snapshots its scene, and closes it.
 * Run with `./gradlew screenshotsUI` from qtrace-core/ — needs a real display (X11) or
 * `xvfb-run -a ./gradlew screenshotsUI` for a fully invisible run. See
 * docs/user-guide/screenshots/README.md for coverage and design notes.
 */
public class ScreenshotHarness {

    /** How long to let a screen finish laying out (and, for the Dashboard, finish its
     *  background scan) before snapshotting. Generous — this only runs on demand, never in CI. */
    private static final long SETTLE_MS = 500;
    private static final long DASHBOARD_SETTLE_MS = 1500;

    private final Path outDir;
    private final Path fixturesDir;
    private QuPathGUI qupath;
    private QTraceController controller;
    private Stage hostStage;

    public static void main(String[] args) throws Exception {
        Path outDir = Path.of(System.getProperty("qtrace.screenshots.outDir", "screenshots"));
        // Opt-in only, and NOT wired by default in the Gradle task: these are real local
        // .qtrace captures (contributor names, project names — whatever is in them), so
        // enabling this must be a deliberate choice, reviewed before the resulting
        // dashboard.png is committed/published. Absent → Dashboard capture is skipped.
        String fixturesProp = System.getProperty("qtrace.screenshots.fixturesDir");
        Path fixturesDir = fixturesProp != null ? Path.of(fixturesProp) : null;
        Files.createDirectories(outDir);

        // Platform.startup's runnable executes ON the FX Application Thread itself — running
        // the whole harness there would deadlock capture()'s latch.await() (see below), so it
        // only boots the toolkit here. The actual harness runs on a plain background thread,
        // which is free to block/sleep between steps while hopping onto the FX thread via
        // Platform.runLater() for each UI operation.
        CountDownLatch toolkitReady = new CountDownLatch(1);
        Platform.startup(toolkitReady::countDown);
        toolkitReady.await();
        Platform.setImplicitExit(false);

        CountDownLatch done = new CountDownLatch(1);
        Thread driver = new Thread(() -> {
            try {
                new ScreenshotHarness(outDir, fixturesDir).runAll();
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                done.countDown();
            }
        }, "screenshot-harness-driver");
        driver.start();
        done.await();
        Platform.exit();
        System.exit(0);
    }

    private ScreenshotHarness(Path outDir, Path fixturesDir) {
        this.outDir = outDir;
        this.fixturesDir = fixturesDir;
    }

    private void runAll() throws Exception {
        // Stage creation must happen on the FX Application Thread — this driver thread isn't
        // it (see the comment in main()), so hop over once via runLater + latch.
        CountDownLatch initDone = new CountDownLatch(1);
        Platform.runLater(() -> {
            hostStage = new Stage();
            // A owner Stage needs a real Scene — Dialog.initOwner() reads owner.getScene()
            // to bind stylesheets and NPEs otherwise (HeavyweightDialog.updateStageBindings).
            hostStage.setScene(new javafx.scene.Scene(new javafx.scene.layout.StackPane(), 10, 10));
            hostStage.setX(-4000);
            hostStage.setY(-4000);
            hostStage.show();
            initDone.countDown();
        });
        initDone.await();

        qupath = mock(QuPathGUI.class);
        when(qupath.getStage()).thenReturn(hostStage);

        controller = mock(QTraceController.class);
        when(controller.getCurrentImageName()).thenReturn("Demo_Slide_01.ome.tiff");
        when(controller.getQuPath()).thenReturn(qupath);

        capture("panel-core", SETTLE_MS, () -> new QTracePanel(qupath, controller).show());
        capture("settings", SETTLE_MS, () -> QTraceSettingsDialog.show(hostStage));
        capture("stamp-dialog", SETTLE_MS, () -> ValidationStamper.show(
            hostStage,
            "a1b2c3d",
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85",
            "5f9c9d6c4d1e2b3a7f8e0d1c2b3a4f5e6d7c8b9a0f1e2d3c4b5a6f7e8d9c0b1a",
            ClassifierFidelity.HIGH,
            "1-In Progress",
            "Demo-Project"
        ));
        captureBatchExportPreflight();
        captureDashboard();

        // Registered last — QTracePluginManager is a JVM-global singleton, and every capture
        // above relies on it being empty (Core behavior: unlocked validator, no attestation,
        // Core-only toolbar). Only the Compliance panel capture below needs it non-null.
        captureCompliancePanel();

        Platform.runLater(() -> hostStage.close());
    }

    // ── Compliance panel — registers a fake QTracePlugin with a demo certified license ──

    private void captureCompliancePanel() throws InterruptedException {
        LicenseInfo demoLicense = new LicenseInfo(
            "demo-user",
            "Dr. Jane Demo",
            "Demo Institution",
            "jane.demo@example.org",
            true,
            Instant.now().minus(30, ChronoUnit.DAYS),
            Instant.now().plus(335, ChronoUnit.DAYS),
            "z6MkDemoPublicKeyXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
            null, null, null
        );
        QTracePluginManager.register(new QTracePlugin() {
            @Override public String getPluginVersion() { return "1.1.4"; }
            @Override public LicenseInfo getActiveLicenseInfo() { return demoLicense; }
        });

        capture("panel-compliance", SETTLE_MS, () -> new QTracePanel(qupath, controller).show());
    }

    // ── Batch Export pre-flight — needs a mocked project with a few dummy entries ──

    @SuppressWarnings("unchecked")
    private void captureBatchExportPreflight() throws InterruptedException {
        Project<BufferedImage> project = mock(Project.class);
        List<ProjectImageEntry<BufferedImage>> entries = Stream.generate(
            () -> (ProjectImageEntry<BufferedImage>) mock(ProjectImageEntry.class))
            .limit(3).collect(Collectors.toList());
        when(project.getImageList()).thenReturn(entries);
        when(qupath.getProject()).thenReturn(project);

        capture("batch-export-preflight", SETTLE_MS,
            () -> QTraceBatchExporter.start(qupath, controller));
    }

    // ── Dashboard — only meaningful with real .qtrace fixtures; skipped otherwise ──

    private void captureDashboard() throws Exception {
        if (fixturesDir == null) {
            System.out.println("[screenshots] dashboard: opt-in only — pass "
                + "-Dqtrace.screenshots.fixturesDir=<dir with real .qtrace files you have "
                + "reviewed for names/PHI> to enable — skipping");
            return;
        }
        if (!Files.isDirectory(fixturesDir)) {
            System.out.println("[screenshots] dashboard: no fixtures dir at " + fixturesDir + " — skipping");
            return;
        }
        List<Path> sampleFiles;
        try (Stream<Path> s = Files.list(fixturesDir)) {
            sampleFiles = s.filter(p -> p.toString().endsWith(".qtrace")).limit(5).collect(Collectors.toList());
        }
        if (sampleFiles.isEmpty()) {
            System.out.println("[screenshots] dashboard: no .qtrace files under " + fixturesDir + " — skipping");
            return;
        }

        // QTraceConfig's export dir resolves against system property "user.home" (isolated
        // fake home, set by the Gradle task) — copying fixtures there is what the Dashboard
        // will scan, without ever touching the real ~/.qTrace export dir.
        Path exportDir = Path.of(System.getProperty("user.home"), "Documents", "QuPath", "scripts", "qtrace");
        Files.createDirectories(exportDir);
        for (Path f : sampleFiles) {
            Files.copy(f, exportDir.resolve(f.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("[screenshots] dashboard: using " + sampleFiles.size()
            + " local fixture(s) from " + fixturesDir);

        capture("dashboard", DASHBOARD_SETTLE_MS, () -> new QTraceDashboard(qupath).show());
    }

    // ── Generic capture: open a screen, wait for it to settle, snapshot the newest ──
    // window, close it. Works uniformly for plain Stage.show() (Panel, Dashboard) and
    // for Dialog/Stage.showAndWait() (Settings, Stamp, Batch pre-flight) — showAndWait()
    // runs a nested event loop that keeps pumping queued Platform.runLater tasks, so the
    // delayed "capture and close" task below still gets to run and unblock it.

    private void capture(String name, long settleMs, Runnable opener) throws InterruptedException {
        List<Window> before = List.copyOf(Window.getWindows());
        Platform.runLater(opener);
        Thread.sleep(settleMs);

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                Window target = Window.getWindows().stream()
                    .filter(w -> !before.contains(w) && w.isShowing())
                    .reduce((a, b) -> b) // last-opened wins
                    .orElse(null);
                if (target == null) {
                    System.err.println("[screenshots] " + name + ": no new window appeared — skipped");
                    return;
                }
                WritableImage snap = target.getScene().getRoot().snapshot(new SnapshotParameters(), null);
                BufferedImage buffered = SwingFXUtils.fromFXImage(snap, null);
                Path out = outDir.resolve(name + ".png");
                ImageIO.write(buffered, "png", out.toFile());
                System.out.println("[screenshots] wrote " + out);
                if (target instanceof Stage stage) stage.close(); else target.hide();
            } catch (IOException e) {
                System.err.println("[screenshots] " + name + ": failed to write PNG — " + e.getMessage());
            } catch (Exception e) {
                System.err.println("[screenshots] " + name + ": capture failed — " + e);
            } finally {
                latch.countDown();
            }
        });
        latch.await();
    }
}
