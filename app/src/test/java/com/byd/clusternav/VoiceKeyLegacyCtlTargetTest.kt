package com.byd.clusternav

import com.byd.clusternav.modules.voicekey.AssistantLauncher
import com.byd.clusternav.modules.voicekey.VoiceKeyBindingStore
import com.byd.clusternav.testsupport.SourceRoots
import com.byd.clusternav.voicekey.VoiceKeyAction
import com.byd.clusternav.voicekey.VoiceKeyConfig
import com.byd.clusternav.voicekey.VoiceKeyMatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Android box B2 · W3 — dòng gán phím đã lưu trỏ nút xe cũ (`ctl:<nút>:<việc>`, FIX286 · R-KC) phải NẠP được (không ném,
 * không làm mất các dòng khác của cùng danh sách) rồi bị BỎ QUA lúc bấm: nút xe BYD đã gỡ cùng lõi HAL.
 *
 * Chuỗi JSON dưới đây là đúng dạng `Prefs.voicekey_bindings` mà bản BYD ≤ 2.98 ghi (`[{k,t,s?}]`).
 */
class VoiceKeyLegacyCtlTargetTest {

    private val stored = """[{"k":24,"t":"ctl:fan:up"},{"k":328,"t":"ai.zalo.kiki.car"},{"k":25,"t":"ctl:trunk:open","s":2}]"""

    @Test
    fun `dong gan ctl cu nap duoc, khong lam mat dong khac`() {
        val rows = VoiceKeyBindingStore.decode(stored)
        assertEquals(listOf(24, 328, 25), rows.map { it.keyCode }, "ba dòng nạp đủ, đúng thứ tự")
        assertEquals("ai.zalo.kiki.car", rows.single { it.keyCode == 328 }.targetSpec)
        // Mã hoá lại (lượt Thêm/Xoá dòng khác) giữ nguyên dòng cũ — người dùng tự xoá ở Cài đặt › Phím vô-lăng.
        assertEquals(rows, VoiceKeyBindingStore.decode(VoiceKeyBindingStore.encode(rows)))
    }

    /**
     * Review Pass 2 [P2] — trước bản vá bộ khớp vẫn NUỐT phím 24 (tăng âm lượng) cho dòng `ctl:fan:up` rồi `launch` không
     * làm gì ⇒ phím âm lượng chết im lặng. Nay dòng đích đã gỡ bị bỏ khỏi danh sách khớp ⇒ phím về hệ thống.
     */
    @Test
    fun `bam phim gan ctl cu thi phim ve he thong, phim gan app van mo app`() {
        val live = AssistantLauncher.liveBindings(VoiceKeyBindingStore.decode(stored))
        assertEquals(listOf(328), live.map { it.keyCode }, "chỉ còn dòng gán app")
        val cfg = VoiceKeyConfig(enabled = true, bindings = live)
        val m = VoiceKeyMatcher()
        val vol = m.onKey(cfg, VoiceKeyAction.DOWN, 24, 10).also { m.onKey(cfg, VoiceKeyAction.UP, 24, 10) }
        assertFalse(vol.consume, "phím 24 KHÔNG bị nuốt")
        assertEquals(null, vol.targetSpec)
        val app = m.onKey(cfg, VoiceKeyAction.DOWN, 328, 20)
        assertTrue(app.consume, "phím gán app vẫn nuốt")
        assertTrue(AssistantLauncher.isRetiredTarget("ctl:fan:up"))
        assertTrue(AssistantLauncher.isRetiredTarget("cam:left"), "đích camera cũ (W2b) cùng đường")
        assertFalse(AssistantLauncher.isRetiredTarget("ai.zalo.kiki.car"), "tên gói không bao giờ bị coi là đích cũ")
        assertFalse(AssistantLauncher.isRetiredTarget("__KACHI_VOICE__"))
    }

    /** Dịch vụ phím khớp trên danh sách đã lọc — không phải danh sách thô. */
    @Test
    fun `dich vu phim khop tren danh sach da loc`() {
        val src = SourceRoots.codeOf("src/main/java/com/byd/clusternav/modules/navaccess/NavAccessibilityService.kt")
        assertTrue(src.contains("bindings = AssistantLauncher.liveBindings(Prefs.voiceKeyBindings(app))"))
    }

    /** `launch` hỏi [AssistantLauncher.isRetiredTarget] ở DÒNG ĐẦU — trước mọi lời gọi chạm `ctx`. */
    @Test
    fun `launch thoat som truoc moi loi goi Context`() {
        val src = SourceRoots.codeOf("src/main/java/com/byd/clusternav/modules/voicekey/AssistantLauncher.kt")
        val body = SourceRoots.body(src, "fun launch(ctx: Context, spec: String): Boolean")
        val guard = body.indexOf("if (isRetiredTarget(spec)) return false")
        assertTrue(guard >= 0, "launch phải bỏ qua đích cũ")
        assertTrue(guard < body.indexOf("ctx"), "thoát TRƯỚC khi chạm Context")
    }
}
