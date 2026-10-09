package com.byd.clusternav.launcher.voice

import com.byd.clusternav.launcher.voice.VoiceLexicon.Token

/**
 * ═══ V1 · BỘ PHÂN TÍCH Ý ĐỊNH — TẤT ĐỊNH, THUẦN KOTLIN ════════════════════════════════════════════════════════
 * Spec `docs/specs/kachi-voice-command.html` R1–R3. `:core`, cấm `android.*` ⇒ 100% kiểm off-car.
 * ## Hình dạng: `động từ × KIỂU ĐỐI TƯỢNG`
 * Lấy thẳng từ [ĐO] RE Kiki §7(c): bộ động từ của người Việt khi nói với xe rất **nhỏ và đều**, nhưng *"Mở"* thì
 * **quá tải nặng** (mở cửa · mở kính · mở app · mở nhạc · mở Cài đặt). Nên động từ **không** quyết định một mình:
 * nó chỉ chọn *loại việc*, còn việc cụ thể do **kiểu của đối tượng** quyết ([VoiceTermKind]).
 * ## Ba luật giải nhập nhằng, không luật nào là `if (id == "…")`
 *  1. **Dãy từ dài nhất thắng** — giải ba cặp nhãn lồng nhau của L-RE2 (xem KDoc [VoiceGrammar]).
 *  2. **Loại động từ chọn ứng viên** — cùng một cụm *"Kính trước-trái"* trỏ tới CẢ datum (xem % mở) lẫn nút
 *     (đóng/mở); *"xem"* lấy datum, *"mở"* lấy nút. 18 nhãn trùng của `CapabilityCatalog.collidingLabels()` đều
 *     giải bằng đúng luật này, không cần bảng ngoại lệ nào.
 *  3. **Từ vựng MỞ thì nói thẳng là mở** — tên bài hát / điểm đến không nằm trong tập đóng; trả
 *     [VoiceUnknownReason.OPEN_VOCAB] hoặc [VoiceIntent.Nav]/[VoiceIntent.Media] kèm nguyên văn, **không** đoán.
 */
object VoiceIntentParser {

    /**
     * Liên từ nối hai lệnh trong một câu ([ĐO] RE Kiki §7c #42: *"… và …"* là tính năng hạng nhất).
     * `internal` từ pha NGHE: [VoicePhrases] phải khai bốn từ này với bộ nhận dạng, nếu không thì câu ghép
     * **nghe** được từng vế mà mất đúng cái từ nối chúng. Đọc lại ở đây thay vì chép sang đó — chép là để lệch.
     *
     * 2.93 VOICE-ROI-CONNECTOR: khai bằng cách viết CÓ DẤU ([CONNECTOR_WORDS]) — tách câu chỉ nhận *"rồi"*, không nhận
     * *"rơi"* hay *"rời"* (cùng bỏ dấu ra `roi`) khi chữ mang dấu; luật ở [VoiceHomograph].
     * 2.98 R2 VOICE-XONG-CONNECTOR: thêm *"xong"* (*"đóng kính lái xong đèn đọc"* từng mất vế sau im lặng). An toàn nhờ cùng cổng
     * *"mọi vế phải hiểu được"* + dòng *"đã bỏ qua"*; câu CHỈ có từ kết thúc (*"xong rồi"*) đã được [VoiceEndWords] nhận trước.
     */
    internal val CONNECTOR_WORDS = VoiceHomograph.Words("và", "rồi", "xong", "and", "then")
    internal val CONNECTORS: Set<String> = CONNECTOR_WORDS.norms

