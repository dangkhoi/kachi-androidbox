package com.kachi.box.launcher.voice

import com.kachi.box.launcher.Lang
import com.kachi.box.launcher.LayoutPreset
import com.kachi.box.launcher.Strings
import com.kachi.box.launcher.voice.VoiceIntentParserHarness.all
import com.kachi.box.launcher.voice.VoiceIntentParserHarness.apps
import com.kachi.box.launcher.voice.VoiceIntentParserHarness.expect
import com.kachi.box.launcher.voice.VoiceIntentParserHarness.one
import com.kachi.box.launcher.voice.VoiceIntentParserHarness.profiles
import com.kachi.box.launcher.voice.VoiceIntentParserHarness.unknown
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Nửa sau của [VoiceIntentParserTest] — câu ĐỜI THƯỜNG (soát 2026-09-14) · bố cục bằng giọng nói (L7) · cụm hỏi giữa câu ·
 * số nói rút gọn · danh từ đầu câu · câu phản hồi · LOG XE 2026-09-17. Tách THUẦN theo CHỦ ĐỀ (568 dòng → trần 500, L6-debt
 * 2026-09-27); thân từng bài giữ nguyên byte, trợ giúp dùng chung ở [VoiceIntentParserHarness].
 */
class VoiceIntentParserEverydayTest {

    @AfterEach fun resetLang() { Strings.current = Lang.VI }

    // ══ L · CÂU PHẢN HỒI SONG NGỮ (R5) ════════════════════════════════════════════════════════════════

    // ══ M · CÂU ĐỜI THƯỜNG (soát 2026-09-14) ══════════════════════════════════════════════════════════
    //
    // Mười câu người ta nói hằng ngày, gõ thử trong lượt soát senior. Bốn câu ĐỎ và chúng đỏ theo bốn kiểu
    // khác nhau — mỗi kiểu nay có một dòng vá ở MỘT chỗ (VoiceSynonyms / VoiceGrammar.VERBS / askAt), không
    // chỗ nào là `if (id == "…")`. Giữ nguyên cả mười ở đây để lần sau sửa ngữ pháp còn biết mình phá cái gì.

    @Test fun `cac cau doi thuong deu hieu duoc`() = expect(
        // Android box B2 · W3: ba câu xe ("mở cửa sổ" · "bật máy lạnh" · "xem pin") nay ra FEATURE_GONE —
        // `VoiceFeatureGoneCarTest`.
        "đổi sang hồ sơ Mặc định" to VoiceIntent.Profile("Mặc định"),
        "phát nhạc" to VoiceIntent.Media(VoiceMediaOp.PLAY),
        // Trước: NO_VERB (*"bài"* là từ khoá NHẠC, mà `headMatch` không nhận loại đó).
        "bài tiếp" to VoiceIntent.Media(VoiceMediaOp.NEXT),
        // ⚠ 1.90: ca *"dừng chiếu"* (→ `cast`) gỡ cùng nút. Chiếu cụm nay bật/tắt bằng nút nổi + Cài đặt.
        "mở VietMap Live" to VoiceIntent.OpenApp("VietMap Live"),
    )

    /** Câu còn lại: KHÔNG hiểu được và **đúng ra là không nên** hiểu. */
    @Test fun `cau con lai noi thang la chua lam duoc`() {
        // *"tắt hết đèn"*: Kachi không có khả năng "mọi đèn" (không nút gộp, không gói lệnh) ⇒ nói thẳng còn hơn
        // tự chọn một cái đèn nào đó. Ngày có gói lệnh "tắt hết đèn", câu này tự hiểu được (từ vựng SINH từ registry).
        // Android box B2 · W3: "đèn" là đồ vật của xe ⇒ "đã gỡ" (không đoán một cái đèn nào, như trước).
        unknown("tắt hết đèn", VoiceUnknownReason.FEATURE_GONE)
    }

