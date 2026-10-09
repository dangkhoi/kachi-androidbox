package com.byd.clusternav.launcher.voice

import com.byd.clusternav.launcher.LauncherActions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ Android box B2 · W3 (2026-10-09) — câu nói về XE ⇒ "điều khiển xe đã bỏ", KHÔNG BAO GIỜ thành một lệnh khác ═══
 *
 * Lõi HAL BYDAuto + bộ đăng ký nút/datum/gói lệnh gỡ ⇒ `VoiceIntent.Control/Macro/Read` không còn. Câu xe phải ra
 * `Unknown(FEATURE_GONE)` — đặc biệt KHÔNG được rơi xuống đường đoán tên app (*"mở kính"* ⇒ mở app *"Kinh Thánh"*). Mỗi
 * bài dưới đây khoá một lỗi tìm ra lúc gỡ (corpus `VoiceGoldenCoverageTest` + soát tay); gỡ bản vá ⇒ đỏ.
 */
class VoiceFeatureGoneCarTest {

    /** App đã cài có tên GẦN ÂM với câu xe — ca nguy hiểm nhất. */
    private val apps = listOf("Kinh Thánh", "Pin Tester", "Điều Hòa Pro", "Cửa Hàng Play", "YouTube")

    private fun one(s: String, places: List<String> = emptyList()) =
        VoiceIntentParser.parseOne(s, apps = apps, places = places)

    private fun assertGone(s: String, places: List<String> = emptyList()) {
        val got = VoiceIntentParser.parse(s, apps = apps, places = places)
        assertTrue(
            got.isNotEmpty() && got.all { it is VoiceIntent.Unknown && it.reason == VoiceUnknownReason.FEATURE_GONE },
            "«$s» phải là FEATURE_GONE, ra: $got",
        )
    }

    @Test
    fun `cau xe trong yeu cau ra FEATURE_GONE`() {
        listOf(
            "bật điều hoà", "bật điều hòa", "tắt máy lạnh", "mở kính", "mở kính lái", "đóng cửa sổ trời", "pin bao nhiêu",
            "pin còn bao nhiêu", "xem pin", "mở cửa", "mở cốp", "tăng nhiệt độ", "đặt nhiệt độ hai mươi tư", "tăng gió",
            "bật đèn đọc", "áp suất lốp bao nhiêu", "bật sưởi ghế", "xe đang khóa hay chưa", "tốc độ bao nhiêu",
        ).forEach { assertGone(it) }
    }

    /** Hồi quy: không có cổng `carObject` thì *"mở kính"* rơi xuống khớp mờ tên app ⇒ mở *"Kinh Thánh"*. */
    @Test
    fun `cau xe KHONG mo app ten gan am`() {
        listOf("mở kính", "mở kính lái", "bật pin", "mở cửa", "bật điều hòa").forEach { s ->
            assertTrue(one(s) !is VoiceIntent.OpenApp, "«$s» không được mở app: ${one(s)}")
            assertGone(s)
        }
    }

    /** Cổng xe chỉ đứng SAU đường chính xác: gọi ĐÚNG nhãn app đã cài vẫn mở app ấy. */
    @Test
    fun `nhan app that van mo duoc ke ca khi chua chu cua xe`() {
        assertEquals(VoiceIntent.OpenApp("Pin Tester"), one("mở Pin Tester"))
        assertEquals(VoiceIntent.OpenApp("Kinh Thánh"), one("mở Kinh Thánh"))
        assertEquals(VoiceIntent.OpenApp("Điều Hòa Pro"), one("mở Điều Hòa Pro"))
    }

    /** Hồi quy: bỏ dấu "đèn" = "đến" ⇒ *"đèn đọc"* từng thành dẫn đường tới mục sổ *"Đọc"*. */
    @Test
    fun `den doc khong thanh dan duong toi muc so trung ten`() {
        assertGone("đèn đọc", places = listOf("Đọc"))
        assertGone("tắt đèn đọc", places = listOf("Đọc"))
        // …còn động từ viết ĐÚNG "đến" thì vẫn là dẫn đường tới nơi đã lưu.
        assertEquals(VoiceIntent.NavigateSaved("Đọc"), one("đến Đọc", places = listOf("Đọc")))
    }

    /** Hồi quy corpus: chữ NHẠC trong câu về xe (đèn viền theo nhạc · nhạc trên cụm) từng thành lệnh nhạc. */
    @Test
    fun `chu nhac trong cau ve xe khong thanh lenh nhac`() {
        listOf(
            "bật đèn viền theo nhạc", "tắt đèn nhảy theo nhạc", "bật dang viền theo nhạc",
            "bật nhạc trên cụm", "tắt nhạc trên đồng hồ", "mở nhạc trên đồng hồ", "bật nhạc trel cụm",
        ).forEach { assertGone(it) }
    }

    /** …nhưng tên BÀI có chữ xe, hay câu nhạc thường, vẫn là nhạc (cổng chỉ soi chỗ đứng của chữ xe). */
    @Test
    fun `cau nhac thuong va ten bai co chu xe van la nhac`() {
        assertEquals(VoiceIntent.Media(VoiceMediaOp.QUERY, "Đèn Đỏ"), one("phát bài Đèn Đỏ"))
        assertEquals(VoiceIntent.Media(VoiceMediaOp.PLAY), one("phát nhạc"))
        assertEquals(VoiceIntent.Media(VoiceMediaOp.PAUSE), one("tắt nhạc"))
    }

    /** "xăng"/"sạc" là chữ của tên nơi — không được biến câu tìm chỗ thành "đã gỡ". */
    @Test
    fun `tim tram xang tram sac khong phai cau xe`() {
        listOf("tìm trạm xăng", "tìm trạm sạc").forEach { s ->
            assertEquals(VoiceUnknownReason.NO_VERB, (one(s) as? VoiceIntent.Unknown)?.reason, "«$s» ra: ${one(s)}")
        }
        assertEquals(VoiceIntent.Nav("trạm sạc gần nhất"), one("chỉ đường đến trạm sạc gần nhất"))
    }

    /** Hồi quy corpus `unknown_app_close`: *"đóng ứng dụng này"* từng MỞ ngăn kéo ứng dụng. */
    @Test
    fun `dong ung dung khong mo ngan keo`() {
        listOf("đóng ứng dụng này", "đóng giúp mình cái ứng dụng đang mở", "tắt ứng dụng").forEach { s ->
            assertEquals(VoiceUnknownReason.APP_CLOSE, (one(s) as? VoiceIntent.Unknown)?.reason, "«$s» ra: ${one(s)}")
        }
        assertEquals(VoiceIntent.Launcher(LauncherActions.APPS), one("mở ứng dụng"))
    }

    @Test
    fun `cau tra loi noi dieu khien xe da bo o ca hai tieng`() {
        val vi = VoiceReply.unknown(VoiceIntent.Unknown(VoiceUnknownReason.FEATURE_GONE, "mở kính"), com.byd.clusternav.launcher.Lang.VI)
        assertTrue(vi.contains("điều khiển xe") && vi.contains("đã bỏ"), vi)
        val en = VoiceReply.unknown(VoiceIntent.Unknown(VoiceUnknownReason.FEATURE_GONE, "mở kính"), com.byd.clusternav.launcher.Lang.EN)
        assertTrue(en.contains("Car control") && en.contains("removed"), en)
    }
}
