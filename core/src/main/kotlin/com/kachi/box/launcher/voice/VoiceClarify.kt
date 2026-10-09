package com.kachi.box.launcher.voice

import com.kachi.box.launcher.Lang
import com.kachi.box.launcher.Strings

/**
 * ═══ V3 · HỎI LẠI CHO TỚI KHI HIỂU ═══════════════════════════════════════════════════════════════════════════
 *
 * Spec `docs/specs/kachi-voice-fast-natural.html` **R8**. Owner 2026-09-16 (**D2**): *"hỏi lại cho tới khi hiểu
 * rồi làm"*. Thuần Kotlin (`:core`) ⇒ kiểm off-car; `:app` chỉ lo việc mở micro lượt hai.
 *
 * ## Vì sao một câu hỏi NGẮN, không phải câu *"không hiểu"*
 * Tới 1.65 mọi câu không hiểu đều ra **một** dòng (`VoiceReply.unknown`) và phiên chết ở đó. Người lái phải bấm
 * lại, nói lại **cả câu** — trong khi thứ máy thiếu thường chỉ là **một từ**: nghe được *"mở"* mà không nghe được
 * đối tượng, hoặc nghe được *"kính"* mà không biết kính nào. Hỏi đúng cái thiếu thì câu trả lời dài một từ.
 *
 * ## Ba dạng câu hỏi, và vì sao KHÔNG có dạng thứ tư
 *  1. **Thiếu đối tượng** ([VoiceUnknownReason.NO_OBJECT]) ⇒ *"Bật gì?"* — lấy đúng động từ người ta vừa nói.
 *  2. **Đối tượng NHẬP NHẰNG** — một từ đầu (*"kính"*) là chữ mở đầu của **≥ 2** cụm trỏ tới **≥ 2** mã khác nhau
 *     ⇒ *"Kính nào — Kính trước-trái, Kính trước-phải, hay …?"*. Danh sách sinh từ chính từ vựng, **không** chép
 *     tay (CLAUDE.md §7): thêm một nút kính ở bộ đăng ký là câu hỏi tự dài ra.
 *  3. **Còn lại** ⇒ *"Chưa rõ — nói lại giúp"*. Một câu thật thà còn hơn một câu hỏi đoán mò.
 * Dạng thứ tư (*"ý anh là X phải không?"*) cố ý không có: đoán một mã rồi hỏi xác nhận là đưa cổng CONFIRM vào
 * đúng chỗ nó không thuộc về, và [ĐO xe] cổng ấy vốn đã là nơi người lái bỏ cuộc.
 *
 * ## Trần [MAX_ROUNDS] = 2
 * Hỏi mãi là một vòng lặp mở trong lúc người ta đang lái. Hết trần thì [giveUp] nói một câu có ích (*"thử nói
 * «bật đèn đọc»"*) rồi đóng — không im lặng, và không hỏi lần thứ ba.
 */
object VoiceClarify {

    /** Tối đa hai lượt hỏi cho một phiên — xem KDoc lớp. */
    const val MAX_ROUNDS = 2

    /**
     * Một câu hỏi ngắn + phần ngữ cảnh để **ghép** với câu trả lời.
     *
     * @property question chữ hiện trên tấm chữ (và đọc lên, nếu máy có giọng).
     * @property carry các từ ĐÃ hiểu của lượt trước, theo đúng thứ tự người ta nói. [combine] dán câu trả lời vào
     *   sau chúng. Rỗng ⇒ câu trả lời đứng một mình.
     */
    data class Ask(val question: String, val carry: List<String>)