    /**
     * ⚠⚠ **ĐỔI KỲ VỌNG CÓ CHỦ Ý (L7, owner duyệt 2026-09-16)** — *"về bố cục 2 cột"*.
     *
     * Từ 1.64 câu này được giữ ở [VoiceUnknownReason.NO_VERB] vì bố cục **chưa** nằm trong tập đóng của giọng
     * nói. Nay nó nằm rồi ([VoiceLayouts]), nên kỳ vọng đổi sang [VoiceIntent.Layout] — đây là *"tính năng mới
     * phủ lên một ca đang để ngỏ"*, không phải *"sửa test cho hết đỏ"*.
     *
     * Thứ bài canh này **luôn** canh thì KHÔNG đổi: `về` vẫn không được thành động từ dẫn đường vô điều kiện
     * ([VoiceGrammar.VERBS] không có nó; xem chú thích ⚠⚠ ở đó). Ba câu dưới khoá đúng ranh giới ấy — chỉ đuôi
     * *"bố cục …"* mới thành [VoiceIntent.Layout], mọi đuôi khác vẫn `NO_VERB`, tuyệt đối không thành `Nav`.
     */
    @Test fun `ve bo cuc la LAYOUT, con ve mot chuoi la thi van NO_VERB`() {
        assertEquals(VoiceIntent.Layout(LayoutPreset.TWO_COL), one("về bố cục 2 cột"))
        unknown("về Bitexco", VoiceUnknownReason.NO_VERB)
        unknown("về chỗ nào đó", VoiceUnknownReason.NO_VERB)
    }

    /**
     * L7 — bảng cách nói bố cục, gồm cả số bằng CHỮ (mô hình nghe trả chữ, không trả chữ số).
     *
     * *"bố cục hai ô"* ra [LayoutPreset.TWO_COL], không phải `TWO_ROW` — quyết định ghi ở KDoc [VoiceLayouts].
     */
    @Test fun `bo cuc bang giong noi`() = expect(
        "bố cục 1 ô" to VoiceIntent.Layout(LayoutPreset.ONE),
        "bố cục một ô" to VoiceIntent.Layout(LayoutPreset.ONE),
        "bố cục 2 cột" to VoiceIntent.Layout(LayoutPreset.TWO_COL),
        "bố cục hai cột" to VoiceIntent.Layout(LayoutPreset.TWO_COL),
        "bố cục hai ô" to VoiceIntent.Layout(LayoutPreset.TWO_COL),
        "bố cục 2 hàng" to VoiceIntent.Layout(LayoutPreset.TWO_ROW),
        "bố cục hai hàng" to VoiceIntent.Layout(LayoutPreset.TWO_ROW),
        "bố cục 3 ô" to VoiceIntent.Layout(LayoutPreset.THREE),
        "bố cục bốn ô" to VoiceIntent.Layout(LayoutPreset.QUAD),
        "đổi sang bố cục 2 cột" to VoiceIntent.Layout(LayoutPreset.TWO_COL),
        "chuyển bố cục 4 ô" to VoiceIntent.Layout(LayoutPreset.QUAD),
        "đổi bố cục 4 ô" to VoiceIntent.Layout(LayoutPreset.QUAD),
        "bố cục 4" to VoiceIntent.Layout(LayoutPreset.QUAD),
    )

    /** Cụm đánh dấu phải có, phần đuôi phải khớp TRỌN — không thì im lặng đi tiếp, không đoán. */
    @Test fun `cau khong phai bo cuc thi VoiceLayouts khong dung vao`() {
        unknown("hai cột", VoiceUnknownReason.NO_VERB)
        unknown("bố cục mười hai ô", VoiceUnknownReason.NO_VERB)
        unknown("bố cục 2 cột màu xanh", VoiceUnknownReason.NO_VERB)
        // Hai chữ *"bố cục"* nằm giữa một câu KHÁC (ở đây là tên bài hát) ⇒ [VoiceLayouts.LEAD_WORDS] chặn:
        // *"mở bài …"* vẫn là một câu nhạc, không bị cướp thành lệnh đổi bố cục.
        assertEquals(
            VoiceIntent.Media(VoiceMediaOp.QUERY, "bố cục hai cột"),
            one("mở bài bố cục hai cột"),
        )
    }

    /** …nhưng vẫn KHÔNG được biến câu hỏi ngoài tập đóng thành một câu trả lời bịa. */
    @Test fun `cum hoi o giua cau khong keo theo cau hoi ngoai tap dong`() {
        unknown("12 x 15 bằng bao nhiêu", VoiceUnknownReason.NO_OBJECT)
        unknown("Thời tiết hôm nay thế nào", VoiceUnknownReason.NO_OBJECT)
    }

