package com.byd.clusternav.launcher.voice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ D1 · CÂU HỎI ĐI QUA **LƯỢT HỎI LẠI** VẪN KHÔNG ĐƯỢC THÀNH LỆNH GHI ══════════════════════════════════════
 *
 * Nguồn: log xe THẬT 2026-09-18 (`logs/20260918`, 53 phiên `VoiceWavProbe` dạng `.zip`). Bản trên xe: ⚠ đính chính
 * 2026-10-02 (FIX286 SR7) — nhãn cũ *"1.76"* sai cho ca dưới: stamp `20260917-200206` có TRƯỚC lúc 1.76 phát hành
 * (18/09 11:45, `ad35e1a`), json không ghi version ⇒ bản của ca này là **[CHƯA BIẾT]**.
 *
 * ## ⚠⚠ Vì sao phải có tệp này, khi đã có `VoiceLogCases0918Test`
 * Tệp kia canh **lượt phân tích ĐẦU**. Nhưng đọc lại chính tệp nhật ký của ca nặng nhất
 * (`20260917-200206-017.json`) thì thấy lượt đầu **không** bắn lệnh nào:
 *
 * ```json
 * { "heard": "tất cả cửa đang khóa hay đang mở",
 *   "decision": "Control(sunroof=1)", "replies": ["✗ Bật Cửa sổ trời — xe không nhận lệnh"],
 *   "clarify": true, "follow_up": true }
 * ```
 *
 * Hai cờ cuối là chỗ mấu chốt: lượt đầu ra `NO_OBJECT`, Kachi **hỏi lại** *"Cửa nào …?"*, người lái đọc một cái
 * tên, và **chính lượt trả lời ấy** mới thành lệnh **mở cửa sổ trời** trên xe đang chạy. Nghĩa là mọi cổng đặt ở
 * lượt đầu — kể cả [VoiceQuestion.isChoice] — đều không đóng được đường này: câu trả lời *"cửa sổ trời"* đứng một
 * mình là một câu ra lệnh hoàn toàn hợp lệ, và nó **không còn mang dấu hiệu nào** của câu hỏi ban đầu.
 *
 * Tệp này đi hết chuỗi THẬT — `parse` → [VoiceClarify.ask] → [VoiceClarify.combine] → `parse` lần hai — đúng
 * đường mà `:app` đang chạy (`VoiceSessionTurns.kt:188`).
 *
 * Android box B2 · W3 (2026-10-09): mọi câu hỏi về xe (cửa · lốp · cửa sổ trời) nay ra `FEATURE_GONE` ở lượt đầu
 * (`VoiceFeatureGoneCarTest`) — không còn lượt hỏi lại nào đi tới lệnh ghi. Còn lại bài canh động từ ĐỌC mang theo.
 */
class VoiceClarifyQuestionTest {

    private val terms = VoiceGrammar.terms()

    private fun one(s: String): VoiceIntent = VoiceIntentParser.parseOne(s)

    private fun ask(s: String): VoiceClarify.Ask? =
        (one(s) as? VoiceIntent.Unknown)?.let { VoiceClarify.ask(it, 0, terms) }

    /** Câu hỏi lại **bắt buộc phải có** — không có thì bài đỏ ngay tại chỗ, kèm ý định thật để đọc. */
    private fun mustAsk(s: String): VoiceClarify.Ask =
        requireNotNull(ask(s)) { "«$s» phải có câu hỏi lại, ý định thật: ${one(s)}" }

    /** Lượt hai như `:app` làm: ghép ngữ cảnh mang theo với câu trả lời, rồi phân tích lại. */
    private fun answer(s: String, said: String): VoiceIntent =
        one(VoiceClarify.combine(mustAsk(s).carry, said))

    // ══ Cổng chính ════════════════════════════════════════════════════════════════════════════════════

    /**
     * Ngữ cảnh mang theo của một câu HỎI là một động từ ĐỌC — và nó phải là động từ **có thật trong bảng**.
     *
     * Chép một chữ không có trong [VoiceGrammar.VERBS] vào [VoiceClarify.READ_VERB] sẽ bịt cổng D1 **mà mọi bài
     * canh hành vi vẫn xanh** (câu ghép rơi vào `NO_VERB` = "không hiểu", nghe như máy chỉ hơi ngớ ngẩn). Nên phép
     * canh ở đây là máy đọc bảng, không phải mắt người.
     */
    @Test fun `READ_VERB phai la mot dong tu DOC cua bang ngu phap`() {
        val hit = VoiceGrammar.VERBS.firstOrNull { it.first == listOf(VoiceClarify.READ_VERB) }
        assertNotNull(hit, "«${VoiceClarify.READ_VERB}» không có trong VoiceGrammar.VERBS")
        assertTrue(VoiceGrammar.isRead(hit!!.second), "«${VoiceClarify.READ_VERB}» phải là động từ ĐỌC, ra: ${hit.second}")
    }

}
