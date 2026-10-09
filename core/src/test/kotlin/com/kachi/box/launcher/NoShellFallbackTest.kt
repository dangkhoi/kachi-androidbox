package com.kachi.box.launcher

import com.kachi.box.carexec.ShellChannelPhase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Android box B3 — luật "không có kênh shell thì người dùng tự cấp ở đâu" (spec `androidbox-plan.html` §4.2). */
class NoShellFallbackTest {

    @Test
    fun `co kenh thi KHONG bay nut tay nao - Kachi tu cap nhu cu`() {
        LauncherRequirements.ALL.forEach { assertNull(NoShellFallback.manualFix(it, shellUsable = true), it.id) }
    }

    @Test
    fun `khong co kenh thi moi quyen tu cap duoc co dung man he thong`() {
        val r = LauncherRequirements
        assertEquals(ManualFix.ACCESSIBILITY_SETTINGS, NoShellFallback.manualFix(r.ACCESSIBILITY, false))
        assertEquals(ManualFix.NOTIFICATION_LISTENER_SETTINGS, NoShellFallback.manualFix(r.NOTIFICATION_LISTENER, false))
        assertEquals(ManualFix.OVERLAY_SETTINGS, NoShellFallback.manualFix(r.OVERLAY, false))
        assertEquals(ManualFix.RUNTIME_RECORD_AUDIO, NoShellFallback.manualFix(r.MICROPHONE, false))
        assertEquals(ManualFix.RUNTIME_LOCATION, NoShellFallback.manualFix(r.LOCATION, false))
        assertEquals(ManualFix.HOME_SETTINGS, NoShellFallback.manualFix(r.DEFAULT_HOME, false))
        assertEquals(ManualFix.DEVELOPER_SETTINGS, NoShellFallback.manualFix(r.SHELL_CHANNEL, false))
        assertNull(NoShellFallback.manualFix(r.FREEFORM, false), "cờ Global — không có màn chuẩn")
        // Chưa biết kênh (chưa dò xong) cũng chỉ đường tay — nút vô hại, Kachi vẫn tự cấp khi kênh lên.
        assertEquals(ManualFix.OVERLAY_SETTINGS, NoShellFallback.manualFix(r.OVERLAY, null))
    }

    @Test
    fun `moi muc SELF deu co duong tay khi khong co kenh - khong muc nao bi bo mac`() {
        LauncherRequirements.ALL.filter { it.fixBy == FixBy.SELF }.forEach {
            assertTrue(NoShellFallback.manualFix(it, false) != null, "mục ${it.id} tự cấp qua shell ⇒ phải có đường tay")
        }
    }

    @Test
    fun `khong micro thi an giong noi va bo quyen micro khoi vong kiem`() {
        assertFalse(NoShellFallback.voiceAvailable(false))
        assertTrue(NoShellFallback.voiceAvailable(true))
        assertTrue(NoShellFallback.voiceAvailable(null), "đọc hỏng ⇒ không ẩn")
        val rep = LauncherRequirements.check(notApplicable = NoShellFallback.notApplicable(false)) { false }
        assertFalse(rep.results.any { it.requirement.id == LauncherRequirements.MICROPHONE.id })
        assertFalse(rep.missing.isEmpty(), "các mục khác vẫn được kiểm")
        val withMic = LauncherRequirements.check(notApplicable = NoShellFallback.notApplicable(true)) { false }
        assertTrue(withMic.missing.any { it.id == LauncherRequirements.MICROPHONE.id })
    }

    /** BOX-FREEFORM-NOSHELL — hồi quy: không kênh thì cờ cửa sổ tự do từng hiện "thiếu" mà không có nút nào. */
    @Test
    fun `khong kenh thi co cua so tu do khong ap dung, co kenh hoac chua biet thi giu`() {
        val freeform = LauncherRequirements.FREEFORM.id
        val noShell = LauncherRequirements.check(notApplicable = NoShellFallback.notApplicable(true, shellUsable = false)) { false }
        assertFalse(noShell.results.any { it.requirement.id == freeform }, "không kênh ⇒ không có ô app ⇒ không áp dụng")
        assertTrue(noShell.missing.none { NoShellFallback.manualFix(it, false) == null && it.fixBy != FixBy.USER },
            "không kênh ⇒ mọi mục thiếu còn lại đều có nút tay")
        listOf(true, null).forEach { s ->
            val rep = LauncherRequirements.check(notApplicable = NoShellFallback.notApplicable(true, shellUsable = s)) { false }
            assertTrue(rep.missing.any { it.id == freeform }, "kênh=$s ⇒ giữ như cũ")
        }
        assertEquals(setOf(LauncherRequirements.MICROPHONE.id, freeform), NoShellFallback.notApplicable(false, shellUsable = false))
    }

