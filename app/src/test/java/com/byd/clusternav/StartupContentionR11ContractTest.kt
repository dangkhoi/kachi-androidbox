package com.byd.clusternav

import com.byd.clusternav.launcher.voice.WakeKeywordsSync
import com.byd.clusternav.launcher.voice.WakeModelCatalog
import com.byd.clusternav.testsupport.SourceRoots
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ 2.96 R11 — khởi động sau nổ máy: dây nối `:app` của ba bản vá (luật thuần ở `:core`) ═══════════════════════════════════
 *
 * Số đo + dòng thời gian: `docs/diagnostics/startup-timeline-2026-10-07.md`. Mỗi bài khoá một lỗi [ĐO log xe 07/10]:
 *  1. TAT-MAY-CAST-HOLD — tiến trình bật lúc màn tắt chiếu trọn rồi mới bị lượt chữa phím giết ⇒ tiến trình con gỡ + chiếu lại.
 *  2. WAKE-RECOPY-LOOP — mỗi lần nổ máy xoá + chép 5 MB + nạp lại bộ nghe "Hey Kachi".
 *  3. PIP-QUERY-INSTALLED — 5 `appops get` lúc lượt tự chiếu mở (Android box B2 · W2c: gỡ cùng chiếu cụm).
 */
class StartupContentionR11ContractTest {

    private val heal = SourceRoots.codeOf("src/main/java/com/byd/clusternav/A11yLifecycleHeal.kt")
    private val wake = SourceRoots.codeOf("src/main/java/com/byd/clusternav/launcher/ClusterNavBridgeWake.kt")

    private fun order(src: String, vararg tokens: String) {
        val at = tokens.map { t -> src.indexOf(t).also { assertTrue(it >= 0, "thiếu '$t'") } }
        assertEquals(at.sorted(), at, "thứ tự sai: ${tokens.joinToString(" → ")}")
    }

    // ─── 1 · TAT-MAY-CAST-HOLD ─────────────────────────────────────────────────────────────────────────────────

    @Test
    fun `lop 1 xep luot thang - khong con giu tu chieu (Android box W1)`() {
        // Android box B2 · W1 — móc `TatMayCastHold` (tự chiếu cụm chờ lớp 1 kết luận) gỡ: không còn chiếu cụm.
        val install = SourceRoots.body(heal, "fun install(ctx: Context) {")
        order(install, "val interactive = interactive(app)", "submit(\"khởi động (tương tác=\$interactive)\") { onProcessStart(app, interactive, startedAt) }")
        assertFalse("TatMayCastHold" in install, "không còn móc giữ tự chiếu")
    }

    /** Android box B2 · W2c — `TatMayCastHold` (+ móc giữ tự chiếu của nút nổi) gỡ hẳn cùng chiếu cụm. */
    @Test
    fun `tep giu tu chieu da xoa`() {
        listOf("TatMayCastHold.kt", "HealCastWait.kt", "modules/clustercast/BubbleAutostart.kt").forEach {
            assertFalse(SourceRoots.exists("src/main/java/com/byd/clusternav/$it"), "$it còn")
        }
    }

    @Test
    fun `A11yLifecycleHeal van duoi tran 500 dong`() {
        val n = SourceRoots.text("src/main/java/com/byd/clusternav/A11yLifecycleHeal.kt").lines().size
        assertTrue(n <= 500, "A11yLifecycleHeal.kt dài $n dòng")
    }

    // ─── 2 · WAKE-RECOPY-LOOP ──────────────────────────────────────────────────────────────────────────────────

    @Test
    fun `ensure so dia voi ban APK, khong voi ban ghim`() {
        val ensure = SourceRoots.body(wake, "fun ensure(app: Context) {")
        assertTrue("if (ready && !keywordsNeedRecopy(app)) return" in ensure)
        assertFalse("keywordsMatchPin(app)) return" in ensure, "so ghim là gốc vòng chép-lại mỗi lần nổ máy")
        val fn = SourceRoots.body(wake, "private fun keywordsNeedRecopy(app: Context): Boolean {")
        assertTrue("app.assets.open(\"\${WakeModelCatalog.ASSET_DIR}/\${WakeModelCatalog.KEYWORDS}\")" in fn)
        assertTrue("WakeKeywordsSync.needsRecopy(disk, shipped) { keywordsMatchPin(app) }" in fn)
    }

    /** Lần bật thứ HAI sau khi chép (đĩa = bản APK thật) KHÔNG được chép lại — lỗi 07/10 là luôn chép lại. */
    @Test
    fun `ban APK that - sau mot lan chep thi lan bat ke khong chep lai`() {
        val asset = File(SourceRoots.path("src/main/assets/${WakeModelCatalog.ASSET_DIR}/${WakeModelCatalog.KEYWORDS}").toString())
        assertTrue(asset.isFile, "thiếu ${asset.path}")
        val bytes = asset.readBytes()
        assertFalse(WakeKeywordsSync.needsRecopy(bytes.copyOf(), bytes) { false })
    }
}
