package com.byd.clusternav.launcher

/**
 * ═══ Android box B2 · W1 (2026-10-09) — khoá lưu bền ĐÃ RỜI giao diện nhưng dữ liệu còn nguyên phạm vi ═══════════════
 *
 * Đợt W1 (`docs/diagnostics/androidbox-b2-inventory-2026-10-09.md` §3) cắt LỐI VÀO của phần chỉ-BYD: mục Cài đặt của dẫn
 * đường lên cụm/HUD, biển báo tốc độ, bong bóng VietMap, cả nhóm Chiếu màn lên cụm và Tiện nghi xe, chip thanh trạng thái,
 * đơn vị hiển thị. Mã runtime + khoá lưu bền còn nằm đó tới W2–W4; riêng [SettingsCatalog.CLUSTERNAV_KEYS] và `ProfileScope`
 * GIỮ NGUYÊN ở đợt này để một lượt đổi/xuất/nhập hồ sơ không làm mất dữ liệu của người dùng đang có.
 *
 * Bảng này là chỗ để hai phép kiểm của danh mục ([SettingsCatalog.orphans] · khoá ClusterNav "không chủ") phân biệt
 * *"quên gom"* với *"cố ý đã rời UI"* — mỗi khoá kèm lý do, giống [SettingsCatalog.NOT_SETTINGS]. Bất biến (chốt ở
 * `init` của [SettingsCatalog] + bài `SettingsCatalogRetiredTest`): khoá ở đây KHÔNG có mục nào sở hữu, không nằm trong
 * [SettingsCatalog.NOT_SETTINGS], và mọi khoá phía ClusterNav vẫn nằm trong [SettingsCatalog.CLUSTERNAV_KEYS] (tức `ProfileScope`
 * còn xếp được — phần lớn theo hồ sơ, bộ ba tự sấy kính theo xe như trước). W4 dọn khoá chết thì xoá dòng tương ứng ở đây cùng lượt.
 */
internal object SettingsCatalogRetired {

    private const val NAV_HUD = "dẫn đường lên cụm/HUD BYD — Android box B2 · W1 gỡ mục Cài đặt; khoá còn theo hồ sơ tới W4"
    private const val BADGE = "biển báo tốc độ trên cụm BYD — Android box B2 · W1 gỡ mục Cài đặt; khoá còn theo hồ sơ tới W4"
    private const val VM_BUBBLE = "bong bóng VietMap trên cụm BYD — Android box B2 · W1 gỡ mục Cài đặt; khoá còn theo hồ sơ tới W4"
    private const val CAST = "chiếu màn lên cụm BYD (nhóm Cài đặt gỡ ở Android box B2 · W1); khoá `simple_cast_prefs` còn theo hồ sơ tới W4"
    private const val CAR = "tiện nghi xe qua HAL BYD (nhóm Cài đặt gỡ ở Android box B2 · W1); khoá giữ phạm vi cũ (hồ sơ / xe) tới W4"
    private const val STRIP = "chip thanh trạng thái = chip dữ liệu xe BYD — Android box B2 · W1 gỡ mục Cài đặt; W3 gỡ khối chip"
    private const val UNITS = "đơn vị chỉ dùng cho dữ liệu xe BYD — Android box B2 · W1 gỡ mục Cài đặt; W3 gỡ cùng dữ liệu xe"

    /** Khoá → lý do. Phía ClusterNav (tệp ở [SettingsCatalog.CLUSTERNAV_KEYS]) và phía launcher (`kachi_workspace`). */
    val KEYS: Map<String, String> = buildMap {
        listOf("enabled", "nav_cluster_screen_mode", "marquee").forEach { put(it, NAV_HUD) }
        listOf(
            "badge_enabled", "show_upcoming_badge", "show_alert_chip", "badge_size_dp", "badge_center_x", "badge_center_y",
        ).forEach { put(it, BADGE) }
        listOf("vm_bubble_enabled", "vm_bubble_hidden", "vm_bubble_x", "vm_bubble_y").forEach { put(it, VM_BUBBLE) }
        listOf(
            "cast_enabled", "split_ratio_left_pct", "cast_bubble_visible", "cast_style",
            "autostart_enabled", "autostart_package", "autostart_split_enabled",
            "autostart_left_package", "autostart_right_package",
        ).forEach { put(it, CAST) }
        listOf(
            "recirc_on_start_enabled", "seat_comfort_enabled", "seat_comfort_mode",
            "seat_level_0", "seat_level_1", "seat_level_2", "seat_level_3", "pm25_filter_enabled",
            "rain_defrost_enabled", "rain_defrost_front", "rain_defrost_rear",
        ).forEach { put(it, CAR) }
        put("top_strip", STRIP)
        put("top_strip_labels", STRIP)
        put("unit_prefs", UNITS)
    }

    /** Khoá phía LAUNCHER trong [KEYS] (tệp `kachi_workspace`, tiền tố hồ sơ) — phần còn lại phải nằm ở `CLUSTERNAV_KEYS`. */
    val LAUNCHER_KEYS: Set<String> = setOf("top_strip", "top_strip_labels", "unit_prefs")
}