    /**
     * Phân tích một câu, trả về **danh sách** ý định theo đúng thứ tự nói (R3).
     * ## Luật tách câu ghép — và vì sao nó không tách bừa
     * Tách ở *"và"/"rồi"* rồi phân tích từng vế; **chỉ chấp nhận** nếu **mọi** vế đều hiểu được. Ngược lại thì
     * phân tích lại NGUYÊN câu như một vế.
     *
     * [ĐO] mẫu câu Kiki #10 là *"Mở bài Cỏ dại và hoa dành dành"* — chữ *"và"* nằm **trong tên bài hát**. Tách vô
     * điều kiện sẽ cắt đôi tên bài và gửi đi một nửa. Luật "mọi vế phải hiểu được" tự xử lý ca đó (*"hoa dành
     * dành"* không có động từ ⇒ quay về nguyên câu) mà không phải biết bài hát nào tên có chữ "và".
     */
    fun parse(
        text: String,
        profiles: List<String> = emptyList(),
        apps: List<String> = emptyList(),
        /**
         * Nhãn trong **sổ địa chỉ của hồ sơ đang dùng** (`SavedPlaces`) — danh sách ĐỘNG, cùng họ [profiles]/[apps].
         *
         * ⚠ Chúng KHÔNG đi vào từ vựng chung: chỉ được tra ở vị trí điểm đến — xem KDoc [VoicePlaces].
         */
        places: List<String> = emptyList(),
        /** 2.91 VOICE-APP-NAMES — tên app đã dạy còn sống ([VoiceAppIndex.aliasesOf]); xem [VoiceGrammar.terms]. */
        aliases: List<VoiceAppAlias> = emptyList(),
    ): List<VoiceIntent> {
        val raw0 = VoiceLexicon.tokenize(text)
        if (raw0.isEmpty()) return listOf(VoiceIntent.Unknown(VoiceUnknownReason.EMPTY, text))
        if (VoiceEndWords.isEnd(text)) return listOf(VoiceIntent.EndSession)
        // Cắt cụm LỊCH SỰ đầu/cuối ("làm ơn …", "cho tôi …", "… hộ tôi", "… nhé", "… đi") — [ĐO golden dataset
        // 2026-09-22] nhóm FAIL lớn nhất: động từ không ở vị trí 0 ⇒ NO_VERB. Chi tiết ở [VoiceLexicon.stripCourtesy].
        val all = VoiceLexicon.stripCourtesy(raw0).ifEmpty { raw0 }
        // H4 — cụm NGHE NHẦM chỉ bật khi CẢ CÂU có từ ngữ cảnh, nên phải tính trên `all`, không trên từng vế.
        val terms = VoiceGrammar.terms(profiles, apps, aliases)

        // Mỗi vế cũng qua [fuzzy] (chữa phương ngữ) — không thì "tắt máy nạnh" (l=n) trong câu ghép rớt DROPPED_CLAUSE.
        fun seg(p: List<Token>) = fuzzy(parseTokens(p, terms, places, text), p, terms, places, text)

        // 2.98 R2 — mỗi vế của câu ghép vẫn có thể là câu MIX không liên từ (*"đóng hết kính mở cửa sổ trời xong mở cốp"*) ⇒ tách tiếp
        // theo động từ, CÙNG cổng của nhánh MIX dưới đây (mọi mảnh phải hiểu được — không thì giữ nguyên vế).
        val parts = splitOnConnectors(all).let { ps ->
            if (ps.size < 2) ps
            else ps.flatMap { p -> VoiceMultiVerb.multiVerbSplit(p)?.takeIf { s -> s.none { seg(it) is VoiceIntent.Unknown } } ?: listOf(p) }
        }
        if (parts.size > 1) {
            // (≤ 2.98 BYD: vế chỉ là tên một nút xe mượn động từ của vế trước — gỡ cùng nút xe, Android box B2 · W3.)
            val each = parts.map { seg(it) }
            if (each.none { it is VoiceIntent.Unknown }) return each
            val whole = fuzzy(parseTokens(all, terms, places, text), all, terms, places, text)
            return listOf(whole) + VoiceDroppedNote.droppedNote(parts, each, whole)
        }
        // MIX KHÔNG LIÊN TỪ ("hạ kính lấy gió ngoài tắt máy lạnh" = 3 lệnh, 0 chữ "và/rồi") — chi tiết ở
        // [VoiceMultiVerb.multiVerbSplit]. CHỈ nhận khi ≥2 vế + MỌI vế hiểu được (an toàn: tên bài không bị cắt).
        VoiceMultiVerb.multiVerbSplit(all)?.let { segs ->
            val each = segs.map { seg(it) }
            if (each.size >= 2 && each.none { it is VoiceIntent.Unknown }) return each
        }
        return listOf(fuzzy(parseTokens(all, terms, places, text), all, terms, places, text))
    }