    /**
     * Câu hỏi cho một ý định không hiểu, hoặc `null` khi **không nên hỏi**.
     *
     * `null` ở bốn ca, và cả bốn đều là *"hỏi cũng không giúp gì"*: câu rỗng ([VoiceUnknownReason.EMPTY] — không
     * có gì để bám), từ vựng mở ([VoiceUnknownReason.OPEN_VOCAB] — Kachi cố ý không làm offline), *"đóng app"*
     * ([VoiceUnknownReason.APP_CLOSE] — hiểu đúng nhưng chưa có cơ chế), và vế bị bỏ của một câu ghép
     * ([VoiceUnknownReason.DROPPED_CLAUSE] — đã có một ý định thật chạy rồi).
     *
     * @param round lượt hỏi thứ mấy (0 = chưa hỏi lần nào). ≥ [MAX_ROUNDS] ⇒ `null`.
     * @param terms từ vựng đang dùng (truyền vào để `:app` khỏi dựng lại, và để test bơm bảng giả).
     * @param lang ngôn ngữ của câu hỏi — phiên nghe truyền ngôn ngữ GIỌNG NÓI (`voiceLangOf`, spec
     *   `kachi-i18n-zh-th-ms.html` R6): câu hỏi này được đọc lên. [Ask.carry] luôn là chữ người ta vừa NÓI (tiếng Việt).
     */
    fun ask(
        unknown: VoiceIntent.Unknown,
        round: Int,
        terms: List<VoiceTerm> = VoiceGrammar.terms(),
        lang: Lang = Strings.current,
    ): Ask? {
        if (round >= MAX_ROUNDS) return null
        when (unknown.reason) {
            VoiceUnknownReason.EMPTY,
            VoiceUnknownReason.OPEN_VOCAB,
            VoiceUnknownReason.APP_CLOSE,
            VoiceUnknownReason.DROPPED_CLAUSE,
            // D3 — nói lại cũng ra đúng câu ấy: tính năng đã bỏ / chưa có nút thì không có gì để hỏi.
            VoiceUnknownReason.FEATURE_GONE,
            -> return null
            else -> Unit
        }
        val raw = VoiceLexicon.tokenize(unknown.text)
        val tokens = raw.filterNot { it.norm in VoiceLexicon.FILLERS }
        if (tokens.isEmpty()) return null
        // ⚠ Hỏi [VoiceQuestion] trên bản CHƯA lọc tiếng đệm. Chữ `hay` có hai vai — tiếng đệm (*"hãy"*) và bộ
        // khung của câu hỏi lựa chọn (*"khóa hay mở"*) — nên lọc trước rồi mới hỏi là xoá đúng dấu hiệu cần đọc.
        // [ĐO lượt soát senior 2026-09-18] chính chỗ này từng làm [VoiceQuestion.looksAsked] **không bao giờ** thấy
        // dạng *"A hay B"*: `hay` nằm trong `FILLERS` nên nó đã bị cắt trước khi đo ⇒ *"kính hay cửa"* hỏi lại với
        // carry rỗng ⇒ trả lời *"cửa sổ trời"* ra một lệnh ghi.
        val asking = VoiceQuestion.isQuestion(raw)
        val asked = VoiceQuestion.looksAsked(raw)

        // VOICE-PROFILE-NAME-PHONETIC (2026-09-26) — câu nêu *"hồ sơ"* mà **không có cái tên**.
        //
        // [ĐO xe 2026-09-26] 8/8 lượt: owner nói *"chuyển sang hồ sơ Test"*, mô hình in ra *"chuyển sang hồ sơ"* —
        // rụng đúng cái tên. Trước bản này câu ấy ra `MISMATCH` ⇒ Kachi đọc *"việc đó không đi với thứ đó — thử nêu
        // mức, hoặc đổi động từ"*: một câu **không nói được phải làm gì tiếp**, cho một câu mà máy đã hiểu 90 %.
        // Nay hỏi thẳng tên hồ sơ, và [carryFor] không dùng ở đây: ngữ cảnh mang theo là **nguyên vế đã hiểu**
        // (*"chuyển sang hồ sơ"*) nên trả lời *"Mặc định"* ghép lại thành một câu phân tích được.
        //
        // Đứng TRƯỚC [ambiguity] vì cụm đánh dấu *"hồ sơ"* hẹp hơn hẳn phép dò họ nhãn: [ĐO] chữ *"số"* (nhãn datum
        // `gear`) khớp giữa chính câu này, nên để [ambiguity] chạy trước là hỏi lại về một cái nút số.
        VoiceProfileNames.missingName(tokens, terms)?.let { names ->
            return Ask(question(Strings.t("hồ sơ", "profile", lang), names, lang), tokens.map { it.raw })
        }

        // (≤ 2.98 BYD: dạng 2 "đối tượng nhập nhằng" — *"Kính nào — …?"* — chỉ có họ nút/datum xe; gỡ ở Android box B2 · W3.)

        // *"&lt;động từ&gt; gì?"* chỉ đúng khi đầu câu THẬT LÀ một động từ. [ĐO xe 2026-09-18] *"hev đi được bao
        // nhiêu"* trước đây ra *"Hev gì?"* — máy lấy một danh từ làm động từ rồi hỏi một câu vô nghĩa; nay câu ấy
        // rơi về [vague] (*"Chưa rõ — nói lại giúp"*). Câu HỎI cũng không đi đường này: nó không có động từ nào để
        // mang theo. Lấy **cả cụm** động từ (*"kiểm tra"*, không phải *"kiểm"*) vì đó là chữ sẽ được ghép lại ở
        // lượt sau — cắt còn một từ là ghép ra một câu không phân tích được.
        val verb = if (asking) null else leadVerb(tokens)
        if (verb != null && unknown.reason == VoiceUnknownReason.NO_OBJECT) {
            return Ask(
                Strings.fIn(lang, "{0} gì?", "{0} what?", capitalize(verb)),
                listOf(verb),
            )
        }
        return Ask(vague(lang), carryFor(tokens, asking, asked))
    }

