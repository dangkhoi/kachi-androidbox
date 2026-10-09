package com.kachi.box.launcher

import com.kachi.box.testsupport.KotlinSource
import com.kachi.box.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * ═══ FIX286 · R-PI — DÂY NỐI phía `:app` của lượt nhập hồ sơ ════════════════════════════════════════════════════════
 *
 * Android box B2 · W2c (2026-10-09): lượt merge khung chiếu cụm lúc nhập (`mergeImportedCast` · `ClusterSnapshotPlan.mergeImport`
 * · `ClusterImportSummary`) GỠ cùng chiếu cụm. Tệp `.kachi` của Kachi BYD mang ảnh `simple_cast_prefs` ⇒ ảnh đó bị bỏ im lặng
 * ở `ProfileTransfer.planImport` (bài chạy thật: `:core` `ProfileTransferTest`). Bài ở đây canh mã Android: lượt nhập vẫn
 * làm sạch TRƯỚC khi ghi, chỉ ghi khoá của hồ sơ MỚI, hộp thoại *Dùng hồ sơ này ngay* chỉ đổi hồ sơ — không còn đường merge
 * nào, không đường chiếu cụm nào mọc lại.
 */
class ProfileImportFix286WiringContractTest {

    private fun launcher(file: String) = SourceRoots.codeOf("src/main/java/com/kachi/box/launcher/$file")

    private val profileIo by lazy { launcher("WorkspacePrefsProfile.kt") }
    private val section by lazy { launcher("SettingsSectionsProfiles.kt") }
    private val repo by lazy { launcher("PrefsWorkspaceRepository.kt") }

    private val importSig = "internal fun WorkspacePrefs.importProfile(data: String, name: String? = null): ProfileImportReport?"

    @Test
    fun `nhap lam sach TRUOC roi moi ghi, chi ghi khoa cua ho so moi, log luc nhap`() {
        val imp = SourceRoots.body(profileIo, importSig)
        val clean = imp.indexOf("val clean = cleanImportedSnapshot(suffix, v)")
        val write = imp.indexOf("copyValue(e, keyOf(plan.target, suffix), value)")
        assertTrue(clean >= 0 && write > clean, "giá trị của tệp phải qua lớp làm sạch TRƯỚC khi ghi (VC-R8)")
        assertTrue(imp.contains("ProfileImportReport(plan.target, plan.kind).also(::logImported)"), "log PI5 lúc nhập")
        assertTrue(repo.contains("prefs.importProfile(data)?.let { report -> ProfileImported(load(), report) }"))
    }

    /** Android box B2 · W2c — không còn phép merge khung chiếu cụm nào, ở bất kỳ tệp nguồn nào. */
    @Test
    fun `khong con duong merge khung chieu cum`() {
        val all = SourceRoots.moduleSourceRoots().flatMap { root ->
            Files.walk(root).use { s -> s.filter { it.toString().endsWith(".kt") }.toList() }
        }.joinToString("\n") { KotlinSource.stripComments(it.toFile().readText()) }
        listOf("""\bmergeImportedCast\(""", """\bmergeImport\(""", """\bClusterImportSummary\b""", """\bClusterSnapshotPlan\b""")
            .forEach { assertEquals(0, Regex(it).findAll(all).count(), "`$it` mọc lại") }
    }

    @Test
    fun `nhap xong mo hop thoai, Dung ngay chi doi ho so, khong cham chieu cum`() {
        val one = SourceRoots.body(section, "private fun importFile(entry: ProfileFiles.Entry)")
        assertTrue(one.contains("SettingsDialogs.offer("), "hộp thoại hai lựa chọn")
        assertTrue(one.contains("{ deps.onSwitchProfile(created.name) }"), "Dùng ngay = đổi hồ sơ")
        assertTrue(one.contains("R.string.kachi_profile_import_use_now") && one.contains("R.string.kachi_profile_import_later"))
        assertTrue(one.contains("R.string.kachi_profile_import_fail, entry.fileName"), "tệp hỏng vẫn nói tên tệp")
        assertFalse(Regex("""setCastEnabled|openProjection|closeProjection|applyCastPending|castEnabled""").containsMatchIn(section),
            "màn hồ sơ không còn đường chiếu cụm nào")
    }

    @Test
    fun `chuoi hop thoai nhap co du hai ngon ngu`() {
        val vi = SourceRoots.text("src/main/res/values/strings_kachi.xml")
        val en = SourceRoots.text("src/main/res/values-en/strings_kachi.xml")
        listOf("kachi_profile_imported", "kachi_profile_import_use_now", "kachi_profile_import_later", "kachi_profile_import_fail")
            .forEach { key ->
                assertTrue(vi.contains("name=\"$key\""), "VI thiếu $key")
                assertTrue(en.contains("name=\"$key\""), "EN thiếu $key")
            }
    }
}
