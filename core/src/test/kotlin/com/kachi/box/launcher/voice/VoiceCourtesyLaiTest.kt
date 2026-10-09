package com.kachi.box.launcher.voice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 2.93 VOICE-COURTESY-LAI-HOMOGRAPH (spec `kachi-293-voice.html` §10) — [VoiceLexicon.stripCourtesy] không cắt chữ «lái» mang dấu
 * ở cuối câu như tiếng đệm «lại».
 *
 * [ĐO off-car 07/10] bỏ dấu thì «lái» = «lại» = `lai`: *"mở một nửa kính lái"* (5 từ) bị cắt thành *"mở một nửa kính"* — mất đúng
 * chữ chỉ kính nào. Luật [VoiceHomograph]: chữ MANG dấu chỉ là đệm khi đúng cách viết «lại»; chữ không dấu ⇒ như cũ (cắt).
 */
class VoiceCourtesyLaiTest {

    private fun strip(text: String): List<String> =
        VoiceLexicon.stripCourtesy(VoiceLexicon.tokenize(text)).map { it.norm }

    @Test
    fun `lai mang dau o cuoi cau duoc giu`() {
        assertEquals(listOf("mo", "mot", "nua", "kinh", "lai"), strip("mở một nửa kính lái"))
        assertEquals(listOf("mo", "mot", "nua", "kinh", "lai"), strip("MỞ MỘT NỬA KÍNH LÁI"), "chữ HOA mô hình in")
        assertEquals(listOf("dong", "cua", "kinh", "ben", "lai"), strip("đóng cửa kính bên lái"))
        // Android box B2 · W3: câu xe không còn lệnh ⇒ báo "đã gỡ", không thành lệnh khác.
        assertEquals(VoiceUnknownReason.FEATURE_GONE, (VoiceIntentParser.parseOne("mở một nửa kính lái") as? VoiceIntent.Unknown)?.reason)
    }

    @Test
    fun `dem lai van cat nhu cu`() {
        assertEquals(listOf("dong", "cua", "so", "troi"), strip("đóng cửa sổ trời lại"), "«lại» đúng cách viết ⇒ đệm")
        assertEquals(listOf("mo", "mot", "nua", "kinh"), strip("mo mot nua kinh lai"), "không dấu ⇒ không dữ liệu ⇒ như cũ")
        assertEquals(VoiceUnknownReason.FEATURE_GONE, (VoiceIntentParser.parseOne("đóng cửa sổ trời lại") as? VoiceIntent.Unknown)?.reason)
    }

    @Test
    fun `mo lai va mo lai di khong doi`() {
        assertEquals(listOf("mo", "lai"), strip("mở lại"))
        assertEquals(listOf("mo", "lai"), strip("mở lại đi"))
        assertEquals(listOf("bat", "suoi", "ghe", "lai"), strip("bật sưởi ghế lái"), "luật «ghế lái» cũ vẫn giữ")
    }
}