    /*
     * ⚠ [SOÁT senior 2026-09-18 · P0] Câu *"Chưa rõ — nói lại giúp"* ở trên **cũng** mang ngữ cảnh, không còn
     * `emptyList()` cứng. [ĐO off-car] *"cốp đã mở chưa"* là một câu HỎI đã nhận ra được (`isQuestion` = true) mà
     * họ *"Cốp …"* không đủ hai nhãn để hỏi lại ⇒ nó rơi xuống đúng dòng này ⇒ carry rỗng ⇒ trả lời *"cửa sổ
     * trời"* ra **`Control(sunroof,1)`**: đúng tai nạn của nhật ký, chỉ đi qua một câu hỏi lại khác.
     *
     * Đánh đổi đã cân: sau câu *"nói lại giúp"* người lái thường **hỏi lại**, không ra lệnh; và nếu họ ra lệnh
     * thật thì lượt ấy thành một câu ĐỌC (mất một lượt nói) — rẻ hơn hẳn một lệnh thân xe không ai xin.
     */

    /** Câu chung khi không bám được vào đâu. */
    fun vague(lang: Lang = Strings.current): String = Strings.t("Chưa rõ — nói lại giúp", "Not sure — say that again", lang)

    /**
     * Câu **bỏ cuộc lịch sự** sau [MAX_ROUNDS] lượt. Nêu một câu mẫu có thật thay vì *"không hiểu"* lần thứ ba —
     * [ĐO] mẫu UX Kiki §5: sau hai lượt vật lộn thì thứ giúp được là **một ví dụ**, không phải một lời xin lỗi.
     *
     * spec `kachi-i18n-zh-th-ms.html` R4/R5 (owner 03/10 *"chỗ voice ghi rõ chỉ hỗ trợ tiếng việt"*): câu mẫu là câu
     * để NÓI ⇒ tiếng Việt ở MỌI tiếng giao diện — bản EN cũ dạy *"turn on the reading light"*, câu ASR tiếng Việt không
     * nghe ra (cùng họ NO_VERB của `VoiceReplyUnknown`). Câu mẫu là ĐỐI SỐ [GIVE_UP_EXAMPLE] của mẫu `{0}` ⇒ bản dịch
     * zh/th/ms không phải mang dấu tiếng Việt (`I18nCoverageTest` cấm). Bản VI ghép ra y byte câu cũ (test khoá).
     */
    fun giveUp(lang: Lang = Strings.current): String = Strings.fIn(
        lang,
        "Vẫn chưa rõ — thử nói \"{0}\"",
        "Still not sure — try saying \"{0}\" in Vietnamese",
        GIVE_UP_EXAMPLE,
    )

