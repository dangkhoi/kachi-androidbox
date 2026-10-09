package com.kachi.box.launcher

import com.kachi.box.testsupport.KotlinSource
import com.kachi.box.testsupport.SourceRoots
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ MÀN CLUSTERNAV CŨ ĐÃ GỠ — VÀ KHÔNG ĐƯỢC MỌC LẠI ════════════════════════════════════════════════════════
 *
 * Spec `docs/specs/kachi-remove-legacy-screen.html` **R1 · R5**. Ngày gỡ: 2026-09-13.
 *
 * ## Vì sao "đã xoá" cũng cần một bài canh
 * Xoá một màn thì trình biên dịch chỉ bắt được **lời gọi tường minh**. Ba đường còn lại im lặng:
 *  - một `Intent` dựng bằng tên lớp dạng chuỗi hoặc một `<activity>` khai lại trong manifest;
 *  - một tệp `MainActivity.kt` mới do người sau "khôi phục cho tiện đối chiếu";
 *  - `R.layout.activity_main` được inflate lại từ một màn khác — và tệp XML đó **vẫn nằm trên đĩa**
 *    (niêm phong T11), nên nó luôn ở đó chờ được dùng lại.
 * Ba đường đó đưa dự án về đúng trạng thái mà đợt này sinh ra để chấm dứt: hai bề mặt cấu hình song song,
 * ghi cùng một bộ khoá, lệch nhau dần.
 *
 * ## Layout cũ `activity_main.xml` — đã XOÁ (Android box B2 · W2a, 2026-10-09)
 * Trước đây tệp ở lại như hiện vật niêm phong T11 (hash ở `ExpansionTransportFenceTest` của `:offcar-planner`). Module
 * niêm phong gỡ cùng bộ đo xe BYD ⇒ tệp mồ côi bị xoá; bài [layout cu da bi xoa va khong ai inflate] canh nó không mọc lại.
 */
class LegacyScreenAbsenceContractTest {

    // ── R1: không còn lớp, không còn khai báo, không còn intent ─────────────────────────────────────────────

    @Test
    fun `khong con tep MainActivity nao trong cay nguon`() {
        assertFalse(
            SourceRoots.exists("src/main/java/com/kachi/box/MainActivity.kt"),
            "màn ClusterNav cũ đã gỡ 2026-09-13 — dựng lại một bản là quay về hai bề mặt cấu hình song song",
        )
    }

    @Test
    fun `manifest khong con khai bao activity MainActivity`() {
        val manifest = SourceRoots.text("src/main/AndroidManifest.xml")
        val declared = Regex("""android:name="\.MainActivity"""").containsMatchIn(manifest)
        assertFalse(declared, "manifest không được khai `.MainActivity` — lớp đó không còn tồn tại")
        // Chiều thứ hai: icon duy nhất vẫn là Kachi (SingleLauncherIconContractTest canh kỹ hơn; ở đây chỉ
        // chắc rằng việc gỡ màn cũ không vô tình mang theo activity còn lại).
        assertTrue(manifest.contains(".launcher.KachiHomeActivity"), "Kachi phải còn là activity chính")
    }

    @Test
    fun `khong mã nào dựng intent hay tham chieu toi man cu`() {
        val offenders = SourceRoots.moduleSourceRoots().flatMap { root ->
            Files.walk(root).use { paths ->
                paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }.toList()
            }.mapNotNull { file ->
                val code = KotlinSource.stripComments(file.toFile().readText())
                val hit = listOf("MainActivity::class", "R.layout.activity_main", "\"com.kachi.box.MainActivity\"")
                    .firstOrNull { it in code }
                hit?.let { "${file.fileName}: $it" }
            }
        }
        assertEquals(
            emptyList<String>(), offenders.sorted(),
            "còn mã trỏ tới màn cũ (intent tường minh / inflate layout cũ). Mọi đường đó nay phải về " +
                "`KachiHomeActivity`, kèm extra `open_settings_group` nếu có nhóm Cài đặt tương ứng:\n$offenders",
        )
    }

    @Test
    fun `bien the layout rong cua man cu da bien mat`() {
        assertFalse(
            SourceRoots.exists("src/main/res/layout-w960dp/activity_main.xml"),
            "bản `layout-w960dp` (bản xe thật render) không bị niêm phong ⇒ phải gỡ cùng màn; giữ lại là giữ " +
                "một tệp 500+ dòng không ai dựng và một bài parity không còn đối tượng",
        )
    }

    /**
     * Layout cũ đã xoá (W2a) — không được khôi phục, và không mã nào được tham chiếu `R.layout.activity_main`
     * (nếu tệp mọc lại, màn cũ sống dậy mà không cần tệp `MainActivity.kt` nào).
     */
    @Test
    fun `layout cu da bi xoa va khong ai inflate`() {
        assertFalse(SourceRoots.exists("src/main/res/layout/activity_main.xml"), "layout màn cũ đã xoá ở B2 · W2a — không khôi phục")
        val users = SourceRoots.moduleSourceRoots().flatMap { root ->
            Files.walk(root).use { paths ->
                paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }.toList()
            }.filter { "R.layout.activity_main" in it.toFile().readText() }.map { it.fileName.toString() }
        }
        assertEquals(emptyList<String>(), users.sorted(), "không mã nào được dùng layout màn cũ, nhưng đang có ở: $users")
    }
}
