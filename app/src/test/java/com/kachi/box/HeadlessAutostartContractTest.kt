package com.kachi.box

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import com.kachi.box.testsupport.SourceRoots
import org.junit.jupiter.api.Test

/**
 * WIRING contract for 1.21 Item 1 — HEADLESS auto-start (owner 2026-08-15, docs/diagnostics/plan-1.21.md).
 *
 * On boot the app must do its setup WITHOUT foregrounding a screen on the main display (bonus: dodges the
 * dudu size-compat letterbox). The runtime needs Android (BroadcastReceiver, Service, SharedPreferences,
 * View), so this locks the wiring by reading the source across the whole boundary:
 *   Prefs (default-ON toggle) → RebindReceiver (gates launchHome → BootSetupService, both boot entries) →
 *   BootSetupService (startForeground-first, enabled-gated grant + cluster-lane re-assert, always stops) →
 *   manifest (exported=false specialUse) → Kachi Settings › Hệ thống (the user toggle).
 *
 * ADDITIVE: auto-cast (FloatingBubbleService via castBootWork) is untouched by all of the above.
 *
 * ⚠ 2026-09-13 (S3): the old ClusterNav screen — which carried a second copy of the boot setup for the
 * user-opens-app case — was removed (`docs/specs/kachi-remove-legacy-screen.html`). Everything it asserted on
 * every open now has to come from one of the TWO boot branches, which is what
 * [`ca hai nhanh boot deu ep ba khoa khong co nut`] locks.
 */
class HeadlessAutostartContractTest {

    // ── source helpers (mirror NavCastUiWiringContractTest) ──────────────────
    private fun app(relative: String): Path {
        val current = Path.of(System.getProperty("user.dir"))
        return if (Files.exists(current.resolve("src"))) current.resolve(relative)
        else current.resolve("app").resolve(relative)
    }

    private fun read(path: Path): String = path.toFile().readText()

    private fun functionBody(source: String, signature: String): String {
        val start = source.indexOf(signature)
        require(start >= 0) { "missing $signature" }
        val after = start + signature.length
        val next = listOf("\n    fun ", "\n    private fun ", "\n    override fun ", "\n    companion object", "\n}")
            .mapNotNull { source.indexOf(it, after).takeIf { i -> i >= 0 } }
            .minOrNull() ?: source.length
        return source.substring(start, next)
    }

    private val prefs by lazy { read(app("src/main/java/com/kachi/box/Prefs.kt")) }
    private val receiver by lazy { read(app("src/main/java/com/kachi/box/RebindReceiver.kt")) }
    private val bootSetup by lazy { read(app("src/main/java/com/kachi/box/BootSetupService.kt")) }
    /** Công tắc + chuỗi setup "mở app" nay ở Kachi Settings › Hệ thống và cầu — màn cũ gỡ 2026-09-13 (S3). */
    private val section by lazy { read(app("src/main/java/com/kachi/box/launcher/SettingsSections.kt")) }
    private val navBridge by lazy { read(app("src/main/java/com/kachi/box/launcher/ClusterNavBridge.kt")) }
    private val manifest by lazy { read(app("src/main/AndroidManifest.xml")) }

    // ── Prefs: default-ON toggle ─────────────────────────────────────────────
    @Test
    fun `prefs declares headless autostart defaulting to true`() {
        assertTrue(prefs.contains("fun headlessAutostart(ctx: Context): Boolean"), "getter declared")
        assertTrue(
            prefs.contains("getBoolean(\"headless_autostart\", true)"),
            "headlessAutostart defaults ON (true) — boot goes headless unless the user opts out",
        )
        assertTrue(
            prefs.contains("fun setHeadlessAutostart(ctx: Context, v: Boolean)") &&
                prefs.contains("putBoolean(\"headless_autostart\""),
            "setter persists the flag",
        )
    }

    /**
     * Android box B2 · W1 (2026-10-09) — ĐỔI GHIM có lý do: `forcedPrefs` (ép `hud`=false · `interpolate`/`acc_booster`=true
     * mỗi lần nổ máy) chỉ phục vụ dẫn đường lên cụm/HUD BYD ⇒ gỡ khỏi CẢ HAI nhánh boot và khỏi `BootSetupService`.
     */
    @Test
    fun `khong nhanh boot nao con ep khoa HUD`() {
        assertTrue("forcedPrefs" !in bootSetup, "BootSetupService không còn ép khoá HUD / bù cự ly")
        assertTrue("forcedPrefs" !in receiver, "nhánh launchHome không còn ép khoá HUD / bù cự ly")
        assertTrue("Prefs.setHud(" !in bootSetup)
    }

