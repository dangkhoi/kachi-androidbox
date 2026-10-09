package com.byd.clusternav.launcher.voice

import com.byd.clusternav.launcher.Lang
import com.byd.clusternav.launcher.LauncherActions
import com.byd.clusternav.launcher.Strings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ V1 · ĐỘ PHỦ — BÀI CANH **SINH TỪ BỘ ĐĂNG KÝ** ════════════════════════════════════════════════════════════
 *
 * Spec `docs/specs/kachi-voice-command.html` R7.
 *
 * ## Vì sao phải SINH chứ không chép 194 câu vào một bảng
 * Bảng chép tay chỉ chứng minh *"194 câu này chạy"*. Thứ cần chứng minh là *"**mọi** khả năng của xe đều gọi được
 * bằng lời"* — và câu đó chỉ đúng nếu phép kiểm **đếm lại từ chính bộ đăng ký** mỗi lần chạy. Thêm một nút vào
 * `ControlRegistry` mà từ vựng không phủ ⇒ bài này **đỏ ngay**, không phải chờ ai đó nhớ ra để thêm ca test. Đây
 * là cùng cơ chế `LangCoverageTest` dùng cho bản dịch, và cùng lý do.
 *
 * Số ca sinh ra hôm nay: **3 hành động launcher** (Android box B2 · W3: 33 nút + 64 datum + 2 gói lệnh gỡ cùng lõi HAL
 * BYDAuto; trước đó 102), cộng phần tiếng Anh.
 * (Bản trước ghi *"65 nút + 123 datum + 4 gói + 2 launcher = 194"* — con số của trước lượt owner gỡ ADAS
 * 2026-09-16 và các lượt purge sau đó. Một KDoc nói sai con số nó đang canh là chỗ người sau đọc rồi tin nhầm.)
 *
 * ## 2.74 · R3 — câu mẫu nay lấy từ [VoiceCommandCatalog], không còn bảng riêng trong tệp này
 * Hai bảng `when (def.kind)` (VI + EN) từng sống ở đây là **bản sao thứ 2 và 3** của cùng một bộ sinh câu (xem
 * KDoc [VoiceCommandCatalog] về cả năm bản). Nay chúng gọi [VoiceCommandCatalog.coverageSentence] — cùng hàm mà
 * màn *Cài đặt › Giọng nói* và dump danh mục đọc, nên một câu hỏng thì cả ba bề mặt đỏ cùng lúc.
 */
class VoiceGrammarCoverageTest {

    // ══ 1–2 · Nút + datum — Android box B2 · W3: gỡ cùng lõi HAL BYDAuto ═══════════════════════════════════════

    // ══ 3 · HÀNH ĐỘNG LAUNCHER ═══════════════════════════════════════════════════════════════════════════════

    @Test
    fun `moi hanh dong launcher goi duoc bang loi`() = LauncherActions.ALL.forEach { a ->
        assertEquals(VoiceIntent.Launcher(a.id), VoiceIntentParser.parseOne("mở ${a.label}"), "\"mở ${a.label}\"")
    }

    // ══ 4 · TIẾNG ANH CƠ BẢN — Android box B2 · W3: câu Anh của nút xe gỡ cùng lõi HAL BYDAuto ═══════════════

    // ══ 5 · CHỐT AN TOÀN CỦA CHÍNH TỪ VỰNG ═════════════════════════════════════════════════════════════

    /**
     * ⚠⚠ Bài **quan trọng nhất** tệp này. Bỏ dấu xong thì tiếng Việt đụng nhau rất nhiều: `"cái"` = `"cài"`,
     * `"của"` = `"cửa"`, `"thể"` = `"thế"`. Một từ đệm trùng **tiền tố** của một cụm trong từ vựng sẽ làm cụm đó
     * **không bao giờ khớp được nữa** — im lặng, không ai đỏ. (Bản đầu của `VoiceLexicon.FILLERS` có đúng ba từ
     * như vậy và nó nuốt mất *"cài đặt"*, *"cửa sổ trời"*, *"thể thao"*.)
     */
    @Test
    fun `khong tu dem nao la tien to cua mot cum trong tu vung`() {
        val terms = VoiceGrammar.terms()
        val bad = VoiceLexicon.FILLERS.filter { f -> terms.any { it.words.firstOrNull() == f } }
        assertTrue(bad.isEmpty(), "từ đệm nuốt mất đầu một cụm thật: $bad — bỏ khỏi VoiceLexicon.FILLERS")
    }

