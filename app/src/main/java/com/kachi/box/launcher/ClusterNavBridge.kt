package com.kachi.box.launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.kachi.box.Prefs
import com.kachi.box.ThemeMode

/**
 * ═══ CẦU DUY NHẤT giữa Kachi Settings và cấu hình/hành động của ClusterNav ═══════════════════════
 *
 * Spec: `docs/specs/kachi-settings-ia-v2.html` §4.2 (kiến trúc), §4.3 (bản đồ khoá), N2 (UI không
 * rải `Prefs.set…`), N5 (đọc HAL trên thread nền).
 *
 * ## Vì sao LẶP LẠI thay vì gọi lại `MainActivity`
 * [ĐO] `activity_main.xml` + `res/values/strings.xml` nằm trong `T11_PATHS` của
 * `ExpansionTransportFenceTest` (hash byte-seal) và `MainActivity.kt` bị ~10 wiring-contract test đọc
 * source ⇒ **không được sửa một byte** để trích hàm ra dùng chung (spec §2 "Ràng buộc cứng", §9).
 * Nên mỗi hàm public ở đây **chép lại đúng chuỗi lời gọi** của màn cũ và KDoc ghi rõ
 * `lặp lại MainActivity.kt:<dòng>` để hai bên còn so được. Khi màn cũ bị gỡ (OQ1) thì cầu này thành
 * nguồn duy nhất.
 *
 * ## Hợp đồng
 *  - Nhận [app] = **applicationContext** (bridge sống lâu hơn Activity — không giữ View, không giữ
 *    Activity; xem test `ClusterNavBridgeWiringContractTest`).
 *  - [toast] = cách hiện thông báo ngắn của tầng gọi. Nó nhận **mã** [BridgeMsg], KHÔNG nhận câu —
 *    tầng `launcher/` bắt mọi chữ đi qua tài nguyên (`LauncherI18nContractTest`), còn nhánh ClusterNav
 *    đặt nhãn lúc chạy vì `strings.xml` đang byte-seal. Cầu đứng giữa nên không mang chữ của bên nào;
 *    KDoc từng giá trị [BridgeMsg] chép nguyên văn VI/EN của màn cũ để tầng Settings dịch lại y hệt.
 *  - [ui] = post một [Runnable] về luồng vẽ.
 *  - [activityProvider] chỉ dùng cho ĐÚNG một đường bắt buộc có Activity ([checkUpdate] →
 *    [UpdateFlow.start], mở dialog + cài APK). Mọi hàm khác chạy được không cần Activity.
 *  - Mọi hàm "trạng thái" trả **mã / dữ liệu thô** ([VoiceKeyStatus], tên gói, mã phím) — không trả câu.
 *
 *
 * ⚠ **2026-09-13 — màn cũ đã GỠ HẲN** (`docs/specs/kachi-remove-legacy-screen.html` R1/R3). Mọi chỉ dẫn
 * `MainActivity.kt:<dòng>` dưới đây là **vết lịch sử**, không phải một tệp còn đọc được: chúng trỏ vào bản trước
 * commit gỡ màn (tra bằng `git log -- app/src/main/java/com/kachi/box/MainActivity.kt`). Giữ số dòng vì đó là
 * cách duy nhất còn lại để so hành vi của cầu với bản gốc; cầu nay là **nguồn duy nhất** của những hành vi đó.
 * Phần Cast và phần Phím nằm ở `ClusterNavBridgeCast.kt` / `ClusterNavBridgeKeys.kt` dưới dạng hàm
 * mở rộng của CHÍNH lớp này (giữ bề mặt phẳng `bridge.castFull(...)`, mà mỗi tệp vẫn dưới trần LOC).
 */
import android.os.Handler

import android.os.Looper

