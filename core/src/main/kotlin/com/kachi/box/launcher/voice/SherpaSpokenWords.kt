package com.kachi.box.launcher.voice

/**
 * ═══ V2 pha NGHE · DẠNG **CÓ DẤU** CỦA TỪ VỰNG KHÔNG DẤU ═════════════════════════════════════════════════════
 *
 * Spec `docs/specs/kachi-voice-engine-v2.html` §Design. Thuần Kotlin (`:core`) ⇒ kiểm off-car.
 *
 * ## Vì sao tệp này phải tồn tại (một chỗ hụt ĐÃ ĐO, không phải phòng xa)
 * [VoiceSynonyms] và [VoiceGrammar.VERBS] khai **không dấu, chữ thường** — đúng hợp đồng của tầng so khớp CHỮ
 * ([VoiceLexicon.deaccent] trả về dạng đó). Hotwords của sherpa thì ngược lại: mô hình VN xuất **CHỮ HOA CÓ DẤU**
 * (`tokens.txt`: `▁ĐÈN`, `▁PIN`…) nên một hotword không dấu **không mã hoá được** bằng bảng BPE ⇒ native bỏ lặng
 * lẽ (xem KDoc [SherpaHotwords]). Đổ thẳng `VoiceSynonyms` vào tệp hotwords vì thế **không** giúp gì — nó chỉ
 * trông như đã giúp.
 *
 * [ĐO] `docs/diagnostics/emulator-voice-e2e-2026-09-15.md` §3 L3 (25 câu WAV, biasing=true, model
 * `zipformer-vi-2025-04-20`): `xem pin` → nghe *"xem tin"* · `mở kính trước trái` → *"mở kín trước trái"* ·
 * `dừng nhạc` → *"rừng nhạc"* — cả ba đều **ra `Unknown`**. Ba từ trung tâm của bộ lệnh (`pin` · `kính` ·
 * `dừng`) chưa bao giờ là hotword: hai từ đầu vì nhãn *"Pin (SOC)"* / *"Kính trước-trái"* bị dấu câu giết cả cụm
 * ([SherpaHotwords] đã vá), từ thứ ba vì **động từ chưa từng là nguồn hotword**.
 *
 * ## Vì sao KHÔNG khôi phục dấu bằng máy
 * Đã thử [ĐO 2026-09-15, script off-car]: dựng bảng `bỏ dấu → có dấu` từ chính nhãn của 4 bộ đăng ký rồi tra
 * ngược. Kết quả sai ở đúng những từ cần nhất — `dung` → *"dụng"* (từ *"ứng dụng"*), `tim` → *"tím"*,
 * `duong` → *"dương"*, `chuyen` → *"chuyến"*. Một hotword **sai dấu** còn tệ hơn không có: nó kéo câu về một từ
 * khác hẳn. Nên dạng có dấu phải được **khai**, và được **canh bằng máy** (xem `SherpaBiasingCoverageTest`):
 *  • mỗi giá trị phải bỏ dấu ra ĐÚNG khoá của nó ⇒ không ai lén thêm một cách nói mới vào đây;
 *  • mỗi khoá phải có thật trong [VoiceSynonyms] / [VoiceGrammar.VERBS] ⇒ không có mục chết;
 *  • MỌI cụm của [VoiceSynonyms] phải nằm ở [ACCENTED] hoặc [NO_VI_FORM] ⇒ thêm một cách nói mà quên dạng có dấu
 *    thì bài canh ĐỎ, không im lặng;
 *  • MỌI [VoiceVerb] phải có ít nhất một dạng có dấu ⇒ thêm động từ mới cũng vậy.
 */
object SherpaSpokenWords {

    /**
     * Dạng **có dấu** của từng cụm trong [VoiceSynonyms] — khoá là ĐÚNG chuỗi đã khai ở đó (không dấu, thường).
     *
     * Đây không phải một từ vựng thứ hai: nó là **cùng một cụm, viết đúng chính tả**. Bài canh ép điều đó
     * (`VoiceLexicon.deaccent(giá trị) == khoá`), nên chỗ này không thể trở thành nơi lén thêm câu lệnh.
     */
    val ACCENTED: Map<String, String> = mapOf(
        // Android box B2 · W3 (2026-10-09): mọi dạng có dấu của nút/datum xe (kính · ghế · đèn · điều hòa · lọc · cốp…)
        // gỡ cùng `VoiceSynonyms.CONTROL`/`TELEMETRY` — bảng không được giữ mục chết (`SherpaBiasingCoverageTest`).
        "bai hat" to "bài hát",
        "bai" to "bài",
        "nhac" to "nhạc",
        "ca khuc" to "ca khúc",
        "duong den" to "đường đến",
        "duong toi" to "đường tới",

        // ═══ H3 · TÊN APP ĐỌC THEO ÂM VIỆT ([VoiceSynonyms.APP_TARGETS]) ═══════════════════════════════
        // Nguồn: `scripts/voice/data/apps.tsv` + §5 của `voice-mishear-2026-09-16.md` (chuỗi mô hình THẬT SỰ
        // in ra). Tên thuần tiếng Anh (*"youtube"*, *"spotify"*…) nằm ở [NO_VI_FORM]: mô hình VN không phát
        // ra token ấy nên bias vô nghĩa — đúng luật đã có, chỉ nay áp cho cả bảng đích.
        "nhac youtube" to "nhạc youtube",
        "youtube nhac" to "youtube nhạc",
        "du tup miu dich" to "du túp miu dích",
        "nhac du tup" to "nhạc du túp",
        "du tup" to "du túp",
        "iu tup" to "iu túp",
        "diu tup" to "diu túp",
        "dut tup" to "dút túp",
        "du tu" to "du tu",
        "spo ti phai" to "spô ti phai",
        "so po ti phai" to "sờ pô ti phai",
        "po ti phai" to "pô ti phai",
        "spo ti phy" to "spo ti phy",
        "zing em pe ba" to "zing em pê ba",
        "ding mo pe ba" to "ding mờ pê ba",
        "ban do google" to "bản đồ google",
        "ban do" to "bản đồ",
        "gu go map" to "gu gồ máp",
        "gu go mep" to "gu gồ mép",
        "gu go" to "gu gồ",
        "cai ban do" to "cái bản đồ",
        "quay" to "quây",
        "guay" to "guây",
        "guey" to "guêy",
        "viet map" to "việt máp",
        "viet mep" to "việt mép",
        "viet mop" to "việt mốp",
        "viet mup" to "việt mụp",
        "viet lap" to "việt láp",
        "viet mat" to "việt mát",
        "ban do viet" to "bản đồ việt",
        // owner 2026-09-23: dạng "VietMap Live" (việt máp lay/live).
        "viet map lay" to "việt máp lay",
        "viet map live" to "việt máp live",
        "vietmap live" to "vietmap live",
        "vietmap lay" to "vietmap lay",
        "map lay" to "máp lay",
        "map live" to "máp live",
    )

