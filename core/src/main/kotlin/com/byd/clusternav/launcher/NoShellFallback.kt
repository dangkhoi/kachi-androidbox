package com.byd.clusternav.launcher

import com.byd.clusternav.carexec.ShellChannelPhase

/**
 * ═══ Android box B3 — KHI KHÔNG CÓ KÊNH SHELL (spec `androidbox-plan.html` §4.2) ══════════════════════════════
 *
 * Phần QUYẾT ĐỊNH, thuần Kotlin (`:core`, cấm `android.*`). Việc đọc Android + mở màn hệ thống nằm ở `:app`
 * (`SystemSettingsOpener`, `PermissionPreflight`, `UpdateFlow`).
 *
 * ## Vì sao cần (khác bản BYD)
 * Kachi BYD tự cấp mọi quyền qua kênh adb loopback (`dadb localhost:5555`) vì màn Cài đặt hệ thống của ROM BYD BỊ KHOÁ
 * ([ĐO] *"Hệ thống IVI không hỗ trợ hoạt động này"*). Trên Android box thường, adb mạng có thể KHÔNG có [CHƯA BIẾT —
 * tuỳ máy], nhưng màn Cài đặt hệ thống (Trợ năng · Truy cập thông báo · Hiển thị trên ứng dụng khác · hộp xin quyền
 * micro · Ứng dụng màn hình chính) là API Android CHUẨN ⇒ đường tự cấp bằng tay. Không rẽ nhánh theo hãng
 * (CLAUDE.md §7): chỉ hỏi *kênh có dùng được không* và *máy có micro không* — trạng thái đọc sẵn, không nhịp mới.
 */

/** Màn hệ thống / hộp xin quyền mở được để người dùng tự cấp một điều kiện. */
enum class ManualFix {
    /** `Settings.ACTION_ACCESSIBILITY_SETTINGS`. */
    ACCESSIBILITY_SETTINGS,

    /** `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`. */
    NOTIFICATION_LISTENER_SETTINGS,

    /** `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` + `package:<gói>`. */
    OVERLAY_SETTINGS,

    /** `Activity.requestPermissions(RECORD_AUDIO)` — hộp xin quyền runtime của hệ thống. */
    RUNTIME_RECORD_AUDIO,

    /** `Activity.requestPermissions(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)`. */
    RUNTIME_LOCATION,

    /** Bật alias HOME rồi `Settings.ACTION_HOME_SETTINGS`; không có ⇒ ý-định HOME (hộp chọn của hệ thống). */
    HOME_SETTINGS,

    /** `Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS` — nơi máy có thể bật gỡ lỗi (qua mạng) cho kênh shell. */
    DEVELOPER_SETTINGS,
}

/** Đường đặt Kachi làm màn hình chính. */
enum class HomeRoute {
    /** Kênh dùng được ⇒ `cmd package set-home-activity` qua dadb (đường cũ, có đọc lại xác nhận). */
    SHELL,

    /** adbd đang hỏi / vừa thu hồi duyệt ⇒ thẻ xin quyền như cũ (người dùng bấm Cho phép là xong). */
    PROMPT,

    /** Không có kênh ⇒ bật alias HOME + màn chọn màn hình chính của hệ thống. */
    SYSTEM_PICKER,
}

/** Đường cài bản OTA. */
enum class OtaRoute {
    /** `pm install -r` qua dadb (đường cũ). */
    SHELL,

    /** Trình cài app của hệ thống (`ACTION_VIEW` + `FileProvider`) — người dùng bấm Cài. */
    SYSTEM_INSTALLER,
}

object NoShellFallback {

    /**
     * Mục [req] đang thiếu thì người dùng tự sửa ở đâu — `null` = không có màn nào (hoặc Kachi tự cấp được).
     *
     * [shellUsable] `true` ⇒ `null` cho mọi mục: Kachi tự cấp qua kênh ở vòng kiểm (hành vi cũ, không thêm nút).
     * `false`/`null` (không có / chưa biết) ⇒ chỉ đường tay. [FREEFORM][LauncherRequirements.FREEFORM] không có màn
     * chuẩn (`enable_freeform_support` là cờ Global, chỉ shell ghi được) ⇒ `null`.
     */
    fun manualFix(req: LauncherRequirement, shellUsable: Boolean?): ManualFix? {
        if (shellUsable == true) return null
        return when (req.id) {
            LauncherRequirements.ACCESSIBILITY.id -> ManualFix.ACCESSIBILITY_SETTINGS
            LauncherRequirements.NOTIFICATION_LISTENER.id -> ManualFix.NOTIFICATION_LISTENER_SETTINGS
            LauncherRequirements.OVERLAY.id -> ManualFix.OVERLAY_SETTINGS
            LauncherRequirements.MICROPHONE.id -> ManualFix.RUNTIME_RECORD_AUDIO
            LauncherRequirements.LOCATION.id -> ManualFix.RUNTIME_LOCATION
            LauncherRequirements.DEFAULT_HOME.id -> ManualFix.HOME_SETTINGS
            LauncherRequirements.SHELL_CHANNEL.id -> ManualFix.DEVELOPER_SETTINGS
            else -> null
        }
    }