    /**
     * H7 — câu không hiểu được thì thử **chữa lỗi chính tả** rồi đọc lại đúng một lần ([VoicePhoneticMatch]).
     *
     * Đứng sau tất cả: mọi luật V1 chạy trước, không đổi một dòng. Chỉ nhận khi lần đọc lại ra một ý định CÓ
     * NGHĨA — không thì giữ nguyên câu báo cũ, kể cả **lý do** không hiểu (bài kiểm đang khoá lý do).
     *
     * ## D1 (log xe 2026-09-18) — BA cổng thêm vào, mỗi cổng chặn một lỗi ĐÃ ĐO
     * Tầng chữa chính tả là chỗ ba lỗi nặng nhất của phiên log sinh ra: nó "sửa" một âm tiết thành một cụm của
     * xe, và lần đọc lại thì có đủ động từ + đối tượng nên ra một ý định **rất tự tin mà sai**.
     *  1. **Tính năng đã bỏ / không có nút** ⇒ trả lời đúng tên, không đem đi chữa. *"bật đèn khẩn cấp"* từng ra
     *     `Control(trunk, 1)` = **mở cốp** (*"cấp"* → *"cốp"*, cặp lẫn owner đã nêu).
     *  2. **Câu nêu một HỌ thứ mà chưa nói cái nào** ⇒ hỏi lại, không đoán sang họ khác. *"áp suất lốp bên trái
     *     là bao nhiêu"* từng ra `Read(soc)` (*"bên"* → *"pin"*) — trả lời phần trăm pin cho một câu hỏi về lốp.
     *  3. **Câu HỎI thì không bao giờ được thành lệnh GHI.** *"tất cả cửa đang khóa hay đang mở"* từng ra
     *     `Control(sunroof, 1)` = **mở cửa sổ trời**. Cổng này đứng SAU phép chữa (chứ không chặn trước) để câu
     *     hỏi nghe trượt vẫn chữa được thành một câu ĐỌC — chỉ chặn đúng đường ra lệnh ghi.
     *
     * ⚠ Cổng (2) và (3) **chỉ áp cho câu HỎI**, không áp cho câu ra lệnh. [ĐO off-car] siết sang cả câu lệnh thì
     * hai phép chữa đang chạy đúng chết theo: *"mở góc sau"* (họ *"Góc …"*) và *"mật độ hai mươi hai độ"* (họ
     * *"Độ …"*) — ở đó chính phép chữa mới là thứ **giải** được cái họ ấy, nên chặn nó là chặn ngược. Câu lệnh
     * nói về một tính năng không có thì đã có cổng (1) lo.
     */
    private fun fuzzy(got: VoiceIntent, t: List<Token>, terms: List<VoiceTerm>, p: List<String>, s: String):
        VoiceIntent {
        if (got !is VoiceIntent.Unknown) return got
        if (got.reason == VoiceUnknownReason.FEATURE_GONE) return got
        if (VoiceFeatureGone.match(t) != null) return VoiceIntent.Unknown(VoiceUnknownReason.FEATURE_GONE, s)
        // Câu HỎI không được chữa chính tả thành một LỆNH (≤ 2.98 BYD: cổng D1 "hỏi không thành lệnh ghi xe"; Android box
        // B2 · W3 không còn lệnh xe, nhưng câu hỏi vẫn không được thành mở app / đổi hồ sơ).
        if (VoiceQuestion.isQuestion(t)) return got
        return VoicePhoneticMatch.orRepair(got, t, terms) { parseTokens(it, terms, p, s) }
    }

