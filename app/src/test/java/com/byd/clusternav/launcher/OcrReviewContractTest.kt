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

    // Android box B2 · W2c — ba bài của nút nổi chiếu cụm (`BubblePipGuard` · `FloatingBubbleService`) và widget VietMap
    // (`VietMapAppWidgetHost` · `VietMapWidgetBridge`) gỡ cùng mã của chúng.

}
