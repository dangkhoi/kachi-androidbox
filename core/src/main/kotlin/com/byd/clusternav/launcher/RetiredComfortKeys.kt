package com.byd.clusternav.launcher

/**
 * ═══ Android box B2 · W2e (2026-10-09) — tên khoá prefs của TIỆN NGHI XE + TỰ SẤY KÍNH BYD đã gỡ mã ═══════════════════
 *
 * Mã ghế mát/sưởi (`SeatComfortApplier`), lọc bụi PM2.5 (`Pm25FilterApplier`), lấy gió trong khi nổ máy (`RecircApplier`)
 * và tự sấy kính khi mưa (`RainDefrostApplier` + `:core RainDefrost*`) đã xoá ở đợt W2e (mục Cài đặt gỡ từ W1). Bảng này
 * giữ ĐÚNG TÊN các khoá còn có thể nằm trên máy / trong tệp hồ sơ cũ để [ProfileScope] vẫn xếp loại được, và giữ NGUYÊN
 * phạm vi cũ (cùng khuôn [RetiredCameraKeys]): tám khoá tiện nghi theo HỒ SƠ (ảnh chụp `clusternav_prefs`, bản chia sẻ mang
 * theo như ≤ 2.98), ba khoá tự sấy theo XE. Không còn ai đọc/ghi chúng — W4 dọn cả bảng.
 *
 * Tệp `.kachi` của Kachi BYD mang các khoá này vẫn nhập được (khoá có kiểu khai ở [TYPES] ⇒ giá trị sai kiểu bị bỏ, không ném).
 */
object RetiredComfortKeys {

    /** Tệp prefs chứa mọi khoá ở đây (tệp chính phía ClusterNav). */
    const val FILE = "clusternav_prefs"

    private const val R_PROFILE = "tiện nghi xe BYD theo người lái (ghế · lọc bụi · lấy gió) — mã gỡ ở Android box B2 · W2e; khoá còn trong ảnh chụp hồ sơ tới W4"
    private const val R_DEVICE = "tự sấy kính khi mưa BYD (cảm biến mưa + nút sấy của chiếc xe) — mã gỡ ở W2e; theo XE như trước, không chép; W4 dọn"

    /** Khoá Boolean theo HỒ SƠ. */
    private val PROFILE_BOOLEAN = listOf("seat_comfort_enabled", "pm25_filter_enabled", "recirc_on_start_enabled")

    /** Khoá Int theo HỒ SƠ (chế độ ghế + mức bốn ghế `seat_level_0..3` — đời BYD chỉ có tối đa 4 ghế). */
    private val PROFILE_INT = listOf("seat_comfort_mode", "seat_level_0", "seat_level_1", "seat_level_2", "seat_level_3")

    /** Khoá theo HỒ SƠ (tệp [FILE]) → lý do. */
    val PROFILE: Map<String, String> = (PROFILE_BOOLEAN + PROFILE_INT).associateWith { R_PROFILE }

    /** Khoá theo XE → lý do (ba khoá tự sấy kính 1.85/V7/V8). */
    val DEVICE: Map<String, String> =
        listOf("rain_defrost_enabled", "rain_defrost_front", "rain_defrost_rear").associateWith { R_DEVICE }

    /** Kiểu khai sẵn của khoá theo hồ sơ (đọc từ `get*` của mã cũ) — lượt áp/nhập bỏ giá trị sai kiểu. */
    val TYPES: Map<String, PrefType> =
        PROFILE_BOOLEAN.associateWith { PrefType.BOOLEAN } + PROFILE_INT.associateWith { PrefType.INT }
}
