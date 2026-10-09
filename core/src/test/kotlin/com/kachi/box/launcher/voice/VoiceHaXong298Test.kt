package com.kachi.box.launcher.voice

import com.kachi.box.launcher.LauncherActions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ 2.98 R2 — liên từ "xong" (spec `kachi-298-plan.html` §3) ══════════════════════════════════════════════════════════
 *
 * R2 `VOICE-XONG-CONNECTOR`: *"… xong …"* từng ra MỘT lệnh, vế sau mất im lặng — *"xong"* không phải liên từ của
 * [VoiceIntentParser.parse]. Gỡ bản vá ⇒ các bài `xong …` dưới đây đỏ.
 *
 * Android box B2 · W3 (2026-10-09): R1 (`VOICE-HA-HOMOGRAPH` — "hạ kính"/"hạ cốp") gỡ cùng lệnh xe; câu mẫu của R2
 * (kính · đèn đọc · cốp) đổi sang lệnh launcher/app — phép tách là của bộ phân tích, không của nút xe.
 */
class VoiceHaXong298Test {

    private val apps = listOf("YouTube", "Spotify")
    private fun p(s: String) = VoiceIntentParser.parse(s, apps = apps)
    private val settings = VoiceIntent.Launcher(LauncherActions.SETTINGS)
    private val appList = VoiceIntent.Launcher(LauncherActions.APPS)

    /** "hả/hạ kính" — câu xe đã gỡ, mọi cách viết đều KHÔNG thành mở một app hay việc launcher. */
    @Test
    fun `ha kinh ha cop khong thanh lenh khac`() {
        listOf("hả kính lái", "hạ kính lái", "hạ cốp", "ha kinh lai").forEach { s ->
            val got = p(s)
            assertTrue(
                got.none { it is VoiceIntent.OpenApp || it is VoiceIntent.Launcher || it is VoiceIntent.Media },
                "«$s» không được thành lệnh khác: $got",
            )
        }
    }

    @Test
    fun `xong noi hai lenh day du`() {
        assertEquals(listOf(settings, VoiceIntent.OpenApp("YouTube")), p("mở cài đặt xong mở YouTube"))
        assertEquals(listOf(VoiceIntent.OpenApp("YouTube"), settings), p("MỞ YOUTUBE XONG MỞ CÀI ĐẶT".lowercase()))
    }

    @Test
    fun `xong cung voi va trong mot cau`() {
        assertEquals(
            listOf(settings, VoiceIntent.OpenApp("YouTube"), appList),
            p("mở cài đặt xong mở YouTube và mở ứng dụng"),
        )
    }

    /** "xong" trong TÊN bài (từ vựng mở) không bị cắt thành lệnh — cùng luật "mọi vế phải hiểu được" của "và"/"rồi". */
    @Test
    fun `xong trong ten bai khong tach`() {
        val got = p("phát bài chưa xong")
        assertEquals(1, got.size, "$got")
        val m = got.single() as VoiceIntent.Media
        assertTrue(VoiceLexicon.deaccent(m.query).contains("xong"), "$got")
    }

    /** Vế sau không hiểu được ⇒ KHÔNG im lặng: phải có dòng *"đã bỏ qua"* (DROPPED_CLAUSE). */
    @Test
    fun `xong ve sau vo nghia thi bao da bo qua`() {
        val got = p("mở cài đặt xong con mèo nhà bên")
        assertEquals(settings, got.first(), "$got")
        assertTrue(got.any { it is VoiceIntent.Unknown && it.reason == VoiceUnknownReason.DROPPED_CLAUSE }, "phải báo đã bỏ qua: $got")
    }

    /** Câu kết thúc phiên (2.96) giữ nguyên — [VoiceEndWords] nhận trước phép tách. */
    @Test
    fun `xong roi van la ket thuc phien`() {
        listOf("xong", "xong rồi", "xong việc", "là xong", "xong rồi cảm ơn nhé").forEach {
            assertEquals(listOf(VoiceIntent.EndSession), p(it), "«$it»")
        }
    }

    /** "xong" đứng CUỐI câu không sinh vế rỗng — câu đi như một vế. */
    @Test
    fun `xong cuoi cau khong lam hong lenh`() {
        assertEquals(listOf(settings), p("mở cài đặt xong"))
    }
}
