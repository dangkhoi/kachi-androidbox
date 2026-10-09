package com.kachi.box.launcher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ S4 · R3/R4 — BÀI CANH: mọi khoá lưu bền phải được **xếp loại** ══════════════════════════════════════════
 *
 * Đây là phép kiểm mà spec R3 đòi: *"thêm mục mới mà không xếp loại thì đỏ"*. Nó đối chiếu với **mã** (danh mục cài
 * đặt + bảng khoá ClusterNav), không với văn xuôi của spec — nên không có cách nào thêm một cấu hình mà lặng lẽ bỏ
 * nó ra ngoài bộ chụp–áp.
 *
 * Hỏng kiểu này **im lặng trên xe**: đổi hồ sơ xong một nửa cấu hình đi theo, nửa kia thì không; không log, không
 * crash, chỉ có người lái thấy "nó không nhớ". Nên chốt ở đây, lúc biên dịch còn chưa xong.
 */
class ProfileScopeTest {

    // ── 1 · Phủ: không khoá nào được rơi ra ngoài ────────────────────────────────────────────────

    @Test
    fun `moi khoa cua danh muc cai dat deu co pham vi xac dinh`() {
        val keys = SettingsCatalog.ENTRIES.mapNotNull { it.prefKey }.toSet()
        assertTrue(keys.isNotEmpty(), "danh mục rỗng thì bài này vô nghĩa — chốt luôn để nó không tự xanh")
        assertEquals(
            emptySet<String>(), ProfileScope.unclassified(keys),
            "mục cài đặt MỚI phải được xếp vào ProfileScope (theo hồ sơ / theo xe / tạm) — xem KDoc ProfileScope",
        )
    }

    @Test
    fun `moi khoa ClusterNav deu co pham vi xac dinh`() {
        assertEquals(
            emptySet<String>(), ProfileScope.unclassified(SettingsCatalog.CLUSTERNAV_KEYS.keys),
            "khoá ClusterNav mới phải được xếp loại — không thì đổi hồ sơ bỏ sót đúng khoá đó, im lặng",
        )
    }

    /** Khoá **cố ý không phải cấu hình** vẫn phải có phạm vi: bộ dọn hồ sơ cần biết nó có được xoá theo không. */
    @Test
    fun `moi khoa luu ben KHONG phai cai dat cung co pham vi xac dinh`() {
        assertEquals(emptySet<String>(), ProfileScope.unclassified(SettingsCatalog.NOT_SETTINGS.keys))
    }

    @Test
    fun `khoa la thi tra ve UNKNOWN chu khong doan bua`() {
        assertEquals(ProfileScope.Scope.UNKNOWN, ProfileScope.scopeOf("khoa_chua_ai_khai"))
        assertEquals(ProfileScope.Scope.UNKNOWN, ProfileScope.scopeOf(""))
    }

    // ── 2 · Xếp đúng chỗ (R3 vs R4) ─────────────────────────────────────────────────────────────

    @Test
    fun `bo cuc o thanh nut chip va lua chon ca nhan deu THEO HO SO`() {
        listOf(
            "preset", "grid_layout", "dock_edge", "dock_enabled", "dock_visible", "slot_0", "slot_5",
            "theme_mode", "wallpaper_prefs", "launcher_autostart", "lang",
            // Sổ địa chỉ (docs/specs/kachi-voice-addresses.html R1): *"nhà"* của người này không phải *"nhà"* của
            // người kia — khoá theo XE ở đây nghĩa là đổi hồ sơ mà câu *"về nhà"* vẫn dẫn về nhà người trước.
            "saved_places",
            // VISUAL-REFRESH P1b · R8 (AC8.4): màu nhấn/tông thẻ là lựa chọn của MỘT người, như `theme_mode`.
            "color_choice",
        ).forEach { assertEquals(ProfileScope.Scope.PROFILE, ProfileScope.scopeOf(it), "khoá $it") }
    }