class ClusterNavBridge(
    app: Context,
    internal val toast: (BridgeMsg) -> Unit,
    internal val ui: (Runnable) -> Unit,
    internal val activityProvider: () -> Activity? = { null },
) {
    /** LUÔN là applicationContext — chặn ngay tại cửa việc lỡ truyền Activity vào một vật sống lâu. */
    internal val app: Context = app.applicationContext

    // ── Dẫn đường ────────────────────────────────────────────────────────────────────────────────
    // Android box B2 · W2d — công tắc *Dẫn đường lên cụm* + chế độ cụm + chạy chữ + *Kết nối lại* + nguồn/đầu ra cụm gỡ cùng
    // dẫn đường cụm/HUD BYD. Còn app dẫn đường / nhạc mặc định (giọng nói) dưới đây.

    /** App dẫn đường MẶC ĐỊNH khi câu KHÔNG nêu tên app (owner 2026-09-18) — key của [VoiceAppTargets]. */
    fun navDefaultApp(): String = Prefs.voiceNavDefaultApp(app)
    fun setNavDefaultApp(key: String) = Prefs.setVoiceNavDefaultApp(app, key)

    /** Các app dẫn đường chọn được (key) — nguồn sự thật [VoiceAppTargets.NAV], không chép tay. */
    fun navAppChoices(): List<String> =
        com.kachi.box.launcher.voice.VoiceAppTargets.NAV.map { it.key }

    // App NHẠC mặc định (owner 2026-09-21) — "" = tự chọn (app đang phát / app đầu tiên).
    fun musicDefaultApp(): String = Prefs.voiceMusicDefaultApp(app)
    fun setMusicDefaultApp(key: String) = Prefs.setVoiceMusicDefaultApp(app, key)

    /** Các app nhạc chọn được (key) — nguồn [VoiceAppTargets.MUSIC], kèm "" đứng đầu = tự chọn. */
    fun musicAppChoices(): List<String> =
        listOf("") + com.kachi.box.launcher.voice.VoiceAppTargets.MUSIC.map { it.key }

    /**
     * Nút "Khởi động lại launcher" (owner 2026-09-25) — restart process Kachi cho sạch khi có lỗi (bind rớt, cụm
     * kẹt, overlay treo). Cách: mở lại [KachiHomeActivity] (NEW_TASK + CLEAR_TASK) rồi `Process.killProcess(myPid)`
     * — process chết, Activity vừa launch làm hệ thống dựng lại process từ đầu (state sạch). Kachi là HOME nên kể
     * cả nếu launch bị chậm, bấm Home vẫn về Kachi. Delay nhỏ để startActivity kịp đăng ký trước khi giết.
     */
    fun restartLauncher() {
        runCatching {
            val i = Intent(app, KachiHomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            app.startActivity(i)
        }
        Handler(Looper.getMainLooper()).postDelayed({
            android.os.Process.killProcess(android.os.Process.myPid())
        }, 300L)
    }

    // Quyền hệ thống (`notificationAccessGranted` · `accessibilityBoosterGranted` · `accessibilityBound`) + nhóm *Hệ thống*
    // (`checkUpdate`; hai cửa chẩn đoán 0 chỗ gọi gỡ ở 2.93 wave 2C) → `ClusterNavBridgeSystem.kt` (tách THUẦN
    // theo trần 500 dòng, L6-debt 2026-09-27; cùng khuôn `ClusterNavBridgeCast.kt` / `ClusterNavBridgeKeys.kt`).

    // Android box B2 · W2c — biển báo tốc độ + bong bóng VietMap trên cụm gỡ cùng mã (mục Cài đặt đã gỡ ở W1).

    // Android box B2 · W2e — tiện nghi xe (ghế mát/sưởi · lọc bụi PM2.5 · lấy gió trong) gỡ cùng mã HAL BYD (nhóm Cài đặt đã gỡ ở W1).

    // ── Hệ thống ─────────────────────────────────────────────────────────────────────────────────

    /** "Tự khởi động nền" — lặp lại `MainActivity.kt:218`. */
    fun headlessAutostart(): Boolean = Prefs.headlessAutostart(app)

    /** Lặp lại `MainActivity.kt:219`. */
    fun setHeadlessAutostart(on: Boolean) = Prefs.setHeadlessAutostart(app, on)

}