    @Test
    fun `moi cum trong tu vung deu khong rong va da chuan hoa`() {
        VoiceGrammar.terms(listOf("Mặc định"), listOf("YouTube")).forEach { t ->
            assertTrue(t.words.isNotEmpty(), "cụm rỗng cho ${t.id}")
            t.words.forEach { w ->
                assertEquals(VoiceLexicon.deaccent(w), w, "cụm của ${t.id} chưa chuẩn hoá: \"$w\"")
            }
        }
    }

    /** Từ vựng phải **sinh ra**, không chép tay: bỏ một bộ đăng ký ra khỏi [VoiceGrammar.terms] là bài này đỏ. */
    @Test
    fun `tu vung phu du bon bo dang ky`() {
        val terms = VoiceGrammar.terms()
        // Android box B2 · W3: nút · datum · gói lệnh gỡ cùng lõi HAL BYDAuto ⇒ phần tĩnh chỉ còn hành động launcher.
        assertEquals(
            LauncherActions.ALL.size, terms.filter { it.kind == VoiceTermKind.LAUNCHER }.map { it.id }.distinct().size,
        )
        assertEquals(setOf(VoiceTermKind.LAUNCHER, VoiceTermKind.MEDIA, VoiceTermKind.NAV), terms.map { it.kind }.toSet() - setOf(VoiceTermKind.PROFILE, VoiceTermKind.APP))
    }

    // ══ 6 · SỐ BẰNG CHỮ ════════════════════════════════════════════════════════════════════════════════

    @Test
    fun `doc so bang chu tieng Viet va tieng Anh`() {
        fun n(s: String): Int? = VoiceLexicon.readNumber(VoiceLexicon.tokenize(s), 0)?.value
        assertEquals(0, n("không"))
        assertEquals(7, n("bảy"))
        assertEquals(10, n("mười"))
        assertEquals(15, n("mười lăm"))
        assertEquals(21, n("hai mươi mốt"))
        assertEquals(22, n("hai mươi hai"))
        assertEquals(24, n("hai mươi tư"))
        assertEquals(30, n("ba mươi"))
        assertEquals(33, n("ba mươi ba"))
        assertEquals(22, n("22"))
        assertEquals(80, n("80%"))
        assertEquals(3, n("three"))
        assertEquals(22, n("twenty two"))
        assertEquals(22, n("twenty-two"))
        assertEquals(VoiceLexicon.MAX, n("tối đa"))
        assertEquals(VoiceLexicon.MAX, n("hết cỡ"))
        assertEquals(VoiceLexicon.MIN, n("tối thiểu"))
        assertEquals(null, n("mèo"))

        // Lối nói RÚT GỌN (soát 2026-09-14): bỏ chữ "mươi". Thiếu bảng này thì "hai lăm" đọc ra 2, rồi `clamp`
        // kéo về `min` ⇒ máy làm SAI một việc và vẫn báo "✓" (xem KDoc `VoiceLexicon.VI_TENS_SHORT`).
        assertEquals(25, n("hai lăm"))
        assertEquals(24, n("hai tư"))
        assertEquals(31, n("ba mốt"))
        assertEquals(24, n("hăm bốn"))
        assertEquals(25, n("hăm lăm"))
        assertEquals(21, n("hăm mốt"))
        // "băm" (= ba mươi) cố ý KHÔNG nhận: bỏ dấu xong nó là "bam", trùng hệt **"bấm"** — một động từ ra lệnh.
        assertEquals(null, n("băm mốt"))
        // "hai linh" không phải một con số (nó là nửa của "hai linh năm") ⇒ chỉ đọc được "hai".
        assertEquals(2, n("hai linh"))
    }

    // ══ 7 · BẢNG AN TOÀN (R4) ══════════════════════════════════════════════════════════════════════════

