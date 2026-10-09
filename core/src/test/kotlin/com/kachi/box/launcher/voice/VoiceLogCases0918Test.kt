package com.kachi.box.launcher.voice

import com.kachi.box.launcher.Lang
import com.kachi.box.launcher.Strings
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ BÀI CANH DỰNG TỪ **LOG XE THẬT 2026-09-18** ══════════════════════════════════════════════════════════════
 *
 * Nguồn: `docs/diagnostics/oncar-voice-cases-findings-2026-09-18.md` (§D1 · §D2 · §D3) +
 * `oncar-voice-music-vietmap-2026-09-18.md` (§BUG A) — **53 phiên voice thật** (owner + anh em) trên xe bản 1.76,
 * mỗi phiên có `.wav` + `.json` của `VoiceWavProbe`. Mỗi ca dưới đây là MỘT chuỗi người ta **nói thật** kèm hành
 * vi **đo được**, nay khoá về đúng.
 *
 * Tách khỏi [VoiceIntentParserTest] vì tệp đó đã 499/500 dòng (CLAUDE.md §4.1) — và tách theo VAI: tệp kia canh
 * *ngữ pháp* của bộ phân tích, tệp này canh *ba lỗi của một phiên log cụ thể*.
 *
 * ## Vì sao nhóm D1 là nhóm quan trọng nhất
 * Ba ca của nó không phải *"máy không hiểu"* — chúng là *"máy hiểu SAI và làm một việc khác"*, trong đó hai việc
 * chạm **thân xe** trên xe đang chạy (mở cốp · mở cửa sổ trời). Một câu không hiểu thì người ta nói lại; một
 * lệnh sai thì không lấy lại được.
 */
class VoiceLogCases0918Test {

    private fun one(s: String): VoiceIntent = VoiceIntentParser.parseOne(s)

    @AfterEach fun resetLang() { Strings.current = Lang.VI }

    // ══ D1 · CÂU HỎI KHÔNG BAO GIỜ ĐƯỢC THÀNH LỆNH GHI ════════════════════════════════════════════════

    /**
     * ⚠⚠ Ca NẶNG NHẤT của phiên log: *"tất cả cửa đang khóa hay đang mở"* → **`Control(sunroof, 1)`** = **mở
     * cửa sổ trời**.
     *
     * Hai thứ cộng lại mới ra được: bỏ dấu thì *"tất"* = *"tắt"* (một động từ), và câu không khớp datum nào nên
     * [VoicePhoneticMatch] "sửa" một âm tiết thành một cụm của xe rồi đọc lại. Bài này khoá **tính chất**, không
     * khoá một mã cụ thể: đúng chỗ này thì đòi *"không phải lệnh ghi"* mạnh hơn đòi *"phải ra Unknown"*, vì ngày
     * có datum đọc khóa cửa thì câu ấy **nên** thành một câu ĐỌC — và bài này vẫn phải xanh.
     */
    @Test fun `D1 · cau hoi lua chon KHONG BAO GIO ra lenh ghi`() {
        listOf(
            "tất cả cửa đang khóa hay đang mở",
            "cửa đang mở hay đang đóng",
            "xe đang khóa hay chưa",
            "kính đang mở hay đang đóng",
        ).forEach { s ->
            val got = one(s)
            // Android box B2 · W3: không còn lệnh ghi nào ⇒ câu hỏi về xe phải ra "đã gỡ", không thành lệnh khác.
            assertEquals(
                VoiceUnknownReason.FEATURE_GONE, (got as? VoiceIntent.Unknown)?.reason,
                "«$s» là câu HỎI về xe — phải là FEATURE_GONE, ra: $got",
            )
        }
    }

    /**
     * *"bật đèn khẩn cấp"* → **`Control(trunk, 1)`** = **mở cốp**.
     *
     * Gốc: bộ đăng ký không có nút đèn khẩn cấp (chỉ có datum ĐỌC `emergency_alarm`), nên câu rơi vào Unknown và
     * tầng chữa chính tả sửa *"cấp"* → *"cốp"* — đúng cặp lẫn owner đã nêu (*"cốp với cấp khác gì nhau đâu"*).
     */
    @Test fun `D1 · den khan cap KHONG duoc thanh mo cop`() {
        val got = one("bật đèn khẩn cấp")
        assertEquals(VoiceUnknownReason.FEATURE_GONE, (got as? VoiceIntent.Unknown)?.reason, "ra: $got")
    }

    // ══ D2 · CÂU HỢP LỆ MÀ TỪ VỰNG CÒN THIẾU ══════════════════════════════════════════════════════════

    // ══ D3 · TÍNH NĂNG ĐÃ BỎ / KHÔNG CÓ NÚT — trả lời lịch sự, đúng tên ═══════════════════════════════

    @Test fun `D3 · cau ve tinh nang da bo tra loi dung ten`() {
        listOf(
            "kiểm tra dây an toàn",
            "gập gương chiếu hậu",
            "xe đang sạc pin hay không",
        ).forEach { s ->
            val got = one(s)
            assertEquals(VoiceUnknownReason.FEATURE_GONE, (got as? VoiceIntent.Unknown)?.reason, "câu: «$s» ra $got")
            val say = VoiceReply.unknown(got as VoiceIntent.Unknown)
            assertTrue(say.contains("Kachi không có tính năng"), "phải nói là Kachi không có tính năng đó, ra: $say")
        }
    }

