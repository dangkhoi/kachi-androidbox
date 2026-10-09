package com.byd.clusternav.launcher.voice

import com.byd.clusternav.launcher.Lang
import com.byd.clusternav.launcher.LauncherActions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Android box B2 · W2b (2026-10-09) — camera BYD (xi-nhan · 360 · theo yêu cầu) gỡ hẳn. Câu nói về camera phải ra
 * `Unknown(FEATURE_GONE)` với tên tính năng ("camera"), KHÔNG được rơi vào một nút xe khác qua khớp mờ/từ đồng nghĩa
 * (bài học *"bật đèn khẩn cấp"* → mở cốp của [VoiceFeatureGone]), và một app đã cài tên *"Camera"* vẫn mở được bằng lời.
 */
class VoiceCameraGoneTest {

    private fun parse(text: String, apps: List<String> = emptyList()): VoiceIntent =
        VoiceIntentParser.parse(text, apps = apps).single()

    @Test
    fun `cau camera ra FEATURE_GONE, khong thanh lenh nao`() {
        listOf(
            "bật camera", "mở camera sau", "tắt camera", "bật camera 360", "bật camera toàn cảnh",
            "mở camera bên trái", "tắt máy quay sau",
        ).forEach { text ->
            val i = parse(text)
            assertTrue(i is VoiceIntent.Unknown && i.reason == VoiceUnknownReason.FEATURE_GONE, "\"$text\" ⇒ $i")
        }
    }

    @Test
    fun `cau tra loi noi dung ten camera`() {
        val u = parse("bật camera") as VoiceIntent.Unknown
        assertEquals("Tính năng camera đã bỏ khỏi Kachi — dùng màn hình của xe", VoiceReply.unknown(u, Lang.VI))
    }

    @Test
    fun `app da cai ten Camera van mo duoc bang loi`() {
        val apps = listOf("Camera")
        val i = parse("mở ứng dụng camera", apps)
        assertTrue(i is VoiceIntent.OpenApp, "app tên Camera phải mở được: $i")
    }

    @Test
    fun `khong con viec camera nao trong tu vung launcher`() {
        assertTrue(LauncherActions.placeable.none { it.id.startsWith("launcher_cam_") })
        assertTrue(VoiceGrammar.terms().none { "camera" in it.words }, "không cụm tĩnh nào còn chữ camera")
    }
}
