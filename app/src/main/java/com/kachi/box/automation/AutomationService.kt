package com.kachi.box.automation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.kachi.box.Lang
import com.kachi.box.Prefs
import com.kachi.box.R
import com.kachi.box.launcher.automation.NavAutomationBook
import com.kachi.box.navAutomationRules

/**
 * ═══ ĐỘNG CƠ NỀN CỦA LỊCH TỰ DẪN ĐƯỜNG ═══════════════════════════════════════════════════════════════════
 *
 * Spec `docs/specs/kachi-automation.html` R4. Foreground service, nhịp [TICK_MS]; mỗi nhịp gọi
 * [ScheduledNavApplier.tick]. Android box B2 · W1 (2026-10-09): nhịp mưa và đồng bộ camera theo xi-nhan gỡ khỏi vòng
 * (HAL BYD); W2e xoá mã tự sấy kính — engine chỉ còn lịch tự dẫn đường.
 *
 * ## Vì sao FGS + `Thread.sleep`, KHÔNG WorkManager / AlarmManager
 * Spec §Quyết định thiết kế: Kachi vốn **thường trú** (nó là HOME, autostart mỗi lần nổ máy), IVI khoá nhiều
 * đường, và nhịp-trong-FGS là công thức **đã chạy thật** trên xe (vòng poll lọc bụi đời BYD, nay đã gỡ).
 * WorkManager có min-interval 15 phút — quá thô cho một khung giờ 7–9h cần độ phân giải một phút.
 *
 * ## Guard vòng: [running] + [generation] — copy đúng cơ chế đã proven
 * [running] = *"đang có vòng của THẾ HỆ hiện tại"* (chặn khởi trùng khi [sync] gọi nhiều lần: boot + autostart +
 * cú gạt công tắc có thể tới trong cùng một giây). [generation] = **danh tính** vòng; một thread CHỈ sống khi
 * `myGen == generation`. Nhờ token thế hệ, chuỗi bật→tắt→bật (tắt lúc thread đang NGỦ [TICK_MS] rồi bật lại)
 * KHÔNG để thread cũ sống cạnh thread mới, và `finally` của thread cũ KHÔNG xoá cờ của thread mới — đúng bài học
 * vòng poll lọc bụi đời BYD đã ghi (chỉ `@Volatile var running` thì thread cũ đọc thấy cờ thread mới vừa bật ⇒ 2 vòng).
 *
 * ## Tự tắt khi không còn việc
 * [anyEnabled] `false` ⇒ service `stopSelf()`. Một FGS thường trú với một thông báo `IMPORTANCE_MIN` mà **không
 * làm gì** là chi phí ròng: nó giữ tiến trình, giữ một dòng trong danh sách thông báo, và làm người đọc log tin
 * rằng automation đang chạy. Công tắc bật lại ⇒ [sync] khởi lại từ đầu.
 */