    /** Phân tích MỘT vế (không tách tiếp) — cửa dùng cho test và cho chỗ đã tự tách. */
    fun parseOne(
        text: String,
        profiles: List<String> = emptyList(),
        apps: List<String> = emptyList(),
        places: List<String> = emptyList(),
        aliases: List<VoiceAppAlias> = emptyList(),
    ): VoiceIntent {
        val t = VoiceLexicon.tokenize(text)
        val terms = VoiceGrammar.terms(profiles, apps, aliases)
        return fuzzy(parseTokens(t, terms, places, text), t, terms, places, text)
    }

    private fun splitOnConnectors(t: List<Token>): List<List<Token>> {
        val out = ArrayList<List<Token>>()
        var start = 0
        t.forEachIndexed { i, tok ->
            if (CONNECTOR_WORDS.matches(tok)) {
                if (i > start) out.add(t.subList(start, i))
                start = i + 1
            }
        }
        if (start < t.size) out.add(t.subList(start, t.size))
        return out
    }

    // ── Thân bộ phân tích ────────────────────────────────────────────────────────────────────────

    @Suppress("ReturnCount")
    private fun parseTokens(
        raw: List<Token>,
        terms: List<VoiceTerm>,
        places: List<String>,
        original: String,
    ): VoiceIntent {
        val t = dropFillers(raw)
        if (t.isEmpty()) return VoiceIntent.Unknown(VoiceUnknownReason.EMPTY, original)
        // WP8 · [VoiceFeatureGone.HARD_BLOCK] — từ chặn cứng, xét TRƯỚC mọi phép khớp (lý do ở KDoc bên đó).
        if (VoiceFeatureGone.blocked(t)) return VoiceIntent.Unknown(VoiceUnknownReason.FEATURE_GONE, original)
        // (a) Cụm hỏi (*"… bao nhiêu?"*) — người Việt hỏi xe bằng cụm hỏi, không bằng động từ đứng đầu.
        val ask = VoiceQuestion.askAt(t)
        if (ask != null) {
            val (at, len) = ask
            val body = dropFillers(t.subList(0, at) + t.subList(at + len, t.size))
            return objectOnlyRead(body, terms, original)
        }
        // (a″) Câu hỏi LỰA CHỌN (*"… hay không"*) — không bao giờ là lệnh ([VoiceQuestion.isChoice]).
        if (VoiceQuestion.isChoice(t)) return objectOnlyRead(VoiceQuestion.strip(t), terms, original)
        // (≤ 2.98 BYD còn (a‴) câu hỏi mức rụng chữ hỏi + (a') *"chỉ số X"* — chỉ phục vụ đọc datum xe, gỡ ở Android box B2 · W3.)

        // (b) Động từ đứng đầu, khớp cụm DÀI nhất.
        val verbHit = VoiceGrammar.VERBS.firstOrNull { VoiceLexicon.phraseAt(t, 0, it.first) }

        // (b') CẢ CÂU chính là TÊN của một việc ⇒ tên thắng động từ.
        //
        // Luật: cụm khớp tại vị trí 0 mà **dài hơn** cụm động từ thì nó là tên của việc. (≤ 2.98 BYD sinh ra cho gói lệnh
        // *"Mở hết kính"* · nút *"Mở khoá cửa"*; Android box B2 · W3 còn mỗi hành động launcher.)
        headMatch(t, terms, verbHit?.first?.size ?: 0)?.let { head ->
            val after = dropFillers(t.subList(head.words.size, t.size))
            // ⚠ [SOÁT 1.69 · P1] Không động từ + cụm không đọc đuôi ⇒ KHÔNG phải lệnh (*"cốp xe bẩn quá"* từng ra **mở cốp**) — KDoc [VoiceGrammar.readsTail].
            if (verbHit == null && after.isNotEmpty() && !VoiceGrammar.readsTail(head)) return@let
            return build(head, VoiceVerb.OPEN, after, terms, places, original)
        }
        // (b½) L7 — *"bố cục 2 cột"* / *"đổi sang bố cục 4 ô"* / *"về bố cục hai hàng"*.
        //
        // Đứng SAU [headMatch] (nhãn registry vẫn thắng) và TRƯỚC [savedPlace]: hai chữ *"bố cục"* là một cụm
        // đánh dấu rất hẹp, còn *"về/đi/đến"* của sổ địa chỉ thì rộng — cái hẹp phải xét trước, nếu không một
        // mục sổ tên *"bố cục"* sẽ nuốt mất cả họ câu này. [VoiceLayouts.match] tự trả `null` cho mọi câu không
        // mang cụm đánh dấu, nên nó không đụng tới một câu nào đang chạy.
        VoiceLayouts.match(t)?.let { return VoiceIntent.Layout(it) }
        // (b'') *"về nhà"* · *"đi làm"* · *"đến công ty"* — động từ CÓ ĐIỀU KIỆN của sổ địa chỉ.
        //
        // Đứng SAU [headMatch] có chủ ý: bỏ dấu thì "đến"="đèn" nên "đèn đọc" phải được nhãn nút giành trước.
        // Chỉ nhận khi cả phần đuôi là một nơi có thật — xem KDoc [VoicePlaces.PLACE_VERBS].
        savedPlace(t, places, terms)?.let { return it }
        // (b''') [ĐO log 1.79] "tìm + TỪ-NHẠC" → tra nhạc; "tìm <phi-nhạc>" giữ NO_VERB. Đứng sau headMatch nên "tìm đường đến X" (NAV) đã giải trước — xem [mediaSearch].
        if (verbHit == null) {
            VoiceMediaNavParse.mediaSearch(t, terms)?.let { return it }
            return VoiceSlotNoVerb.pick(t, terms, original) ?: VoiceIntent.Unknown(VoiceUnknownReason.NO_VERB, original)
        }
        val verb = verbHit.second
        val rest = dropFillers(t.subList(verbHit.first.size, t.size))

        // (c) Điểm đến là từ vựng MỞ ⇒ KHÔNG đem so với từ vựng của xe. Một điểm đến bất kỳ có thể chứa đúng một
        //     cụm của xe (vd "trạm sạc") và khớp nó lên là biến câu dẫn đường thành lệnh sạc pin.
        if (verb == VoiceVerb.NAV) return VoiceMediaNavParse.nav(rest, places, original, terms)

        // (d) Quét từ trái sang, lấy **cách hiểu ĐẦU TIÊN có nghĩa**.
        //
        // ⚠ Không dừng ở cụm khớp ĐẦU: cụm 1 từ ngắn đụng từ thường (datum `gear` nhãn "Số" ⇒ "chuyển sang hồ sơ
        // Vợ" khớp "số" giữa câu → MISMATCH). Đi tiếp tới cách hiểu đầu tiên hợp động từ ⇒ ca đó tự giải.
        // (c') H3 — app ≥2 từ ngay sau động từ thắng nhãn NGẮN giữa câu; lý do + ba cổng ở [VoiceTailClause.appAtHead].
        VoiceTailClause.appAtHead(rest, terms, verb)?.let { return it }
        var firstMiss: VoiceIntent? = null
        rest.indices.forEach { i ->
            val cands = VoiceGrammar.matchAt(rest, i, terms)
            if (cands.isNotEmpty()) {
                val term = choose(cands)
                // (d') H3 — **luật dãy dài nhất thắng áp cho cả TÊN APP**, không chỉ cho từ vựng chung.
                //
                // [ĐO xe 2026-09-16, tester 1.66] "Mở Google được mà Google Map chưa hiểu": nhãn app "Google"
                // (1 từ) khớp ở vị trí 0 → dừng luôn, cách nói 2 từ "google map" không được hỏi tới (luật dài-trước
                // hụt ở ranh giới hai bảng).
                //
                // Chỉ nhận khi cách nói **dài hơn hẳn** cụm vừa khớp ⇒ nhãn thật vẫn thắng khi hoà (*"mở
                // google"* vẫn mở app Google), đúng cam kết ở KDoc [VoiceTailClause.spokenApp]. Và chỉ cho
                // động từ MỞ/BẬT: *"đóng google map"* phải đi tiếp để ra [VoiceUnknownReason.APP_CLOSE], còn
                // *"xem …"* thì đã có nhánh riêng — bẻ chúng về đây là đổi hành vi ngoài phạm vi phép đo.
                if (VoiceGrammar.isAction(verb) && !VoiceTailClause.closesApp(verb)) {
                    VoiceTailClause.appByTargetName(rest, i, term.words.size)?.let { return it }
                }
                val after = dropFillers(rest.subList(i + term.words.size, rest.size))
                // W3 — chữ nhạc trong một câu về XE (đèn theo nhạc · nhạc trên cụm) ⇒ "đã gỡ", không phải lệnh nhạc.
                if (term.kind == VoiceTermKind.MEDIA && VoiceFeatureGone.carMedia(rest.subList(0, i), after)) {
                    return VoiceIntent.Unknown(VoiceUnknownReason.FEATURE_GONE, original)
                }
                val built = build(term, verb, after, terms, places, original)
                if (built !is VoiceIntent.Unknown) return built
                if (firstMiss == null) firstMiss = built
            }
        }
        // (e) Chưa có cách hiểu nào CÓ NGHĨA ⇒ ba đường cuối (cách gọi app tiếng Việt · tên app bị ASR bóp méo ·
        //     tên hồ sơ bóp méo) — thứ tự + cổng ở KDoc [VoiceLastResort] (tệp riêng: đây đã sát trần 500 dòng).
        // Android box B2 · W3 — câu nói về một bộ phận XE (*"mở kính"* · *"bật điều hoà"*) không được rơi xuống các đường ĐOÁN tên
        // app phía dưới (thành mở một app tên gần giống) ⇒ trả lời "điều khiển xe đã bỏ" ([VoiceFeatureGone.carObject]).
        if (firstMiss == null && verb != VoiceVerb.PLAY && VoiceFeatureGone.match(rest) != null) return VoiceIntent.Unknown(VoiceUnknownReason.FEATURE_GONE, original)
        return VoiceLastResort.pick(verb, rest, terms, original) ?: firstMiss ?: noObject(verb, rest, original, terms)
    }

