package com.byd.clusternav.launcher

import com.byd.clusternav.testsupport.KotlinSource
import com.byd.clusternav.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 2.93 nhóm WIDGET (spec `docs/specs/kachi-293-widget.html`) — bài canh NỐI DÂY: luật thuần đã có bài hành vi ở `:core`; ở
 * đây khoá rằng tầng vẽ GỌI đúng luật ấy (CLAUDE.md §8 — "hàm mới có chỗ gọi") và các quyết định tầng vẽ không lùi.
 */
class Widget293WiringContractTest {

    private fun code(name: String) = SourceRoots.codeOf("src/main/java/com/byd/clusternav/launcher/$name")
    private fun strip(t: String) = KotlinSource.stripComments(t)

    /** Có ít nhất một chỗ gọi [token] ở mã :app/:core đã bỏ chú thích, ngoài tệp khai báo [except]. */
    private fun called(token: String, except: String): Boolean = SourceRoots.moduleSourceRoots().any { root ->
        java.nio.file.Files.walk(root).use { s ->
            s.filter { it.toString().endsWith(".kt") && it.fileName.toString() != except }.anyMatch { strip(it.toFile().readText()).contains(token) }
        }
    }

    @Test
    fun `suc chua o - bo chon noi, drawer nhan co khung that, chu du 5 tieng`() {
        assertNull(WidgetCapacity.say(null, 6))
        assertNull(WidgetCapacity.say(WidgetCapacity.Hint(fits = true, capacity = 8), 6), "vừa ⇒ im lặng")
        assertNull(WidgetCapacity.say(WidgetCapacity.Hint(fits = false, capacity = 6), 6), "sức chứa ≥ số chọn ⇒ không nói câu tự mâu thuẫn")
        assertEquals(4, WidgetCapacity.say(WidgetCapacity.Hint(fits = false, capacity = 4), 6))
        assertEquals(0, WidgetCapacity.say(WidgetCapacity.Hint(fits = false, capacity = 0), 6), "không nổi một mục")
        val open = SourceRoots.body(code("DrawerController.kt"), "fun open(index: Int)")
        assertTrue("fitOf = slotFrame(index)?.let { (w, h) -> { ids: List<String> -> WidgetCapacity.of(activity, ids, w, h) } }" in open)
        assertTrue("slotFrame = { workspace.slotFrame(it) }" in code("KachiHomeActivity.kt"))
        assertTrue("if (selected.size < cap) scheduleFitHint(v)" in SourceRoots.body(code("AppDrawer.kt"), "private fun refreshPlaceBtn()"))
        listOf("values", "values-en", "values-zh-rCN", "values-th", "values-ms").forEach { dir ->
            val xml = SourceRoots.text("src/main/res/$dir/strings_kachi.xml")
            val hint = Regex("""<string name="kachi_drawer_fit_hint">([^<]+)</string>""").find(xml)?.groupValues?.get(1)
            assertTrue(hint != null && "%1\$d" in hint && "%2\$d" in hint, "$dir: kachi_drawer_fit_hint")
            assertTrue("<string name=\"kachi_drawer_fit_none\">" in xml, "$dir: kachi_drawer_fit_none")
        }
    }

}