    /**
     * ═══ V3 · R7 — MẶC ĐỊNH **KHÔNG HỎI GÌ CẢ** (owner chốt 2026-09-16) ══════════════════════════════
     *
     * Đây là bài canh của một **đổi hành vi**, không phải một bài canh bảng: tới 1.65 bốn dòng
     * [VoiceRiskTable.CONTROL_RULES] + gói kính + đổi hồ sơ + điểm đến mở **luôn** hỏi lại. [ĐO xe 2026-09-16]
     * cái giá thật: *"mở kính lái"* → nút gộp → hộp *"Hạ hết 4 kính?"* → 5,4 s chờ → người lái nói *"ừ"* → bị
     * bỏ → **huỷ, không nói gì**. Owner: *"cái nào nguy hiểm lái xe mới hỏi, chứ mở cửa hỏi làm gì"*.
     *
     * Thử làm nó ĐỎ: bỏ tham số `confirmIds` ở `VoiceRiskTable.of` (quay lại bảng cứng) ⇒ nửa đầu bài này đỏ.
     *
     * ⚠ 2.86 (FIX286 · SR5b): bài này canh **hàm thuần** với tập rỗng — vẫn đúng nguyên văn. Mặc định của **sản
     * phẩm** nay có một ngoại lệ (mở cửa sổ trời), áp ở tầng đọc prefs qua `VoiceRiskTable.effectiveIds`; bài canh
     * của ngoại lệ ấy ở `VoiceConfirmDefault286Test`.
     */
    @Test
    fun `mac dinh KHONG hoi gi ca — tap rong thi moi viec la NORMAL`() {
        listOf(
            VoiceIntent.Profile("Vợ"),
            VoiceIntent.Nav("Bitexco"),
            VoiceIntent.Media(VoiceMediaOp.QUERY, "Diễm Xưa"),
        ).forEach { i ->
            assertEquals(VoiceRisk.NORMAL, VoiceRiskTable.of(i), "mặc định phải CHẠY LUÔN: $i")
        }
        assertEquals(emptySet<String>(), VoiceRiskTable.defaultIds(), "W3: mặc định cửa sổ trời (nút xe) đã gỡ")
        assertEquals(VoiceRisk.SAFE, VoiceRiskTable.of(VoiceIntent.Unknown(VoiceUnknownReason.FEATURE_GONE, "mở kính")))
    }

    @Test
    fun `bat mot ma thi DUNG ma do hoi lai, cac ma khac khong`() {
        val only = setOf(VoiceRiskTable.ID_PROFILE)
        assertEquals(VoiceRisk.CONFIRM, VoiceRiskTable.of(VoiceIntent.Profile("Vợ"), only))
        assertEquals(VoiceRisk.NORMAL, VoiceRiskTable.of(VoiceIntent.Media(VoiceMediaOp.QUERY, "Diễm Xưa"), only))
        // Sổ địa chỉ: tập ĐÓNG người dùng tự gõ ⇒ không có mã, không bật được (spec `kachi-voice-addresses` §4.2).
        assertEquals(null, VoiceRiskTable.confirmId(VoiceIntent.NavigateSaved("Nhà")))
    }

    /**
     * Android box B2 · W3 — `voice_confirm_ids` lưu từ Kachi BYD còn mã `control:sunroof` / `macro:…` (nút xe đã
     * gỡ): nạp được, không ném, không làm việc nào khác thành CONFIRM, và không còn là ô tích nào trong Cài đặt.
     */
    @Test
    fun `tap da luu co ma nut xe cu van dung duoc va bi bo qua`() {
        val legacy = setOf("control:sunroof", "macro:mac_win_open_all", "control:trunk")
        val eff = VoiceRiskTable.effectiveIds(legacy, chosenSinceDefaults = true)
        listOf(
            VoiceIntent.Profile("Vợ"), VoiceIntent.Media(VoiceMediaOp.QUERY, "Diễm Xưa"), VoiceIntent.OpenApp("YouTube"),
            VoiceIntent.Launcher(com.byd.clusternav.launcher.LauncherActions.SETTINGS), VoiceIntent.Nav("Bitexco"),
        ).forEach { assertEquals(VoiceRisk.NORMAL, VoiceRiskTable.of(it, eff), "$it") }
        assertTrue(VoiceRiskTable.askableIds().none { it in legacy }, "mã nút xe cũ không còn là ô tích")
        assertTrue(VoiceRiskTable.askableIds().none { it.startsWith("control:") || it.startsWith("macro:") })
        assertEquals(legacy, VoiceRiskTable.effectiveIds(legacy, chosenSinceDefaults = false), "chưa chọn lại ⇒ cộng mặc định RỖNG")
    }

