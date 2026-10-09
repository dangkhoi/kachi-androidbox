package com.byd.clusternav.launcher.voice

import com.byd.clusternav.launcher.Lang
import com.byd.clusternav.launcher.LayoutPreset
import com.byd.clusternav.launcher.Strings
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.byd.clusternav.launcher.voice.VoiceIntentParserHarness.all
import com.byd.clusternav.launcher.voice.VoiceIntentParserHarness.apps
import com.byd.clusternav.launcher.voice.VoiceIntentParserHarness.expect
import com.byd.clusternav.launcher.voice.VoiceIntentParserHarness.one
import com.byd.clusternav.launcher.voice.VoiceIntentParserHarness.profiles
import com.byd.clusternav.launcher.voice.VoiceIntentParserHarness.unknown

/**
 * ═══ V1 · BÀI CANH BỘ PHÂN TÍCH Ý ĐỊNH ════════════════════════════════════════════════════════════════════════
 *
 * Spec `docs/specs/kachi-voice-command.html` R7. Nguồn câu mẫu: **44 mẫu câu tiếng Việt [ĐO] nguyên văn** trong
 * `docs/diagnostics/kiki-car-RE-2026-09-14.md` §7(c) — chúng là chứng cứ *"người Việt thật sự nói thế nào với trợ
 * lý trên xe"*, đã qua kiểm chứng thị trường; ta chuyển thể sang tập đóng của Kachi.
 *
 * ## Vì sao nhiều ca kỳ vọng [VoiceIntent.Unknown] — và đó là ĐÚNG
 * Radio, truyền hình, tin tức, hỏi đáp, toán: Kachi **cố ý không làm** (phương án C, RE §8.2 — Kiki giữ phần từ
 * vựng mở, Kachi giữ phần xe + launcher chạy offline). Khoá chúng lại bằng test để lần sau không ai "tiện tay"
 * thêm một nhánh đoán mò: một câu trả lời sai còn tệ hơn một câu *"tôi không làm được việc này"*.
 */
class VoiceIntentParserTest {

    @AfterEach fun resetLang() { Strings.current = Lang.VI }

    // ══ A · DẪN ĐƯỜNG (Kiki §7c #1–#6) — điểm đến là từ vựng MỞ, giữ nguyên văn ════════════════════════

    @Test fun `dan duong giu nguyen van diem den`() = expect(
        "Dẫn đường đến chợ Bến Thành" to VoiceIntent.Nav("chợ Bến Thành"),
        "Chỉ đường đến số 72 Nguyễn Cơ Thạch" to VoiceIntent.Nav("số 72 Nguyễn Cơ Thạch"),
        "Chỉ đường đến trạm xăng gần nhất" to VoiceIntent.Nav("trạm xăng gần nhất"),
        "Chỉ đường đến Hà Nội" to VoiceIntent.Nav("Hà Nội"),
        "Dẫn đường tới sân bay Nội Bài" to VoiceIntent.Nav("sân bay Nội Bài"),
        "Navigate to Ben Thanh market" to VoiceIntent.Nav("Ben Thanh market"),
    )

    /**
     * *"trạm sạc"* chứa cụm *"sạc"* của xe ở nhiều biến thể — nếu đem điểm đến so với từ vựng xe thì một
     * câu dẫn đường sẽ biến thành lệnh sạc pin. Ca này khoá luật *"sau động từ NAV thì KHÔNG khớp từ vựng xe"*.
     */
    @Test fun `diem den KHONG bi khop nham vao tu vung xe`() = expect(
        "Chỉ đường đến trạm sạc gần nhất" to VoiceIntent.Nav("trạm sạc gần nhất"),
        "Dẫn đường đến chợ Gió" to VoiceIntent.Nav("chợ Gió"),
    )

    // ══ B · NHẠC (Kiki §7c #7–#13) — tên bài/ca sĩ/thể loại = từ vựng MỞ ══════════════════════════════