class AutomationService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Hợp đồng `startForegroundService()`: lên foreground trong ~5 s hoặc bị giết. Làm TRƯỚC mọi việc khác
        // (cùng khuôn `BootSetupService`); nền tảng từ chối ⇒ dừng sạch thay vì chết bằng RemoteServiceException.
        if (!startForegroundOnce()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (!anyEnabled(applicationContext)) {
            Log.i(TAG, "không automation nào bật — dừng service")
            finish()
            return START_NOT_STICKY
        }
        startLoop(applicationContext)
        // START_STICKY: engine này là một nhịp **dài hạn** (cả chuyến xe), khác `BootSetupService` (việc một lần).
        // Hệ thống thu hồi vì thiếu RAM thì dựng lại là hành vi đúng; `onStartCommand` idempotent nhờ [running].
        return START_STICKY
    }

    override fun onDestroy() {
        // Vô hiệu hoá vòng đang chạy: thread sẽ thoát ở lần kiểm thế hệ kế tiếp thay vì sống lâu hơn service
        // (đúng họ lỗi "đường sống lâu hơn thứ nó phục vụ" — ba họ lỗi lặp lại của repo, §5 project-context).
        synchronized(GUARD) {
            generation++
            running = false
        }
        super.onDestroy()
    }

    private fun finish() {
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    private fun startForegroundOnce(): Boolean = runCatching {
        startForeground(NOTIFICATION_ID, notification())
        true
    }.getOrElse {
        Log.e(TAG, "startForeground bị từ chối", it)
        false
    }

    private fun notification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= 26) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                // IMPORTANCE_MIN: đây là một nhịp nền im lặng, không phải một tin cần người lái đọc.
                NotificationChannel(CHANNEL_ID, "Kachi automation", NotificationManager.IMPORTANCE_MIN),
            )
        }
        @Suppress("DEPRECATION")
        return android.app.Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Kachi")
            .setContentText(Lang.t("Tự động hoá đang chạy", "Automation running"))
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "KachiAutomation"

        /**
         * Nhịp chính. Luật dẫn-theo-lịch cần độ phân giải một phút (R2.3: *"kiểm mỗi ~1 phút"*).
         *
         * BG-13 (2026-09-25): KHÔNG còn nhịp 250 ms khi camera bật. Sự kiện xi-nhan đến qua socket theo thời gian
         * thật; HOLD hết hạn nay được `CameraSignalController` hẹn bằng `postDelayed` đúng mốc (`CameraHold`).
         * Vòng này chỉ còn đồng bộ công tắc mỗi phút (và [sync] gọi thẳng khi công tắc đổi).
         */
        const val TICK_MS = 60_000L

        /** Thông báo riêng, KHÔNG dùng lại id của `BootSetupService` (1043) / cast bubble (1042) — ba FGS cùng sống. */
        private const val NOTIFICATION_ID = 1044
        private const val CHANNEL_ID = "kachi_automation"

        /** Khoá của cặp [running]/[generation] — hai cờ phải đổi cùng nhau dưới cùng một khoá. */
        private val GUARD = Any()

        @Volatile private var running = false
        @Volatile private var generation = 0

        /**
         * Có automation nào đang bật không — **cổng duy nhất** trả lời câu đó.
         *
         * Luật dẫn đường phải **có ít nhất một luật đang bật**, không chỉ *"chuỗi luật không rỗng"*: một sổ toàn
         * luật đã tắt thì engine chỉ ngủ rồi dậy vô ích suốt chuyến.
         */
        fun anyEnabled(ctx: Context): Boolean {
            val app = ctx.applicationContext
            // Android box B2 · W1 — vòng chỉ còn LỊCH TỰ DẪN ĐƯỜNG: tự sấy kính khi mưa (HAL BYD) và camera theo xi-nhan (HAL
            // helper BYD) không còn là lý do giữ FGS — cả hai đã rời Cài đặt nên người dùng không còn công tắc nào để tắt chúng.
            return runCatching {
                NavAutomationBook.decode(Prefs.navAutomationRules(app)).any { it.enabled }
            }.getOrDefault(false)
        }

        /**
         * Đồng bộ service với công tắc — **gọi sau MỌI lượt đổi cấu hình** (sửa/xoá/bật/tắt luật) và ở
         * đường khởi động.
         *
         * Idempotent: đang chạy mà còn việc ⇒ no-op (guard [running]); hết việc ⇒ service tự `stopSelf` ở
         * [onStartCommand]. Degrade-safe: `startForegroundService` bị nền tảng chặn (hiếm, ROM lạ) ⇒ log rồi bỏ,
         * KHÔNG ném vào luồng vẽ của người đang bấm công tắc.
         */
        fun sync(ctx: Context) {
            val app = ctx.applicationContext
            runCatching {
                if (!anyEnabled(app)) {
                    // TẮT: vô hiệu vòng NGAY (không chờ service chết) rồi mới xin dừng. Thiếu bước này thì thread
                    // đang ngủ còn chạy thêm một nhịp và có thể mở app dẫn đường sau khi người dùng đã tắt luật.
                    synchronized(GUARD) {
                        generation++
                        running = false
                    }
                    app.stopService(Intent(app, AutomationService::class.java))
                    Log.i(TAG, "sync: không còn automation nào bật ⇒ dừng")
                    return
                }
                val intent = Intent(app, AutomationService::class.java)
                if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(intent) else app.startService(intent)
                Log.i(TAG, "sync: có automation bật ⇒ đảm bảo engine đang chạy")
            }.onFailure { Log.w(TAG, "sync thất bại (degrade-safe, thử lại lần sau)", it) }
        }

        /**
         * Khởi vòng nhịp nếu chưa có vòng nào của thế hệ hiện tại.
         *
         * Thread daemon: nó **không** được giữ tiến trình sống lâu hơn cần thiết; FGS mới là thứ giữ tiến trình,
         * và khi FGS chết thì [onDestroy] đã ++thế hệ nên vòng tự thoát.
         */
        private fun startLoop(app: Context) {
            val myGen: Int
            synchronized(GUARD) {
                if (running) return
                running = true
                myGen = ++generation
            }
            Thread({
                var ticks = 0
                var lastNavMs = 0L
                try {
                    // Nhịp ĐẦU chạy ngay (không ngủ trước): bật công tắc lúc 7h05 mà phải chờ tới 7h06 mới đánh
                    // giá là một phút không giải thích được với người vừa bấm.
                    while (myGen == generation && anyEnabled(app)) {
                        val nowMs = android.os.SystemClock.elapsedRealtime()
                        // Nav theo THỜI GIAN TRÔI (giữ mốc elapsed — không phụ thuộc số nhịp). Android box B2: nhịp mưa
                        // và đồng bộ camera xi-nhan gỡ khỏi vòng (W1), mã tự sấy kính xoá ở W2e.
                        if (nowMs - lastNavMs >= TICK_MS) {
                            lastNavMs = nowMs
                            runCatching { ScheduledNavApplier.tick(app) }.onFailure { Log.w(TAG, "tick nav lỗi", it) }
                        }
                        ticks++
                        runCatching { Thread.sleep(TICK_MS) }
                        // Kiểm LẠI sau khi ngủ: công tắc có thể đã tắt trong lúc đó. KHÔNG đọc [running] ở đây —
                        // một thread của thế hệ khác có thể vừa bật lại cờ ấy; danh tính thế hệ mới là điều kiện đúng.
                        if (myGen != generation) break
                    }
                } finally {
                    // CHỈ thế hệ hiện tại được nhả cờ — tránh `finally` của thread cũ xoá cờ của thread mới.
                    synchronized(GUARD) { if (myGen == generation) running = false }
                    Log.i(TAG, "vòng automation (gen $myGen) kết thúc sau $ticks nhịp")
                }
            }, "kachi-automation").apply { isDaemon = true }.start()
        }
    }
}