    /**
     * *"&lt;động từ nơi chốn&gt; &lt;nơi&gt;"* ⇒ [VoiceIntent.NavigateSaved], hoặc `null` khi câu **không** phải thế.
     *
     * Ba điều kiện, và điều kiện thứ ba là thứ làm cả cơ chế này an toàn:
     *  1. động từ khớp tại **vị trí 0** (dài trước ngắn — *"đi đến"* thắng *"đi"*);
     *  2. mệnh đề *"bằng &lt;app&gt;"* ở cuối được cắt ra trước, đúng như mọi câu dẫn đường khác;
     *  3. **toàn bộ** phần còn lại phải khớp một nơi ([VoicePlaces.match]) — không khớp thì trả `null` và câu đi
     *     tiếp y như chưa có gì xảy ra. Nhờ (3), *"về bố cục 2 cột"* vẫn là [VoiceUnknownReason.NO_VERB] như bộ
     *     test đang đòi, mà không cần một danh sách từ cấm nào.
     */
    private fun savedPlace(t: List<Token>, places: List<String>, terms: List<VoiceTerm>): VoiceIntent? {
        val verb = VoicePlaces.PLACE_VERB_SPELLED.firstOrNull { (w, sp) ->
            VoiceLexicon.phraseAt(t, 0, w) && w.indices.all { VoiceHomograph.spelledOk(t[it], sp[it]) }
        }?.first ?: return null
        val after = dropFillers(t.subList(verb.size, t.size))
        if (after.isEmpty()) return null
        val hit = VoiceTailClause.appAfterMarker(after, VoiceAppKind.NAV, terms)
        val body = if (hit == null) after else after.subList(0, hit.second)
        val label = VoicePlaces.match(body.map { it.norm }, places) ?: return null
        return VoiceIntent.NavigateSaved(label, hit?.first)
    }

