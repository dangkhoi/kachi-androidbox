package com.byd.clusternav

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * HEADLESS boot setup (1.21 Item 1). Short-lived foreground service started by [RebindReceiver] on
 * BOOT_COMPLETED / MY_PACKAGE_REPLACED when "Tự khởi động nền" ([Prefs.headlessAutostart]) is ON, so the
 * app performs its boot setup WITHOUT foregrounding any screen (bonus: dodges the dudu size-compat
 * letterbox — no activity auto-foregrounds on boot).
 *
 * Android box B2 · W1 (2026-10-09): the boot setup is now just
 *   1. accessibility grant + force-bind ([NavConnect.grantAccessibility]) when the physical-key feature is ON,
 *   2. the voice-key keep-alive ([VoiceKeyKeepAliveService.sync]),
 *   3. the scheduled-navigation engine ([com.byd.clusternav.automation.AutomationService.sync]) and the Gemini
 *      assistant re-apply for a hold-mic binding.
 * Everything BYD-only that used to run here (cluster-lane / HUD outputs, speed sign, VietMap autostart, seat / PM2.5 /
 * recirculation apply, forced HUD prefs) is removed from this entry point (docs/specs/androidbox-plan.html §4.1).
 *
 * Safety:
 *  • [startForeground] is called FIRST (well within the ~5 s startForegroundService() budget) so a
 *    background start can never be killed with RemoteServiceException.
 *  • The setup runs on a background thread wrapped in runCatching → it can NEVER crash the process.
 *  • The service ALWAYS [finish]es (stopForeground + stopSelf) — the call sits OUTSIDE the runCatching, so
 *    an exception or interrupt still tears the service down.
 *  • The FGS (and therefore the process) is kept alive until the async dadb grant reports back (bounded by
 *    [GRANT_TIMEOUT_MS]) — the plan's rationale for a foreground service: the grant takes ~3–5 s, longer
 *    than a BroadcastReceiver's budget, so a pure-boot stopSelf must not kill it early.
 */
