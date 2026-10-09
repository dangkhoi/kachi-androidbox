package com.byd.clusternav.launcher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorkspaceStateTest {

    @Test fun `mac dinh 3 widget, con lai o trong`() {
        val s = WorkspaceState()
        assertEquals(LayoutPreset.THREE, s.preset)
        // Chốt trần ô để ai đổi phải NGHĨ: mỗi ô chứa app cần một màn ảo riêng, và ô quá nhỏ thì app vô dụng.
        // Xem KDoc WorkspaceState.SLOT_CAP. Nới 4 → 6 ở P9 bước 3.
        assertEquals(6, WorkspaceState.SLOT_CAP)
        assertEquals(WorkspaceState.SLOT_CAP, s.slots.size)
        s.slots.forEach { assertSame(SlotContent.Empty, it) }
    }

    @Test fun `Android box - bo cuc mac dinh chi widget KHONG doc xe`() {
        // W0 (2026-10-09): `w_board`/`w_energy`/`w_pm25` đọc HAL BYD ⇒ trên Android box chỉ ra "—". Mặc định nay là
        // đồng hồ · đang phát · lưới lối tắt — widget LOCAL có thật trong bộ đăng ký.
        val d = WorkspaceState.DEFAULT
        assertEquals(LayoutPreset.THREE, d.preset)
        val ids = d.slots.take(LayoutPreset.THREE.slotCount).map { (it as SlotContent.Widget).ids.single() }
        assertEquals(listOf("w_clock", "w_media", "w_apps"), ids)
        ids.forEach { id ->
            assertEquals(WidgetKind.LOCAL, WidgetRegistry.byId(id)?.kind, "$id phải là widget có thật, không đọc xe")
        }
        d.slots.drop(LayoutPreset.THREE.slotCount).forEach { assertSame(SlotContent.Empty, it) }
        assertEquals(d, d.sanitized(), "mặc định phải qua được lượt dọn ô lạ lúc nạp")
    }

    @Test fun `withSlot gan app va widget`() {
        val s = WorkspaceState()
            .withSlot(0, SlotContent.App("com.google.android.apps.maps"))
            .withSlot(2, SlotContent.Widget("w_media"))
        assertEquals(SlotContent.App("com.google.android.apps.maps"), s.slots[0])
        assertEquals(SlotContent.Widget("w_media"), s.slots[2])
        assertSame(SlotContent.Empty, s.slots[1])
    }

    @Test fun `withSlot index ngoai pham vi giu nguyen`() {
        val s = WorkspaceState()
        assertEquals(s, s.withSlot(9, SlotContent.App("x")))
        assertEquals(s, s.withSlot(-1, SlotContent.App("x")))
    }

    @Test fun `visibleSlots theo preset`() {
        val s = WorkspaceState(preset = LayoutPreset.THREE)
        assertEquals(3, s.visibleSlots().size)
        assertEquals(1, s.withPreset(LayoutPreset.ONE).visibleSlots().size)
        assertEquals(4, s.withPreset(LayoutPreset.QUAD).visibleSlots().size)
    }

    @Test fun `doi preset giu gan o an`() {
        val s = WorkspaceState(preset = LayoutPreset.QUAD)
            .withSlot(3, SlotContent.App("com.netflix"))
            .withPreset(LayoutPreset.THREE)   // ô 3 ẩn nhưng vẫn nhớ
        assertEquals(SlotContent.App("com.netflix"), s.slots[3])
        assertEquals(3, s.visibleSlots().size)
        assertEquals(SlotContent.App("com.netflix"), s.withPreset(LayoutPreset.QUAD).slots[3])
    }

    @Test fun `clearSlot ve trong`() {
        val s = WorkspaceState().withSlot(1, SlotContent.App("x")).clearSlot(1)
        assertSame(SlotContent.Empty, s.slots[1])
    }

    // Owner 2026-09-15: chọn GMaps ở ô 1 rồi chọn lại GMaps ở ô 2 → GMaps hiện CẢ hai ô. Một app không được ở hai ô.
    @Test fun `mot app mot o - dat app o o moi go khoi o cu`() {
        val gmaps = "com.google.android.apps.maps"
        val s = WorkspaceState()
            .withSlot(0, SlotContent.App(gmaps))   // ô 1 = GMaps
            .withSlot(1, SlotContent.App(gmaps))   // ô 2 cũng chọn GMaps ⇒ phải CHUYỂN, không nhân đôi
        assertSame(SlotContent.Empty, s.slots[0], "ô cũ phải trống — không để lại app trùng")
        assertEquals(SlotContent.App(gmaps), s.slots[1], "ô mới giữ GMaps")
        assertEquals(1, s.slots.count { it is SlotContent.App && it.pkg == gmaps }, "GMaps chỉ được ở đúng 1 ô")
    }

    @Test fun `sanitized chua state cu co app trung o - giu o dau`() {
        val gmaps = "com.google.android.apps.maps"
        // Mô phỏng state NẠP từ prefs cũ: gmaps ở CẢ ô 0 và ô 1 (dựng thẳng qua constructor, không qua withSlot).
        val dirty = WorkspaceState(slots = List(WorkspaceState.SLOT_CAP) { i ->
            if (i == 0 || i == 1) SlotContent.App(gmaps) else SlotContent.Empty
        })
        val clean = dirty.sanitized()
        assertEquals(SlotContent.App(gmaps), clean.slots[0], "giữ ô đầu (đang hiện cửa sổ thật)")
        assertSame(SlotContent.Empty, clean.slots[1], "ô trùng sau về trống")
        assertEquals(1, clean.slots.count { it is SlotContent.App && it.pkg == gmaps })
    }

    @Test fun `sanitized khong doi tham chieu khi khong co trung`() {
        val s = WorkspaceState().withSlot(0, SlotContent.App("a")).withSlot(1, SlotContent.App("b"))
        assertSame(s, s.sanitized(), "không trùng ⇒ trả chính nó")
    }

    // ── Mã khả năng đã BIẾN MẤT khỏi bộ đăng ký (owner gỡ hẳn) ───────────────────────────────────
    //
    // ⚠ Bài này khoá bài học của lượt **ADAS-PURGE 2026-09-16**: owner gỡ 10 nút + 17 datum + 3 nhóm, nhưng cấu
    // hình ĐÃ LƯU trên xe vẫn trỏ tới chúng. [ĐO đọc source] đường cũ không sập mà **tệ hơn**: `WidgetViews.build`
    // rơi xuống `telemetry(...)` → `TelemetryReadout.of` trả null → ô hiện `"ADAS_FCW"` + `"—"` **mãi mãi**. Thanh
    // nút (`ControlDockView` nhánh `null -> Unit`) và chip thanh trên (`TopStripChips.render` `mapNotNull`) đã bỏ
    // mã lạ từ trước — ô giữa màn là bề mặt cuối cùng còn giữ rác.
    @Test fun `sanitized bo ma widget khong con trong bo dang ky`() {
        val dirty = WorkspaceState(slots = List(WorkspaceState.SLOT_CAP) { i ->
            when (i) {
                0 -> SlotContent.Widget(listOf("adas_fcw"))              // nút ADAS đã xoá ⇒ cả ô về trống
                1 -> SlotContent.Widget(listOf("w_media", "radar_zones"))   // một mã sống + một mã chết
                2 -> SlotContent.Widget(listOf("g_adas"))                // nhóm đã xoá
                3 -> SlotContent.Widget(listOf("w_clock"))               // mã sống ⇒ giữ nguyên
                else -> SlotContent.Empty
            }
        })
        assertEquals(
            listOf("adas_fcw", "radar_zones", "g_adas"), dirty.unknownWidgetIds(),
            "phải NÓI RA được mã nào sắp bị bỏ — mất một ô đã lưu mà im lặng là kênh im lặng",
        )
        val clean = dirty.sanitized()
        assertSame(SlotContent.Empty, clean.slots[0], "ô chỉ có mã đã xoá ⇒ về trống, không phải ô 'ADAS_FCW —'")
        assertEquals(SlotContent.Widget(listOf("w_media")), clean.slots[1], "giữ mã còn sống, bỏ mã đã xoá")
        assertSame(SlotContent.Empty, clean.slots[2], "mã NHÓM đã xoá cũng phải rụng")
        assertEquals(SlotContent.Widget(listOf("w_clock")), clean.slots[3], "mã sống không bị đụng tới")
        assertEquals(emptyList<String>(), clean.unknownWidgetIds(), "chạy lại phải sạch (idempotent)")
    }

    /**
     * Android box B2 · W3 (2026-10-09): lõi HAL BYDAuto gỡ ⇒ MỌI widget xe, nút xe, datum, nhóm và gói lệnh không
     * còn bộ đăng ký nào nhận. Hồ sơ lưu từ Kachi BYD (hoặc tệp `.kachi` nhập vào) mang các mã ấy phải nạp lên thành
     * ô TRỐNG — không phải ô ghi HOA mã + "—", không sập.
     */
    @Test fun `sanitized bien o xe da luu thanh o trong`() {
        val carIds = listOf(
            "w_board", "w_energy", "w_tire", "w_speed", "w_pm25", "w_car",         // widget xe
            "ac_auto", "temp", "fan", "win_lf", "trunk", "sunroof", "readl", "seath", // nút xe
            "soc", "tyre_p_fl", "speed", "inside_temp", "fuel_range_km",            // datum xe
            "g_tyres", "g_doors", "mac_leave", "mac_win_close_all",                 // nhóm + gói lệnh
        )
        val dirty = WorkspaceState(slots = List(WorkspaceState.SLOT_CAP) { i ->
            when (i) {
                0 -> SlotContent.Widget(listOf("w_board"))
                1 -> SlotContent.Widget(carIds)
                2 -> SlotContent.Widget(listOf("ac_auto", "w_clock", "soc"))
                else -> SlotContent.Empty
            }
        })
        carIds.forEach { assertNull(CapabilityCatalog.kindOf(it), "tiền đề: '$it' đã gỡ") }
        val clean = dirty.sanitized()
        assertSame(SlotContent.Empty, clean.slots[0], "ô w_board đã lưu ⇒ trống")
        assertSame(SlotContent.Empty, clean.slots[1], "ô toàn mã xe ⇒ trống")
        assertEquals(SlotContent.Widget(listOf("w_clock")), clean.slots[2], "ô trộn ⇒ chỉ giữ widget còn sống")
        assertEquals(emptyList<String>(), clean.unknownWidgetIds())
    }

    @Test fun `mot app mot o - app khac khong bi anh huong`() {
        val gmaps = "com.google.android.apps.maps"
        val s = WorkspaceState()
            .withSlot(0, SlotContent.App("com.youtube"))
            .withSlot(1, SlotContent.App(gmaps))
            .withSlot(2, SlotContent.App(gmaps))   // chuyển GMaps sang ô 3
        assertEquals(SlotContent.App("com.youtube"), s.slots[0], "app KHÁC giữ nguyên")
        assertSame(SlotContent.Empty, s.slots[1])
        assertEquals(SlotContent.App(gmaps), s.slots[2])
    }

    @Test fun `swap doi cho 2 o`() {
        val s = WorkspaceState()
            .withSlot(0, SlotContent.App("a"))
            .withSlot(3, SlotContent.Widget("w_media"))
            .swap(0, 3)
        assertEquals(SlotContent.Widget("w_media"), s.slots[0])
        assertEquals(SlotContent.App("a"), s.slots[3])
    }

    @Test fun `swap index xau hoac trung giu nguyen`() {
        val s = WorkspaceState().withSlot(0, SlotContent.App("a"))
        assertEquals(s, s.swap(0, 9))
        assertEquals(s, s.swap(-1, 0))
        assertEquals(s, s.swap(2, 2))
    }
}