    /**
     * Cụm khớp tại vị trí 0 và **dài hơn** động từ đứng đó ([verbWords] = 0 khi không có động từ nào).
     *
     * Chỉ nhận MACRO/CONTROL/LAUNCHER: chúng là những thứ **được đặt tên như một việc**. Một datum hay một app
     * đứng trần thì không phải câu lệnh (*"pin"* một mình không nói lên là xem hay làm gì), nên chúng vẫn phải đi
     * qua đường động từ. `internal`: [VoiceBareCover] hỏi đúng phép khớp này (không chép một bản thứ hai).
     */
    internal fun headMatch(t: List<Token>, terms: List<VoiceTerm>, verbWords: Int): VoiceTerm? {
        val cands = VoiceGrammar.matchAt(t, 0, terms).filter { it.kind == VoiceTermKind.LAUNCHER }
        if (cands.isEmpty()) return null
        val best = choose(cands)
        return best.takeIf { it.words.size > verbWords }
    }

    /**
     * Câu HỎI chỉ có đối tượng (*"pin bao nhiêu"*). ≤ 2.98 BYD tra datum xe (`Read`); Android box B2 · W3 không còn datum nào ⇒
     * câu hỏi về xe nói "điều khiển xe đã bỏ" ([VoiceFeatureGone]), còn lại là thiếu đối tượng.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun objectOnlyRead(body: List<Token>, terms: List<VoiceTerm>, original: String): VoiceIntent =
        if (VoiceFeatureGone.match(body) != null) VoiceIntent.Unknown(VoiceUnknownReason.FEATURE_GONE, original)
        else VoiceIntent.Unknown(VoiceUnknownReason.NO_OBJECT, original)

    /**
     * Chọn ứng viên: **dài nhất trước**, rồi mới tới loại động từ (luật 1 rồi luật 2).
     *
     * Thứ tự đó không hoán đổi được: ưu tiên loại trước sẽ cho phép một cụm NGẮN đúng loại thắng một cụm DÀI khác
     * loại, tức mở lại đúng cái cửa mà L-RE2 đã đóng (*"chế độ đèn pha"* → *"đèn pha"*).
     */
    private fun choose(cands: List<VoiceTerm>): VoiceTerm {
        val longest = cands.maxOf { it.words.size }
        return cands.first { it.words.size == longest }
    }

