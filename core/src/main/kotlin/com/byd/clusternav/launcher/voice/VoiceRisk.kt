package com.byd.clusternav.launcher.voice

import com.byd.clusternav.launcher.Lang
import com.byd.clusternav.launcher.Strings

/**
 * Mức rủi ro của một ý định.
 *  • [SAFE]    — chỉ ĐỌC, không đổi gì ngoài màn hình.
 *  • [NORMAL]  — đổi trạng thái xe/launcher, đảo lại được bằng đúng một câu ngược lại.
 *  • [CONFIRM] — phải có một cú chạm "đồng ý" nữa mới bắn.
 */
enum class VoiceRisk { SAFE, NORMAL, CONFIRM }

/**
 * ═══ V1 · BẢNG AN TOÀN ════════════════════════════════════════════════════════════════════════════════════════
 *
 * Spec `docs/specs/kachi-voice-command.html` R4. `:core` thuần ⇒ kiểm off-car.
 *
 * ## Vì sao voice cần một cổng mà nút bấm KHÔNG cần
 * Owner đã bỏ mọi cổng an toàn cho nút bấm (2026-09-10, *"tự dùng tự chịu"*) và điều đó đúng với **nút**: muốn mở
 * khoá xe thì phải nhìn đúng ô, chạm đúng chỗ — bản thân thao tác đã là một xác nhận. Câu nói thì không có tính
 * chất đó: nó **kích hoạt được bằng tai nghe nhầm**. Một câu của người ngồi ghế sau, một dòng thoại trên đài, một
 * lần nhận dạng sai — cả ba đều dựng ra đúng cùng một lệnh mà không ai chạm vào màn.
 *
 * ⇒ Cổng này **không** phải cổng an toàn kiểu cũ (không đọc tốc độ, không chặn theo số). Nó chỉ hỏi lại **đúng bốn
 * việc** mà hậu quả không tự đảo lại được trong vài giây, và hỏi bằng một cú chạm — người lái vẫn toàn quyền.
 *
 * ## Bảng khai TƯỜNG MINH, không suy ra từ `domain`/`tier`
 * Suy từ `domain = BODY` sẽ kéo theo cả gạt mưa và gập gương (vô hại), còn bỏ sót *"dừng chiếu cụm"* (thuộc
 * INFOTAINMENT nhưng làm **tắt màn hình đồng hồ đang dẫn đường**). Mức rủi ro là một thuộc tính của **việc**, không
 * phải của nhóm — nên nó được viết ra, và mỗi dòng có lý do đọc được.
 */
object VoiceRiskTable {

    // Android box B2 · W3 (2026-10-09): bảng `Rule`/`CONTROL_RULES` (hạ hết kính · mở cốp · mở cửa sổ trời — hỏi mặc định
    // từ 2.86) và `MACRO_IDS` (gói *"mở hết kính"*) gỡ cùng nút / gói lệnh xe. Mã `control:` / `macro:` đã lưu trong
    // `voice_confirm_ids` vẫn đọc lên được và bị bỏ qua ([of] không còn ý định nào ra chúng).

    // ── V3 · R7 — mã của MỘT VIỆC CÓ THỂ HỎI, và tập đang được bật ───────────────────────────────

    /** Mã của việc *"đổi hồ sơ"* trong tập `voice_confirm_ids`. */
    const val ID_PROFILE = "profile"

    /** Mã của việc *"dẫn đường tới một chuỗi do nhận dạng tự do đọc ra"*. */
    const val ID_NAV_QUERY = "nav_query"

    /** Mã của việc *"mở bài/nghệ sĩ do nhận dạng tự do đọc ra"*. */
    const val ID_MEDIA_QUERY = "media_query"

    /** Tiền tố mã của nút / gói lệnh xe trong tập ĐÃ LƯU ≤ 2.98 — chỉ còn để [launcherAskableIds] lọc chúng khỏi màn chọn. */
    const val PREFIX_CONTROL = "control:"
    const val PREFIX_MACRO = "macro:"

