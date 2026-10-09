package com.kachi.box

import com.kachi.box.modules.navaccess.AccessibilityRebind
import com.kachi.box.modules.navaccess.KachiKeyService
import com.kachi.box.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * BOX-RENAME-PACKAGE (2026-10-09) — danh tính component của Kachi sau khi đổi gói Kotlin `com.byd.clusternav` →
 * `com.kachi.box` và đổi tên hai component được cấp quyền (`NavNotificationListener` → [MediaSessionListener],
 * `NavAccessibilityService` → [KachiKeyService]).
 *
 * Chuỗi component của dịch vụ trợ năng có HAI nguồn: `:core` [AccessibilityRebind.ACC_COMP] (viết tay — `:core` không có
 * BuildConfig) và `:app` [KeyServiceConnect.ACC_COMP] (dựng từ applicationId + FQN lớp). Lệch nhau ⇒ bộ đo kẹt/bound đọc
 * `dumpsys accessibility` theo một chuỗi, còn lệnh `settings put secure enabled_accessibility_services` ghi chuỗi kia.
 * Bài này khoá hai nguồn trùng nhau và khớp manifest; sau bản đầu, đổi tên nữa = người dùng mất quyền đã cấp.
 */
class KachiComponentIdentityTest {

    private val manifest by lazy { SourceRoots.text("src/main/AndroidManifest.xml") }

    @Test
    fun `chuoi component tro nang o core va app trung nhau va dung ten moi`() {
        assertEquals("com.kachi.box/com.kachi.box.modules.navaccess.KachiKeyService", KeyServiceConnect.ACC_COMP)
        assertEquals(KeyServiceConnect.ACC_COMP, AccessibilityRebind.ACC_COMP, ":core viết tay phải khớp :app dựng từ lớp")
    }

    @Test
    fun `manifest khai dung hai component da doi ten`() {
        assertEquals("com.kachi.box.MediaSessionListener", MediaSessionListener::class.java.name)
        assertEquals("com.kachi.box.modules.navaccess.KachiKeyService", KachiKeyService::class.java.name)
        assertTrue(manifest.contains("android:name=\".MediaSessionListener\""), "bộ nghe thông báo")
        assertTrue(manifest.contains("android:name=\".modules.navaccess.KachiKeyService\""), "dịch vụ trợ năng")
        listOf("NavNotificationListener", "NavAccessibilityService", "com.byd.clusternav")
            .forEach { assertFalse(manifest.contains(it), "manifest còn tên cũ `$it`") }
    }

    @Test
    fun `action va affinity rieng cua app dung tu applicationId`() {
        assertEquals("${BuildConfig.APPLICATION_ID}.REBIND_WATCHDOG", RebindReceiver.ACTION_WATCHDOG)
        listOf("\${applicationId}.REBIND_WATCHDOG", "\${applicationId}.behind", "\${applicationId}.stage")
            .forEach { assertTrue(manifest.contains(it), "manifest phải dựng `$it` từ applicationId") }
    }
}
