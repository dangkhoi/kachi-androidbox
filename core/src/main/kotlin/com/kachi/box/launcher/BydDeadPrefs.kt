package com.kachi.box.launcher

/**
 * ═══ Android box B2 · W4 (2026-10-09) — khoá prefs CHẾT của Kachi BYD, chỉ để DỌN một lần ═══════════════════════════════
 *
 * Thuần Kotlin (`:core`) ⇒ kiểm off-device (`BydDeadPrefsTest`). Đợt W1–W3 gỡ mã chiếu cụm · camera · VietMap · biển
 * tốc độ · dẫn đường cụm/HUD · tiện nghi xe · tự sấy kính · chip dữ liệu xe · đơn vị · kiểm-từng-nút; các khoá của chúng tạm ở
 * bảng `Retired*Keys` để [ProfileScope] còn xếp loại. W4 gỡ hẳn mấy bảng đó: khoá chết KHÔNG còn trong [ProfileScope] (không
 * theo hồ sơ, không theo xe, không vào ảnh chụp, không vào bản chia sẻ). Bảng này KHÔNG phải một phạm vi — nó chỉ trả lời
 * *"lượt dọn một lần được xoá gì"*.
 *
 * Ba nơi khoá chết còn nằm sau khi cài đè lên một máy từng chạy Kachi BYD (cùng `applicationId` thì mới có — Android box mới
 * cài sạch thì lượt dọn chỉ đặt dấu):
 *  1. **tệp prefs của tính năng đã gỡ** ([DEAD_FILES]) — xoá cả tệp;
 *  2. **khoá trong `clusternav_prefs`** ([isDeadClusterNavKey]) — sống cạnh khoá còn dùng (phím · giọng nói · lịch) nên xoá theo TÊN;
 *  3. **hậu tố hồ sơ trong `kachi_workspace`** ([isDeadLauncherKey]) + khoá chết bên trong ảnh chụp `__cn__clusternav_prefs`
 *     ([cleanSnapshot]).
 *
 * Xoá khoá chết là VÔ HẠI nếu chạy lại ⇒ dấu [MARK] ghi SAU cùng (chết máy giữa chừng thì lần sau dọn lại, không mất gì).
 * Tệp `.kachi` nhập từ Kachi BYD mang các khoá này: lượt nhập bỏ IM LẶNG (hậu tố ngoài [ProfileScope.LAUNCHER_SUFFIXES] ·
 * khoá ảnh chụp ngoài [ProfileScope.CLUSTERNAV_KEYS]) — không đi qua bảng này.
 */
object BydDeadPrefs {

    /** Dấu "đã dọn" — tệp `kachi_workspace`, theo XE ([ProfileScope.DEVICE_KEYS] + [SettingsCatalog.NOT_SETTINGS]). */
    const val MARK = "migrated_androidbox_v1"

    /** Tệp chính phía ClusterNav — nơi khoá chết sống lẫn khoá còn dùng. */
    const val CLUSTERNAV_FILE = "clusternav_prefs"

    /**
     * Tệp prefs chỉ của tính năng BYD đã gỡ ⇒ xoá CẢ TỆP. Tên đọc từ mã đã gỡ (lịch sử git, `ee2e34a~1`): chiếu cụm
     * (`simple_cast_prefs` · `cast-v2-app-catalog` · `clustercast` = `ClusterProfile.PREF` + sổ theme + memo bóng mod) ·
     * phiên dẫn đường cụm (`navigation_session_v2`) · widget VietMap (`vietmap_widget_bridge`) · dấu tiền đề VietMap
     * (`app_prereq_marks`) · kiểm-từng-nút (`kachi_captest`) · sổ dọn hình học chiếu cụm (`kachi_housekeeping`).
     */
    val DEAD_FILES: List<String> = listOf(
        "simple_cast_prefs", "cast-v2-app-catalog", "clustercast", "navigation_session_v2",
        "vietmap_widget_bridge", "app_prereq_marks", "kachi_captest", "kachi_housekeeping",
    )

