package com.byd.clusternav.launcher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ Android box B2 · W1/W4 — mục + khoá Cài đặt chỉ-BYD đã gỡ hẳn ═══════════════════════════════════════════════════
 *
 * W1 gỡ mục Cài đặt, W4 gỡ khoá khỏi danh mục ClusterNav + phạm vi hồ sơ (lượt dọn một lần: [BydDeadPrefs]). Bài này
 * khoá: (1) khoá BYD không còn trong danh mục (không hàng "ma", không vào ảnh chụp); (2) mục BYD không mọc lại; (3) phép kiểm
 * mồ côi vẫn bắt khoá lạ (không còn bảng "tha" nào).
 */
class SettingsCatalogRetiredTest {

    private val removed = setOf(
        "enabled", "nav_cluster_screen_mode", "marquee", "badge_enabled", "show_upcoming_badge", "show_alert_chip",
        "badge_size_dp", "badge_center_x", "badge_center_y", "vm_bubble_enabled", "vm_bubble_hidden", "vm_bubble_x",
        "vm_bubble_y", "interpolate", "acc_booster", "lane", "source_mode", "anim_opt", "hud",
    )

    @Test
    fun `khoa BYD da go khong con trong danh muc ClusterNav`() {
        assertEquals(emptySet<String>(), removed intersect SettingsCatalog.CLUSTERNAV_KEYS.keys, "khoá BYD còn trong danh mục")
        assertEquals(emptySet<String>(), removed intersect SettingsCatalog.CLUSTERNAV_HIDDEN_KEYS.keys, "khoá BYD còn ở bảng ẩn")
        assertEquals(emptySet<String>(), removed intersect SettingsCatalog.CLUSTERNAV_COMPANION_KEYS.keys)
        removed.forEach { assertNull(SettingsCatalog.groupOf(it), "'$it' vẫn có mục sở hữu") }
        assertTrue(removed.all(BydDeadPrefs::isDeadClusterNavKey), "mọi khoá gỡ phải được lượt dọn một lần xoá khỏi máy")
    }

    @Test
    fun `phep kiem mo coi bat ca khoa BYD da go`() {
        val lạ = setOf("khoa_moi_ai_do_them", "top_strip", "unit_prefs")
        assertEquals(lạ, SettingsCatalog.orphans(lạ))
        assertFalse("top_strip" in ProfileScope.LAUNCHER_SUFFIXES)
    }

    @Test
    fun `nhom va muc chi-BYD da go`() {
        val gone = setOf(
            "nav_enabled", "nav_cluster_mode", "nav_marquee", "nav_reconnect",
            "badge_enabled", "badge_upcoming", "badge_alert_chip", "badge_size", "badge_center",
            "vm_bubble_enabled", "vm_bubble_autostart", "vm_bubble_pos",
            "bars_top_strip", "bars_top_strip_labels", "display_units", "system_nav_stop",
        )
        val ids = SettingsCatalog.ENTRIES.map { it.id }.toSet()
        assertEquals(emptySet<String>(), gone intersect ids, "mục Cài đặt chỉ-BYD mọc lại")
        assertTrue(SettingsCatalog.ENTRIES.none { it.id.startsWith("cast_") || it.id.startsWith("car_") })
        assertEquals(
            listOf("places_list", "places_add", "nav_default_app", "nav_automation"),
            SettingsCatalog.entriesOf(SettingsGroup.NAV).map { it.id },
            "nhóm Dẫn đường chỉ còn Sổ địa chỉ · app mặc định · lịch tự dẫn",
        )
        assertEquals("Dẫn đường", SettingsGroup.NAV.label)
        assertEquals("Navigation", SettingsGroup.NAV.labelEn)
        assertTrue("cụm" !in SettingsGroup.NAV.label + SettingsGroup.NAV.sub, "nhóm Dẫn đường không còn nói về cụm đồng hồ")
    }
}