    /**
     * Mã *"việc này là việc gì"* để tra trong tập `voice_confirm_ids`, hoặc `null` khi việc ấy **không hỏi được**
     * (chỉ đọc / không hiểu / sổ địa chỉ / lệnh nhạc thường).
     *
     * ⚠ Giá trị (bật hay tắt) **không** vào mã: người dùng tích *"Kính (tất cả)"* là tích cả hai chiều. Nhét giá
     * trị vào mã sẽ đẻ ra hai dòng `windows_all` trong màn chọn, và không ai đọc được khác nhau ở đâu.
     */
    fun confirmId(intent: VoiceIntent): String? = when (intent) {
        is VoiceIntent.Profile -> ID_PROFILE
        // owner 2026-09-24: Nav KHÔNG hỏi xác nhận (cổng nav_query "lòng vòng khó đoán, khỏi đi"). Dẫn THẲNG —
        // geocode được thì đi, không được thì mở app + nói chưa tra được. `ID_NAV_QUERY` giữ để tương thích chuỗi
        // `voice_confirm_ids` cũ đã lưu (đọc lên không lỗi) nhưng KHÔNG còn là mã hỏi-được.
        is VoiceIntent.Nav -> null
        is VoiceIntent.Media -> if (intent.op == VoiceMediaOp.QUERY && intent.query.isNotBlank()) ID_MEDIA_QUERY else null
        else -> null
    }

    /** Mọi mã có thể bật trong Cài đặt, theo thứ tự hiện ra. Sinh từ hai bảng trên — không chép tay. */
    fun askableIds(): List<String> =
        listOf(ID_PROFILE, ID_MEDIA_QUERY)   // owner 2026-09-24: bỏ ID_NAV_QUERY (nav không hỏi nữa)

    /**
     * Android box B2 · W1 — mã HIỆN ở mục Cài đặt *"Hỏi xác nhận trước khi chạy"*: [askableIds] trừ nút xe ([PREFIX_CONTROL])
     * và gói lệnh xe ([PREFIX_MACRO]) — Android box không có nút xe BYD. Mã đã lưu thì không đụng (tập vẫn ghi nguyên vẹn).
     */
    fun launcherAskableIds(): List<String> =
        askableIds().filterNot { it.startsWith(PREFIX_CONTROL) || it.startsWith(PREFIX_MACRO) }

    /**
     * ═══ V3 · R7 — MẶC ĐỊNH **KHÔNG HỎI GÌ CẢ** ═════════════════════════════════════════════════════════════
     *
     * ## ⚠⚠ Đổi hành vi 2026-09-16 — owner chốt, và đây là lý do
     * Tới 1.65 bốn dòng [CONTROL_RULES] + gói kính + đổi hồ sơ + dẫn đường **luôn** hỏi lại. Owner sau lượt xe
     * 09-16: *"cái nào nguy hiểm lái xe mới hỏi, chứ mở cửa hỏi làm gì? cần document lại cái nào cần đồng ý để
     * tôi chọn"*, và bảng B trả lời B1–B12 = **chạy luôn**, B13 = *"muốn có mục trong Cài đặt để tự bật"*.
     *
     * [ĐO xe 2026-09-16] cái giá thật của cổng mặc-định-bật: câu *"mở kính lái"* rơi vào nút gộp `windows_all`
     * ⇒ hộp *"Hạ hết 4 kính?"* ⇒ mở micro **5,4 s** ⇒ người lái nói *"ừ"* ⇒ bị bỏ ⇒ **huỷ, không nói gì**. Tức
     * một câu đúng, nghe đúng, và kết quả là im lặng sau 20 giây. Cổng an toàn ấy không chặn được lỗi nào trong
     * lượt đó; nó chỉ chặn đúng thứ nó phải cho qua.
     *
     * ⇒ Cơ chế **giữ nguyên** (bảng lý do, hộp hỏi, cổng "không bao giờ tự đồng ý"); thứ đổi là **ai bật nó**.
     * Tập rỗng ⇒ mọi việc là [VoiceRisk.NORMAL]. Cầu kiểm thử (`--ez auto_confirm`) không đổi một dòng nào.
     *
     * ⚠ 2.86 (FIX286 · SR5, owner 03/10): MỘT ngoại lệ có tên — **mở cửa sổ trời** nằm trong tập mặc định
     * ([Rule.askByDefault] → [defaultIds] → [effectiveIds], áp ở tầng đọc prefs). Tham số mặc định của hàm này vẫn
     * là tập RỖNG: hàm thuần không đoán prefs, chỗ gọi truyền tập hiệu lực.
     *
     * @param confirmIds tập mã đang bật = [effectiveIds] của prefs `voice_confirm_ids` (device-level).
     */
    fun of(intent: VoiceIntent, confirmIds: Set<String> = emptySet()): VoiceRisk = when (intent) {
        is VoiceIntent.Unknown -> VoiceRisk.SAFE
        // Sổ địa chỉ (spec `kachi-voice-addresses.html` §4.2) — **KHÔNG** hỏi lại, và đó là một quyết định, không
        // phải một chỗ bỏ sót: nhãn đến từ một tập ĐÓNG mà chính người dùng đã gõ trong Cài đặt, địa chỉ thì họ
        // đã đọc lại lúc lưu, và đi nhầm đường thì quay đầu được.
        is VoiceIntent.NavigateSaved -> VoiceRisk.NORMAL
        else -> if (confirmId(intent)?.let { it in confirmIds } == true) VoiceRisk.CONFIRM else VoiceRisk.NORMAL
    }