    @Test
    fun `cau hinh ClusterNav con dung THEO HO SO`() {
        listOf(
            "voicekey_enabled", "voicekey_bindings", "voicekey_custom_buttons", "headless_autostart", "theme_choice",
            "voice_music_default_app", "nav_automation_rules",
        ).forEach { assertEquals(ProfileScope.Scope.PROFILE, ProfileScope.scopeOf(it), "khoá $it") }
    }

    /**
     * Android box B2 · W4 — khoá của tính năng BYD đã gỡ (chiếu cụm · camera · biển báo · bong bóng · tiện nghi · sấy kính ·
     * chip · đơn vị · kiểm-từng-nút) KHÔNG còn bảng phạm vi nào nhận: [ProfileScope.scopeOf] ra UNKNOWN (không theo hồ sơ,
     * không theo xe), không vào ảnh chụp, không là hậu tố. Máy cũ được [BydDeadPrefs] dọn một lần; tệp `.kachi` cũ
     * mang chúng bị lượt nhập bỏ im lặng (`ProfileTransferTest`). Bài này canh chiều "không mọc lại vào phạm vi".
     */
    @Test
    fun `khoa BYD da go khong con pham vi nao nhan`() {
        val dead = listOf(
            "enabled", "nav_cluster_screen_mode", "marquee", "badge_enabled", "badge_size_dp", "vm_bubble_x", "vm_bubble_hidden",
            "camera_signal_enabled", "camera_pos_left", "camera_rot_rear", "seat_comfort_mode", "seat_level_0",
            "pm25_filter_enabled", "recirc_on_start_enabled", "rain_defrost_front", "cast_enabled", "cast_enabled_pending",
            "bubbleX", "last_display_id", "doze_whitelist_applied", "captest_results", "migrated_cluster_profile_v1",
            "config_size_com.byd.androidauto", "config_bounds_a.b__L30", "top_strip", "top_strip_labels",
            "top_strip_migrated_ux5b", "unit_prefs",
        )
        dead.forEach {
            assertEquals(ProfileScope.Scope.UNKNOWN, ProfileScope.scopeOf(it), "khoá $it đã gỡ mã — không được còn phạm vi")
            assertFalse(it in ProfileScope.CLUSTERNAV_PROFILE_KEYS, "khoá $it không được vào ảnh chụp")
            assertFalse(it in ProfileScope.LAUNCHER_SUFFIXES, "khoá $it không được là hậu tố hồ sơ")
        }
        listOf("simple_cast_prefs", "cast-v2-app-catalog").forEach {
            assertFalse(it in ProfileScope.CLUSTERNAV_KEYS, "tệp $it rời ảnh chụp")
            assertFalse(ProfileScope.snapshotSuffix(it) in ProfileScope.LAUNCHER_SUFFIXES, "hậu tố ảnh $it không còn")
        }
        assertTrue(ProfileScope.DEVICE_KEY_PREFIXES.isEmpty(), "họ config_ chiếu cụm gỡ cùng mã")
        assertTrue(SettingsCatalog.CLUSTERNAV_DYNAMIC_KEY_PREFIXES.isEmpty(), "không còn tiền tố dựng động phía ClusterNav")
    }

    /**
     * R4 — phần cứng/hệ thống/bộ máy hồ sơ. Chép chúng theo hồ sơ hoặc là vô nghĩa (danh sách hồ sơ nằm trong hồ
     * sơ?), hoặc **mất dữ liệu** (xoá một hồ sơ mà mất lịch sử mở app của cả xe).
     */
    @Test
    fun `danh sach ho so, lich su mo app, dau don va co he thong deu THEO XE`() {
        listOf(
            "profiles", "active_profile", "boot_profile", "migrated_scenes_v1", "recent_apps",
            "freeform_state", "enable_freeform_support", BydDeadPrefs.MARK,
        ).forEach { assertEquals(ProfileScope.Scope.DEVICE, ProfileScope.scopeOf(it), "khoá $it") }
    }