    @Test fun `D3 · tinh nang chua bao gio co nut thi noi la chua dieu khien duoc`() {
        val got = one("bật đèn khẩn cấp") as VoiceIntent.Unknown
        val say = VoiceReply.unknown(got)
        assertTrue(say.contains("không điều khiển được"), "ra: $say")
        assertTrue(say.contains("đèn khẩn cấp"), "phải gọi đúng tên tính năng, ra: $say")
        Strings.current = Lang.EN
        assertTrue(VoiceReply.unknown(got).contains("hazard"), "câu tiếng Anh phải nêu tên, ra: ${VoiceReply.unknown(got)}")
    }

    // ══ ⑤ · CHỌN APP DẪN ĐƯỜNG khi tên app bị rụng âm cuối ════════════════════════════════════════════

    /**
     * [ĐO xe 2026-09-18 · §BUG A] Một dòng log, **ba** lỗi: owner nói *"…bằng VietMap"*, mô hình in ra
     * *"bằng vietma"*, Kachi bắn `google.navigation:q=chợ bến thành **bằng vietma**` tới **Google Maps**.
     *
     * Sau vá: tách đúng app **và** địa chỉ sạch rác.
     */
    @Test fun `chon app dan duong chiu duoc ten rung am cuoi`() {
        val want = VoiceIntent.Nav("chợ bến thành", VoiceAppTargets.VIETMAP)
        listOf(
            "dẫn đường tới chợ bến thành bằng vietmap",
            "dẫn đường tới chợ bến thành bằng vietma",
            "dẫn đường tới chợ bến thành bằng việt map",
        ).forEach { assertEquals(want, one(it), "câu: «$it»") }
        assertEquals(VoiceIntent.Nav("sân bay", VoiceAppTargets.WAZE), one("dẫn đường đến sân bay bằng waze"))
    }

    /**
     * …và phép khớp mờ KHÔNG được cắt một điểm đến có chữ *"bằng"* trong tên.
     *
     * Ba cổng của [VoiceAppTargets.bySpokenLoose] + ba cổng của [VoiceTailClause.appAfterMarker] cùng giữ điều
     * này; câu *"cầu Bằng Lăng"* là ca thật mà KDoc `withTarget` đã nêu từ V1.1.
     */
    @Test fun `khop mo ten app khong cat mat diem den`() {
        assertEquals(VoiceIntent.Nav("cầu Bằng Lăng"), one("dẫn đường tới cầu Bằng Lăng"))
        assertEquals(VoiceIntent.Nav("chợ Bằng"), one("dẫn đường tới chợ Bằng"))
        // Rụng âm cuối chỉ nhận khi cụm đủ dài: `bySpokenLoose` từ chối cụm < 5 ký tự.
        assertEquals(null, VoiceAppTargets.bySpokenLoose(listOf("y")))
        assertEquals(null, VoiceAppTargets.bySpokenLoose(listOf("map")))
        assertEquals(VoiceAppTargets.VIETMAP, VoiceAppTargets.bySpokenLoose(listOf("vietma"))?.key)
        // …và chỉ rụng ĐÚNG một ký tự cuối, không phải mọi cụm gần giống.
        assertEquals(null, VoiceAppTargets.bySpokenLoose(listOf("vietm")))
        assertEquals(null, VoiceAppTargets.bySpokenLoose(listOf("vietmax")))
    }

    /**
     * ⑤ · Ba cụm đánh dấu *"bằng / qua / dùng &lt;app&gt;"* — owner nói cả ba, nên cả ba phải tách được app.
     *
     * *"dùng"* là cụm mới (2026-09-18). Bỏ dấu thì `dung` trùng **ba** từ: *"dừng"* (động từ PAUSE), *"đừng"*,
     * *"đúng"* — xem KDoc [VoiceLexicon.BY_APP_MARKERS] về ba cổng cho phép nó vào. Dòng *"vietmáp"* là ca **khớp
     * CHÍNH XÁC** (bỏ dấu ra đúng `vietmap`), cố ý đặt cạnh *"vietma"* để thấy hai đường khác nhau: một cái không
     * cần phép khớp mờ, một cái cần.
     */
    @Test fun `chon app dan duong nhan ca ba cum danh dau`() {
        val want = VoiceIntent.Nav("chợ bến thành", VoiceAppTargets.VIETMAP)
        listOf(
            "dẫn đường tới chợ bến thành bằng vietmap",
            "dẫn đường tới chợ bến thành qua vietmap",
            "dẫn đường tới chợ bến thành dùng vietmap",
            "dẫn đường tới chợ bến thành bằng vietmáp",
            "dẫn đường tới chợ bến thành dùng vietma",
        ).forEach { assertEquals(want, one(it), "câu: «$it»") }
    }

    /**
     * ⚠ …và cụm *"dùng"* KHÔNG được đổi một câu nào đang chạy.
     *
     * Ba câu dưới đây mang đúng chuỗi `dung` sau khi bỏ dấu nhưng **không** có hình dạng `… dùng <tên app>` kết
     * thúc câu, nên cổng (a)+(b) của [VoiceTailClause.appAfterMarker] loại chúng ngay.
     */
    @Test fun `cum danh dau dung KHONG cuop cau dung nhac nao`() {
        assertEquals(VoiceIntent.Media(VoiceMediaOp.PAUSE), one("dừng nhạc"))
        assertEquals(VoiceIntent.Media(VoiceMediaOp.PAUSE), one("tạm dừng"))
        assertEquals(VoiceIntent.Media(VoiceMediaOp.PLAY, app = VoiceAppTargets.YOUTUBE), one("phát nhạc trên youtube"))
        assertTrue(one("đừng mở youtube") !is VoiceIntent.OpenApp, "ra: ${one("đừng mở youtube")}")
    }
}