    // ── FIX286 · SR5(b) — tập hỏi **hiệu lực** (mặc định + lựa chọn của người dùng) ─────────────────────────

    /**
     * Mã hỏi-được nằm trong tập mặc định. ≤ 2.98 BYD = *"mở cửa sổ trời"* (FIX286 · SR5); Android box B2 · W3 gỡ nút xe ⇒
     * rỗng — quyết định 2026-09-16 *"mặc định không hỏi gì"* áp nguyên vẹn.
     */
    fun defaultIds(): Set<String> = emptySet()

    /**
     * ═══ Tập hỏi HIỆU LỰC từ hai thứ lưu bền: tập đã lưu và mốc *"người dùng đã chọn từ 2.86"* ═══════════════
     *
     * @param stored tập `voice_confirm_ids` đã lưu, `null` khi khoá **vắng** (chưa ai từng lưu).
     * @param chosenSinceDefaults mốc `voice_confirm_default_v286`: lượt lưu gần nhất xảy ra khi màn Cài đặt ĐÃ bày
     *   [defaultIds] (mọi lượt ghi từ 2.86 đặt mốc này).
     *
     * Ba ca, và ca giữa là lý do mốc tồn tại:
     *  1. khoá vắng ⇒ đúng [defaultIds];
     *  2. khoá có mà chưa có mốc ⇒ tập ấy được lưu **trước 2.86**, lúc mặc định còn là rỗng — người dùng chưa từng
     *     được hỏi về nóc ⇒ cộng [defaultIds] vào (đúng *"khoá đã có ⇒ thêm một lần"* của spec SR5b);
     *  3. có mốc ⇒ tập đã lưu là lựa chọn THẬT của người dùng, kể cả khi họ vừa bỏ tích nóc ⇒ dùng nguyên.
     *
     * THUẦN và KHÔNG ghi gì: phép cộng ở ca 2 làm lúc ĐỌC, mỗi lần — không có lượt di trú nào phải ghi ngược vào
     * prefs. Nhờ vậy tiến trình `:wake` (đọc bản prefs có thể cũ — `VOICE-WAKE-PREFS-STALE`) cũng tính ra cùng một
     * luật, và lỗi nếu có chỉ nghiêng về phía **hỏi thêm**, không bao giờ về phía mở nóc không hỏi.
     */
    fun effectiveIds(stored: Set<String>?, chosenSinceDefaults: Boolean): Set<String> = when {
        stored == null -> defaultIds()
        chosenSinceDefaults -> stored
        else -> stored + defaultIds()
    }

