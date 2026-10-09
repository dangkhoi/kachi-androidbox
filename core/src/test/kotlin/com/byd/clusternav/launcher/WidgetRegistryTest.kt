package com.byd.clusternav.launcher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WidgetRegistryTest {

    @Test fun `co 4 widget id duy nhat`() {
        // Chốt số lượng để ai thêm widget phải nghĩ. Android box B2 · W3 (2026-10-09): sáu widget đọc dữ liệu xe
        // (Năng lượng · Lốp · Tốc độ · Bảng tổng hợp · PM2.5 · Xe) gỡ cùng lõi HAL BYDAuto ⇒ còn đồng hồ · nhạc ·
        // trình chiếu ảnh (U4) · lưới lối tắt (`w_apps`, spec kachi-launcher-shortcuts-autostart.html R1.3).
        assertEquals(listOf("w_clock", "w_media", "w_photos", "w_apps"), WidgetRegistry.ALL.map { it.id })
    }

    @Test fun `moi widget con lai la LOCAL`() {
        WidgetRegistry.ALL.forEach { assertEquals(WidgetKind.LOCAL, it.kind, it.id) }
    }

    @Test fun `ma widget xe cu khong con tra duoc`() {
        listOf("w_energy", "w_tire", "w_speed", "w_board", "w_pm25", "w_car").forEach {
            assertNull(WidgetRegistry.byId(it), "$it phải đã gỡ")
        }
    }
}