    // ⚠ Hai bài về PHẠM VI câu nói về kính (cụm mơ hồ vs tường minh · mức Nửa) đã sang
    // `VoiceWindowScopeTest` ở lượt D 2026-09-19 — tệp này đứng sát trần 500 dòng, tách theo CHỦ ĐỀ.

    /**
     * ═══ [SOÁT 1.69 · P1] Câu KHÔNG có động từ mà chỉ *bắt đầu* bằng một cái tên ⇒ **không phải một lệnh** ═══
     *
     * Bệnh: nhánh (b') của [VoiceIntentParser] gắn một **động từ ngầm** cho cụm khớp tại vị trí 0, còn phần
     * đuôi thì các nhánh TOGGLE/COVER/BUTTON/MACRO của `build` **không hề đọc**. Cộng với 29 cụm MỘT từ trong
     * bộ đăng ký trùng tiếng Việt đời thường sau khi bỏ dấu (`cop` · `kinh` · `gio` · `chieu` · `tieng`…),
     * [ĐO off-car 2026-09-17] ba câu dưới đây từng ra **hành động thân xe**:
     *  • *"chiều nay mấy giờ về"* ⇒ `Control(cast, 1)`
     *  • *"cốp xe bẩn quá"* ⇒ `Control(trunk, 1)` — mở cốp trên xe đang chạy
     *  • *"kính bẩn quá"* ⇒ `Control(windows_all, 1)` — hạ hết kính (sau lượt D: `Control(window, 1)`)
     *
     * Bài này khoá đúng điều đó. Gỡ dòng `verbHit != null || after.isEmpty() || readsTail(head)` ở nhánh (b')
     * thì ba dòng đầu đỏ ngay — đã thử, đúng ba giá trị ghi trên.
     */
    @Test fun `danh tu dau cau khong co dong tu KHONG duoc thanh hanh dong`() {
        listOf("chiều nay mấy giờ về", "cốp xe bẩn quá", "kính bẩn quá", "tiếng gì lạ vậy").forEach {
            val got = VoiceIntentParser.parseOne(it)
            assertTrue(got is VoiceIntent.Unknown, "«$it» KHÔNG phải lệnh — phải là Unknown, ra: $got")
        }
    }

    // ══ LOG XE 2026-09-17 — số & câu hỏi (nguồn: /tmp/kvlog, 81 lượt thật) ══════════════════════════════
    //
    // Mỗi ca dưới đây là MỘT chuỗi owner/bạn bè NÓI THẬT trên xe + hành vi SAI đo được, nay khoá về đúng.

    /**
     * Log xe (build cũ) từng BẮN NHẦM điều khiển cho câu về feature ĐÃ GỠ / không phải control / xe không có.
     * 1.73 hiện trả Unknown (→ hỏi lại) — KHÓA lại để không tái phát thành bắn nhầm nguy hiểm (đèn pha, cửa sổ trời).
     * ⚠ [ĐO xe 2026-09-18] `chỉ số xăng` **rời khỏi danh sách này**: nay ra `Read(fuel_pct)`, một datum có thật và
     * đúng thứ câu ấy hỏi (`VoiceLogCases0918Test`). Thứ bài này canh — không bắn nhầm một **LỆNH GHI** — vẫn nguyên.
     */
    @Test fun `log xe · cau feature-da-go KHONG ban nham control`() {
        listOf(
            "chuyển chế độ lái",                     // was: Control(headl) — chế độ lái đã gỡ, KHÔNG được bật đèn pha
            "mở xi nhan trái", "mở xi nhan phải",     // was: Control(drl) — xi nhan không phải control
            "tất cả cửa đang khóa hay đang mở",       // was: Control(sunroof=1) — câu HỎI, KHÔNG được mở cửa sổ trời
        ).forEach { s ->
            assertTrue(one(s) is VoiceIntent.Unknown, "«$s» phải Unknown (hỏi lại), KHÔNG bắn nhầm — ra: ${one(s)}")
        }
    }

}