    /**
     * Tên bài / điểm đến ⇒ [VoiceRisk.CONFIRM], **khác** mọi việc khác trong bảng này.
     *
     * ## Vì sao một việc VÔ HẠI lại phải hỏi lại
     * Ba dòng trên hỏi vì **hậu quả** không đảo lại được (xe mở khoá, kính hạ hết). Dòng này hỏi vì **nguồn**:
     * từ 1.50 phần đuôi của câu do bộ nhận dạng **TỰ DO** đọc ra (R16 — không ngữ pháp, không tập đóng), tức
     * chính xác kém hơn hẳn phần còn lại của câu. Máy nghe *"Diễm Xưa"* thành *"điểm xưa"* thì:
     *  • nếu là bài hát — app mở nhầm kết quả, người lái phải sửa tay **giữa lúc đang lái**;
     *  • nếu là điểm đến — [ĐO] Google Maps/Waze **bắt đầu dẫn đường luôn** (`navigate=yes`), tức xe được chỉ
     *    sang một hướng khác mà không ai kịp đọc.
     * Một cú chạm "Đồng ý" sau khi nghe máy đọc lại *"Tìm bài «Diễm Xưa» trên YouTube Music"* rẻ hơn hẳn cả hai.
     *
     * Câu rỗng thì không có gì để đọc lại ⇒ [VoiceRisk.NORMAL] (*"phát nhạc"* vẫn là một cú chạm như trước).
     */
    private fun openVocabWhy(lang: Lang = Strings.current): String = Strings.t(
        "đoạn trong ngoặc do nhận dạng tự do đọc ra — kém chính xác hơn phần còn lại của câu",
        "the quoted part came from free-form recognition — less accurate than the rest",
        lang,
    )

    /**
     * Nhãn + lý do của MỘT mã hỏi-được, cho màn Cài đặt bày ra. `null` = mã lạ (pref cũ) ⇒ chỗ gọi bỏ qua.
     *
     * (≤ 2.98 BYD còn nhãn nút / gói lệnh xe — mã `control:` / `macro:` đã lưu nay ra `null`, chỗ gọi bỏ qua.)
     */
    fun askableLabel(id: String): Pair<String, String>? = when {
        id == ID_PROFILE -> Strings.t("Đổi hồ sơ", "Switch profile") to Strings.t(
            "đổi hồ sơ thay toàn bộ bố cục và cấu hình đang dùng",
            "switching profile replaces the whole layout and current settings",
        )
        id == ID_NAV_QUERY -> Strings.t("Dẫn đường tới nơi vừa đọc", "Navigate to a spoken place") to openVocabWhy()
        id == ID_MEDIA_QUERY -> Strings.t("Mở bài vừa đọc", "Play a spoken track") to openVocabWhy()
        else -> null
    }

    /**
     * Vì sao việc này phải hỏi lại — hiện thẳng trong hộp xác nhận, không giấu trong mã. [lang] = ngôn ngữ của câu
     * hỏi ([VoiceReply.confirmQuestion]): phiên nói truyền tiếng GIỌNG NÓI (spec `kachi-i18n-zh-th-ms.html` R6).
     */
    fun reason(intent: VoiceIntent, lang: Lang = Strings.current): String? = when (intent) {
        is VoiceIntent.Profile -> Strings.t(
            "đổi hồ sơ thay toàn bộ bố cục và cấu hình đang dùng",
            "switching profile replaces the whole layout and current settings",
            lang,
        )
        is VoiceIntent.Nav -> openVocabReason(intent.query, lang)
        is VoiceIntent.Media -> if (intent.op == VoiceMediaOp.QUERY) openVocabReason(intent.query, lang) else null
        else -> null
    }

    /** Lý do cho dòng TỪ VỰNG MỞ — xem KDoc [askableLabel]. Câu rỗng thì không có gì để đọc lại ⇒ không lý do. */
    private fun openVocabReason(query: String, lang: Lang): String? = if (query.isBlank()) null else openVocabWhy(lang)
}
