package com.byd.clusternav.system

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [DisplayOwnershipRegistry] — launcher sở hữu display 0 + VD nó đã đăng ký (id ≥ 1 bất kỳ); mọi display khác không chủ ⇒
 * từ chối; [WindowMutation.NO_DISPLAY] (force-stop…) luôn qua. Thuần JVM.
 *
 * B4 · DISPLAY-OWNER-DYNAMIC (2.89) khoá: bản cũ ghi cứng `ownerOf(1) = CAST` + `registerVirtualDisplay` đòi `id > 1` ⇒ sau
 * khởi động nguội ô `kachi-slot-0` nhận display 1 [ĐO xe 15/09 · máy ảo 05/10] và mọi lệnh mở app vào ô bị REJECT.
 * Android box B2 · W2c: nhánh chiếu cụm (`CAST`, id cụm dò live) gỡ — các ca của nó gỡ theo.
 */
class DisplayOwnershipRegistryTest {

    private fun reg() = DisplayOwnershipRegistry()

    private fun launcherMutation(displayId: Int) =
        WindowMutation.LaunchOnDisplay("com.foo/.Main", displayId, windowingMode = 1)

    @Test
    fun `ownerOf - display 0 LAUNCHER, every secondary id unknown until registered`() {
        val r = reg()
        assertEquals(DisplayOwner.LAUNCHER, r.ownerOf(0))
        assertNull(r.ownerOf(1), "display 1 KHÔNG mặc định thuộc ai — ai tạo màn phụ trước thì nó là 1")
        assertNull(r.ownerOf(7))
    }

    @Test
    fun `chi con mot chu - nhanh chieu cum da go`() {
        assertEquals(listOf(DisplayOwner.LAUNCHER), DisplayOwner.values().toList())
    }

    @Test
    fun `slot VD with id 1 registered by the launcher is LAUNCHER`() {
        val r = reg()
        r.registerVirtualDisplay(1)   // khởi động nguội: ô kachi-slot-0 là màn phụ đầu tiên ⇒ display 1
        assertEquals(DisplayOwner.LAUNCHER, r.ownerOf(1))
        assertTrue(r.validate(launcherMutation(1), DisplayOwner.LAUNCHER).allowed)
    }

    @Test
    fun `unknown secondary display is rejected (fail-safe deny) and the reason names issuer and target`() {
        val r = reg()
        r.registerVirtualDisplay(3)
        val res = r.validate(launcherMutation(5), DisplayOwner.LAUNCHER)
        assertFalse(res.allowed)
        val reason = (res as ValidationResult.Reject).reason
        assertTrue(reason.contains("không có chủ") && reason.contains("LAUNCHER") && reason.contains("5"), reason)
    }

    @Test
    fun `registered virtual display is owned by LAUNCHER, released on unregister`() {
        val r = reg()
        r.registerVirtualDisplay(7)
        assertEquals(DisplayOwner.LAUNCHER, r.ownerOf(7))
        assertTrue(r.registeredVirtualDisplays().contains(7))
        assertTrue(r.validate(launcherMutation(7), DisplayOwner.LAUNCHER).allowed)
        r.unregisterVirtualDisplay(7)
        assertNull(r.ownerOf(7))
        assertFalse(r.registeredVirtualDisplays().contains(7))
        assertFalse(r.validate(launcherMutation(7), DisplayOwner.LAUNCHER).allowed)
    }

    @Test
    fun `registering the main display 0 or a negative id as a virtual display is rejected, id 1 is accepted`() {
        val r = reg()
        assertThrowsIllegalArgument { r.registerVirtualDisplay(0) }
        assertThrowsIllegalArgument { r.registerVirtualDisplay(-1) }
        r.registerVirtualDisplay(1)
        assertTrue(r.registeredVirtualDisplays().contains(1))
    }

    @Test
    fun `LAUNCHER to main display 0 is ALLOWED`() {
        assertTrue(reg().validate(launcherMutation(0), DisplayOwner.LAUNCHER).allowed)
    }

    @Test
    fun `force-stop and Raw NO_DISPLAY are ALLOWED`() {
        val r = reg()
        assertTrue(r.validate(WindowMutation.ForceStop("com.foo"), DisplayOwner.LAUNCHER).allowed)
        val m = WindowMutation.Raw("settings put global x 1", WindowMutation.NO_DISPLAY)
        assertTrue(r.validate(m, DisplayOwner.LAUNCHER).allowed)
    }

    private fun assertThrowsIllegalArgument(block: () -> Unit) {
        try {
            block()
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // ok
        }
    }
}
