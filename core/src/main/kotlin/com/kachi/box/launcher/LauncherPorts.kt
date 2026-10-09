package com.kachi.box.launcher

/**
 * Cổng ra ngoài của launcher (Port/Adapter) — lõi launcher thuần, test được trên JVM; adapter thật ở :app, off-device dùng
 * [NoCar].
 *
 * Android box B2 · W3 (2026-10-09): `CarDataPort` (đọc dữ liệu xe) và `CarControlPort` (ghi nút xe) gỡ cùng lõi HAL
 * BYDAuto; còn đúng [AppLauncher].
 */

/**
 * Đóng một app THẬT khỏi ô workspace (bộ mở có kênh: [ShellAppLauncher]; chưa có kênh: [NoCar]).
 *
 * 2.93 · SLOT-DEAD-OPENSLOT / wave 2C · SLOT-DEAD-FREEFORM-REST — đường cửa sổ nổi đã gỡ; hợp đồng còn đúng MỘT việc.
 */
interface AppLauncher {
    /** Đóng/đưa [pkg] ra khỏi ô (trả fullscreen / dừng). */
    fun closeSlot(pkg: String)
}

/** Mặc định chưa có kênh shell / off-device: không đóng app nào. (Tên giữ từ bản BYD — chỗ gọi không đổi.) */
object NoCar : AppLauncher {
    override fun closeSlot(pkg: String) {}
}
