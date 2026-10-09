package com.byd.clusternav.launcher

import com.byd.clusternav.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Android box B3 — dây nối "không có kênh shell / không có micro" (spec `androidbox-plan.html` §4.2). Luật thuần ở
 * `:core NoShellFallbackTest`; bài này canh rằng MỌI lối vào hỏi đúng luật đó, và đường lùi OTA không mở rộng hơn cần.
 */
class NoShellWiringContractTest {

    private val base = "src/main/java/com/byd/clusternav"
    private fun code(rel: String) = SourceRoots.codeOf("$base/$rel")
    private val manifest by lazy { File("src/main/AndroidManifest.xml").readText() }

    @Test
    fun `hang quyen chi bay nut tay khi kenh KHONG dung duoc`() {
        val sys = code("launcher/SettingsSections.kt")
        assertTrue(sys.contains("NoShellFallback.manualFix(req, usable)"), "nút tay theo luật :core")
        assertTrue(sys.contains("val usable = ShellAccessUi.usableNow()"), "đọc trạng thái kênh SẴN CÓ, không dò mới")
        val row = SourceRoots.body(code("launcher/SettingsRows.kt"), "fun permissionRow(")
        // Không kênh ⇒ câu "Kachi đang tự xin lại" là SAI — câu tay phải thắng trước.
        assertTrue(row.indexOf("kachi_perm_manual_hint") in 0 until row.indexOf("kachi_perm_self_fixing"))
    }

    @Test
    fun `mo man he thong luon boc loi va noi that khi that bai`() {
        val opener = code("launcher/NoShellUi.kt")
        assertTrue(opener.contains("catch (e: ActivityNotFoundException)"))
        assertTrue(opener.contains("catch (e: SecurityException)"))
        assertTrue(SourceRoots.body(opener, "fun openHomePicker(").let { it.indexOf("enableHomeEntry") in 0 until it.indexOf("ACTION_HOME_SETTINGS") },
            "alias HOME tắt sẵn ⇒ phải bật TRƯỚC khi mở màn chọn")
        assertTrue(code("launcher/SettingsSections.kt").contains("R.string.kachi_perm_open_failed"), "mở hỏng ⇒ nói thật")
        assertTrue(code("launcher/KachiHomeActivity.kt").contains("panels.refreshPermissionsPage()"), "quay về ⇒ đọc lại quyền")
    }

    @Test
    fun `tuy chon nha phat trien tat thi khong mo man tu dong ngay ma chi cach bat`() {
        // [ĐO máy ảo 09/10] tắt mà mở ACTION_APPLICATION_DEVELOPMENT_SETTINGS ⇒ DevelopmentSettingsDisabledActivity đóng ngay.
        val fn = SourceRoots.body(code("launcher/SettingsSections.kt"), "private fun openManualFix(")
        assertTrue(fn.indexOf("SystemSettingsOpener.devOptionsOn(activity) == false") in 0 until fn.indexOf("SystemSettingsOpener.open(activity, fix)"))
        assertTrue(fn.contains("R.string.kachi_perm_dev_options_off") && fn.contains("SystemSettingsOpener.openDeviceInfo(activity)"))
        assertTrue(code("launcher/NoShellUi.kt").contains("Settings.Global.DEVELOPMENT_SETTINGS_ENABLED"))
    }

    @Test
    fun `dat HOME khong kenh di man chon cua he thong`() {
        val sys = code("launcher/SettingsSections.kt")
        assertTrue(sys.contains("NoShellFallback.homeRoute(ShellAccessUi.usableNow(), com.byd.clusternav.ShellReadiness.phase())"))
        assertTrue(sys.indexOf("HomeRoute.SYSTEM_PICKER") in 0 until sys.indexOf("deps.bridge.setDefaultHome"))
    }