    /**
     * `voicekey_learn` là cờ **bật-một-lần** rồi dịch vụ tự tắt. Chép nó theo hồ sơ ⇒ đổi hồ sơ là máy vào chế độ
     * học phím mà không ai bấm gì; xếp theo xe thì sai nghĩa (nó không phải cấu hình của xe).
     */
    @Test
    fun `co tam va du lieu canh doi cu la TRANSIENT — khong chep, khong giu`() {
        listOf("voicekey_learn", "scenes", "boot_scene").forEach {
            assertEquals(ProfileScope.Scope.TRANSIENT, ProfileScope.scopeOf(it), "khoá $it")
        }
        assertFalse(ProfileScope.isProfileScoped("voicekey_learn"))
    }

    // ── 3 · Bất biến giữa các danh sách ──────────────────────────────────────────────────────────

    /**
     * ⚠⚠ Khoá theo XE **không được** có mặt trong [ProfileScope.LAUNCHER_SUFFIXES]: danh sách đó là thứ `deleteProfile`
     * dùng để dọn sạch, nên một khoá của cả xe lọt vào đây nghĩa là **xoá một hồ sơ làm mất lựa chọn của cả xe**.
     */
    @Test
    fun `khoa theo xe khong bao gio nam trong hau to theo ho so`() {
        val leaked = ProfileScope.LAUNCHER_SUFFIXES.filter { it in ProfileScope.DEVICE_KEYS }
        assertEquals(emptyList<String>(), leaked, "xoá một hồ sơ mà mất cấu hình của cả xe — lỗi tệ hơn lỗi đang vá")
    }

    @Test
    fun `khong hau to nao bi khai hai lan`() {
        val dup = ProfileScope.LAUNCHER_SUFFIXES.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertEquals(emptySet<String>(), dup)
    }

    /** R1 — hai khoá của "cảnh" đã ra khỏi bộ theo-hồ-sơ; chúng chỉ còn sống tới lượt chuyển đổi. */
    @Test
    fun `hau to theo ho so KHONG con scenes va boot_scene`() {
        assertFalse("scenes" in ProfileScope.LAUNCHER_SUFFIXES)
        assertFalse("boot_scene" in ProfileScope.LAUNCHER_SUFFIXES)
    }

    /** `slot_*` **sinh theo** SLOT_CAP, không chép tay — trần ô đã đổi một lần (4 → 6). */
    @Test
    fun `o duoc sinh theo SLOT_CAP, khong chep tay`() {
        val slots = ProfileScope.LAUNCHER_SUFFIXES.filter { it.startsWith(SettingsCatalog.SLOT_KEY_PREFIX) }
        assertEquals(
            (0 until WorkspaceState.SLOT_CAP).map { "${SettingsCatalog.SLOT_KEY_PREFIX}$it" }, slots,
            "nới trần ô mà danh sách này không tự theo thì hai ô cuối thành khoá mồ côi, im lặng",
        )
    }

    /** Ảnh chụp cũng **sinh theo** bảng khoá: thêm một tệp prefs ClusterNav là có ngay một hậu tố ảnh chụp. */
    @Test
    fun `moi tep ClusterNav theo ho so co dung mot hau to anh chup`() {
        val expected = ProfileScope.CLUSTERNAV_KEYS.keys.map { "${ProfileScope.SNAPSHOT_INFIX}$it" }
        assertEquals(expected, ProfileScope.SNAPSHOT_SUFFIXES)
        assertTrue(ProfileScope.LAUNCHER_SUFFIXES.containsAll(expected), "ảnh chụp phải bị xoá theo khi xoá hồ sơ")
        assertEquals("__cn__clusternav_prefs", ProfileScope.snapshotSuffix("clusternav_prefs"))
    }

