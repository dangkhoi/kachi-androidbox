package com.kachi.box.launcher

import com.kachi.box.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Android box B2 · W4 — lượt dọn MỘT LẦN khoá chết của Kachi BYD ([BydDeadPrefs] · `WorkspacePrefsBydCleanup.kt`).
 *
 * Phép thuần (khoá nào chết) canh ở `BydDeadPrefsTest` (`:core`); bài này canh DÂY NỐI: chạy ở `init` của
 * `PrefsWorkspaceRepository` SAU ba lượt di trú có sẵn (trước `load()` đầu tiên), dấu chặn mọi lần sau, dấu ghi SAU lượt
 * dọn (dọn lại là vô hại, đặt dấu trước mà chết máy giữa chừng thì khoá chết nằm mãi), và không thêm luồng/lệnh shell.
 */
class BydPrefsCleanupWiringContractTest {

    private val cleanup = SourceRoots.codeOf("src/main/java/com/kachi/box/launcher/WorkspacePrefsBydCleanup.kt")

    @Test
    fun `chay o init sau ba luot di tru co san`() {
        val init = SourceRoots.body(SourceRoots.codeOf("src/main/java/com/kachi/box/launcher/PrefsWorkspaceRepository.kt"), "init {")
        val fill = init.indexOf("prefs.fillNewProfileKeysOnce()")
        val clean = init.indexOf("prefs.cleanBydDeadPrefsOnce()")
        assertTrue(fill >= 0 && clean > fill, "lượt dọn phải ở init, ngay sau lượt rót khoá mới")
    }

    @Test
    fun `dau chan lan sau va ghi SAU luot don`() {
        val body = SourceRoots.body(cleanup, "internal fun WorkspacePrefs.cleanBydDeadPrefsOnce()")
        assertTrue(body.contains("stored[BydDeadPrefs.MARK] == true) return"), "dấu phải chặn ngay dòng đầu (0 chi phí lần sau)")
        val mark = body.indexOf("putBoolean(BydDeadPrefs.MARK, true)")
        listOf("deleteSharedPreferences(", "cnDead.forEach { e.remove(it) }", "wsDead.forEach { e.remove(it) }").forEach {
            val at = body.indexOf(it)
            assertTrue(at in 0 until mark, "`$it` phải chạy TRƯỚC khi đặt dấu")
        }
        assertTrue(!cleanup.contains("Thread(") && !cleanup.contains("exec(") && !cleanup.contains("ShellTransport"), "không luồng/lệnh shell mới")
    }
}
