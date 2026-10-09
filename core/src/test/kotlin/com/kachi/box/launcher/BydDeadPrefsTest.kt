package com.kachi.box.launcher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Android box B2 · W4 — hồ sơ `.kachi` xuất từ **Kachi BYD 2.97** (giả lập đủ họ khoá BYD) + lượt dọn khoá chết một lần.
 *
 * Fixture dựng theo đúng khuôn tệp 2.97: header `kachi-profile\tv1\t<tên>\t{full|share}` + một [PrefSnapshot] mang MỌI hậu tố
 * theo hồ sơ của bản BYD (bố cục · chip thanh trên · đơn vị · ảnh `__cn__clusternav_prefs` · ảnh hai tệp chiếu cụm), và
 * trong ảnh `clusternav_prefs` đủ họ khoá đã gỡ: dẫn đường cụm/HUD · biển báo · bong bóng VietMap · camera (cả 2.93 từng
 * camera) · tiện nghi · sấy kính — có cả giá trị SAI KIỂU (bản BYD lưu `camera_zoom` Int, tệp sửa tay ghi chuỗi). Yêu cầu:
 * nhập được, không ném, không báo lỗi kiểu cho dữ liệu của tính năng đã gỡ, và không khoá BYD nào lọt vào kế hoạch ghi.
 */
class BydDeadPrefsTest {

    private val cnSuffix = ProfileScope.snapshotSuffix("clusternav_prefs")

    private val bydClusterNavShot: Map<String, Any?> = linkedMapOf(
        // còn dùng trên Android box
        "voicekey_enabled" to true,
        "voicekey_bindings" to "[{\"k\":328,\"t\":\"__KACHI_VOICE__\"},{\"k\":24,\"t\":\"ctl:fan:up\",\"s\":2}]",
        "headless_autostart" to true,
        "nav_automation_rules" to "R1",
        "theme_choice" to "dark",
        // dẫn đường cụm/HUD + ép giá trị
        "enabled" to true, "marquee" to false, "nav_cluster_screen_mode" to 3, "source_mode" to 0, "lane" to true,
        "interpolate" to true, "hud" to false, "acc_booster" to true, "anim_opt" to true,
        // biển báo + bong bóng
        "badge_enabled" to true, "show_upcoming_badge" to true, "show_alert_chip" to false, "badge_size_dp" to 96,
        "badge_center_x" to 1780, "badge_center_y" to 80, "vm_bubble_enabled" to true, "vm_bubble_hidden" to false,
        "vm_bubble_x" to 10, "vm_bubble_y" to 20,
        // camera (cũ + từng camera 2.93), một giá trị sai kiểu
        "camera_signal_enabled" to true, "camera_on_cluster" to false, "camera_pos_left" to "TL", "camera_shape" to "RECT",
        "camera_zoom" to "120", "camera_projection_left" to "WIDE", "camera_size_rear" to 300, "camera_xy_front" to "500,500",
        // tiện nghi + sấy kính
        "seat_comfort_enabled" to true, "seat_comfort_mode" to 1, "seat_level_0" to 2, "seat_level_3" to 1,
        "pm25_filter_enabled" to true, "recirc_on_start_enabled" to true, "rain_defrost_front" to true,
    )

    private fun bydFile(kind: ProfileTransfer.Kind): String = ProfileTransfer.encodeHeader("Seal của tôi", kind) + "\n" +
        PrefSnapshot.encode(
            linkedMapOf(
                "preset" to "THREE", "dock_edge" to "BOTTOM", "dock_enabled" to "launcher_apps,ac_temp,win_lf,mac_leave",
                "top_strip" to "chip_energy,tyre_p_fl", "top_strip_labels" to true, "top_strip_migrated_ux5b" to true,
                "unit_prefs" to "p=bar;t=C", "slot_0" to "widget:w_board", "slot_1" to "app:com.google.android.youtube",
                "theme_mode" to "NIGHT", "saved_places" to "Nhà|1 Lê Lợi||",
                cnSuffix to PrefSnapshot.encode(bydClusterNavShot),
                ProfileScope.snapshotSuffix("simple_cast_prefs") to PrefSnapshot.encode(mapOf("cast_enabled" to true, "last_display_id" to 4)),
                ProfileScope.snapshotSuffix("cast-v2-app-catalog") to PrefSnapshot.encode(mapOf("bubbleX" to 5)),
                "@hậu tố lạ" to "x",
            ),
        )