    /** Không khớp đối tượng nào ⇒ vẫn còn vài động từ tự đứng một mình được (nhạc), phần còn lại là từ vựng mở. */
    private fun noObject(verb: VoiceVerb, rest: List<Token>, original: String, terms: List<VoiceTerm>): VoiceIntent = when {
        verb == VoiceVerb.NEXT -> VoiceIntent.Media(VoiceMediaOp.NEXT)
        verb == VoiceVerb.PREV -> VoiceIntent.Media(VoiceMediaOp.PREV)
        verb == VoiceVerb.PLAY && rest.isEmpty() -> VoiceIntent.Media(VoiceMediaOp.PLAY)
        verb == VoiceVerb.PAUSE && rest.isEmpty() -> VoiceIntent.Media(VoiceMediaOp.PAUSE)
        // "phát <cái gì đó không biết>" — đúng hình dạng "mở nhạc", chỉ thiếu từ đánh dấu. Coi là từ vựng mở.
        verb == VoiceVerb.PLAY -> VoiceTailClause.withTarget(rest, VoiceAppKind.MUSIC, terms) { body, app ->
            if (body.isEmpty()) VoiceIntent.Media(VoiceMediaOp.PLAY, app = app)
            else VoiceIntent.Media(VoiceMediaOp.QUERY, VoiceTailClause.text(body), app)
        }
        else -> VoiceIntent.Unknown(VoiceUnknownReason.NO_OBJECT, original)
    }

