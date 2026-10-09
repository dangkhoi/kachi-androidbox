package com.byd.clusternav.launcher

/**
 * ═══ Android box B2 · W2c (2026-10-09) — tên khoá prefs của CHIẾU CỤM · VIETMAP · BIỂN TỐC ĐỘ BYD đã gỡ mã ═══════════════
 *
 * Mã chiếu cụm (`SimpleCastRuntime`, nút nổi, `CastAppCatalog`), VietMap (widget, bong bóng, tiền đề app) và biển tốc độ
 * đã xoá ở đợt W2c. Bảng này giữ ĐÚNG TÊN các khoá còn có thể nằm trên máy / trong tệp hồ sơ cũ để [ProfileScope] vẫn xếp
 * loại được (không khoá "không phân loại") — cùng khuôn [RetiredCameraKeys]. Mọi khoá ở đây theo XE (= không chép theo
 * hồ sơ, không vào bản chia sẻ): không còn ai đọc chúng, chép đi chép lại chỉ là rác.
 *
 * ⚠ Tệp `simple_cast_prefs` và `cast-v2-app-catalog` RỜI ảnh chụp hồ sơ ở đợt này: tệp `.kachi` cũ mang ảnh của hai tệp đó
 * (hậu tố `__cn__simple_cast_prefs` · `__cn__cast-v2-app-catalog`) bị lượt nhập bỏ IM LẶNG (hậu tố không còn trong
 * [ProfileScope.LAUNCHER_SUFFIXES]). Khoá badge/bong bóng/`enabled` nằm trong `clusternav_prefs` thì vẫn theo hồ sơ tới W4
 * ([SettingsCatalogRetired]). W4 dọn cả bảng này.
 */
object RetiredClusterKeys {

    /** Hai tệp prefs của chiếu cụm đã rời ảnh chụp hồ sơ (tên giữ cho bài canh + lượt dọn W4). */
    const val SIMPLE_CAST_FILE = "simple_cast_prefs"
    const val CAST_CATALOG_FILE = "cast-v2-app-catalog"

    private const val R_CAST = "chiếu màn lên cụm BYD — mã gỡ ở Android box B2 · W2c; tệp simple_cast_prefs rời ảnh chụp hồ sơ, không chép"
    private const val R_CATALOG = "CastAppCatalog (nút nổi chiếu cụm) — mã gỡ ở W2c; không chép"
    private const val R_MARK = "dấu một-lần / sổ trạng thái của chiếu cụm · VietMap trên xe BYD — mã gỡ ở W2c; không chép"

    /** Khoá theo XE → lý do. [ProfileScope.DEVICE_KEYS] cộng bảng này. */
    val DEVICE_KEYS: Map<String, String> = buildMap {
        listOf(
            "cast_enabled", "split_ratio_left_pct", "cast_bubble_visible", "cast_style",
            "autostart_enabled", "autostart_package", "autostart_split_enabled",
            "autostart_left_package", "autostart_right_package",
            "cast_enabled_pending", "cast_enabled_committed", "car_type_dadb", "profileOverride",
            "cluster_theme_ledger", "cluster_bubble_old_mod",
        ).forEach { put(it, R_CAST) }
        listOf(
            "bubbleX", "bubbleY", "autoCast", "castable", "keepSession", "migrationVersion",
            "bubbleEnabled", "favorites", "protected", "legacyDefaultCandidate", "rectStyle",
        ).forEach { put(it, R_CATALOG) }
        listOf(
            "migrated_cluster_profile_v1", "vm_float_whitelist_applied",
            "badge_corner", "badge_dx", "badge_dy", "bubble_auto",
        ).forEach { put(it, R_MARK) }
    }

    /** Tiền tố khoá dựng động theo XE → lý do (khung/DPI từng app khi chiếu, V2 + V-CLUSTER). */
    val DEVICE_KEY_PREFIXES: Map<String, String> = buildMap {
        listOf("config_size_", "config_overscan_", "config_density_", "config_bounds_")
            .forEach { put(it, "khung/DPI từng app khi chiếu cụm BYD — mã gỡ ở W2c; không chép") }
        put("scale-", R_CATALOG)
        put("dpi:", R_CATALOG)
    }
}