    @Test
    fun `o app khong kenh co duong mo toan man`() {
        val card = code("launcher/ShellAccessCard.kt")
        assertTrue(card.contains("NoShellFallback.offerFullscreen(ShellReadiness.phase())"))
        assertTrue(card.contains("AppOpener(activity).openByIntent(pkg)"), "đường U3 sẵn có, không đường mở thứ hai")
        assertTrue(code("launcher/LauncherWindows.kt").contains("ShellAccessUi.slotTap({ embedding() }, pkg)"))
    }

    @Test
    fun `khong micro thi moi loi vao giong noi deu gac`() {
        assertTrue(code("launcher/KachiHomeActivity.kt").contains("DeviceMic.voiceAvailable(this)"), "nút mic")
        assertTrue(code("launcher/ControlDockView.kt").contains("id == LauncherActions.VOICE && !DeviceMic.voiceAvailable(ui)"), "ô thanh nút")
        val wake = code("launcher/voice/VoiceWakePrefsMain.kt")
        assertTrue(wake.contains("fun keyHold(ctx: Context): Boolean = DeviceMic.voiceAvailable(ctx) &&"), "HOLD")
        assertTrue(wake.contains("wakeSwitch = Prefs.wakeSwitchOn(ctx) && DeviceMic.voiceAvailable(ctx)"), ":wake không mở mic không có")
        assertTrue(code("KachiApplication.kt").contains("if (com.byd.clusternav.launcher.DeviceMic.voiceAvailable(this)) VoiceEngine.preload(this)"))
        assertTrue(code("modules/voicekey/AssistantLauncher.kt").contains("DeviceMic.voiceAvailable(app)"), "phím Kachi nghe")
        assertTrue(code("launcher/SettingsVoiceSection.kt").contains("R.string.kachi_voice_no_mic_hw"), "nhóm Giọng nói nói thật")
        assertTrue(code("launcher/PermissionPreflight.kt").contains("NoShellFallback.notApplicable(DeviceMic.feature(ctx))"))
    }

    @Test
    fun `OTA khong kenh di trinh cai he thong qua FileProvider chi thu muc update`() {
        val flow = code("UpdateFlow.kt")
        assertTrue(flow.indexOf("OtaRoute.SYSTEM_INSTALLER") in 0 until flow.indexOf("UpdateChecker.install(app, f)"))
        val install = code("OtaSystemInstall.kt")
        assertTrue(install.indexOf("NoShellFallback.archiveMatches") in 0 until install.indexOf("startActivity"), "kiểm gói TRƯỚC khi cài")
        // Review Pass 2 [P2]: kiểm người ký; đọc APK trên luồng NỀN (verify trước runOnUiThread); API không deprecated.
        assertTrue(install.contains("NoShellFallback.signersMatch("))
        assertFalse(install.contains("getPackageArchiveInfo"), "đi qua PackageQueries.archiveInfo (overload API 33)")
        val sys = SourceRoots.body(flow, "OtaRoute.SYSTEM_INSTALLER) {")
        assertTrue(sys.indexOf("OtaSystemInstall.verify(app, f)") in 0 until sys.indexOf("runOnUiThread"), "đọc APK trên luồng nền")
        assertTrue(install.contains("FLAG_GRANT_READ_URI_PERMISSION"))
        assertTrue(manifest.contains("android.permission.REQUEST_INSTALL_PACKAGES"))
        val provider = manifest.substringAfter("androidx.core.content.FileProvider").substringBefore("</provider>")
        assertTrue(provider.contains("android:exported=\"false\""), "provider không exported")
        assertTrue(provider.contains("android:grantUriPermissions=\"true\""))
        assertTrue(provider.contains("\${applicationId}.ota"))
        val paths = File("src/main/res/xml/ota_paths.xml").readText().substringAfter("<paths>")
        assertTrue(paths.contains("<files-path name=\"update\" path=\"update/\" />"))
        assertFalse(Regex("""path="(\.|/)?"""").containsMatchIn(paths), "không chia sẻ gốc filesDir")
        assertFalse(paths.contains("external"), "không chia sẻ bộ nhớ ngoài")
        assertTrue(Regex("<[a-z-]+-path ").findAll(paths).count() == 1, "đúng MỘT đường chia sẻ")
    }
}
