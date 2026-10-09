package com.kachi.box.launcher

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * S5 — khoá phần THUẦN của "đặt/đọc màn hình chính": chuỗi lệnh + phép xác nhận từ output resolve-activity.
 * Đây là phần mà `LocalDeviceShell.setHomeActivity` và `KachiAutostart.ensureHomeActivity` cùng dùng.
 */
class HomeActivityCmdTest {

    private val comp = "com.kachi.box/com.kachi.box.launcher.KachiHomeActivity"

    /** Android box B2 · W2f — dời nguyên nghĩa từ `HomeGuardPolicyTest.wantsKachiHome - mot trong hai dau` (lớp đó đã gỡ). */
    @Test
    fun `wantsKachiHome - mot trong hai dau`() {
        assertTrue(HomeActivityCmd.wantsKachiHome(homeChosen = true, keepHomeOnBoot = false))
        assertTrue(HomeActivityCmd.wantsKachiHome(homeChosen = false, keepHomeOnBoot = true))
        assertTrue(HomeActivityCmd.wantsKachiHome(homeChosen = true, keepHomeOnBoot = true))
        assertFalse(HomeActivityCmd.wantsKachiHome(homeChosen = false, keepHomeOnBoot = false), "Bỏ chọn ⇒ không đụng HOME")
    }

    @Test
    fun `set dung dinh dang set-home-activity`() {
        assertEquals("cmd package set-home-activity $comp", HomeActivityCmd.set(comp))
    }

    @Test
    fun `RESOLVE doc dung category HOME kem --brief`() {
        assertTrue(HomeActivityCmd.RESOLVE.startsWith("cmd package resolve-activity"), HomeActivityCmd.RESOLVE)
        assertTrue(HomeActivityCmd.RESOLVE.contains("--brief"), "cần --brief để in đúng component")
        assertTrue(HomeActivityCmd.RESOLVE.contains("android.intent.category.HOME"), HomeActivityCmd.RESOLVE)
    }

    @Test
    fun `isHome dung khi resolve tra ve dung component (co trim)`() {
        assertTrue(HomeActivityCmd.isHome(comp, comp))
        assertTrue(HomeActivityCmd.isHome("  $comp  ", comp), "parseComponent phải trim khoảng trắng")
    }

    @Test
    fun `isHome false khi HOME la launcher khac`() {
        assertFalse(HomeActivityCmd.isHome("com.android.launcher3/com.android.launcher3.Launcher", comp))
    }

    /**
     * Review Pass 3 · P1 — `resolve-activity --brief` in `flattenToShortString()` (AOSP r47 `PackageManagerShellCommand:923`):
     * gói = tiền tố lớp ⇒ dạng ngắn. Output nguyên dạng hai dòng của `--brief` (dòng priority có khoảng trắng).
     */
    @Test
    fun `isHome nhan dang SHORT ma resolve-activity --brief in ra`() {
        val brief = "priority=0 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true\ncom.kachi.box/.launcher.KachiHomeActivity\n"
        assertTrue(HomeActivityCmd.isHome(brief, comp))
        assertFalse(HomeActivityCmd.isHome("com.kachi.box/.launcher.KachiHome", comp), "alias khác lớp ⇒ khác component")
        assertFalse(HomeActivityCmd.isHome("com.other/.launcher.KachiHomeActivity", comp))
        assertTrue(ComponentText.same("com.kachi.box/.launcher.KachiHome", "com.kachi.box/com.kachi.box.launcher.KachiHome"))
    }

    @Test
    fun `isHome false khi output rong hoac la cau loi co khoang trang`() {
        assertFalse(HomeActivityCmd.isHome("", comp))
        assertFalse(HomeActivityCmd.isHome("No activity found for Intent", comp))
    }
}
