package com.byd.clusternav.perf

import com.byd.clusternav.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 2.96 · R18 — dây nối của các tối ưu standby/chạy thường (luật thuần ở `:core` `StandbyPerfR18Test`). Mỗi hàm mới phải có
 * call site thật (CLAUDE.md §8) và mỗi đường cũ giữ nguyên khi màn bật.
 */
class StandbyPerfR18WiringContractTest {

    private fun code(p: String) = SourceRoots.codeOf("src/main/java/com/byd/clusternav/$p")

    @Test
    fun `do o hoi ban doc dung chung TRUOC khi chay lenh, ghi lai ban tu chay`() {
        val sweep = SourceRoots.body(code("launcher/SlotLiveProbe.kt"), "private fun sweep()")
        val fresh = sweep.indexOf("StackListSnapshot.fresh(")
        val shell = sweep.indexOf("shell(\"am stack list\")")
        assertTrue(fresh in 0 until shell, "dùng lại trước, tự chạy sau")
        assertTrue(sweep.contains("StackListSnapshot.record(it)"))
    }

    @Test
    fun `HomeGuard man tat thi khong doc HOME, nhip theo man`() {
        val guard = code("launcher/HomeGuard.kt")
        val tick = SourceRoots.body(guard, "private fun tick(")
        val off = tick.indexOf("if (interactive == false)")
        assertTrue(off >= 0 && off < tick.indexOf("DefaultHome.currentPackage(app)"), "cổng màn tắt đứng TRƯỚC lượt đọc HOME")
        assertTrue(SourceRoots.body(guard, "private fun loop(").contains("nextDelayMs(SystemClock.elapsedRealtime() - tripStartMs, prevInteractive)"))
    }

    @Test
    fun `boot bo am start khi man chinh da resumed - bo dem noi o vong doi`() {
        val auto = code("KachiAutostart.kt")
        assertTrue(auto.contains("BootHomeUp.needsStart(HomeResumed.count())"))
        assertTrue(auto.indexOf("BootHomeUp.needsStart") < auto.indexOf("seam(\"am start -n \$launchComp\")"))
        val home = code("launcher/KachiHomeActivity.kt")
        assertTrue(SourceRoots.body(home, "override fun onResume()").contains("HomeResumed.up()"))
        assertTrue(home.contains("handler.removeCallbacks(tick); HomeResumed.down()"), "onPause trả bộ đếm")
    }

    @Test
    fun `keep-alive phim thoai chi ghi moc ngu khi doi`() {
        val src = code("VoiceKeyKeepAliveService.kt")
        assertTrue(src.contains("if (A11yBindJournal.shouldPersistDeepSleep(prev, now)) Prefs.setLastDeepSleepMs(app, now)"))
    }

}
