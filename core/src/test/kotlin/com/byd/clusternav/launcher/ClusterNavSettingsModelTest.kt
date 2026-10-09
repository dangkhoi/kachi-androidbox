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
 *  • phím vô-lăng: tắt thì không hỏi `bound` (Android box B2 · W4: phần tự-chiếu, cỡ biển báo, chế độ cụm gỡ cùng mã).
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
}