    /** Câu mẫu của [giveUp] — một lệnh có thật mà bộ phân tích hiểu (`VoiceClarifyTest` khoá: ra `Launcher`). */
    internal const val GIVE_UP_EXAMPLE = "mở cài đặt"

    /**
     * Ghép câu trả lời của lượt hỏi với ngữ cảnh đã có: `["mở"] + "kính lái"` → `"mở kính lái"`.
     *
     * Trả nguyên [answer] khi nó **đã** chứa đủ ngữ cảnh (người ta trả lời cả câu: *"mở kính lái"*). Không kiểm
     * điều đó thì câu ghép thành *"mở mở kính lái"* — bộ phân tích đọc `mo` hai lần và rơi lại vào NO_OBJECT,
     * tức lượt hỏi làm mọi thứ tệ hơn đúng ở ca người dùng hợp tác nhất.
     *
     * ## [SOÁT 2026-09-16 · P3] Hai vế của phép so phải được **cắt từ cùng một cách**
     * Bản trước so `VoiceLexicon.tokenize(a).map { it.norm }` (một phần tử = **một từ**) với
     * `carry.map { deaccent(it) }` (một phần tử = **nguyên chuỗi**, còn nguyên khoảng trắng bên trong). Một phần
     * tử [carry] nhiều từ vì thế **không bao giờ khớp** ⇒ vế đã có bị ghép lại lần nữa (*"mở kính mở kính lái"*).
     * Hôm nay không ai thấy vì [carry] luôn là `listOf(verb)` một từ (:76-80) — nhưng đó là một **bất biến ngầm**
     * không được ghi ở đâu và không được test nào giữ: đúng thứ hỏng im lặng ở lần ai đó mang theo cả cụm
     * *"bật đèn"*. Nay cả hai vế đi qua **cùng** [VoiceLexicon.tokenize].

     */
    fun combine(carry: List<String>, answer: String): String {
        val a = answer.trim()
        if (a.isEmpty()) return carry.joinToString(" ")
        if (carry.isEmpty()) return a
        val answerNorms = VoiceLexicon.tokenize(a).map { it.norm }
        val carryNorms = carry.flatMap { c -> VoiceLexicon.tokenize(c).map { it.norm } }
        if (carryNorms.isNotEmpty() && answerNorms.take(carryNorms.size) == carryNorms) return a
        return (carry + a).joinToString(" ")
    }

