package com.kachi.box.perf

import com.kachi.box.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 2.98 · R6 follow-up D/E/F/G/H — dây nối (CLAUDE.md §8: hàm mới phải có call site thật). Luật thuần ở `:core`:
 * `ShellLogGateTest` · `CastPrefsPruneTest` · `FileByteBudgetTest` · `UpdateApkSweepTest`.
 * Bằng chứng từng mục: docs/diagnostics/perf-inventory-2026-10-08.md §4.
 */
class PerfR6FollowupWiringContractTest {

    private fun code(p: String) = SourceRoots.codeOf("src/main/java/com/kachi/box/$p")

    /** E/G/H — một lượt dọn mỗi tiến trình, cài ở Application TRƯỚC dòng chốt cuối `EarlyShellChannel.start`. */
    @Test
    fun `EGH don mot lan moi tien trinh tu KachiApplication`() {
        val app = code("KachiApplication.kt")
        val at = app.indexOf("com.kachi.box.housekeeping.StartupHousekeeping.install(this)")
        val gate = app.indexOf("if (isBackgroundVoiceProcess()) return")
        val last = app.indexOf("EarlyShellChannel.start(this)")
        assertTrue(at > gate, "chỉ tiến trình launcher (sau cổng :tts/:wake)")
        assertTrue(at in 0 until last, "trước dòng chốt cuối EarlyShellChannel.start")

        val hk = code("housekeeping/StartupHousekeeping.kt")
        assertTrue(hk.contains("started.compareAndSet(false, true)"), "một lần mỗi tiến trình")
        assertTrue(hk.contains("Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)"))
        assertTrue(hk.contains("step(\"diag-cap\") { DiagStorageCap.enforce(app) }"), "G")
        assertTrue(hk.contains("step(\"ota-apk\") { UpdateApkHousekeeping.sweep(app) }"), "H")
        // Android box B2 · W1 — E (khoá hình học chiếu cụm) gỡ khỏi lượt dọn: không còn chiếu cụm.
        assertFalse(hk.contains("CastPrefsHousekeeping"), "E đã gỡ")
        assertFalse(Regex("""scheduleAtFixedRate|postDelayed|AlarmManager""").containsMatchIn(hk), "không nhịp định kỳ mới")
    }

    /** H — dọn APK OTA theo phán quyết thuần; cùng thư mục với đường tải. */
    @Test
    fun `H xoa APK OTA theo UpdateApkSweep`() {
        val sweep = code("housekeeping/UpdateApkHousekeeping.kt")
        assertTrue(sweep.contains("File(app.filesDir, UpdateChecker.UPDATE_DIR)"))
        assertTrue(sweep.contains("UpdateApkSweep.verdict("))
        assertTrue(sweep.contains("PackageQueries.archiveInfo(pm, f.absolutePath)"))
        assertTrue(sweep.contains("PackageQueries.packageInfo(pm, app.packageName)?.longVersionCode"))
        assertTrue(code("UpdateChecker.kt").contains("File(ctx.applicationContext.filesDir, UPDATE_DIR)"), "một nguồn tên thư mục")
    }

}