    @Suppress("LongParameterList")
    private fun build(
        term: VoiceTerm,
        verb: VoiceVerb,
        after: List<Token>,
        terms: List<VoiceTerm>,
        places: List<String>,
        original: String,
    ): VoiceIntent =
        when (term.kind) {
            // *"Mở ứng dụng VTV Go"*: cụm "Ứng dụng" khớp hành động launcher (mở NGĂN KÉO), nhưng còn một cái
            // tên đứng sau — và cái tên đó mới là thứ người ta muốn. Đuôi khớp một app đã cài ⇒ mở thẳng app đó.
            VoiceTermKind.LAUNCHER -> when {
                !VoiceGrammar.isAction(verb) -> VoiceIntent.Unknown(VoiceUnknownReason.MISMATCH, original)
                // W3 [ĐO corpus `unknown_app_close`]: *"đóng ứng dụng này"* từng MỞ ngăn kéo app — động từ ĐÓNG/TẮT không mở.
                VoiceTailClause.closesApp(verb) -> VoiceIntent.Unknown(VoiceUnknownReason.APP_CLOSE, original)
                else -> VoiceTailClause.appInTail(after, terms)?.let { (app, tail) ->
                    if (VoiceTailClause.closesApp(verb)) VoiceIntent.Unknown(VoiceUnknownReason.APP_CLOSE, original)
                    else VoiceIntent.OpenApp(app, VoiceTailClause.slotAt(tail))
                } ?: VoiceIntent.Launcher(term.id)
            }

            VoiceTermKind.PROFILE -> VoiceIntent.Profile(term.id)

            // V1.1 — *"mở YouTube **vào ô số 2**"*: cái đuôi sau tên app quyết định app đi vào ô nào. Không có
            // đuôi ⇒ `null` ⇒ y như trước (mở toàn màn).
            //
            // ⚠ Nhánh này CỐ Ý xét động từ (mọi nhánh khác đều xét): tới 1.63 nó dựng `OpenApp` cho **mọi** động
            // từ, nên [ĐO] `đóng YouTube` / `tắt YouTube` lại **MỞ** YouTube — xem [VoiceUnknownReason.APP_CLOSE].
            VoiceTermKind.APP ->
                if (VoiceTailClause.closesApp(verb)) VoiceIntent.Unknown(VoiceUnknownReason.APP_CLOSE, original)
                else VoiceIntent.OpenApp(term.id, VoiceTailClause.slotAt(after))

            VoiceTermKind.NAV -> VoiceMediaNavParse.nav(after, places, original, terms)

            VoiceTermKind.MEDIA -> VoiceMediaNavParse.media(verb, after, original, terms)
        }

    // ── Nhạc / dẫn đường ─────────────────────────────────────────────────────────────────────────
    // Ba bộ dựng `media`/`mediaSearch`/`nav` tách sang [VoiceMediaNavParse] (trần 500 dòng — CLAUDE.md §4.1).

    // ── Câu hỏi ĐỌC ──────────────────────────────────────────────────────────────────────────────
    // Cụm dẫn *"chỉ số X"* nay khai ở [VoiceQuestion.READ_LEADS] — [VoiceClarify] cần cùng bảng ấy (xem KDoc ở đó).

    // ── Tiện ích ─────────────────────────────────────────────────────────────────────────────────

    private fun dropFillers(t: List<Token>): List<Token> = VoiceLexicon.dropLeadingFillers(t)
}