    @Test fun `mo bai mo nhac ra Media QUERY`() = expect(
        "Mở bài Nồng nàn Hà Nội" to VoiceIntent.Media(VoiceMediaOp.QUERY, "Nồng nàn Hà Nội"),
        "Mở nhạc Trữ tình" to VoiceIntent.Media(VoiceMediaOp.QUERY, "Trữ tình"),
        "Mở nhạc Bolero" to VoiceIntent.Media(VoiceMediaOp.QUERY, "Bolero"),
        "Mở nhạc trẻ remix" to VoiceIntent.Media(VoiceMediaOp.QUERY, "trẻ remix"),
        "Phát bài Diễm xưa" to VoiceIntent.Media(VoiceMediaOp.QUERY, "Diễm xưa"),
    )

    @Test fun `lenh phat khong co ten bai`() = expect(
        "Phát nhạc" to VoiceIntent.Media(VoiceMediaOp.PLAY),
        "Mở nhạc" to VoiceIntent.Media(VoiceMediaOp.PLAY),
        "Tắt nhạc" to VoiceIntent.Media(VoiceMediaOp.PAUSE),
        "Dừng nhạc" to VoiceIntent.Media(VoiceMediaOp.PAUSE),
        "Chuyển bài tiếp theo" to VoiceIntent.Media(VoiceMediaOp.NEXT),
        "Bài trước" to VoiceIntent.Media(VoiceMediaOp.PREV),
        "Next track" to VoiceIntent.Media(VoiceMediaOp.NEXT),
    )

    // ══ C · ÂM LƯỢNG (Kiki §7c #14–#16) ═══════════════════════════════════════════════════════════════

    // ══ D · NHỮNG THỨ KACHI CỐ Ý KHÔNG LÀM (Kiki §7c #18–#41, #44) ════════════════════════════════════

    @Test fun `radio truyen hinh tin tuc hoi dap deu KHONG doan mo`() {
        unknown("Mở radio", VoiceUnknownReason.NO_OBJECT)
        unknown("Mở kênh VOV Giao thông", VoiceUnknownReason.NO_OBJECT)
        unknown("Tin thời sự hôm nay", VoiceUnknownReason.NO_VERB)
        unknown("Quốc ca được sáng tác năm nào", VoiceUnknownReason.NO_VERB)
        unknown("Thời tiết hôm nay thế nào", VoiceUnknownReason.NO_OBJECT)
        unknown("12 x 15 bằng bao nhiêu", VoiceUnknownReason.NO_OBJECT)
        unknown("Kiki ơi", VoiceUnknownReason.NO_VERB)
        // Android box B2 · W3: "tốc độ" là số liệu xe ⇒ "đã gỡ" (vẫn KHÔNG đoán mở gì).
        unknown("Tốc độ giới hạn ở đây là 50km/h", VoiceUnknownReason.FEATURE_GONE)
        unknown("Tìm trạm xăng gần đây", VoiceUnknownReason.NO_VERB)
        unknown("", VoiceUnknownReason.EMPTY)
    }

    // ══ E · NÚT XE — 5 kiểu ControlKind ═══════════════════════════════════════════════════════════════

    // ══ F · BA CẶP NHÃN LỒNG NHAU (backlog L-RE2) — luật "dãy dài nhất thắng" ══════════════════════════

    /**
     * ═══ WP8 · ĐIỀU KIỆN KẾT NẠP của [VoiceFeatureGone.HARD_BLOCK] — canh bằng MÁY, không bằng lời ═══════════
     *
     * Bảng chặn cứng được hỏi **trước mọi phép khớp**, nên một từ lọt vào đó sẽ giết **mọi** câu chứa nó — kể cả
     * câu của một tính năng đang chạy tốt, và giết **im lặng** (người lái chỉ thấy *"đã bỏ"*). KDoc của bảng đặt ra
     * đúng một điều kiện cho việc thêm từ: *không nhãn/từ-đồng-nghĩa nào còn chứa nó*. Điều kiện ấy tới nay chỉ là
     * một câu văn kèm *"[ĐO grep sau purge]"* — tức nó đúng ở thời điểm viết và không có gì giữ cho nó còn đúng.
     *
     * Bài này biến nó thành phép kiểm: mỗi từ chặn cứng phải **không** xuất hiện trong từ vựng SINH từ bộ đăng ký.
     * Hệ quả thực dụng: hôm nào ai đó thêm lại một nút có chữ `hud` trong nhãn, bài này đỏ **trước** khi nút đó ra
     * xe và chết không hiểu vì sao.
     *
     * ⚠ Chỉ phủ được từ vựng TĨNH (nút · datum · nhóm · gói lệnh). Nhãn app do máy cài sinh ra là động ⇒ một app
     * tên *"HUD …"* vẫn lọt; đó là giới hạn đã biết, không phải chỗ quên.
     */
    @Test fun `moi tu chan cung KHONG duoc nam trong tu vung dang song`() {
        val vocab = VoiceGrammar.terms(profiles, apps)
        VoiceFeatureGone.HARD_BLOCK.forEach { blocked ->
            val clash = vocab.filter { blocked in it.words }
            assertTrue(
                clash.isEmpty(),
                "từ chặn cứng \"$blocked\" còn nằm trong từ vựng đang sống ⇒ nó sẽ giết chính tính năng đó: " +
                    clash.joinToString { "${it.kind}/${it.id}=${it.words}" },
            )
        }
    }

