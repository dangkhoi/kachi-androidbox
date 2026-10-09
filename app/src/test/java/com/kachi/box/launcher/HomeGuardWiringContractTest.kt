package com.kachi.box.launcher

import com.kachi.box.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Android box B2 · W2f (2026-10-09) — `HomeGuard` (2.96 · R8: nhịp tiến trình giành lại HOME từ launcher BYD 5.7.5, [ĐO xe
 * 07/10 fw 2606]) + luật thuần `HomeGuardPolicy` đã XOÁ: hành vi chỉ của ROM BYD (W1 đã gỡ chỗ cài). Còn lại đúng một
 * điều kiện dùng chung — `HomeActivityCmd.wantsKachiHome` — cho lượt đặt lại HOME lúc khởi động nguội (`KachiAutostart`).
 *
 * Bài này chặn: lớp/nhịp giành HOME mọc lại, và lượt khởi động mất cổng "người dùng đã chọn Kachi" (đặt HOME cả xe là đổi
 * state hệ thống — chỉ khi người dùng bày tỏ, CLAUDE.md §4).
 */
class HomeGuardWiringContractTest {

    private val application by lazy { SourceRoots.codeOf("src/main/java/com/kachi/box/KachiApplication.kt") }
    private val autostart by lazy { SourceRoots.codeOf("src/main/java/com/kachi/box/KachiAutostart.kt") }

    @Test
    fun `HomeGuard va luat cua no da xoa`() {
        assertFalse(SourceRoots.exists("src/main/java/com/kachi/box/launcher/HomeGuard.kt"), "HomeGuard.kt phải đã xoá")
        assertFalse(SourceRoots.exists("src/main/java/com/kachi/box/launcher/HomeGuardPolicy.kt"), "HomeGuardPolicy.kt phải đã xoá")
        val onCreate = SourceRoots.body(application, "override fun onCreate()")
        assertTrue("HomeGuard" !in onCreate, "không còn cài nhịp giữ HOME")
        assertTrue(onCreate.contains("EarlyShellChannel.start(this)"), "dòng chốt cuối giữ nguyên")
        assertTrue("HomeGuard" !in autostart, "KachiAutostart không còn nhắc HomeGuard trong mã")
    }

    @Test
    fun `KachiAutostart gac dat lai HOME bang lua chon ben cua nguoi dung`() {
        val gate = "HomeActivityCmd.wantsKachiHome(prefs.homeChosen(), prefs.keepHomeOnBoot())"
        assertTrue(autostart.contains("if ($gate)"), "lượt nổ máy chỉ đặt lại HOME khi người dùng đã chọn Kachi")
        val after = autostart.substringAfter("if ($gate)")
        assertTrue(
            after.indexOf("DefaultHome.enableHomeEntry(app)") in 0 until after.indexOf("ensureHomeActivity(seam, comp)"),
            "bật alias TRƯỚC set-home-activity, cả hai trong nhánh đã chọn",
        )
    }
}