    /**
     * Bảng ảnh chụp phải **sinh từ** [SettingsCatalog.CLUSTERNAV_KEYS], không chép tay: chép tay thì thêm một khoá
     * ClusterNav ở bản sau sẽ có mặt ở bảng kia mà vắng ở đây ⇒ đổi hồ sơ bỏ sót đúng khoá mới.
     */
    @Test
    fun `bang anh chup la bang khoa ClusterNav tru theo-xe, tam, va khoa launcher da so huu`() {
        val expected = SettingsCatalog.CLUSTERNAV_KEYS.keys -
            ProfileScope.DEVICE_KEYS.keys - ProfileScope.TRANSIENT_KEYS.keys -
            ProfileScope.LAUNCHER_OWNED_CLUSTERNAV_KEYS.keys +
            // +1 (2026-09-28): khoá TRẠNG THÁI theo hồ sơ, cố ý không có trong danh mục Cài đặt vì không có hàng
            // trên UI. Vẫn phải cộng vào đây, nếu không nó rơi khỏi ảnh chụp và đổi hồ sơ là mất sổ đã-dẫn.
            ProfileScope.CLUSTERNAV_PROFILE_STATE_KEYS.keys
        assertEquals(expected, ProfileScope.CLUSTERNAV_PROFILE_KEYS)
        assertTrue("voicekey_learn" !in ProfileScope.CLUSTERNAV_PROFILE_KEYS)
    }

    /**
     * Hai khoá của *tự dẫn đường theo lịch* phải ở **CÙNG** phạm vi, và phạm vi đó là HỒ SƠ (owner 2026-09-28).
     *
     * Tách chúng ra là mời hỏng theo cả hai chiều: luật theo hồ sơ mà dấu đã-dẫn theo xe ⇒ đổi hồ sơ là dẫn lại
     * lần hai trong cùng khung giờ; ngược lại thì sổ đã-dẫn đọc rỗng ở hồ sơ mới, cũng dẫn hai lần.
     */
    @Test
    fun `luat lich va so da-dan CUNG mot pham vi HO SO`() {
        listOf("nav_automation_rules", "nav_automation_fired").forEach {
            assertEquals(
                ProfileScope.Scope.PROFILE, ProfileScope.scopeOf(it),
                "$it phải theo HỒ SƠ — hồ sơ 'Trip Đà Lạt' cần lịch khác hồ sơ đi làm hằng ngày",
            )
            assertTrue(
                it in ProfileScope.CLUSTERNAV_PROFILE_KEYS,
                "$it phải nằm trong bảng ảnh chụp, nếu không đổi hồ sơ sẽ không mang nó theo",
            )
        }
        assertEquals(
            listOf("clusternav_prefs"),
            ProfileScope.CLUSTERNAV_PROFILE_STATE_KEYS.values.distinct(),
            "sổ đã-dẫn phải được chụp từ ĐÚNG tệp mà dịch vụ đọc nó; sai tệp thì đọc rỗng, im lặng, dẫn hai lần",
        )
    }

    @Test
    fun `moi tep trong bang anh chup deu la tep prefs da khai ly do`() {
        val declared = SettingsCatalog.CLUSTERNAV_PREFS_FILES.keys
        val unknown = ProfileScope.CLUSTERNAV_KEYS.keys.filterNot { it in declared }
        assertEquals(emptyList<String>(), unknown, "ảnh chụp ghi vào một tệp chưa ai khai = ghi vào hư không")
    }

    /**
     * ⚠ `lang` **không** được nằm ở cả hai đường: nó đã là hậu tố theo hồ sơ của launcher. Hai chỗ cùng nhớ một lựa
     * chọn là **bẫy hai-bản-sao** mà dự án đã trả giá bốn lần.
     */
    @Test
    fun `khoa launcher da so huu khong di qua anh chup`() {
        assertTrue("lang" in ProfileScope.LAUNCHER_SUFFIXES)
        assertFalse("lang" in ProfileScope.CLUSTERNAV_PROFILE_KEYS)
        assertEquals(ProfileScope.Scope.PROFILE, ProfileScope.scopeOf("lang"))
    }

    /** Mọi danh sách loại trừ phải kèm **lý do** — không thì chúng thành chỗ làm im bài test. */
    @Test
    fun `moi khoa bi xep rieng deu kem ly do`() {
        listOf(
            ProfileScope.DEVICE_KEYS, ProfileScope.DEVICE_KEY_PREFIXES, ProfileScope.TRANSIENT_KEYS,
            ProfileScope.PROFILE_KEY_PREFIXES, ProfileScope.LAUNCHER_OWNED_CLUSTERNAV_KEYS,
        ).forEach { table ->
            assertTrue(table.values.all { it.isNotBlank() }, "thiếu lý do: $table")
        }
    }
}
