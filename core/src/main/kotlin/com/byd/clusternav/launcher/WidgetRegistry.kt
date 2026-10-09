package com.byd.clusternav.launcher

/**
 * Nguồn dữ liệu widget: LOCAL (Android thuần: đồng hồ/nhạc/ảnh/lối tắt).
 *
 * Android box B2 · W3 (2026-10-09): `CAR` (dữ liệu xe BydHal) và `BOARD` (bảng gộp nhiều mini đọc xe) gỡ cùng lõi HAL —
 * sáu widget xe (`w_energy` · `w_tire` · `w_pm25` · `w_car` · `w_speed` · `w_board`) không còn. Ô đã lưu mang mã ấy là
 * mã lạ ⇒ `WorkspaceState.sanitized` cho nó rụng thành ô trống.
 */
enum class WidgetKind { LOCAL }

data class WidgetDef(
    val id: String,
    override val label: String,
    val icon: String,
    val kind: WidgetKind,
    /** Nhãn tiếng Anh (U5 · T2) — tham số mặc định, xem KDoc [Strings]. */
    override val labelEn: String? = null,
) : Localized

/** Widget dựng tay. */
object WidgetRegistry {
    val ALL: List<WidgetDef> = listOf(
        // Android box B2 · W3: nhãn bỏ "+ thời tiết" — nhiệt độ ngoài trời đọc từ HAL xe đã gỡ (owner chốt: đồng hồ không còn
        // nhiệt độ; thời tiết thật cần nguồn khác — ngoài phạm vi).
        WidgetDef("w_clock",  "Đồng hồ",             "ic-sun",   WidgetKind.LOCAL, "Clock"),
        WidgetDef("w_media",  "Đang phát",           "ic-music", WidgetKind.LOCAL, "Now playing"),
        // U4 phần (b): widget TRÌNH CHIẾU ảnh — owner nêu cả hình nền LẪN widget riêng. Đọc cùng thư mục ảnh với
        // hình nền, nhưng chạy ĐỘC LẬP: người dùng có thể muốn một khung ảnh trong ô mà KHÔNG đổi nền màn hình.
        WidgetDef("w_photos", "Trình chiếu ảnh",     "ic-photo", WidgetKind.LOCAL, "Photo slideshow"),
        // F1 (owner 01/10, spec kachi-launcher-shortcuts-autostart R1.3) — lưới icon lối tắt ứng dụng, CÙNG danh sách
        // với khối trên thanh nút (`app_shortcuts` theo hồ sơ). Nhãn khác khối thanh nút ("Lối tắt ứng dụng") để hai mục
        // không thành cặp nhãn trùng ([CapabilityCatalog.collidingLabels]).
        WidgetDef("w_apps",   "Lưới lối tắt app",    "ic-apps",  WidgetKind.LOCAL, "App shortcut grid"),
    )

    fun byId(id: String): WidgetDef? = ALL.firstOrNull { it.id == id }
}
