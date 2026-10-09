package com.kachi.box.launcher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ 2.93 · SETTINGS-DISPLAY-SUBTITLE — câu phụ rail của nhóm *Hiển thị* kể ĐỦ nội dung trang ═════════════════════
 *
 * [ĐO mã 05/10] câu cũ *"Đơn vị đo, sáng/tối và ngôn ngữ"* không nhắc Màu sắc (P1b) · độ trong suốt nền (2.87 R-OP) · cỡ
 * thanh nút (2.89 B3) — ba mục đã vào trang này sau khi câu được viết. Rail là thứ người dùng đọc để biết *"chỗ nào có
 * gì"* (KDoc [SettingsGroup.sub]); thiếu mục là giấu tính năng. Bài khoá: mỗi mục của trang có mặt trong câu phụ (VI + EN)
 * và câu vẫn trong trần hai dòng. Ba tiếng bảng (zh/th/ms) do `I18nCoverageTest` đòi cặp (vi, en) mới.
 */
class SettingsDisplaySubtitleTest {

    private val g = SettingsGroup.DISPLAY

    /** Mục của trang Hiển thị → từ khoá câu phụ phải nhắc (VI, EN). "Kính thật" là một dòng của mục sáng/tối. */
    // Android box B2 · W1 — mục `display_units` gỡ (đơn vị chỉ cho dữ liệu xe) ⇒ câu phụ thôi nhắc "Đơn vị" (bài dưới).
    private val mustMention = mapOf(
        "display_theme" to ("sáng/tối" to "light/dark"),
        "display_color" to ("màu" to "colours"),
        "display_bar_scale" to ("cỡ thanh nút" to "bar size"),
        "display_lang" to ("ngôn ngữ" to "language"),
    )

    @Test
    fun `cau phu ke du cac muc cua trang Hien thi`() {
        val ids = SettingsCatalog.entriesOf(g).map { it.id }.toSet()
        assertTrue(ids.containsAll(mustMention.keys), "trang Hiển thị đổi mục ($ids) — sửa câu phụ cùng lượt")
        mustMention.forEach { (id, words) ->
            assertTrue(g.sub.contains(words.first), "$id: câu phụ VI thiếu '${words.first}': ${g.sub}")
            assertTrue(g.subEn.contains(words.second), "$id: câu phụ EN thiếu '${words.second}': ${g.subEn}")
        }
        // Độ trong suốt nền (2.87 R-OP) là hàng của mục Màu — owner thấy thiếu đích danh ⇒ câu phụ nêu riêng.
        assertTrue(g.sub.contains("trong suốt") && g.subEn.contains("transparency"))
        assertTrue(g.sub.length <= SettingsCatalog.GROUP_SUB_MAX && g.subEn.length <= SettingsCatalog.GROUP_SUB_MAX)
    }

    @Test
    fun `cau cu da thay - khong con ban thieu muc`() {
        assertEquals("Kiểu sáng/tối, màu, độ trong suốt, cỡ thanh nút, ngôn ngữ", g.sub)
        assertEquals("Theme light/dark, colours, transparency, bar size, language", g.subEn)
    }

    @Test
    fun `khong con hua don vi khi muc don vi da go`() {
        assertTrue(SettingsCatalog.entriesOf(g).none { it.id == "display_units" }, "Android box B2 · W1: mục đơn vị đã gỡ")
        assertTrue("đơn vị" !in (g.label + g.sub).lowercase() && "unit" !in (g.labelEn + g.subEn).lowercase())
    }
}
