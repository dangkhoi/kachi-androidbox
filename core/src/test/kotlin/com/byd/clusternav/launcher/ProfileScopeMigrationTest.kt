package com.byd.clusternav.launcher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ Rót khoá ClusterNav theo hồ sơ đang sống xuống MỌI hồ sơ đã chụp, chỉ điền chỗ trống ════════════════════════
 *
 * Hỏng ở đây là **mất cấu hình của người lái ngay lượt nâng cấp**: ghi đè thì xoá lựa chọn vừa đặt; bỏ khoá vắng thì giá
 * trị của hồ sơ vừa rời tràn sang. Android box B2 · W2c: họ tiền tố `cast_geometry`, khoá hoãn `cast_enabled` và lượt rót
 * V-CLUSTER gỡ cùng chiếu cụm — các ca của chúng gỡ theo; ca khoá cố định + sổ đã-rót giữ nguyên ý.
 */
class ProfileScopeMigrationTest {

    private val cn = "clusternav_prefs"
    private val cnKeys = ProfileScope.CLUSTERNAV_KEYS.getValue(cn)
    private val newCam = listOf("camera_projection", "camera_zoom")

    @Test
    fun `rot chi dien cho trong, gia tri dang song, khoa vang thanh null tuong minh`() {
        val shots = mapOf(
            "A" to mapOf<String, Any?>("voicekey_enabled" to false),
            "B" to emptyMap(),
        )
        val live = mapOf<String, Any?>("voicekey_enabled" to true, "voicekey_bindings" to "[]")
        val out = ProfileScopeMigration.rotDown(shots, live, listOf("voicekey_enabled", "voicekey_bindings", "theme_choice"))
        assertEquals(false, out.getValue("A")["voicekey_enabled"], "khoá có sẵn trong ảnh không bị đụng")
        assertEquals("[]", out.getValue("A")["voicekey_bindings"])
        assertTrue(out.getValue("B").containsKey("theme_choice") && out.getValue("B")["theme_choice"] == null)
        assertEquals(true, out.getValue("B")["voicekey_enabled"])
        assertTrue(ProfileScopeMigration.rotDown(shots + out, live, listOf("voicekey_enabled")).isEmpty(), "chạy hai lần không đổi")
    }

    /**
     * Khoá ca QA máy ảo 2.92 [ĐO]: hồ sơ «Mặc định» lưu bằng 2.9x (chưa có khoá kiểu/thu phóng); ở hồ sơ A chọn giá trị
     * mới rồi đổi sang ⇒ «Mặc định» ăn giá trị của A. Bản vá: rót `null` ⇒ lượt áp XOÁ ⇒ về mặc định.
     */
    @Test
    fun `ca QA 2_92 — khoa moi khong ro sang ho so luu bang ban cu`() {
        val old = mapOf<String, Any?>("camera_dewarp_amount" to 0, "camera_shape" to "RECT")
        val types = ProfileScopeTypes.CLUSTERNAV
        val liveAfterChoice = mapOf<String, Any?>("camera_projection" to "WIDE", "camera_zoom" to 120, "camera_dewarp_amount" to 100)

        val leak = PrefSnapshotPlan.apply(liveAfterChoice, old, cnKeys, types)
        assertFalse("camera_projection" in leak.writes || "camera_zoom" in leak.writes, "mô tả bệnh: $leak")

        val filled = ProfileScopeMigration.fillNewKeys(mapOf("Mặc định" to old), emptyMap(), newCam).getValue("Mặc định")
        assertTrue(filled.containsKey("camera_projection") && filled["camera_projection"] == null)
        assertTrue(filled.containsKey("camera_zoom") && filled["camera_zoom"] == null)
        assertEquals(0, filled["camera_dewarp_amount"], "khoá có sẵn trong ảnh không bị đụng")
        val edit = PrefSnapshotPlan.apply(liveAfterChoice, filled, cnKeys, types)
        assertTrue(edit.writes.containsKey("camera_projection") && edit.writes["camera_projection"] == null)
        assertTrue(edit.writes.containsKey("camera_zoom") && edit.writes["camera_zoom"] == null)
        assertEquals(0, edit.writes["camera_dewarp_amount"])
    }

