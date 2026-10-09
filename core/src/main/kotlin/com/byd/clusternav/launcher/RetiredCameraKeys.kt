package com.byd.clusternav.launcher

/**
 * Tên khoá prefs của camera BYD (bộ chỉnh *Từng camera* 2.93) — mã camera đã GỠ ở Android box B2 · W2b (2026-10-09).
 *
 * Giữ ĐÚNG TÊN khoá (không còn đường đọc/ghi nào) để phạm vi hồ sơ (`ProfileScopeCluster`) và bản chia sẻ
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
}
