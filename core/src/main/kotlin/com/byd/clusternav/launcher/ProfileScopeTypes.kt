package com.byd.clusternav.launcher

/**
 * Kiểu KHAI SẴN của khoá ClusterNav theo hồ sơ mà tầng này biết chắc (đọc từ chỗ `get*`/`put*` thật). Lượt áp/nhập bỏ
 * giá trị sai kiểu — kể cả khi tệp sống đang VẮNG khoá (ca mà phép so với kiểu sống không bắt được).
 *
 * Android box B2 · W2c: tách từ `ProfileScopeCluster.DECLARED_TYPES` (tệp đó gỡ cùng chiếu cụm); bỏ các khoá của
 * `simple_cast_prefs` / `cast-v2-app-catalog` (hai tệp đã rời ảnh chụp). Khoá camera lấy ở [RetiredCameraKeys.TYPES].
 *
 * Ca đắt nhất (senior review V-CLUSTER Pass 2): `voicekey_bindings` đọc bằng `getString` mỗi lần bấm phím vô-lăng — tệp
 * nhập đặt nó thành Boolean trên máy chưa từng gán phím (tệp sống VẮNG khoá) ⇒ dịch vụ phím nổ. Bài canh `:app`
 * `ClusterProfileScopeCoverageTest` đòi MỌI khoá của ảnh chụp có kiểu ở bảng này.
 */
object ProfileScopeTypes {

    val CLUSTERNAV: Map<String, PrefType> = buildMap {
        putAll(RetiredCameraKeys.TYPES)
        // `Prefs.enabled/marquee` getBoolean · `Prefs.navClusterScreenMode` getInt · khoá biển báo/bong bóng đời BYD
        // (mã đọc đã gỡ, khoá còn theo hồ sơ tới W4 — `SettingsCatalogRetired`).
        listOf("enabled", "marquee", "badge_enabled", "show_upcoming_badge", "show_alert_chip", "vm_bubble_enabled", "vm_bubble_hidden")
            .forEach { put(it, PrefType.BOOLEAN) }
        listOf("nav_cluster_screen_mode", "badge_size_dp", "badge_center_x", "badge_center_y", "vm_bubble_x", "vm_bubble_y")
            .forEach { put(it, PrefType.INT) }
        listOf("voicekey_enabled", "seat_comfort_enabled", "pm25_filter_enabled", "recirc_on_start_enabled", "headless_autostart")
            .forEach { put(it, PrefType.BOOLEAN) }
        listOf(
            "voicekey_bindings", "voicekey_custom_buttons", "voice_music_default_app", "nav_automation_rules",
            "nav_automation_fired", "theme_choice",
        ).forEach { put(it, PrefType.STRING) }
        listOf("seat_comfort_mode", "seat_level_0", "seat_level_1", "seat_level_2", "seat_level_3")
            .forEach { put(it, PrefType.INT) }
    }
}
