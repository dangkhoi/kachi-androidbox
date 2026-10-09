package com.kachi.box.launcher

import com.kachi.box.BuildConfig
import com.kachi.box.testsupport.KotlinSource
import com.kachi.box.testsupport.SourceRoots
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Android box B1 (spec `androidbox-plan` §9, 2026-10-09) — khoá việc tách gói khỏi Kachi BYD.
 *
 * Trước B1, ~15 chỗ ở `:app` viết cứng `com.byd.launcher` (lệnh `appops`/`appwidget grantbind`, action broadcast nội bộ,
 * đường log). Đổi `applicationId` sang `com.kachi.box` mà sót một chỗ thì lệnh tự cấp quyền nhắm vào gói KHÁC (hoặc gói
 * của bản BYD cài cùng máy) — compile vẫn xanh, chạy thì câm. Bài này đỏ nếu mã (không tính chú thích) ở bất kỳ module
 * nguồn nào, manifest hay `res/` còn tên gói cũ.
 */
class OwnPackageLiteralContractTest {

    private val old = "com.byd" + ".launcher"

    @Test
    fun `ma nguon khong con ten goi Kachi BYD`() {
        assertEquals("com.kachi.box", BuildConfig.APPLICATION_ID)
        val roots = SourceRoots.moduleSourceRoots()
        assertTrue(roots.size >= 3, "phải quét đủ app · core · car-integration, thấy $roots")
        val hits = roots.flatMap { root ->
            Files.walk(root).use { s -> s.filter { it.toString().endsWith(".kt") || it.toString().endsWith(".java") }.toList() }
        }.filter { KotlinSource.stripComments(it.toFile().readText()).contains(old) }
        assertTrue(hits.isEmpty(), "mã còn viết cứng gói BYD: $hits")
    }

    @Test
    fun `manifest va res khong con ten goi Kachi BYD`() {
        val xmlComment = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
        val manifest = SourceRoots.path("src/main/AndroidManifest.xml")
        val res = manifest.parent.resolve("res")
        val files = listOf(manifest) + Files.walk(res).use { s -> s.filter { it.toString().endsWith(".xml") }.toList() }
        val hits = files.filter { xmlComment.replace(it.toFile().readText(), "").contains(old) }
        assertTrue(hits.isEmpty(), "manifest/res còn gói BYD: $hits")
    }

    @Test
    fun `lenh grantbind nham dung goi cua chinh app`() {
        // Companion của AppWidgetSlotHost là private ⇒ canh nguồn: gói phải dựng từ BuildConfig, không viết cứng.
        val src = SourceRoots.codeOf("src/main/java/com/kachi/box/launcher/AppWidgetSlotHost.kt")
        assertTrue(
            src.contains("const val GRANT_CMD = \"appwidget grantbind --package \" + BuildConfig.APPLICATION_ID + \" --user 0\""),
            "GRANT_CMD phải nhắm gói của chính app (BuildConfig.APPLICATION_ID)",
        )
    }
}
