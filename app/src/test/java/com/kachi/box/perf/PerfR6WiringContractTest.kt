package com.kachi.box.perf

import com.kachi.box.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 2.98 · R4 + R6 — dây nối của các sửa hiệu năng dài hạn (docs/diagnostics/perf-inventory-2026-10-08.md). Luật thuần ở
 * `:core` `SlotVdNameTest`; ở đây khoá call site thật (CLAUDE.md §8).
 */
class PerfR6WiringContractTest {

    private fun code(p: String) = SourceRoots.codeOf("src/main/java/com/kachi/box/$p")

    /** R4 [ĐO máy ảo 08/10]: tên có dấu thời gian ⇒ +124 B `display_settings.xml` mỗi lần dựng ô, vĩnh viễn. */
    @Test
    fun `man ao o dat ten on dinh qua SlotVdName, khong dau thoi gian`() {
        val host = code("launcher/VdAppHost.kt")
        assertTrue(host.contains("SlotVdName.pick(slot, SlotVdOwner.liveNames())"))
        assertFalse(host.contains("kachi-slot-\$slot-\${System.currentTimeMillis()}"), "tên có dấu thời gian quay lại ⇒ tệp phình")
        // §6: khoá xoay (bug "YouTube co vào giữa") vẫn chạy y nguyên, cùng thứ tự.
        val lock = host.indexOf("wm set-user-rotation lock -d \$displayId 0")
        val fix = host.indexOf("wm set-fix-to-user-rotation -d \$displayId enabled")
        assertTrue(lock in 0 until fix, "hai lệnh khoá xoay giữ nguyên thứ tự")
        assertTrue(code("launcher/SlotVdOwner.kt").contains("fun liveNames(): Set<String>"))
    }

}
