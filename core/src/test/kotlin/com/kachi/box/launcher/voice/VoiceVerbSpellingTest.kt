package com.kachi.box.launcher.voice

import com.kachi.box.launcher.Lang
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 2.93 — động từ đầu vế đọc theo dấu ([VoiceVerbSpelling]) và [P3] `VoiceClarify.leadVerb` dùng cùng phép.
 *
 * [ĐO off-car 07/10] *"tất cả kính lên"* (không hiểu ⇒ hỏi lại) mang chữ *"tất"* sang lượt sau như động từ *"tắt"*: trả lời
 * *"mở cốp"* ghép thành *"tất mở cốp"* = `Control(trunk, 0)` — đóng cốp khi người lái xin mở.
 */
class VoiceVerbSpellingTest {

    private fun tok(s: String) = VoiceLexicon.tokenize(s)

    @Test
    fun `chu mang dau chi la dong tu khi dung cach viet`() {
        assertTrue(VoiceVerbSpelling.isVerb(tok("tắt đèn"), 1))
        assertTrue(VoiceVerbSpelling.isVerb(tok("MỞ CỐP"), 1), "chữ HOA — so theo chữ thường")
        assertTrue(VoiceVerbSpelling.isVerb(tok("kiểm tra pin"), 2), "cụm động từ hai từ")
        assertFalse(VoiceVerbSpelling.isVerb(tok("tất cả kính"), 1), "«tất» ≠ «tắt»")
        assertTrue(VoiceVerbSpelling.isVerb(tok("tat ca kinh"), 1), "không dấu ⇒ không dữ liệu ⇒ như cũ")
        assertFalse(VoiceVerbSpelling.isVerb(tok("tắt"), 0))
    }

    /** Senior review wave 2 [P3] — cụm của bảng động từ CHƯA khai cách viết có dấu ⇒ không dữ liệu ⇒ như cũ (là động từ). */
    @Test
    fun `cum dong tu chua khai cach viet co dau thi nhu cu`() {
        assertTrue(VoiceVerbSpelling.isVerb(tok("dẫn tới chợ Bến Thành"), 2))
        assertTrue(VoiceVerbSpelling.isVerb(tok("đưa tôi đến sân bay"), 3))
        assertTrue(VoiceVerbSpelling.isVerb(tok("coi thử pin"), 2))
        assertFalse(VoiceVerbSpelling.isVerb(tok("đừng mở"), 1), "«dừng» ĐÃ khai ⇒ «đừng» không phải động từ")
    }

}
