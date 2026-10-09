package com.byd.clusternav.launcher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ Android box B2 · W1 — khoá đã rời giao diện ([SettingsCatalogRetired]) KHÔNG làm mất dữ liệu hồ sơ ═════════════
 *
 * W1 gỡ các mục Cài đặt chỉ-BYD nhưng (theo chỉ thị đợt) giữ nguyên `CLUSTERNAV_KEYS` / `ProfileScope` để một lượt đổi /
 * xuất / nhập hồ sơ không xoá giá trị người dùng đang có. Bài này khoá ba điều: (1) khoá đã rời UI thật sự KHÔNG còn mục
 * nào nhận (không có hàng "ma" trên rail); (2) mọi khoá ấy vẫn được xếp phạm vi như cũ (chưa rơi khỏi ảnh chụp); (3) phép kiểm
 * mồ côi vẫn bắt được khoá lạ — bảng tha ĐÚNG danh sách, không tha theo mẫu.
 */
class SettingsCatalogRetiredTest {

    @Test
    fun `khoa da roi UI khong con muc nao nhan`() {
        SettingsCatalog.RETIRED_UI_KEYS.keys.forEach { key ->
            assertNull(SettingsCatalog.groupOf(key), "'$key' đã rời UI mà vẫn có mục sở hữu")
            assertTrue(key !in SettingsCatalog.NOT_SETTINGS, "'$key' không thể vừa 'rời UI' vừa 'không phải cấu hình'")
        }
        assertTrue(SettingsCatalog.RETIRED_UI_KEYS.values.all { it.isNotBlank() }, "mỗi khoá phải kèm lý do")
    }

    @Test
    fun `khoa da roi UI van duoc xep pham vi - khong mat du lieu`() {
        // Phạm vi giữ NGUYÊN như trước W1: phần lớn theo HỒ SƠ (đi trong ảnh chụp), riêng bộ ba tự sấy kính theo XE
        // (`DEVICE_KEYS`) — không khoá nào rơi về UNKNOWN (= lượt chụp/áp/xuất hồ sơ bỏ sót, tức mất dữ liệu).
        val unknown = SettingsCatalog.RETIRED_UI_KEYS.keys.filter { ProfileScope.scopeOf(it) == ProfileScope.Scope.UNKNOWN }
        assertEquals(emptyList<String>(), unknown, "khoá rời UI phải còn được xếp phạm vi (W4 mới dọn)")
        assertEquals(ProfileScope.Scope.PROFILE, ProfileScope.scopeOf("cast_enabled"))
        assertEquals(ProfileScope.Scope.PROFILE, ProfileScope.scopeOf("badge_size_dp"))
        val clusterNav = SettingsCatalog.RETIRED_UI_KEYS.keys - SettingsCatalogRetired.LAUNCHER_KEYS
        assertTrue(SettingsCatalog.CLUSTERNAV_KEYS.keys.containsAll(clusterNav), "khoá ClusterNav rời UI rơi khỏi CLUSTERNAV_KEYS")
    }

    @Test
    fun `phep kiem mo coi van bat khoa la ben canh khoa da roi UI`() {
        val lạ = setOf("khoa_moi_ai_do_them")
        assertEquals(lạ, SettingsCatalog.orphans(lạ + SettingsCatalog.RETIRED_UI_KEYS.keys))
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