    // ══ G · NHÃN TRÙNG giữa ĐỌC và HÀNH ĐỘNG — loại động từ quyết định ════════════════════════════════

    // ══ H · ĐỌC THÔNG TIN ═════════════════════════════════════════════════════════════════════════════

    // ══ I · GÓI LỆNH · LAUNCHER · HỒ SƠ · APP ═════════════════════════════════════════════════════════

    @Test fun `launcher ho so va app`() = expect(
        "Mở Cài đặt" to VoiceIntent.Launcher("launcher_settings"),
        "Mở ứng dụng" to VoiceIntent.Launcher("launcher_apps"),
        "Mở ứng dụng VTV Go" to VoiceIntent.OpenApp("VTV Go"),
        "Mở YouTube" to VoiceIntent.OpenApp("YouTube"),
        "Chuyển sang hồ sơ Vợ" to VoiceIntent.Profile("Vợ"),
        "Đổi sang Mặc định" to VoiceIntent.Profile("Mặc định"),
    )

    // ══ J · CÂU GHÉP (Kiki §7c #42) ═══════════════════════════════════════════════════════════════════

    @Test fun `cau ghep tach theo thu tu noi`() {
        assertEquals(
            listOf(VoiceIntent.Nav("Bitexco"), VoiceIntent.Media(VoiceMediaOp.QUERY, "trẻ")),
            all("Chỉ đường đến Bitexco và mở nhạc trẻ"),
        )
        // Android box B2 · W3: hai câu ghép mẫu cũ là lệnh xe ⇒ việc launcher.
        assertEquals(listOf(VoiceIntent.Launcher("launcher_settings"), VoiceIntent.Launcher("launcher_apps")), all("Mở cài đặt rồi mở ứng dụng"))
    }

    /**
     * [ĐO] mẫu câu Kiki #10 — chữ *"và"* nằm TRONG tên bài hát. Tách vô điều kiện sẽ gửi đi nửa cái tên.
     * Luật *"mọi vế phải hiểu được, không thì trả nguyên câu"* tự xử ca này mà không cần biết bài hát nào có chữ "và".
     */
    @Test fun `va nam trong ten bai hat thi KHONG tach`() = assertEquals(
        listOf(VoiceIntent.Media(VoiceMediaOp.QUERY, "Cỏ dại và hoa dành dành")),
        all("Mở bài Cỏ dại và hoa dành dành"),
    )

    // ══ K · KHÔNG DẤU · HOA THƯỜNG · DẤU CÂU ══════════════════════════════════════════════════════════

    /**
     * Chuỗi vào tầng này có thể tới từ bàn phím xe (thường **không dấu**), từ kịch bản test, và mai kia từ ASR.
     * Không chịu được cả ba dạng thì đường thử bằng chữ vô dụng ngay từ đầu.
     */
    @Test fun `khong dau hoa thuong dau cau deu ra cung mot y dinh`() {
        val want = VoiceIntent.Launcher("launcher_settings")
        listOf("Mở cài đặt", "mo cai dat", "MỞ CÀI ĐẶT", "  mở  cài   đặt !! ", "Kachi, mở cài đặt")
            .forEach { assertEquals(want, one(it), "câu: \"$it\"") }
    }
}