    /** Khoá chết theo TÊN trong [CLUSTERNAV_FILE]. */
    private val CLUSTERNAV_DEAD_KEYS: Set<String> = setOf(
        // dẫn đường lên cụm/HUD + sáu khoá ép-giá-trị + nguồn + log chẩn đoán GMaps
        "enabled", "nav_cluster_screen_mode", "marquee", "source_mode", "lane", "interpolate", "hud", "acc_booster",
        "anim_opt", "bubble_auto", "nav_verbose_log",
        // biển báo tốc độ + bong bóng VietMap + dấu miễn pin/vẽ nổi VietMap
        "show_upcoming_badge", "show_alert_chip", "vm_float_whitelist_applied", "doze_whitelist_applied",
        // tiện nghi xe (ghế · lọc bụi · lấy gió)
        "seat_comfort_enabled", "seat_comfort_mode", "pm25_filter_enabled", "recirc_on_start_enabled",
    )

    /**
     * Họ khoá chết theo TIỀN TỐ trong [CLUSTERNAV_FILE]: `camera_` (≈ 55 khoá camera xi-nhan / từng camera 2.93), `badge_` ·
     * `vm_bubble_` (biển báo · bóng VietMap), `seat_level_` (mức từng ghế), `rain_defrost_` (tự sấy kính), `mod_` (công tắc
     * module đời ClusterNav, khoá băm tên). [ĐO grep 09/10] không khoá còn dùng nào của tệp này mang các tiền tố ấy.
     */
    private val CLUSTERNAV_DEAD_PREFIXES: List<String> =
        listOf("camera_", "badge_", "vm_bubble_", "seat_level_", "rain_defrost_", "mod_")

    /** Hậu tố hồ sơ chết trong `kachi_workspace` (chip thanh trên · đơn vị · ảnh chụp hai tệp chiếu cụm). */
    private val LAUNCHER_DEAD_SUFFIXES: List<String> = listOf(
        "top_strip", "top_strip_labels", "top_strip_migrated_ux5b", "unit_prefs",
        ProfileScope.snapshotSuffix("simple_cast_prefs"), ProfileScope.snapshotSuffix("cast-v2-app-catalog"),
    )

    /** Khoá [key] của [CLUSTERNAV_FILE] (cũng là khoá BÊN TRONG ảnh chụp tệp ấy) có phải khoá chết không. */
    fun isDeadClusterNavKey(key: String): Boolean =
        key in CLUSTERNAV_DEAD_KEYS || CLUSTERNAV_DEAD_PREFIXES.any { key.startsWith(it) }

    /**
     * Khoá [key] của `kachi_workspace` có phải `<hồ sơ>__<hậu tố chết>` không. So ĐUÔI (không cần danh sách hồ sơ) ⇒ dọn
     * được cả khoá mồ côi của hồ sơ đã xoá; [ĐO] không hậu tố còn dùng nào kết thúc bằng `__<hậu tố chết>` (bài canh).
     */
    fun isDeadLauncherKey(key: String): Boolean = LAUNCHER_DEAD_SUFFIXES.any { key.endsWith("__$it") }

    /** Ảnh chụp đã bỏ khoá chết; `null` = không có gì để bỏ (chỗ gọi khỏi ghi lại, giữ nguyên byte). */
    fun cleanSnapshot(shot: Map<String, Any?>): Map<String, Any?>? {
        val kept = shot.filterKeys { !isDeadClusterNavKey(it) }
        return if (kept.size == shot.size) null else kept
    }

    /** Mục `tệp/khoá` của sổ đã-rót ([ProfileScopeMigration.FILLED_LEDGER_KEY]) trỏ khoá chết ⇒ bỏ khỏi sổ. */
    fun isDeadLedgerEntry(entry: String): Boolean =
        entry.substringBefore('/') == CLUSTERNAV_FILE && isDeadClusterNavKey(entry.substringAfter('/'))

    /** Mã nút / gói lệnh xe trong tập hỏi lại `voice_confirm_ids` đã lưu ≤ 2.98 (`control:<nút>` · `macro:<gói>`). */
    private val RETIRED_CONFIRM_PREFIXES = listOf("control:", "macro:")

    /** Tập hỏi lại đã bỏ mã nút / gói lệnh xe; `null` = không có gì để bỏ. */
    fun cleanConfirmIds(ids: Set<String>): Set<String>? {
        val kept = ids.filterTo(LinkedHashSet()) { id -> RETIRED_CONFIRM_PREFIXES.none { id.startsWith(it) } }
        return if (kept.size == ids.size) null else kept
    }
}