    /**
     * Ngữ cảnh mang sang lượt trả lời của một câu hỏi lại kiểu *"&lt;họ&gt; nào — …?"*.
     *
     * Ba ca, theo thứ tự đó:
     *  1. **câu gốc là câu HỎI** ⇒ [READ_VERB]. Đây là cổng D1 (xem KDoc [ambiguity]) — kể cả khi câu hỏi có một
     *     động từ hành động lẫn trong đó, lượt trả lời vẫn phải đi đường ĐỌC;
     *  2. **câu gốc mở đầu bằng một động từ** ⇒ mang chính động từ ấy. Bản trước bỏ nó đi, nên *"kiểm tra áp
     *     suất"* → hỏi *"Áp nào …?"* → người lái đọc *"áp lốp trước trái"* → **`NO_VERB`**: máy hỏi một câu rồi
     *     không hiểu nổi câu trả lời của chính nó. Một tên datum đứng trần không phải một lệnh
     *     (`VoiceGrammar.readsTail` — cổng [SOÁT 1.69 · P1]), nên động từ phải đi cùng;
     *  3. **không động từ nào, mà câu CÓ dấu hiệu hỏi** ([asked] = [VoiceQuestion.looksAsked] đo trên bản CHƯA
     *     lọc tiếng đệm — xem [ask]) ⇒ [READ_VERB].
     *  4. còn lại ⇒ rỗng (vd cả câu chỉ có một từ *"lọc"*).
     *
     * ## ⚠⚠ [SOÁT senior 2026-09-18 · P0] Vì sao phải có ca (3)
     * `carry` **rỗng** là ô nhớ duy nhất mà tai nạn của nhật ký còn đi qua được: câu trả lời đứng một mình thì
     * *"cửa sổ trời"* → `Control(sunroof,1)`, *"mở cốp"* → `Control(trunk,1)`, *"rời xe"* → `Macro(mac_leave)` —
     * đều là lệnh ghi hoàn toàn hợp lệ. [ĐO off-car, lượt soát] *"cốp đã mở chưa"* / *"điều hòa đang bật không"* /
     * *"kính hạ hết chưa"* đều rơi vào đúng ô ấy vì [VoiceQuestion.isQuestion] (chặt, vì nó đổi cách phân tích cả
     * câu) chưa dám nhận chúng. Ở đây thì ngưỡng phải LỎNG: chọn sai về phía ĐỌC chỉ tốn một lượt nói lại, chọn
     * sai về phía HÀNH ĐỘNG là mở cửa sổ trời trên xe đang chạy.
     *
     * Ca (4) vẫn còn — và **cố ý còn**: một danh ngữ trần không có dấu hiệu hỏi nào (*"lọc"*) đúng là một câu ra
     * lệnh nói thiếu, và owner đã chốt ở R8 rằng hỏi lại xong thì **làm**.
     */
    private fun carryFor(
        tokens: List<VoiceLexicon.Token>,
        asking: Boolean,
        asked: Boolean,
    ): List<String> = when {
        asking -> listOf(READ_VERB)
        else -> leadVerb(tokens)?.let { listOf(it) } ?: if (asked) listOf(READ_VERB) else emptyList()
    }

    /**
     * Cụm động từ ở **vị trí 0**, trả về NGUYÊN VĂN (*"kiểm tra"*), hoặc `null`. Dài trước ngắn, như mọi nơi.
     *
     * 2.93 [P3] — chữ MANG dấu chỉ là động từ khi đúng cách viết ([VoiceVerbSpelling.isVerb]): [ĐO off-car 07/10] *"tất cả kính
     * lên"* mang *"tất"* sang lượt sau như động từ *"tắt"* ⇒ trả lời *"mở cốp"* ghép thành *"tất mở cốp"* = `Control(trunk, 0)`.
     */
    private fun leadVerb(tokens: List<VoiceLexicon.Token>): String? {
        val hit = VoiceGrammar.VERBS.firstOrNull { VoiceLexicon.phraseAt(tokens, 0, it.first) } ?: return null
        if (!VoiceVerbSpelling.isVerb(tokens, hit.first.size)) return null
        return tokens.take(hit.first.size).joinToString(" ") { it.raw }
    }

    /**
     * Động từ ĐỌC mang theo cho lượt trả lời của một câu HỎI — xem KDoc [ambiguity].
     *
     * Phải là một cụm của [VoiceGrammar.VERBS] trỏ tới [VoiceVerb.READ]. `VoiceClarifyQuestionTest` ép bằng máy:
     * đổi thành một chữ không có trong bảng ấy là bịt cổng D1 **mà vẫn xanh**, nên phép canh không thể là mắt người.
     */
    const val READ_VERB = "xem"

    private fun question(head: String, labels: List<String>, lang: Lang): String {
        val list = when (labels.size) {
            2 -> Strings.fIn(lang, "{0} hay {1}", "{0} or {1}", labels[0], labels[1])
            else -> Strings.fIn(lang, "{0}, hay {1}", "{0}, or {1}", labels.dropLast(1).joinToString(", "), labels.last())
        }
        // {0} = đầu cụm viết hoa (đầu câu tiếng Việt) · {2} = đầu cụm viết thường (giữa câu tiếng Anh) · {1} = danh sách.
        return Strings.fIn(lang, "{0} nào — {1}?", "Which {2} — {1}?", capitalize(head), list, head.lowercase())
    }

    private fun capitalize(s: String): String =
        if (s.isEmpty()) s else s.substring(0, 1).uppercase() + s.substring(1)
}
