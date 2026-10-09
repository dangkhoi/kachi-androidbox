package com.byd.clusternav.launcher.voice

import com.byd.clusternav.launcher.Lang
import com.byd.clusternav.launcher.LayoutPreset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 2.96 R12 (owner 07/10) — câu phản hồi THUẬN MIỆNG. Khoá đúng các câu owner chỉ ra (*"Tắt kính lái"* → đóng kính;
 * *"Sưởi ghế phụ: Tắt"* → *"đã tắt sưởi ghế phụ"*) + các câu 2.95 đọc ra sai ngữ pháp (*"Đã gió: AUTO"*, *"Đã bố cục 2
 * cột"*, *"Đã camera sau"*). Bảng trước → sau: `docs/diagnostics/voice-phrasing-296.md`.
 */
class VoicePhrasing296Test {

    private val vi = Lang.VI
    private fun spoken(vararg lines: String) = VoiceFeedbackPhrase.merge(lines.toList(), vi)

    @Test fun `app, bo cuc, dan duong, gio AUTO, camera - het cau DA + danh tu`() {
        assertEquals("✓ Đã mở YouTube", VoiceReply.done(VoiceIntent.OpenApp("YouTube"), vi))
        assertEquals("✓ Đã mở YouTube vào ô 2", VoiceReply.done(VoiceIntent.OpenApp("YouTube", slot = 2), vi))
        assertTrue(VoiceReply.done(VoiceIntent.Layout(LayoutPreset.TWO_COL), vi).startsWith("✓ Đã chuyển sang bố cục "))
        assertEquals("✓ Đã bắt đầu dẫn đường tới Bitexco", VoiceReply.done(VoiceIntent.Nav("Bitexco"), vi))
        assertEquals("Đã phát nhạc", spoken(VoiceReply.done(VoiceIntent.Media(VoiceMediaOp.PLAY), vi)))
        assertEquals("Đã chuyển bài tiếp theo", spoken(VoiceReply.done(VoiceIntent.Media(VoiceMediaOp.NEXT), vi)))
    }

    @Test fun `nhieu viec - mot loi dan cho mot day, DANG tach rieng`() {
        // Android box B2 · W3: câu mẫu cũ là nút xe (đèn đọc · sưởi ghế) ⇒ đổi sang việc app/nhạc.
        val a = VoiceReply.done(VoiceIntent.OpenApp("YouTube"), vi)
        val b = VoiceReply.done(VoiceIntent.Media(VoiceMediaOp.NEXT), vi)
        assertEquals("Đã mở YouTube, chuyển bài tiếp theo", spoken(a, b))
        assertEquals("Đã mở YouTube, đang mở VietMap", spoken(a, "✓ Đang mở VietMap"))
        // Dòng KHÔNG mang lời dẫn (ca một-phần, câu xem-trước) vẫn nhận "Đã" như 2.95.
        assertEquals("Đã mở YouTube, dẫn đường tới X", spoken(a, "✓ Dẫn đường tới X"))
    }

    @Test fun `ca mot-phan KHONG noi DA cho viec chua xay ra`() {
        val i = VoiceIntent.Media(VoiceMediaOp.PLAY)
        val t = VoiceAppTargets.ALL.first()
        assertTrue(VoiceReply.musicAppOpened(i, t, vi).startsWith("✓ Phát nhạc"), "giữ câu xem-trước — không 'Đã phát nhạc' khi chưa có phiên")
        assertTrue(VoiceReply.navOpenedNoHandover(VoiceIntent.Nav("X"), t, vi).startsWith("✓ Dẫn đường tới X"))
    }

    @Test fun `tam biet tu nhien`() {
        assertEquals("Tạm biệt, hẹn gặp lại", spoken(VoiceReply.bye(vi)))
        assertEquals("Bye, see you", VoiceReply.bye(Lang.EN))
    }

    /** Tiếng Anh giữ khuôn lời dẫn "Done: " (giọng EN) + chữ thường đầu dòng. */
    @Test fun `tieng Anh giu khuon`() {
        val en = Lang.EN
        assertEquals("Done: close driver window", VoiceFeedbackPhrase.merge(listOf("✓ Close driver window"), en))
    }
}