    /** Mỗi mã bày ra trong Cài đặt phải có NHÃN + LÝ DO đọc được — một ô tích trống nghĩa là một ô không ai tích. */
    @Test
    fun `moi ma hoi-duoc deu co nhan va ly do`() {
        val ids = VoiceRiskTable.askableIds()
        assertTrue(ids.isNotEmpty(), "tiền đề: danh sách việc hỏi-được không rỗng")
        assertEquals(ids.size, ids.distinct().size, "mã trùng ⇒ hai ô tích ghi đè nhau")
        ids.forEach { id ->
            val pair = VoiceRiskTable.askableLabel(id)
            assertTrue(pair != null, "thiếu nhãn cho mã $id")
            assertTrue(pair!!.first.isNotBlank() && pair.second.isNotBlank(), "nhãn/lý do rỗng cho $id")
        }
    }

    /** Mọi việc phải hỏi lại đều phải nói được **vì sao** — hộp xác nhận không được là một cú chạm trống nghĩa. */
    @Test
    fun `moi viec CONFIRM deu co ly do doc duoc`() {
        listOf(
            VoiceIntent.Profile("Vợ"),
            VoiceIntent.Media(VoiceMediaOp.QUERY, "Diễm Xưa"),
        ).forEach { i ->
            assertTrue(!VoiceRiskTable.reason(i).isNullOrBlank(), "thiếu lý do cho $i")
        }
    }

    // ══ 8 · TỪ VỰNG DỰNG MỘT LẦN (soát 2026-09-14) ════════════════════════════════════════════════════

    /**
     * Phần tĩnh của từ vựng nay `by lazy` (không dựng lại ~600 cụm mỗi câu). Cái giá phải canh của mọi bộ nhớ đệm
     * là **rò rỉ giữa hai lần gọi**: danh sách hồ sơ/app là thứ THAY ĐỔI (đổi hồ sơ, cài/gỡ app), nên một lần gọi
     * cũ mà còn dính lại thì người dùng gọi được một app đã gỡ — hoặc tệ hơn, một hồ sơ đã xoá.
     */
    @Test
    fun `tu vung dung mot lan nhung phan dong khong ro ri giua hai lan goi`() {
        val a = VoiceGrammar.terms(profiles = listOf("Vợ"), apps = listOf("VTV Go"))
        val b = VoiceGrammar.terms(profiles = listOf("Bố"), apps = listOf("YouTube"))
        val c = VoiceGrammar.terms()

        fun ids(t: List<VoiceTerm>, k: VoiceTermKind) = t.filter { it.kind == k }.map { it.id }.toSet()
        assertEquals(setOf("Vợ"), ids(a, VoiceTermKind.PROFILE))
        assertEquals(setOf("Bố"), ids(b, VoiceTermKind.PROFILE))
        assertEquals(setOf("VTV Go"), ids(a, VoiceTermKind.APP))
        assertEquals(setOf("YouTube"), ids(b, VoiceTermKind.APP))
        assertTrue(ids(c, VoiceTermKind.PROFILE).isEmpty() && ids(c, VoiceTermKind.APP).isEmpty(),
            "gọi không tham số phải KHÔNG còn hồ sơ/app của lần gọi trước")

        // Phần tĩnh thì phải y hệt nhau ở cả ba lần — nếu không, cùng một câu sẽ hiểu khác nhau tuỳ lúc gọi.
        val static = { t: List<VoiceTerm> ->
            t.filter { it.kind != VoiceTermKind.PROFILE && it.kind != VoiceTermKind.APP }
        }
        assertEquals(static(c), static(a))
        assertEquals(static(c), static(b))
        assertTrue(c.zipWithNext().all { (x, y) -> x.words.size >= y.words.size }, "phải xếp DÀI trước NGẮN")
    }

    // ══ 9 · CÂU TRẢ LỜI (R5) ══════════════════════════════════════════════════════════════════════════

    /** Huỷ ở hộp hỏi lại phải nói ra còn mấy vế không chạy — im lặng là để người ta tưởng nửa sau đã chạy. */
    @Test
    fun `cau huy noi ro con may viec khong chay`() {
        Strings.current = Lang.VI
        val i = VoiceIntent.OpenApp("YouTube")
        assertTrue(VoiceReply.cancelled(i, 0).contains("đã huỷ"))
        assertTrue(!VoiceReply.cancelled(i, 0).contains("không chạy"), "không có vế sau thì đừng doạ")
        assertTrue(VoiceReply.cancelled(i, 2).contains("2"))
        Strings.current = Lang.EN
        assertTrue(VoiceReply.cancelled(i, 2).contains("cancelled"))
        Strings.current = Lang.VI
    }

}
