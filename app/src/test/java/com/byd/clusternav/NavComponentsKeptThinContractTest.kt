package com.byd.clusternav

import com.byd.clusternav.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * ═══ Android box B2 · W2d — hai component GIỮ TÊN sau khi gỡ dẫn đường cụm/HUD ═══════════════════════════════════════
 *
 * `NavNotificationListener` còn là component được cấp quyền đọc thông báo (widget nhạc cần nó để `getActiveSessions`), và
 * `NavAccessibilityService` còn là dịch vụ lọc phím vật lý. Cả hai đổi tên = component mới ⇒ máy đã cài mất quyền, nên giữ
 * tên; bài này canh THÂN của chúng không mọc lại phần dẫn đường, và dịch vụ Hỗ trợ không còn xin đọc cửa sổ (chi phí theo
 * dõi cửa sổ của system_server — chỉ nên trả khi có người đọc).
 */
class NavComponentsKeptThinContractTest {

    private fun code(rel: String) = SourceRoots.codeOf("src/main/java/com/byd/clusternav/$rel")

    @Test
    fun `bo nghe thong bao con la component mong, widget nhac van dung no`() {
        val listener = code("NavNotificationListener.kt")
        listOf("onNotificationPosted", "onNotificationRemoved", "NavRepository", "SourceArbiter", "ClusterBroadcaster", "NotificationParser")
            .forEach { assertFalse(listener.contains(it), "thân dẫn đường mọc lại: `$it`") }
        assertTrue(listener.contains("class NavNotificationListener : NotificationListenerService()"))
        assertTrue(code("launcher/MediaBridge.kt").contains("ComponentName(app, NavNotificationListener::class.java)"),
            "widget nhạc hỏi phiên nhạc qua CHÍNH component này")
        assertTrue(SourceRoots.text("src/main/AndroidManifest.xml").contains("android:name=\".NavNotificationListener\""))
        assertTrue(code("launcher/PermissionPreflight.kt").contains("cmd notification allow_listener"), "Preflight vẫn tự cấp quyền")
    }

    @Test
    fun `dich vu Ho tro chi loc phim, khong xin doc cua so va khong nhan su kien`() {
        val cfg = SourceRoots.text("src/main/res/xml/nav_accessibility_config.xml")
        val tag = cfg.substring(cfg.indexOf("<accessibility-service"))
        assertTrue(tag.contains("android:accessibilityFlags=\"flagRequestFilterKeyEvents\""), tag)
        assertTrue(tag.contains("android:canRequestFilterKeyEvents=\"true\""), tag)
        listOf("packageNames", "flagRetrieveInteractiveWindows", "flagReportViewIds", "canRetrieveWindowContent", "accessibilityEventTypes")
            .forEach { assertFalse(tag.contains(it), "cấu hình còn `$it`") }
        val service = code("modules/navaccess/NavAccessibilityService.kt")
        assertTrue(service.contains("override fun onKeyEvent(event: KeyEvent?): Boolean"), "lọc phím phải còn")
        assertTrue(service.contains("override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit"))
    }

    /** Không mã `:app` nào còn đọc cửa sổ / cây node của app khác — điều kiện để bỏ được hai cờ đọc cửa sổ ở trên. */
    @Test
    fun `khong ma nao doc cua so cua app khac`() {
        val root = SourceRoots.moduleSourceRoots().first { it.endsWith(java.nio.file.Paths.get("app", "src", "main", "java")) }
        val hits = Files.walk(root).use { s -> s.filter { it.toString().endsWith(".kt") }.toList() }.filter { p ->
            Regex("""\b(rootInActiveWindow|getWindows\(|windowsOnAllDisplays|findAccessibilityNodeInfosBy)""")
                .containsMatchIn(SourceRoots.codeOf("src/main/java/" + root.relativize(p).joinToString("/")))
        }.map { it.fileName.toString() }
        assertEquals(emptyList<String>(), hits)
    }
}