    // ── RebindReceiver: gate launchHome → BootSetupService on both boot entries ──
    @Test
    fun `boot completed gates launchHome on the toggle and starts BootSetupService`() {
        val body = functionBody(receiver, "override fun onReceive")
        val boot = body.substring(
            body.indexOf("Intent.ACTION_BOOT_COMPLETED"),
            body.indexOf("Intent.ACTION_LOCKED_BOOT_COMPLETED"),
        )
        assertTrue(boot.contains("Prefs.headlessAutostart(context)"), "BOOT_COMPLETED reads the toggle")
        assertTrue(boot.contains("startBootSetup(context)"), "BOOT_COMPLETED starts the headless setup when ON")
        assertTrue(boot.contains("launchHome(context)"), "BOOT_COMPLETED falls back to launchHome when OFF")
        // Untouched behaviour that must remain.
        assertTrue(boot.contains("scheduleWatchdog(context)"), "watchdog still scheduled")
        // Android box B2 · W1 — auto-cast (castBootWork: SimpleCastRuntime · nút nổi · CastAutomationService) GỠ.
        assertTrue(!boot.contains("castBootWork("), "auto-cast removed on Android box")
    }

    @Test
    fun `package replaced gates launchHome on the toggle and starts BootSetupService`() {
        val body = functionBody(receiver, "override fun onReceive")
        val replaced = body.substring(body.indexOf("Intent.ACTION_MY_PACKAGE_REPLACED"))
        assertTrue(replaced.contains("Prefs.headlessAutostart(context)"), "MY_PACKAGE_REPLACED reads the toggle")
        assertTrue(replaced.contains("startBootSetup(context)"), "MY_PACKAGE_REPLACED starts the headless setup when ON")
        assertTrue(replaced.contains("launchHome(context)"), "MY_PACKAGE_REPLACED falls back to launchHome when OFF")
        assertTrue(!replaced.contains("castBootWork("), "auto-cast removed on Android box (B2 · W1)")
    }

    @Test
    fun `startBootSetup helper launches the headless service as a foreground service`() {
        val helper = functionBody(receiver, "private fun startBootSetup")
        assertTrue(helper.contains("startForegroundService("), "started as a foreground service (dadb grant > receiver budget)")
        assertTrue(helper.contains("BootSetupService::class.java"), "starts BootSetupService")
        assertTrue(helper.contains("runCatching"), "best-effort — never throws out of the receiver")
    }

    // ── BootSetupService: startForeground-first, gated setup, always stops ──
    @Test
    fun `boot setup service goes foreground first then does the gated setup off the main thread`() {
        val onStart = functionBody(bootSetup, "override fun onStartCommand")
        assertTrue(onStart.contains("startForegroundOnce()"), "startForeground gate present")
        assertTrue(
            onStart.indexOf("startForegroundOnce()") < onStart.indexOf("Thread("),
            "startForeground happens BEFORE the background work (5 s startForegroundService budget)",
        )
        // Android box B2 · W1 — grant gated ONLY on the physical-key switch (the GMaps screen-read for the cluster is gone),
        // and the BYD-only boot work (cluster-lane / HUD outputs, speed sign, VietMap, seat / PM2.5 / recirc) is not here.
        assertTrue(onStart.contains("if (Prefs.voiceKeyEnabled(applicationContext))"), "setup gated on the physical-key switch")
        assertTrue(!onStart.contains("Prefs.enabled(applicationContext)"), "no longer gated on Nav+HUD")
        assertTrue(onStart.contains("KeyServiceConnect.grantAccessibility(applicationContext)"), "relocated accessibility grant + force-bind")
        assertTrue(onStart.contains("VoiceKeyKeepAliveService.sync(applicationContext)"), "voice-key keep-alive still synced")
        listOf("NavRepository", "NavigationSpeedSignOwner", "VietMapAutostartService", "SeatComfortApplier",
            "Pm25FilterApplier", "RecircApplier").forEach { assertTrue(!onStart.contains(it), "BYD boot work '$it' removed") }
        assertTrue(onStart.contains("runCatching"), "wrapped so it never crashes the process")
    }

