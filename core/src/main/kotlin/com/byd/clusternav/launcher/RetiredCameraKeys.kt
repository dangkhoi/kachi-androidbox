package com.byd.clusternav.launcher

/**
 * Tên khoá prefs của camera BYD (bộ chỉnh *Từng camera* 2.93) — mã camera đã GỠ ở Android box B2 · W2b (2026-10-09).
 *
 * Giữ ĐÚNG TÊN khoá (không còn đường đọc/ghi nào) để phạm vi hồ sơ ([ProfileScope]) và bản chia sẻ
 * (`ProfileSharePolicy`) KHÔNG đổi trước đợt dọn prefs/hồ sơ W4: tệp `.kachi` và ảnh chụp hồ sơ cũ mang các khoá này vẫn
 * được xếp loại như trước (không khoá "không phân loại", không mất/lộ dữ liệu khác). Tên dựng lại đúng khuôn của
 * `CameraCamConfig` cũ: bốn camera (`rear · left · right · front`) × khoá hồ sơ (góc · vị trí · cỡ · hình · kiểu) +
 * khoá xe (xoay · lật). W4 gỡ cả bảng này cùng các khoá camera khác.
 */
object RetiredCameraKeys {

    private val CAMS = listOf("rear", "left", "right", "front")

    /** Khoá theo HỒ SƠ — chuỗi (`putString`). */
    val PROFILE_STRING: List<String> = CAMS.flatMap { c ->
        listOf("camera_pos_$c", "camera_xy_$c", "camera_shape_$c", "camera_projection_$c")
    }

    /** Khoá theo HỒ SƠ — số (`putInt`). */
    val PROFILE_INT: List<String> = CAMS.map { "camera_size_$it" }

    /** Mọi khoá theo HỒ SƠ, theo thứ tự camera rồi thứ tự hàng của bộ chỉnh cũ (góc · vị trí · cỡ · hình · kiểu). */
    val PROFILE_KEYS: List<String> = CAMS.flatMap { c ->
        listOf("camera_pos_$c", "camera_xy_$c", "camera_size_$c", "camera_shape_$c", "camera_projection_$c")
    }

    /** Khoá theo XE (xoay · lật). */
    val DEVICE_KEYS: List<String> = CAMS.flatMap { listOf("camera_rot_$it", "camera_mirror_$it") }

    // ── Android box B2 · W2c — hai bảng phân loại camera dời từ `ProfileScopeCluster` (tệp đó gỡ cùng chiếu cụm) ──

    private const val R_PROFILE = "sở thích trình bày camera BYD (đã gỡ ở W2b) — khoá còn theo hồ sơ trong ảnh clusternav_prefs tới W4"
    private const val R_DEVICE = "hiệu chỉnh/lắp đặt camera BYD của chiếc xe (đã gỡ ở W2b) — theo XE, không chép; W4 dọn"

    /** Khoá camera theo HỒ SƠ (tệp `clusternav_prefs`) → lý do. Hợp với [DEVICE] = đúng bộ khoá camera Kachi BYD 2.93. */
    val PROFILE: Map<String, String> = buildMap {
        listOf(
            "camera_signal_enabled", "camera_on_cluster", "camera_pos_left", "camera_pos_right", "camera_shape",
            "camera_dewarp_amount", "camera_projection", "camera_zoom",
        ).forEach { put(it, R_PROFILE) }
        PROFILE_KEYS.forEach { put(it, R_PROFILE) }
    }

    /** Khoá camera theo XE → lý do (gồm `camera_rotation` đời 2.67 chỉ còn để di trú). */
    val DEVICE: Map<String, String> = buildMap {
        listOf(
            "camera_rot_left", "camera_rot_right", "camera_mirror_left", "camera_mirror_right",
            "camera_view_left", "camera_view_right", "camera_pano_left", "camera_pano_right",
            "camera_cam_left", "camera_cam_right", "camera_render", "camera_gl_texmatrix",
            "camera_span", "camera_strip_left", "camera_strip_right", "camera_circle_scale",
            "camera_dewarp_cx", "camera_dewarp_cy", "camera_dewarp_k", "camera_dewarp_focal", "camera_dewarp_scale",
            "camera_dewarp_pan_x", "camera_dewarp_pan_y", "camera_wide_kappa", "camera_wide_focal", "camera_wide_pan_x",
            "camera_rotation",
        ).forEach { put(it, R_DEVICE) }
        DEVICE_KEYS.forEach { put(it, R_DEVICE) }
    }

    /** Kiểu khai sẵn của khoá camera theo hồ sơ (đọc từ `get*` của mã camera cũ) — lượt áp/nhập bỏ giá trị sai kiểu. */
    val TYPES: Map<String, PrefType> = buildMap {
        listOf("camera_signal_enabled", "camera_on_cluster").forEach { put(it, PrefType.BOOLEAN) }
        listOf("camera_pos_left", "camera_pos_right", "camera_shape", "camera_projection").forEach { put(it, PrefType.STRING) }
        listOf("camera_dewarp_amount", "camera_zoom").forEach { put(it, PrefType.INT) }
        PROFILE_STRING.forEach { put(it, PrefType.STRING) }
        PROFILE_INT.forEach { put(it, PrefType.INT) }
    }
}
