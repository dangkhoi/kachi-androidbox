package com.kachi.box.launcher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import android.provider.Settings
import android.util.Log

/**
 * ═══ Android box B3 — phần chạm Android của [NoShellFallback] ═══════════════════════════════════════════════
 * Spec `androidbox-plan.html` §4.2. Chỉ ĐỌC trạng thái sẵn có (một lần `hasSystemFeature`) và mở màn hệ thống khi
 * người dùng BẤM — không nhịp, không luồng, không lệnh shell.
 */
object DeviceMic {
    /** `PackageManager.FEATURE_MICROPHONE` — phần cứng không đổi trong đời tiến trình ⇒ đọc một lần. `null` = đọc hỏng. */
    @Volatile private var cached: Boolean? = null
    @Volatile private var read = false

    fun feature(ctx: Context): Boolean? {
        if (read) return cached
        cached = runCatching { ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE) }.getOrNull()
        read = true
        return cached
    }

    /** Có bày giọng nói không ([NoShellFallback.voiceAvailable]: đọc hỏng ⇒ giữ). */
    fun voiceAvailable(ctx: Context): Boolean = NoShellFallback.voiceAvailable(feature(ctx))
}

object SystemSettingsOpener {

    private const val TAG = "KachiNoShell"

    /** Mã `requestPermissions` — `KachiHomeActivity.onRequestPermissionsResult` dựng lại trang quyền. */
    const val REQ_RUNTIME = 0x4B42

    /**
     * Mở đúng màn/hộp hệ thống cho [fix]. `false` = máy không có màn đó / bị từ chối ⇒ bên gọi NÓI THẬT.
     * HOME: bật alias trước (alias TẮT sẵn trong manifest ⇒ chưa bật thì Kachi không có trong danh sách chọn).
     */
    fun open(activity: Activity, fix: ManualFix): Boolean {
        val pkg = activity.packageName
        return when (fix) {
            ManualFix.RUNTIME_RECORD_AUDIO -> request(activity, arrayOf(android.Manifest.permission.RECORD_AUDIO))
            ManualFix.RUNTIME_LOCATION -> request(
                activity,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
            )
            ManualFix.ACCESSIBILITY_SETTINGS -> start(activity, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            ManualFix.NOTIFICATION_LISTENER_SETTINGS -> start(activity, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            ManualFix.OVERLAY_SETTINGS ->
                start(activity, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$pkg".toUri()))
            ManualFix.DEVELOPER_SETTINGS -> start(activity, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            ManualFix.HOME_SETTINGS -> openHomePicker(activity)
        }
    }

    /**
     * Tuỳ chọn nhà phát triển có đang bật không (`Settings.Global.DEVELOPMENT_SETTINGS_ENABLED`, mọi app đọc được).
     * [ĐO máy ảo kachi_box 09/10] tắt mà mở `ACTION_APPLICATION_DEVELOPMENT_SETTINGS` ⇒ hệ thống chuyển sang
     * `DevelopmentSettingsDisabledActivity` rồi đóng ngay — người dùng không thấy gì. `null` = đọc hỏng.
     */
    fun devOptionsOn(ctx: Context): Boolean? = runCatching {
        Settings.Global.getInt(ctx.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
    }.getOrNull()

    /** Màn "Giới thiệu máy" — nơi chạm 7 lần vào Số bản dựng để bật Tuỳ chọn nhà phát triển. */
    fun openDeviceInfo(activity: Activity): Boolean = start(activity, Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))

    /** Bật alias HOME + màn "Ứng dụng màn hình chính"; máy không có màn đó ⇒ ý-định HOME (hộp chọn của hệ thống). */
    fun openHomePicker(activity: Activity): Boolean {
        DefaultHome.enableHomeEntry(activity)
        return (start(activity, Intent(Settings.ACTION_HOME_SETTINGS)) ||
            start(activity, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)))
            .also { if (it) homePickerPending = true }
    }

    /**
     * `BOX-HOME-RESULT-LOST` — vừa mở màn chọn màn hình chính ⇒ lượt dựng trang *Hệ thống & quyền* KẾ TIẾP (lúc quay về)
     * nói kết quả. Dấu chỉ nói "có lượt chọn đang chờ", KHÔNG nói kết quả: kết quả đọc từ HOME thật lúc dựng lại
     * ([NoShellFallback.homePickerNote]). Lấy ra là hết (một lần) ⇒ không có câu cũ nào sống tiếp.
     */
    @Volatile private var homePickerPending = false

    fun takeHomePickerPending(): Boolean = homePickerPending.also { homePickerPending = false }

    private fun request(activity: Activity, perms: Array<String>): Boolean = runCatching {
        activity.requestPermissions(perms, REQ_RUNTIME)
        true
    }.onFailure { Log.w(TAG, "requestPermissions hỏng: ${it.javaClass.simpleName}") }.getOrDefault(false)

    private fun start(activity: Activity, intent: Intent): Boolean = try {
        activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Log.i(TAG, "mở ${intent.action}")
        true
    } catch (e: ActivityNotFoundException) {
        Log.w(TAG, "máy không có màn ${intent.action}")
        false
    } catch (e: SecurityException) {
        Log.w(TAG, "bị từ chối mở ${intent.action}: ${e.message}")
        false
    }
}