    @Test
    fun `tep Kachi BYD 2_97 nhap duoc, khoa BYD bi bo im lang`() {
        ProfileTransfer.Kind.values().forEach { kind ->
            val plan = ProfileTransfer.planImport(bydFile(kind), null, emptyList())!!
            assertEquals(emptyList<String>(), plan.dropped, "$kind: dữ liệu tính năng đã gỡ không phải lỗi kiểu")
            assertEquals(ProfileScope.LAUNCHER_SUFFIXES.toSet(), plan.writes.keys, "$kind: kế hoạch chỉ phủ hậu tố còn sống")
            listOf("top_strip", "top_strip_labels", "top_strip_migrated_ux5b", "unit_prefs").forEach {
                assertFalse(it in plan.writes, "$kind: hậu tố $it đã gỡ")
            }
            assertEquals("THREE", plan.writes["preset"], "$kind: bố cục nhập như cũ")
            val shot = PrefSnapshot.decode(plan.writes[cnSuffix] as String)
            val scope = ProfileScope.CLUSTERNAV_KEYS.getValue("clusternav_prefs")
            assertTrue(shot.keys.all { it in scope }, "$kind: ảnh chỉ còn khoá trong phạm vi — ${shot.keys - scope.toSet()}")
            assertTrue(shot.keys.none(BydDeadPrefs::isDeadClusterNavKey), "$kind: không khoá BYD nào lọt")
            assertEquals(bydClusterNavShot["voicekey_bindings"], shot["voicekey_bindings"], "$kind: phím vẫn đi theo")
            assertEquals(true, shot["headless_autostart"])
        }
        assertEquals("Nhà|1 Lê Lợi||", ProfileTransfer.planImport(bydFile(ProfileTransfer.Kind.FULL), null, emptyList())!!
            .writes["saved_places"], "bản đầy đủ giữ sổ địa chỉ")
    }

    @Test
    fun `moi khoa trong pham vi con song khong bi coi la khoa chet`() {
        val live = SettingsCatalog.CLUSTERNAV_KEYS.keys + SettingsCatalog.CLUSTERNAV_HIDDEN_KEYS.keys +
            ProfileScope.CLUSTERNAV_PROFILE_KEYS + ProfileScope.DEVICE_KEYS.keys + ProfileScope.TRANSIENT_KEYS.keys
        assertEquals(emptyList<String>(), live.filter(BydDeadPrefs::isDeadClusterNavKey), "lượt dọn sẽ xoá khoá còn dùng")
        val profileKeys = ProfileScope.LAUNCHER_SUFFIXES.map { "Mặc định__$it" }
        assertEquals(emptyList<String>(), profileKeys.filter(BydDeadPrefs::isDeadLauncherKey), "lượt dọn sẽ xoá hậu tố còn dùng")
        assertFalse(BydDeadPrefs.MARK in BydDeadPrefs.DEAD_FILES)
        val liveFiles = SettingsCatalog.PREFS_FILES.keys + SettingsCatalog.CLUSTERNAV_PREFS_FILES.keys
        assertEquals(emptySet<String>(), BydDeadPrefs.DEAD_FILES.toSet() intersect liveFiles, "không xoá tệp prefs còn dùng")
    }

    @Test
    fun `khoa chet theo ten, tien to va hau to ho so`() {
        bydClusterNavShot.keys.filter { it !in setOf("voicekey_enabled", "voicekey_bindings", "headless_autostart", "nav_automation_rules", "theme_choice") }
            .forEach { assertTrue(BydDeadPrefs.isDeadClusterNavKey(it), "khoá $it phải bị dọn") }
        listOf("bubble_auto", "nav_verbose_log", "mod_123456", "camera_rot_left", "doze_whitelist_applied", "vm_float_whitelist_applied")
            .forEach { assertTrue(BydDeadPrefs.isDeadClusterNavKey(it), it) }
        listOf("A__top_strip", "Vợ__unit_prefs", "x_y__top_strip_labels", "A____cn__simple_cast_prefs", "B____cn__cast-v2-app-catalog")
            .forEach { assertTrue(BydDeadPrefs.isDeadLauncherKey(it), it) }
        listOf("A__preset", "A____cn__clusternav_prefs", "profiles", "A__dock_enabled")
            .forEach { assertFalse(BydDeadPrefs.isDeadLauncherKey(it), it) }
    }

    @Test
    fun `anh chup va so da-rot bo khoa chet, giu khoa song, khong doi gi thi tra null`() {
        val cleaned = BydDeadPrefs.cleanSnapshot(bydClusterNavShot)!!
        assertEquals(setOf("voicekey_enabled", "voicekey_bindings", "headless_autostart", "nav_automation_rules", "theme_choice"), cleaned.keys)
        assertNull(BydDeadPrefs.cleanSnapshot(cleaned), "ảnh sạch ⇒ null (giữ nguyên byte)")
        assertTrue(BydDeadPrefs.isDeadLedgerEntry("clusternav_prefs/camera_zoom"))
        assertFalse(BydDeadPrefs.isDeadLedgerEntry("clusternav_prefs/voicekey_bindings"))
        assertFalse(BydDeadPrefs.isDeadLedgerEntry("kachi_workspace/enabled"), "chỉ khoá của clusternav_prefs")
    }

    @Test
    fun `tap hoi lai bo ma nut xe cu, giu ma launcher`() {
        assertEquals(setOf("profile", "media_query"), BydDeadPrefs.cleanConfirmIds(setOf("profile", "control:sunroof", "macro:mac_win_open_all", "media_query")))
        assertNull(BydDeadPrefs.cleanConfirmIds(setOf("profile")))
        assertNull(BydDeadPrefs.cleanConfirmIds(emptySet()))
    }

    @Test
    fun `dau don la khoa theo XE va khong phai cai dat`() {
        assertEquals(ProfileScope.Scope.DEVICE, ProfileScope.scopeOf(BydDeadPrefs.MARK))
        assertTrue(BydDeadPrefs.MARK in SettingsCatalog.NOT_SETTINGS)
    }
}
