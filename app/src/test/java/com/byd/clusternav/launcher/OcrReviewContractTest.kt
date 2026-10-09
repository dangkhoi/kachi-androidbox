package com.byd.clusternav.launcher

import com.byd.clusternav.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ KHOÁ LẠI NĂM BẢN VÁ CỦA LƯỢT SOÁT ĐỘC LẬP 2026-09-16 ════════════════════════════════════════════════════
 *
 * Nguồn: `docs/diagnostics/ocr-review-2026-09-16.md`. Năm chỗ dưới đây đều là **đường sống lâu hơn thứ nó phục
 * vụ** hoặc **lá chắn mang hình dạng lá chắn mà không chắn gì** — cả hai đều compile xanh và đều im lặng trên xe,
 * đúng họ lỗi mà CLAUDE.md §8 cảnh báo.
 *
 * Bài này quét **NGUỒN ĐÃ BỎ CHÚ THÍCH** ([SourceRoots.codeOf]) vì cả năm chỗ đều nằm trong `View`/`Service`/
 * `AppWidgetHost` — không dựng được trong JVM thuần, mà phần thuần thì không có. Thứ khoá lại được ở đây là
 * **hình dạng của nhánh**, và đó đúng là thứ đã sai.
 */
class OcrReviewContractTest {

    private fun code(rel: String) = SourceRoots.codeOf(rel)

    /**
     * **[P1] `SlotAppHost.release()` phải ĐÓNG cổng thử lại, không phải mở nó.**
     *
     * `tryStart`/`tryShellStart` tự hẹn lại qua `h.postDelayed(…, 150)` tới 40 lượt (≈6 giây) và cổng duy nhất
     * chặn chúng là `if (embedded || tries <= 0) return`. `release()` đặt `embedded = false` ⇒ một lượt đã hẹn,
     * về sau khi ô đã tháo, đi qua cổng và gọi `startActivity` / `am start --display` lên một `ActivityView`
     * **vừa được release** — mở app lên một màn ảo không còn tồn tại.
     */
    @Test
    fun `SlotAppHost release go moi luot thu lai TRUOC khi ha co embedded`() {
        val release = SourceRoots.body(code("src/main/java/com/byd/clusternav/launcher/SlotAppHost.kt"), "fun release()")
        val removeAt = release.indexOf("h.removeCallbacksAndMessages(null)")
        val flagAt = release.indexOf("embedded = false")
        assertTrue(removeAt >= 0, "phải gỡ hàng đợi của chính handler này — nếu không, lượt đã hẹn sống lâu hơn ô")
        assertTrue(flagAt > removeAt, "hạ cờ TRƯỚC khi gỡ là mở đúng cái cổng vừa định đóng")
    }

    /**
     * **[P2] Hộp thoại của `CapTestConsole` bung từ luồng nền phải hỏi màn còn sống không.**
     *
     * `KachiLog.snapshot` chạy `logcat -d` (chặn, vài giây). Trong khoảng ấy người dùng đóng được màn Cài đặt
     * hoặc đổi bảng màu (`recreate()`), và `show()` trên một activity đã huỷ ném `BadTokenException` — không ai
     * bắt ⇒ sập launcher. Cùng lá chắn mà `VoiceTextConsole.ask` đã dựng.
     */
    @Test
    fun `CapTestConsole khong bung hop thoai tren mot man da huy`() {
        val body = SourceRoots.body(
            code("src/main/java/com/byd/clusternav/launcher/CapTestConsole.kt"), "private fun snapshotLogs()",
        )
        assertTrue("isFinishing" in body && "isDestroyed" in body, "phải kiểm vòng đời trước khi show()")
        assertTrue("runCatching" in body, "và bọc khe hở còn lại giữa phép kiểm và lời gọi")
    }

    // Android box B2 · W2c — ba bài của nút nổi chiếu cụm (`BubblePipGuard` · `FloatingBubbleService`) và widget VietMap
    // (`VietMapAppWidgetHost` · `VietMapWidgetBridge`) gỡ cùng mã của chúng.

    /**
     * **[P2] `BydHal.root()` phải chặn VÒNG, không chỉ chặn tự-trỏ.**
     *
     * `while (c.cause != null && c.cause !== c)` chỉ bắt vòng MỘT mắt xích; một vòng hai mắt xích (`a.cause = b;
     * b.cause = a` — sinh ra khi một tầng bọc lại đúng cái lỗi nó vừa nhận) làm vòng lặp chạy vĩnh viễn. Hàm này
     * nằm trên đường ghi nav (~4 lần/giây) nên đơ ở đây = đơ luồng ghi HAL trên xe đang chạy.
     * `LocalShellRetryPolicy.causeChain` đã chặn đúng cách từ trước — đây là cùng lá chắn, đặt vào chỗ còn thiếu.
     */
    @Test
    fun `BydHal root khong bao gio lap vo han tren chuoi cause co vong`() {
        val root = SourceRoots.body(code("src/main/java/com/byd/clusternav/modules/hal/BydHal.kt"), "fun root(t: Throwable)")
        assertTrue("IdentityHashMap" in root, "phải nhớ mắt xích ĐÃ ĐI QUA (so theo danh tính, không theo equals)")
        assertTrue("MAX_CAUSE_DEPTH" in root, "và có trần độ sâu — cùng con số với LocalShellRetryPolicy")
        assertFalse("c.cause !== c" in root, "phép kiểm cũ chỉ bắt được vòng một mắt xích")
    }
}