    /**
     * Cụm **cố ý không** có dạng nói tiếng Việt ⇒ không vào tệp hotwords.
     *
     * Hai loại, cùng một lý do: mô hình VN không phát ra được token đó nên bias vô nghĩa (KDoc [SherpaBiasing]).
     *  • tên/chữ tiếng Anh (*"purifier"*, *"state of charge"*…) — [VoiceIntentParser] khớp chữ lo phần này;
     *  • cụm mang **chữ số** (*"4 kinh"*) — [SherpaHotwords] bỏ token số, phần còn lại không còn nghĩa.
     */
    val NO_VI_FORM: Set<String> = setOf(
        // Android box B2 · W3: chữ Anh / chữ số của nút xe gỡ cùng `VoiceSynonyms.CONTROL`.
        "destination",
        "music",
        "song",
        "track",
        // ── H3 · tên app viết NGUYÊN BẢN tiếng Anh ───────────────────────────────────────────────────
        // Cùng lý do với khối trên, có thêm một phép đo: [ĐO] `voice-mishear-2026-09-16.md` §4 — kiểu nói
        // `tieng_anh_viet` đúng **11,3 %** (thấp nhất trong 5 kiểu). Đường chữa của chúng là **cách đọc âm
        // Việt** ở [ACCENTED], không phải bias một chuỗi mà mô hình VN không phát ra được.
        "google",
        "google map",
        "google maps",
        "spotify",
        "vietmap",
        "waze",
        "youtube",
        "youtube music",
        "yt",
        "yt music",
        "zing",
        "zing mp3",
    )

    /**
     * Dạng **có dấu** của [VoiceGrammar.VERBS], nhóm theo [VoiceVerb].
     *
     * Vì sao động từ cũng phải bias: [ĐO] w09 `dừng nhạc` → *"rừng nhạc"*. Danh từ nghe đúng, **động từ** nghe
     * sai — mà động từ mới là thứ quyết định việc gì xảy ra. Mỗi dạng ở đây phải bỏ dấu ra đúng một cụm đã khai
     * cho CHÍNH [VoiceVerb] đó (bài canh ép), nên bảng này không thể lệch khỏi bộ động từ thật.
     */
    val VERBS: Map<VoiceVerb, List<String>> = mapOf(
        VoiceVerb.ON to listOf("bật"),
        VoiceVerb.OFF to listOf("tắt"),
        VoiceVerb.OPEN to listOf("mở", "đưa"),
        VoiceVerb.CLOSE to listOf("đóng"),
        VoiceVerb.UP to listOf("tăng"),
        VoiceVerb.DOWN to listOf("giảm"),
        VoiceVerb.SET to listOf("đặt", "chỉnh"),
        VoiceVerb.READ to listOf("xem", "đọc", "hiện", "kiểm tra", "cho xem", "đọc to"),
        VoiceVerb.SWITCH to listOf("đổi", "chuyển", "đổi sang", "chuyển sang"),
        VoiceVerb.NAV to listOf(
            "dẫn đường", "chỉ đường", "dẫn đường đến", "dẫn đường tới",
            "chỉ đường đến", "chỉ đường tới", "tìm đường đến",
        ),
        VoiceVerb.PLAY to listOf("phát", "nghe"),
        VoiceVerb.PAUSE to listOf("dừng", "tạm dừng"),
        VoiceVerb.NEXT to listOf("tiếp", "tiếp theo", "bài tiếp", "bài tiếp theo", "bài kế tiếp", "chuyển bài"),
        VoiceVerb.PREV to listOf("trước", "bài trước", "quay lại bài"),
    )

    /**
     * Mọi cụm **có dấu** đáng đưa vào tệp hotwords: động từ trước (chúng quyết định VIỆC), rồi cách nói đời
     * thường. Thứ tự ổn định để tệp hotwords `diff` được giữa hai lượt đo.
     */
    val ALL: List<String> get() = VERBS.values.flatten() + ACCENTED.values
}
