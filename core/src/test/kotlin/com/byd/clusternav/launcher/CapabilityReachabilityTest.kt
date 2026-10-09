package com.byd.clusternav.launcher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * RW0 — **CHẠM TỚI ĐƯỢC**: mọi khả năng phải có ĐƯỜNG cho người dùng đặt vào ô, không chỉ "vẽ được".
 *
 * ⚠⚠ Vì sao có bài này (lỗi thật, tìm ra ở lượt soát 2026-09-11): `WidgetViews` **vẽ được** ô hành động và
 * `ActionMacros` (gỡ ở W3) **có** 4 gói lệnh, nhưng màn chọn của ngăn kéo chỉ liệt kê mục ĐỌC ⇒ người dùng **không có nút nào**
 * để đặt một hành động vào ô giữa màn. Số đo *"3 gói lệnh ở ô giữa màn"* của phiên trước đạt được bằng cách **gieo
 * cấu hình bằng tay**, nên nó KHÔNG chứng minh người dùng làm được. Cả một gói tính năng (W2) không giao được.
 *
 * Bài học đóng vào test: *"vẽ được" ≠ "đặt được"*. Một khả năng chỉ tính là xong khi có đường **đi từ tay người dùng**
 * tới nó. Nguồn của màn chọn (từ Android box B2 · W3) = [WidgetRegistry.ALL] (ngăn kéo) + khối Launcher của bộ chọn
 * nút thanh ([CapabilityPicker.launcherPicks]).
 */
class CapabilityReachabilityTest {

    /**
     * Đúng những gì màn chọn (ngăn kéo + màn Cài đặt) bày ra cho người dùng.
     *
     * Android box B2 · W3 (2026-10-09): nhóm, lĩnh vực, nút xe, datum và gói lệnh gỡ cùng lõi HAL BYDAuto ⇒ còn hai
     * nguồn: widget dựng tay (ngăn kéo) + khối Launcher của bộ chọn nút thanh.
     */
    private fun reachable(): Set<String> =
        WidgetRegistry.ALL.map { it.id }.toSet() + CapabilityPicker.launcherPicks().map { it.id }.toSet()

    @Test
    fun `moi kha nang deu co duong dat vao o`() {
        val hidden = CapabilityCatalog.all().map { it.id }.filter { it !in reachable() }
        assertTrue(
            hidden.isEmpty(),
            "có khả năng KHÔNG bày ở màn chọn nào ⇒ người dùng không đặt được, tính năng coi như không giao: $hidden",
        )
    }

    /**
     * S4 · R12 — hai hành động launcher phải đặt được, và đặt được ĐÚNG MỘT chỗ: khối Launcher của bộ chọn nút.
     *
     * Cùng bài học "vẽ được ≠ đặt được": `ControlDockView` nay dựng được ô loại [CapabilityKind.LAUNCHER], nhưng
     * nếu bộ chọn không bày khối đó thì người dùng không có nút nào để đưa chúng lên thanh.
     */
    @Test
    fun `hai hanh dong launcher deu dat duoc va chi o khoi Launcher`() {
        val reach = reachable()
        val section = CapabilityPicker.launcherPicks().map { it.id }
        // F1 (2026-10-02): `placeable` = ba việc gọi bằng lời + khối lối tắt (`LauncherActions.BLOCKS`).
        LauncherActions.placeable.forEach { a ->
            assertTrue(a.id in reach, "hành động '${a.label}' (${a.id}) không có đường đặt vào thanh nút")
            assertTrue(a.id in section, "phải nằm trong khối Launcher, không rải vào lĩnh vực của xe")
            assertEquals(CapabilityKind.LAUNCHER, CapabilityCatalog.kindOf(a.id), "phải phân loại là LAUNCHER")
        }
        assertEquals(LauncherActions.placeable.size, section.size, "khối Launcher bày ĐÚNG các hành động đó, không thêm")
    }

}