    @Test
    fun `ho so CHUA co anh khong bi de anh`() {
        val shots = mapOf("A" to emptyMap(), "B" to mapOf<String, Any?>("voicekey_enabled" to true))
        val out = ProfileScopeMigration.fillNewKeys(shots, mapOf("theme_choice" to "dark"), listOf("theme_choice"))
        assertEquals(setOf("B"), out.keys, "chưa có ảnh = bản sao của hiện tại (S4 · R5) — không đẻ ảnh cho nó")
        assertEquals("dark", out.getValue("B")["theme_choice"])
    }

    @Test
    fun `khoa moi rot gia tri DANG SONG, chi dien cho trong, chay hai lan khong doi`() {
        val shots = mapOf(
            "A" to mapOf<String, Any?>("enabled" to true),
            "B" to mapOf<String, Any?>("vm_bubble_hidden" to false),
        )
        val live = mapOf<String, Any?>("vm_bubble_hidden" to true)
        val out = ProfileScopeMigration.fillNewKeys(shots, live, listOf("vm_bubble_hidden"))
        assertEquals(true, out.getValue("A")["vm_bubble_hidden"], "khoá đã có giá trị sống ⇒ ai cũng bắt đầu từ cái đang dùng")
        assertFalse("B" in out, "B đã có khoá (kể cả khác giá trị sống) ⇒ của B, không chạm")
        assertTrue(ProfileScopeMigration.fillNewKeys(shots + out, live + ("vm_bubble_hidden" to false), listOf("vm_bubble_hidden")).isEmpty())
    }

    @Test
    fun `so da rot — chi khoa chua co trong so, so day thi khong con gi`() {
        val scope = ProfileScope.CLUSTERNAV_KEYS
        val all = ProfileScopeMigration.ledgerOf(scope)
        assertTrue(ProfileScopeMigration.pendingKeys(scope, all).isEmpty())
        assertEquals(scope, ProfileScopeMigration.pendingKeys(scope, emptySet()), "sổ rỗng (lần đầu) ⇒ duyệt mọi khoá")
        val older = all - newCam.map { ProfileScopeMigration.ledgerEntry(cn, it) }.toSet()
        assertEquals(mapOf(cn to newCam), ProfileScopeMigration.pendingKeys(scope, older))
        assertTrue(all.none { it.count { c -> c == '/' } != 1 }, "mỗi mục đúng một dấu tách tệp/khoá")
        assertEquals(ProfileScope.Scope.DEVICE, ProfileScope.scopeOf(ProfileScopeMigration.FILLED_LEDGER_KEY), "sổ theo XE")
    }

    @Test
    fun `tep prefs MOI - ho so da chup o tep khac duoc rot, ho so chua chup o dau khong`() {
        val newFile = "kachi_new_prefs"
        val shots = mapOf(
            cn to mapOf("A" to mapOf<String, Any?>("enabled" to true), "B" to emptyMap()),
            newFile to mapOf("A" to emptyMap(), "B" to emptyMap()),
        )
        val captured = ProfileScopeMigration.captured(shots)
        assertEquals(setOf("A"), captured, "đã chụp = ảnh khác rỗng ở BẤT KỲ tệp nào")
        val live = mapOf<String, Any?>("new_key" to 7)
        assertTrue(ProfileScopeMigration.fillNewKeys(shots.getValue(newFile), live, listOf("new_key", "gone_key")).isEmpty())
        val out = ProfileScopeMigration.fillNewKeys(shots.getValue(newFile), live, listOf("new_key", "gone_key"), captured = captured)
        assertEquals(setOf("A"), out.keys, "B chưa chụp ở tệp nào = bản sao của hiện tại (S4 · R5) — không đẻ ảnh")
        assertEquals(7, out.getValue("A")["new_key"])
        assertTrue(out.getValue("A").containsKey("gone_key") && out.getValue("A")["gone_key"] == null, "vắng ở tệp sống ⇒ null tường minh")
    }

    /** Android box B2 · W2c — chỉ còn MỘT tệp ảnh chụp ClusterNav đi theo hồ sơ ngoài hai tệp một-khoá. */
    @Test
    fun `bang tep anh chup khong con tep chieu cum`() {
        assertEquals(setOf("clusternav_prefs", "clusternav_theme"), ProfileScope.CLUSTERNAV_KEYS.keys)
    }
}