class BootSetupService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForegroundService() contract: go foreground within ~5 s or the system kills us. Do it FIRST,
        // before any (blocking) work; if the platform denies it, stop cleanly.
        if (!startForegroundOnce()) { stopSelf(startId); return START_NOT_STICKY }
        Thread({
            runCatching {
                // Accessibility grant + 1.20 force-bind: cần khi Nav+HUD (booster đọc GMaps) HOẶC voice-key
                // (nút vật lý → trợ lý) bật. Voice-key KHÔNG phụ thuộc Nav+HUD (owner 2026-09-01: hai tính năng
                // RIÊNG — trước gate chung Nav+HUD nên phím-thoại chết sau boot khi Nav+HUD tắt). Chỉ escalate khi
                // service CHƯA bound (idempotent: grantAccessibility verify dumpsys trước khi toggle → no-op/no
                // flicker nếu đã bound). Async trên thread riêng, báo về main looper → đếm latch; giữ FGS sống tới
                // khi grant xong (bounded GRANT_TIMEOUT_MS).
                // Android box B2 · W1: chỉ phím vật lý cần trợ năng (bộ đọc màn GMaps cho cụm là phần BYD đã gỡ khỏi
                // lối vào) ⇒ cổng chỉ còn công tắc phím, không còn công tắc "Dẫn đường lên cụm" (`Prefs.enabled`).
                if (Prefs.voiceKeyEnabled(applicationContext)) {
                    if (!NavConnect.isAccessibilityBound(applicationContext)) {
                        val latch = CountDownLatch(1)
                        NavConnect.grantAccessibility(applicationContext) { latch.countDown() }
                        latch.await(GRANT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    }
                }
                // #3 (deep-pass 2026-09-23): giữ tiến trình sống khi phím-thoại bật (ssc_skip không DROP watchdog).
                runCatching { VoiceKeyKeepAliveService.sync(applicationContext) }
                // Android box B2 · W1 — gỡ phần chỉ-BYD của lượt nổ máy: ép prefs HUD, đầu ra dẫn đường lên cụm/HUD,
                // biển tốc độ, tự mở VietMap, áp ghế / lọc bụi / lấy gió trong (docs/specs/androidbox-plan.html §4.1).
                // AUTOMATION (1.85, spec kachi-automation R4/R5) — dựng lại động cơ nền khi nổ máy nếu có
                // automation nào BẬT. Phải có mặt ở CẢ HAI đường boot: `KachiAutostart` chỉ chạy khi *"Tự mở
                // Kachi"* (`launcher_autostart`) BẬT, còn dịch vụ này chạy theo *"Chạy dịch vụ nền"*
                // (`headless_autostart`) — hai công tắc RIÊNG. Gác automation sau một công tắc không liên quan là
                // đúng bẫy đã ăn ở P7 (cảnh khởi động chết theo `launcher_autostart`, im lặng). `sync` idempotent
                // + tự dừng khi hết việc ⇒ gọi ở hai chỗ không nhân đôi gì.
                com.byd.clusternav.automation.AutomationService.sync(applicationContext)
                // F4e boot (owner 08-25): boot headless KHÔNG mở màn nào ⇒ trợ lý
                // hệ thống chưa được đặt = Gemini ⇒ hold-mic → keyevent 231 route sai. Đặt luôn ở đây NẾU có
                // binding Gemini, để hold-mic → Gemini ready NGAY sau nổ máy mà KHÔNG cần mở app (owner
                // 08-25: "kể cả khởi động nền hay full app đều enable service gemini lên là OK").
                // retry NONE: boot owner KHÔNG ở màn hình để bấm "Cho phép gỡ lỗi USB" ⇒ MỘT lần, không chờ
                // ~31s (tránh treo boot — F6). Hỏng (chưa cấp quyền) ⇒ bỏ; owner bấm *Cài đặt › Phím vô-lăng ›
                // Kiểm tra / Sửa ngay* thì `ClusterNavBridge.checkFix` re-apply với AWAIT_ADB_APPROVAL (đường này
                // trước 2026-09-13 nằm ở `MainActivity.onCreate`; nay là một VIỆC do người dùng bấm, đúng lúc họ
                // đang ở trước màn xe để bấm "Cho phép gỡ lỗi USB").
                if (com.byd.clusternav.modules.voicekey.AssistantLauncher.hasGeminiBinding(applicationContext)) {
                    val err = com.byd.clusternav.modules.voicekey.AssistantLauncher.setSystemAssistant(
                        applicationContext, com.byd.clusternav.carexec.LocalShellRetry.NONE,
                    )
                    if (err.isNotEmpty()) Log.i(TAG, "boot re-apply Gemini assistant: $err (owner mở app sẽ thử lại có chờ cấp quyền)")
                }
            }.onFailure { Log.e(TAG, "headless boot setup failed", it) }
            finish(startId)
        }, "boot-setup").start()
        return START_NOT_STICKY
    }

    /** ALWAYS the last step: leave the foreground state + stop the service. Safe to call once per start. */
    private fun finish(startId: Int) {
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            .onFailure { Log.w(TAG, "stopForeground failed", it) }
        stopSelf(startId)
    }

    private fun startForegroundOnce(): Boolean = runCatching {
        startForeground(NOTIFICATION_ID, notification())
        true
    }.getOrElse {
        Log.e(TAG, "startForeground denied", it)
        false
    }

    private fun notification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= 26) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Kachi khởi động", NotificationManager.IMPORTANCE_MIN),
            )
        }
        @Suppress("DEPRECATION")
        return android.app.Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Kachi")
            .setContentText(Lang.t("Đang khởi động nền…", "Starting in background…"))
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "BootSetup"
        // Distinct from the cast bubble service (1042) / CastAutomationService so the two can coexist on boot.
        private const val NOTIFICATION_ID = 1043
        private const val CHANNEL_ID = "clusternav_boot_setup"
        // Upper bound on how long the FGS lingers waiting for the async dadb accessibility grant to report
        // back (settle 1.2 s + toggle 0.8 s + dumpsys/dadb round-trips ≈ 3–6 s). Bounded so we ALWAYS stop.
        private const val GRANT_TIMEOUT_MS = 8_000L
    }
}
