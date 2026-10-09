package com.byd.clusternav.launcher

import com.byd.clusternav.launcher.ClusterNavSettingsModel.VkStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * IA v2 · T1 — quyết định thuần của các điều khiển ClusterNav dựng lại trong Kachi Settings.
 *
 * ## Bài nào đáng giá ở đây
 * Không phải bài "hàm trả về đúng cái nó viết" — mà bài khoá đúng **chỗ dễ chép sai** khi T4 dựng section:
 *  • mã lưu chủ đề là `"system"` chứ KHÔNG phải `"auto"` (đoán sai ⇒ `Choice.fromCode` lùi im lặng về SYSTEM, tức
 *    đúng-vì-may, và sẽ sai ngày có mã mới);
 *  • loại trừ tự-chiếu **bất đối xứng** (bật thì tắt cái kia, tắt thì không đụng) — chép thành đối xứng là mất trạng
 *    thái "cả hai cùng tắt";
 *  • dải cỡ biển báo đọc biên từ [BadgeLayout] chứ không viết cứng 60/240;
 *  • chế độ cụm chỉ có HAI nấc thật, và phép quy đổi lúc đọc phải giống `Prefs.navClusterScreenMode`.
 */
class ClusterNavSettingsModelTest {

    // ── Chủ đề ───────────────────────────────────────────────────────────────────────────────────

    @Test
    fun `ma luu chu de khop ThemeMode Choice cua man cu`() {
        assertEquals("light", ClusterNavSettingsModel.themeChoiceCode(ThemeMode.DAY))
        assertEquals("dark", ClusterNavSettingsModel.themeChoiceCode(ThemeMode.NIGHT))
        // ⚠ KHÔNG phải "auto": `ThemeMode.Choice` của ClusterNav chỉ có system/light/dark (ThemeMode.kt:26–30).
        assertEquals("system", ClusterNavSettingsModel.themeChoiceCode(ThemeMode.AUTO))
    }

    @Test
    fun `moi che do giao dien deu co ma luu, khong ca nao roi ra ngoai`() {
        // `when` đã exhaustive nên bài này canh thứ khác: ba mã phải KHÁC NHAU. Ánh xạ hai chế độ về cùng một mã là
        // cách hỏng im lặng nhất — nút vẫn bấm được, màn nâng cao vẫn đổi, chỉ là đổi sai chiều.
        val codes = ThemeMode.values().map { ClusterNavSettingsModel.themeChoiceCode(it) }
        assertEquals(codes.size, codes.distinct().size, "hai chế độ dùng chung một mã lưu: $codes")
        assertTrue(codes.all { it.isNotBlank() })
    }

    // ── Trạng thái phím vô-lăng ──────────────────────────────────────────────────────────────────

    @Test
    fun `ba trang thai phim vo-lang dung nhu man cu`() {
        assertEquals(VkStatus.ACTIVE, ClusterNavSettingsModel.voiceKeyStatus(enabled = true, bound = true))
        assertEquals(VkStatus.DISCONNECTED, ClusterNavSettingsModel.voiceKeyStatus(enabled = true, bound = false))
        assertEquals(VkStatus.OFF, ClusterNavSettingsModel.voiceKeyStatus(enabled = false, bound = false))
    }

    @Test
    fun `tat thi khong hoi tiep du dich vu con dang noi`() {
        // [ĐO] MainActivity.kt:998–1016 hỏi `!enabled` TRƯỚC. `NavAccessibilitySource.connected` là cờ của dịch vụ
        // hệ thống và nó còn `true` một lúc sau khi người dùng tắt ⇒ hỏi `bound` trước sẽ báo "ĐANG HOẠT ĐỘNG" cho
        // một tính năng vừa bị tắt.
        assertEquals(VkStatus.OFF, ClusterNavSettingsModel.voiceKeyStatus(enabled = false, bound = true))
    }

    // ── Chế độ cụm ───────────────────────────────────────────────────────────────────────────────

    @Test
    fun `chi hai nac che do cum, dung gia tri da do duoc`() {
        val opts = ClusterNavSettingsModel.clusterModeOptions()
        assertEquals(listOf(0, 3), opts.map { it.value }, "OFF=0 và FULL=3 (giá trị PROVEN rc=0) — không bày 1/2")
        assertTrue(opts.all { it.label.isNotBlank() && it.labelEn.isNotBlank() })
    }

    @Test
    fun `quy doi gia tri da luu giong het Prefs navClusterScreenMode`() {
        assertEquals(ClusterNavSettingsModel.NavClusterMode.OFF, ClusterNavSettingsModel.clusterModeOf(0))
        assertEquals(ClusterNavSettingsModel.NavClusterMode.ON, ClusterNavSettingsModel.clusterModeOf(3))
        // Giá trị đời cũ (SIMPLE=1 / SMALL=2) phải gộp về Bật — đúng phép quy đổi lúc ĐỌC của Prefs.kt:74–78. Bộ
        // chọn hiện một nấc còn runtime dùng nấc khác là sai IM LẶNG, chỉ lộ ra khi nhìn cụm.
        assertEquals(ClusterNavSettingsModel.NavClusterMode.ON, ClusterNavSettingsModel.clusterModeOf(1))
        assertEquals(ClusterNavSettingsModel.NavClusterMode.ON, ClusterNavSettingsModel.clusterModeOf(2))
        assertEquals(ClusterNavSettingsModel.NavClusterMode.ON, ClusterNavSettingsModel.clusterModeOf(99))
    }
}
