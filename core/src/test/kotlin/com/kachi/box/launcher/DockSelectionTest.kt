package com.kachi.box.launcher

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * ═══ THANH NÚT PHẢI TỰ RỤNG MÃ ĐÃ XOÁ ═══════════════════════════════════════════════════════════════════════
 *
 * Bug 2026-09-22 ("đặt 10 hiện 6"): owner gỡ `lock/door/steer_heat` + macro `mac_leave/mac_door_light` (1.94/1.95),
 * `window` (hợp nhất vào `win_lf`) — nhưng cấu hình thanh nút ĐÃ LƯU vẫn giữ mã đó, `ControlDockView.rebuild` bỏ
 * qua IM LẶNG ⇒ số "đang bật" (10) không khớp số nút hiện (6). Đây là **bổ sung còn thiếu** của quy trình gỡ mã:
 * ô có `WorkspaceState.sanitized`, chip có `TopStripConfig.decode` lọc, **thanh nút** thì trước đây KHÔNG.
 *
 * Bài này khoá [DockSelection.sanitize]: gỡ một control mà quên bịt đường thanh nút thì đây đỏ ngay.
 */
class DockSelectionTest {

    /**
     * Mã đã gỡ: sáu mã 1.94/1.95 + Android box B2 · W3 (2026-10-09) — MỌI nút xe, datum xe và gói lệnh gỡ cùng lõi
     * HAL BYDAuto. Cấu hình cũ lưu chúng phải TỰ RỤNG khi nạp.
     */
    private val removed = listOf(
        "lock", "door", "window", "steer_heat", "mac_leave", "mac_door_light",
        "trunk", "readl", "fan", "fuel_range_km", "defrost", "seath", "pm25", "temp", "win_lf", "mac_win_close_all",
    )

    @Test fun `sanitize bo het ma xe da xoa`() {
        removed.forEach { id ->
            assertTrue(CapabilityCatalog.kindOf(id) == null, "tiền đề: '$id' đã gỡ khỏi catalog")
        }
        val old = listOf("lock", LauncherActions.APPS, "trunk", "readl", "fan", LauncherActions.VOICE, "mac_door_light", "seath")
        assertEquals(
            listOf(LauncherActions.APPS, LauncherActions.VOICE), DockSelection.sanitize(old),
            "chỉ giữ mã còn sống, thứ tự nguyên vẹn — số 'đang bật' phải khớp số nút hiện",
        )
    }

    @Test fun `sanitize giu nguyen ma con song`() {
        val live = listOf(LauncherActions.SHORTCUTS, LauncherActions.SETTINGS, LauncherActions.APPS, LauncherActions.VOICE)
        assertEquals(live, DockSelection.sanitize(live), "mọi hành động launcher phải giữ, đúng thứ tự")
    }

    @Test fun `sanitize danh sach rong tra rong`() {
        assertEquals(emptyList<String>(), DockSelection.sanitize(emptyList()))
    }
}
