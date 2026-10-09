package com.byd.clusternav.launcher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ Android box B2 · W4 — chữ NGƯỜI DÙNG THẤY không còn tên hãng xe / sản phẩm cũ ════════════════════════════════════
 *
 * Kachi Android box không liên kết với hãng nào và không chạy trên đầu BYD DiLink. Mọi chữ hiển thị/đọc lên đi qua ba
 * nguồn ở phía `:core`: lời gọi dịch có chữ cố định ([I18nPairs.scanned] — quét cả `:app` + `:core`), dữ liệu lúc chạy
 * ([I18nPairs.runtime] — nhãn danh mục · nhóm Cài đặt · quyền · bộ chọn · câu "tính năng đã bỏ") và ba bảng dịch
 * `i18n/{zh,th,ms}.tsv`. Tài nguyên `values-…/strings….xml` do `LauncherI18nLocalesContractTest` (`:app`) canh.
 *
 * Tên lớp/gói (`com.byd.clusternav`) và chú thích không thuộc phạm vi (backlog `BOX-RENAME-PACKAGE`).
 */
class UserFacingBrandTest {

    private val forbidden = listOf("BYD", "DiLink", "ClusterNav")

    private fun hits(text: String): List<String> = forbidden.filter { text.contains(it, ignoreCase = true) }

    @Test
    fun `loi goi dich va du lieu luc chay khong nhac BYD hay DiLink`() {
        val rows = I18nPairs.scanned() + I18nPairs.runtime()
        assertTrue(rows.size > 150, "bộ quét hụt (${rows.size} cặp)")
        val bad = rows.filter { hits(it.vi + " " + it.en).isNotEmpty() }.map { "${it.where}: ${it.vi} | ${it.en}" }
        assertEquals(emptyList<String>(), bad, "chữ người dùng thấy còn tên hãng/sản phẩm cũ")
    }

    @Test
    fun `bang dich zh th ms khong nhac BYD hay DiLink`() {
        listOf("zh", "th", "ms").forEach { lang ->
            val text = javaClass.getResourceAsStream("/i18n/$lang.tsv")!!.bufferedReader().readText()
            val bad = text.lines().filter { it.isNotBlank() && !it.startsWith("#") && hits(it).isNotEmpty() }
            assertEquals(emptyList<String>(), bad, "i18n/$lang.tsv còn dòng nhắc tên hãng/sản phẩm cũ")
        }
    }

    @Test
    fun `nut noi chuyen goi Kachi, khong goi xe`() {
        val voice = LauncherActions.byId(LauncherActions.VOICE)!!
        assertEquals("Nói với Kachi", voice.label)
        assertEquals("Talk to Kachi", voice.labelEn)
        assertEquals("Theo máy", I18nPairs.inLang(Lang.VI) { LangMode.AUTO.label() })
    }
}