    /** BOX-KEYS-NOSHELL-TOAST — hồi quy: không kênh mà nút Phím vật lý vẫn đi dadb ⇒ toast "gỡ lỗi USB" sai. */
    @Test
    fun `phim vat ly di duong tay chi khi kenh da do la khong co`() {
        assertTrue(NoShellFallback.accessibilityByHand(false, ShellChannelPhase.ENVIRONMENT))
        assertTrue(NoShellFallback.accessibilityByHand(false, ShellChannelPhase.UNKNOWN))
        assertFalse(NoShellFallback.accessibilityByHand(true, ShellChannelPhase.UP), "có kênh ⇒ dadb như cũ")
        assertFalse(NoShellFallback.accessibilityByHand(false, ShellChannelPhase.NEEDS_APPROVAL), "adbd đang hỏi ⇒ câu gỡ lỗi USB là ĐÚNG")
        assertFalse(NoShellFallback.accessibilityByHand(false, ShellChannelPhase.STARTING), "đang dò ⇒ cổng tự chờ")
        assertFalse(NoShellFallback.accessibilityByHand(false, ShellChannelPhase.CHECKING))
    }

    /** BOX-HOME-RESULT-LOST — hồi quy: câu kết quả bị lượt dựng lại xoá; nay đọc từ HOME thật, chỉ khi có lượt chọn chờ. */
    @Test
    fun `cau ket qua sau man chon HOME doc tu trang thai that`() {
        assertEquals(HomePickerNote.NOW_HOME, NoShellFallback.homePickerNote(pickerOpened = true, isHome = true))
        assertEquals(HomePickerNote.NOT_YET, NoShellFallback.homePickerNote(pickerOpened = true, isHome = false))
        assertNull(NoShellFallback.homePickerNote(pickerOpened = false, isHome = true), "không có lượt chọn ⇒ không câu cũ nào")
        assertNull(NoShellFallback.homePickerNote(pickerOpened = false, isHome = false))
    }

    @Test
    fun `duong dat HOME theo trang thai kenh`() {
        assertEquals(HomeRoute.SHELL, NoShellFallback.homeRoute(true, ShellChannelPhase.UP))
        assertEquals(HomeRoute.PROMPT, NoShellFallback.homeRoute(false, ShellChannelPhase.NEEDS_APPROVAL))
        listOf(ShellChannelPhase.ENVIRONMENT, ShellChannelPhase.UNKNOWN, ShellChannelPhase.STARTING).forEach {
            assertEquals(HomeRoute.SYSTEM_PICKER, NoShellFallback.homeRoute(false, it), it.name)
        }
    }

    @Test
    fun `duong cai OTA - kenh dang do thi van cho dadb, da do khong co thi trinh cai he thong`() {
        assertEquals(OtaRoute.SHELL, NoShellFallback.otaRoute(true, ShellChannelPhase.UP))
        assertEquals(OtaRoute.SHELL, NoShellFallback.otaRoute(false, ShellChannelPhase.CHECKING))
        assertEquals(OtaRoute.SHELL, NoShellFallback.otaRoute(false, ShellChannelPhase.STARTING))
        listOf(ShellChannelPhase.ENVIRONMENT, ShellChannelPhase.NEEDS_APPROVAL, ShellChannelPhase.UNKNOWN).forEach {
            assertEquals(OtaRoute.SYSTEM_INSTALLER, NoShellFallback.otaRoute(false, it), it.name)
        }
    }

    @Test
    fun `APK cho trinh cai he thong phai dung goi dang chay`() {
        assertTrue(NoShellFallback.archiveMatches("com.kachi.box", "com.kachi.box"))
        assertFalse(NoShellFallback.archiveMatches("com.byd.launcher", "com.kachi.box"))
        assertFalse(NoShellFallback.archiveMatches(null, "com.kachi.box"), "APK đọc không ra ⇒ không cài")
        assertFalse(NoShellFallback.archiveMatches("", "com.kachi.box"))
    }

    /** Review Pass 2 [P2] — khác người ký CHẮC CHẮN ⇒ chặn; đọc không ra (rỗng) ⇒ để trình cài hệ thống quyết. */
    @Test
    fun `nguoi ky APK khac ban dang cai thi chan, doc khong ra thi khong chan`() {
        assertTrue(NoShellFallback.signersMatch(setOf("aa"), setOf("aa")))
        assertTrue(NoShellFallback.signersMatch(setOf("bb"), setOf("aa", "bb")), "lịch sử xoay khoá")
        assertFalse(NoShellFallback.signersMatch(setOf("cc"), setOf("aa", "bb")))
        assertTrue(NoShellFallback.signersMatch(emptySet(), setOf("aa")))
        assertTrue(NoShellFallback.signersMatch(setOf("aa"), emptySet()))
    }

    @Test
    fun `the o app co nut mo toan man khi kenh da do la khong dung duoc`() {
        assertTrue(NoShellFallback.offerFullscreen(ShellChannelPhase.ENVIRONMENT))
        assertTrue(NoShellFallback.offerFullscreen(ShellChannelPhase.NEEDS_APPROVAL))
        assertFalse(NoShellFallback.offerFullscreen(ShellChannelPhase.CHECKING))
        assertFalse(NoShellFallback.offerFullscreen(ShellChannelPhase.UP))
    }
}