    /**
     * Máy có micro không ⇒ có bày giọng nói không. `null` (không đọc được) ⇒ GIỮ (không ẩn một tính năng chỉ vì một
     * lượt đọc hỏng — cùng luật [RequirementState.UNKNOWN]).
     */
    fun voiceAvailable(hasMicFeature: Boolean?): Boolean = hasMicFeature != false

    /** Điều kiện KHÔNG áp dụng cho máy này (bỏ khỏi vòng kiểm, không phải "thiếu"): không micro ⇒ quyền micro vô nghĩa. */
    fun notApplicable(hasMicFeature: Boolean?): Set<String> =
        if (voiceAvailable(hasMicFeature)) emptySet() else setOf(LauncherRequirements.MICROPHONE.id)

    /** Đường đặt HOME theo trạng thái kênh đã đo ([usable] = `ShellReadinessPolicy.usable`). */
    fun homeRoute(usable: Boolean, phase: ShellChannelPhase): HomeRoute = when {
        usable -> HomeRoute.SHELL
        phase == ShellChannelPhase.NEEDS_APPROVAL -> HomeRoute.PROMPT
        else -> HomeRoute.SYSTEM_PICKER
    }

    /**
     * Đường cài OTA. Kênh dùng được ⇒ dadb. Kênh còn đang dò (STARTING/CHECKING) ⇒ vẫn dadb (cổng thi hành tự CHỜ —
     * chưa "thử xong" thì chưa kết luận là không có). Còn lại (đã đo không có / cần duyệt / chưa biết sau F4) ⇒ trình
     * cài hệ thống.
     */
    fun otaRoute(usable: Boolean, phase: ShellChannelPhase): OtaRoute = when {
        usable -> OtaRoute.SHELL
        phase == ShellChannelPhase.STARTING || phase == ShellChannelPhase.CHECKING -> OtaRoute.SHELL
        else -> OtaRoute.SYSTEM_INSTALLER
    }

    /**
     * Trước khi đưa APK cho trình cài hệ thống: gói trong APK phải ĐÚNG gói đang chạy. Đường dadb `pm install -r` cũng
     * không kiểm, nhưng ở đường hệ thống người dùng chỉ thấy "Cài" — lỡ kênh OTA có APK của gói khác thì đó là cài một
     * app MỚI chứ không phải cập nhật. Khác chữ ký cùng gói thì trình cài tự từ chối.
     */
    fun archiveMatches(archivePackage: String?, ownPackage: String): Boolean =
        !archivePackage.isNullOrBlank() && archivePackage == ownPackage

    /**
     * Review Pass 2 [P2] — người ký của APK ([archiveSigners], SHA-256) có trùng người ký của bản đang cài ([ownSigners],
     * gồm cả lịch sử xoay khoá) không. Một bên RỖNG (đọc không ra) ⇒ `true`: để trình cài hệ thống quyết (nó vẫn từ chối
     * khác chữ ký) — không chặn bản cập nhật hợp lệ vì một lượt đọc hỏng. Chỉ chặn khi CHẮC CHẮN khác.
     */
    fun signersMatch(archiveSigners: Set<String>, ownSigners: Set<String>): Boolean =
        archiveSigners.isEmpty() || ownSigners.isEmpty() || archiveSigners.any { it in ownSigners }

    /** Ô app chưa có bộ chiếu: thẻ xin quyền có nút "Mở toàn màn hình" khi kênh đã ĐO là không dùng được. */
    fun offerFullscreen(phase: ShellChannelPhase): Boolean =
        phase == ShellChannelPhase.ENVIRONMENT || phase == ShellChannelPhase.NEEDS_APPROVAL
}
