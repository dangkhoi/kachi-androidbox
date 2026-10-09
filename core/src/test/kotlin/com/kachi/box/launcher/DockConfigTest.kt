package com.kachi.box.launcher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Cấu hình thanh nút (`DockConfig`) sau Android box B2 · W3 (2026-10-09): bộ đăng ký nút xe đã gỡ ⇒ thanh chỉ nhận
 * hành động LAUNCHER. Thay `ControlRegistryTest` (bài ghim `ControlRegistry` đã gỡ cùng lõi HAL BYDAuto).
 */
class DockConfigTest {

    @Test fun `default dock = vien duoi + chi hanh dong launcher`() {
        val d = DockConfig()
        assertEquals(DockEdge.BOTTOM, d.edge)
        assertEquals(
            listOf(LauncherActions.APPS, LauncherActions.SETTINGS, LauncherActions.VOICE, LauncherActions.SHORTCUTS),
            d.enabled,
        )
        assertTrue(d.enabled.all { CapabilityCatalog.kindOf(it) == CapabilityKind.LAUNCHER })
        assertEquals(d.enabled, DockSelection.sanitize(d.enabled), "mặc định phải qua được lượt lọc lúc nạp")
    }

    @Test fun `setEnabled them va xoa hanh dong launcher`() {
        val d = DockConfig(enabled = emptyList())
        val added = d.setEnabled(LauncherActions.APPS, true)
        assertEquals(listOf(LauncherActions.APPS), added.enabled)
        assertFalse(LauncherActions.APPS in added.setEnabled(LauncherActions.APPS, false).enabled)
    }

    @Test fun `ma la va ma nut xe cu khong vao duoc thanh`() {
        val d = DockConfig()
        listOf("khong_ton_tai", "", "ac_auto", "tyre_p_fl", "soc", "mac_leave").forEach {
            assertEquals(d, d.setEnabled(it, true), "mã '$it' KHÔNG được vào cấu hình bền")
        }
    }

    /**
     * Hồi quy W3: sau khi gỡ nút xe, `kindOf` còn trả READ cho widget — nếu cổng chỉ hỏi "có trong catalog không"
     * thì `w_clock` vào được thanh mà `ControlDockView` bỏ qua ⇒ "đặt N hiện ít hơn" (họ lỗi 2026-09-22).
     */
    @Test fun `widget khong vao duoc thanh nut`() {
        assertEquals(CapabilityKind.READ, CapabilityCatalog.kindOf("w_clock"), "tiền đề: w_clock là widget")
        val d = DockConfig()
        assertEquals(d, d.setEnabled("w_clock", true))
    }

    @Test fun `thanh da luu co nut xe va widget tu rung khi nap`() {
        val saved = listOf("ac_auto", LauncherActions.APPS, "w_board", "w_clock", LauncherActions.VOICE, "mac_leave")
        assertEquals(listOf(LauncherActions.APPS, LauncherActions.VOICE), DockSelection.sanitize(saved))
    }

    @Test fun `withEdge + isVertical`() {
        assertTrue(DockConfig().withEdge(DockEdge.LEFT).isVertical())
        assertTrue(DockConfig().withEdge(DockEdge.RIGHT).isVertical())
        assertFalse(DockConfig().withEdge(DockEdge.TOP).isVertical())
        assertFalse(DockConfig().withEdge(DockEdge.BOTTOM).isVertical())
    }
}
