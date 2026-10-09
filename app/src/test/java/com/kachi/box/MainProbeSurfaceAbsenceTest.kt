package com.kachi.box

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MainProbeSurfaceAbsenceTest {
    private val manifest by lazy { app("src/main/AndroidManifest.xml").toFile().readText() }
    private val receiver by lazy { app("src/main/java/com/kachi/box/RebindReceiver.kt").toFile().readText() }

    @Test
    fun `main receiver is private and exposes no test action`() {
        val declaration = Regex(
            """<receiver\s+android:name="\.RebindReceiver"\s+android:exported="([^"]+)"[\s\S]*?</receiver>"""
        ).find(manifest) ?: error("RebindReceiver declaration missing")
        assertTrue(declaration.groupValues[1] == "false", "main RebindReceiver must be exported=false")
        assertFalse(declaration.value.contains("com.kachi.box.TEST_"))
    }

    @Test
    fun `main source has no mass raw id or free form HAL handler`() {
        listOf(
            "com.kachi.box.TEST_",
            "TEST_ADAS_MASS",
            "TEST_HAL_WRITE",
            "getIntExtra(\"id\"",
            "getStringExtra(\"name\"",
            "featureIdsMatching(",
            "hal.setInt(",
        ).forEach { forbidden ->
            assertFalse(receiver.contains(forbidden), "main RebindReceiver retains forbidden probe token: $forbidden")
        }
    }

    @Test
    fun `boot package replacement and watchdog behavior remain declared and wired`() {
        listOf(
            "android.intent.action.MY_PACKAGE_REPLACED",
            "android.intent.action.BOOT_COMPLETED",
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            "\${applicationId}.REBIND_WATCHDOG",
        ).forEach { action -> assertTrue(manifest.contains(action), "missing manifest action $action") }
        listOf(
            "Intent.ACTION_BOOT_COMPLETED",
            "Intent.ACTION_LOCKED_BOOT_COMPLETED",
            "Intent.ACTION_MY_PACKAGE_REPLACED",
            "rebind(context)",
            "scheduleWatchdog(context)",
        ).forEach { token -> assertTrue(receiver.contains(token), "missing receiver behavior $token") }
        // Android box B2 · W1 — lượt dựng lại chiếu cụm lúc boot (`castBootWork`) gỡ khỏi receiver.
        assertTrue(!receiver.contains("castBootWork("), "castBootWork must be gone on Android box")
    }

    @Test
    fun `main and release contain no T10 vehicle probe component or action`() {
        val forbidden = listOf(
            "HudSignProbeReceiver",
            "HudSignProbeActivity",
            "com.kachi.box.vehicleTest.T10_",
        )
        listOf(app("src/main"), app("src/release")).filter(Files::exists).forEach { sourceRoot ->
            Files.walk(sourceRoot).use { paths ->
                paths.filter(Files::isRegularFile)
                    .filter {
                        it.toString().endsWith(".kt") ||
                            it.toString().endsWith(".java") ||
                            it.toString().endsWith(".xml")
                    }
                    .forEach { file ->
                        val content = Files.readString(file)
                        forbidden.forEach { token ->
                            assertFalse(content.contains(token), "$file exposes forbidden T10 token $token")
                        }
                    }
            }
        }
    }

    /**
     * Android box B2 · W2a: bề mặt probe HAL/T10 sống ở source set `vehicleTest` (receiver `HAL_PROBE` exported, activity
     * `HudSignProbe` exported, bản manifest riêng) — đã xoá. Build type `vehicleTest` giữ (debuggable cho `run-as` QA máy ảo)
     * nhưng KHÔNG được có mã/manifest riêng: một receiver exported mọc lại ở đó là cửa ghi HAL từ uid shell.
     */
    @Test
    fun `vehicleTest build type has no source set of its own`() {
        assertFalse(Files.exists(app("src/vehicleTest")), "src/vehicleTest (probe HAL/T10) đã gỡ ở B2 · W2a — không dựng lại")
        assertFalse(Files.exists(app("src/testVehicleTest")), "src/testVehicleTest đã gỡ cùng probe")
    }

    private fun app(relative: String): Path {
        val current = Path.of(System.getProperty("user.dir"))
        return if (Files.exists(current.resolve("src"))) current.resolve(relative) else current.resolve("app").resolve(relative)
    }
}