    @Test
    fun `boot setup grant only escalates when the accessibility service is not already bound`() {
        val onStart = functionBody(bootSetup, "override fun onStartCommand")
        assertTrue(
            onStart.contains("KeyServiceConnect.isAccessibilityBound"),
            "grant gated on REAL bound (không cờ connected in-process kẹt khi hệ unbind ngầm) — đã bound là no-op",
        )
    }

    @Test
    fun `boot setup service always stops foreground and self`() {
        assertTrue(bootSetup.contains("startForeground("), "calls startForeground")
        val finish = functionBody(bootSetup, "private fun finish")
        assertTrue(finish.contains("stopForeground("), "stopForeground on finish")
        assertTrue(finish.contains("stopSelf("), "stopSelf on finish")
        // finish(startId) sits OUTSIDE the runCatching in onStartCommand, so it ALWAYS runs.
        val onStart = functionBody(bootSetup, "override fun onStartCommand")
        assertTrue(onStart.contains("finish(startId)"), "onStartCommand always calls finish")
    }

    @Test
    fun `boot setup does not touch the auto-cast track`() {
        // Auto-cast is already headless (FloatingBubbleService is the sole autostart driver via castBootWork).
        assertTrue(!bootSetup.contains("FloatingBubbleService"), "must not start the bubble/cast service")
        assertTrue(!bootSetup.contains("openProjection"), "must not drive projection")
        assertTrue(!bootSetup.contains("castEnabled"), "must not read/steer the cast master")
    }

    // ── manifest: exported=false specialUse ─────────────────────────────────
    @Test
    fun `manifest declares BootSetupService as private special-use foreground service`() {
        val decl = Regex("""<service\s+android:name="\.BootSetupService"[\s\S]*?</service>""")
            .find(manifest)?.value ?: error("BootSetupService declaration missing")
        assertTrue(decl.contains("android:exported=\"false\""), "BootSetupService must be exported=false")
        assertTrue(decl.contains("android:foregroundServiceType=\"specialUse\""), "declared as a specialUse FGS")
        // [ĐO on-car 2026-09-15, DiLink3/Android 10] `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE">`
        // (thẻ API 34) làm PackageParser Android 10 từ chối cài: "Unknown element under <service>: property" → GUI
        // install FAIL. Đội xe chỉ chạy A10 (DL3) / A12 (DL5) — không có A14+ nào cần subtype. Bất biến MỚI: KHÔNG có
        // `<property>` dưới <service>. (Bài canh trước pin "có property" theo targetSdk 34 — đã đảo có bằng chứng.)
        assertTrue(!decl.contains("<property"), "KHONG duoc co <property> duoi <service> — Android 10 PackageParser tu choi cai")
    }

    // ── Bề mặt người dùng: ô tick ở Kachi Settings › Hệ thống ───────────────
    //
    // Tới 2026-09-13 hai bài dưới đọc `MainActivity` + hai biến thể `activity_main.xml` (ô tick
    // `cb_headless_autostart`). Màn đó đã gỡ (S3 · R1) ⇒ ô tick chỉ còn ở nhóm *Hệ thống* của Kachi Settings,
    // dựng bằng mã và ghi qua cầu.
    @Test
    fun `the system settings group wires the headless autostart switch through the bridge`() {
        assertTrue(section.contains("deps.bridge.headlessAutostart()"), "reads the current pref for the tick")
        assertTrue(section.contains("deps.bridge.setHeadlessAutostart("), "persists the flag on toggle")
        assertTrue(navBridge.contains("Prefs.setHeadlessAutostart(app, on)"), "the bridge writes the real key")
    }

    @Test
    fun `the app-open setup survived the screen removal`() {
        // Relocating to BootSetupService must NOT lose the setup that used to run when the user opened the app.
        // Android box B2 · W2d: the Nav-on-cluster master switch (which self-granted accessibility + re-asserted the
        // cluster lane) is gone with cluster navigation; the app-open accessibility self-grant now lives ONLY in the
        // permission preflight (key service), and the bridge no longer touches the cluster lane.
        val preflight = SourceRoots.codeOf("src/main/java/com/kachi/box/launcher/PermissionPreflight.kt")
        assertTrue(preflight.contains("KeyServiceConnect.grantAccessibility("), "app-open still self-grants accessibility (keys)")
        assertTrue(!navBridge.contains("NavigationOutputTarget"), "the bridge no longer re-asserts the cluster lane")
    }
}
