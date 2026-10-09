package com.byd.clusternav.launcher.voice

/**
 * ═══ V1 · TỪ ĐỒNG NGHĨA — KHAI **MỘT CHỖ** ════════════════════════════════════════════════════════════════════
 *
 * Tách khỏi `VoiceGrammar.kt` ngày 2026-09-16 (vòng H3/H4): bảng này nhận thêm cách gọi **giọng Nam** và cách nói
 * **tên app theo âm Việt**, và tệp cũ chạm trần 500 dòng (CLAUDE.md §4.1 · `VoiceCommandWiringContractTest`).
 * Tách theo VAI, không cắt cho đủ số: tệp kia nói về *cách dựng từ vựng*, tệp này chỉ là **dữ liệu cách gọi**.
 *
 * ## Vì sao ở đây mà không rải vào từng dòng registry
 * `ControlRegistry`/`TelemetryRegistry` là **hợp đồng với màn hình**: `label` là chữ hiện trên nút, và hàng trăm
 * bài test đang assert đúng chuỗi đó (KDoc `Localized.label`). Nhét thêm một danh sách "còn gọi là…" vào mỗi dòng
 * sẽ (a) làm 188 dòng dữ liệu phình ra vì một tính năng duy nhất dùng tới, và (b) mời người sau đặt cách-gọi-miệng
 * vào ô `label` cho tiện — tức đổi chữ trên nút. Ở đây thì nhãn vẫn là nhãn, cách nói là cách nói.
 *
 * ## Luật khai
 *  • **Chỉ khai cái mà nhãn KHÔNG phủ.** Nhãn *"Đèn đọc"* đã tự khớp, không cần khai lại — [VoiceGrammar] sinh cụm
 *    từ nhãn VI/EN/ngắn của **mọi** dòng.
 *  • Viết **không dấu, chữ thường** — cùng dạng [VoiceLexicon.deaccent] trả về, để khỏi có hai luật chuẩn hoá.
 *  • Cụm trùng nhau giữa hai mã là **hợp lệ**: [VoiceIntentParser] chọn theo loại động từ (xem KDoc ở đó).
 *  • Mọi cụm khai ở đây **phải** có dạng có dấu ở [SherpaSpokenWords.ACCENTED] hoặc được khai là không có
 *    ([SherpaSpokenWords.NO_VI_FORM]) — `SherpaBiasingCoverageTest` ép bằng máy.
 */
object VoiceSynonyms {

    // Android box B2 · W3 (2026-10-09): `CONTROL` / `TELEMETRY` (cách nói thêm cho nút / datum xe) và `MISHEARD` (bí danh nghe
    // nhầm có điều kiện — toàn mã lọc bụi) gỡ cùng `ControlRegistry` / `TelemetryRegistry`.

    /** Cụm chỉ **loại đối tượng**, không chỉ một mã — dùng để gỡ nghĩa cho động từ quá tải (RE Kiki §7c: *"Mở"*). */
    val MEDIA_WORDS: List<String> = listOf("bai hat", "bai", "nhac", "ca khuc", "song", "music", "track")

    /** Cụm mở đầu một ĐIỂM ĐẾN (đứng sau một động từ không phải NAV, vd *"tìm đường tới …"*). */
    val NAV_WORDS: List<String> = listOf("duong den", "duong toi", "destination")

    /**
     * ═══ V1.1 · CÁCH NÓI TÊN **APP ĐÍCH** ([VoiceAppTargets]) — khai MỘT chỗ, như mọi cách nói khác ═══════════
     *
     * Spec R17(d). Cùng hợp đồng với mọi bảng cách nói: **không dấu, chữ thường**.
     *
     * ## Vòng 2026-09-16 — phản hồi tester 1.66: *"Mở Google được mà Google Map chưa hiểu"*
     * [ĐO] `docs/diagnostics/voice-mishear-2026-09-16.md` §3: loại ý định `app` đúng **12,8 %** — thấp nhất bảng,
     * kém thứ nhì tới 4 lần. §5 cho biết vì sao: mô hình `zipformer-vi` là mô hình **tiếng Việt**, nên nó in ra
     * đúng cái nó có — *"mở gu gồ máp"* → `mở google map`, *"mở du túp"* → `mở youtube`, *"mở za lô"* → `mở zalô`.
     * Tức chuỗi model in ra **đã đúng brand**, chỉ là từ vựng của Kachi không có cách viết ấy.
     *
     * Nguồn chữ: `scripts/voice/data/apps.tsv` (người soạn tay, có cột vùng miền) + các dòng `open_app` của
     * `scripts/voice/data/aliases-proposed.tsv` — nhưng **không nhập cả 101 dòng `synonym`**.
     *
     * ### Luật LOẠI (viết ra để lần sau khỏi cãi)
     * Chỉ nhận một chuỗi khi nó là **cách đọc nhận ra được của chính thương hiệu**. Bỏ khi:
     *  1. **≤ 2 chữ cái** hoặc ≤ 1 từ mà không phải tên thương hiệu (`mở r`, `mở ip`, `mv at`, `mở g ba`);
     *  2. **trùng một từ tiếng Việt thường** (`mở ra`, `mở hoa`, `mở mắt`, `mở các`, `mở dựa`, `mở dây`, `mở se`,
     *     `ở quê`, `ở hà nội`) — một bí danh như thế cướp câu đang chạy tốt **mà im lặng**, đúng lỗi §7 CLAUDE.md;
     *  3. **rác nhận dạng** không đọc ra thương hiệu nào (`goovel`, `mở sy`, `mở sei`, `myat ubusc`, `mở rưng ba`);
     *  4. **mã không có trong bảng đích** (`zalo`, `carplay`, `androidauto` — xem [VoiceAppPhonetics] về đường đi
     *     của chúng).
     *
     * ⇒ Đếm bằng máy trên đúng 101 dòng `synonym`: nhận **4**, bỏ **97**. Bốn dòng nhận là `mở du tu` (youtube) ·
     * `xem còn mấy phút nữa đầy` (`charging_eta_min`) · `đọc ngày` và `các bụi` (hai dòng sau vào [MISHEARD], có
     * điều kiện). Tỉ lệ thấp ấy **không** phải sự cẩn thận quá mức: 97 dòng kia là chuỗi mô hình nghe **hỏng**
     * (`goovel`, `mở sy`, `myat ubusc`) hoặc trùng từ thường (`mở ra`, `mở hoa`, `mở mắt`) — nhận chúng là dựng
     * một từ vựng theo lỗi của một mô hình, và cái sai ấy sẽ sống lâu hơn chính mô hình đó.
     *
     * Phần còn lại của bảng dưới lấy từ `apps.tsv` — cách **đọc** do người soạn tay, không phải chuỗi máy nghe
     * nhầm; §5 của bảng nghe nhầm dùng để **xác nhận** rằng mô hình thật sự in ra chúng.
     *
     * ⚠ Các cụm này CỐ Ý **không** vào từ vựng chung ([VoiceGrammar.terms]): chúng chỉ được tra ở hai vị trí —
     * ngay sau cụm đánh dấu *"bằng / trên / với"* ([VoiceTailClause.appAfterMarker]) và ở đầu phần đuôi sau động
     * từ ([VoiceTailClause.spokenApp]). Thả *"quây"* hay *"youtube"* vào từ vựng chung là đổi cách hiểu của những
     * câu đang chạy tốt (*"quay lại bài"* là lệnh PREV).
     */
    val APP_TARGETS: Map<String, List<String>> = mapOf(
        VoiceAppTargets.YT_MUSIC to listOf(
            "youtube music", "yt music", "nhac youtube", "youtube nhac",
            "du tup miu dich", "nhac du tup",
        ),
        VoiceAppTargets.YOUTUBE to listOf(
            "youtube", "yt",
            "du tup", "iu tup", "diu tup", "dut tup", "du tu",
        ),
        VoiceAppTargets.SPOTIFY to listOf("spotify", "spo ti phai", "so po ti phai", "po ti phai", "spo ti phy"),
        VoiceAppTargets.ZING to listOf("zing mp3", "zing", "zing em pe ba", "ding mo pe ba"),
        // [ĐO] `docs/diagnostics/emulator-voice-e2e-2026-09-15.md` §3 L6 (t45): *"mở bản đồ"* → `Unknown` trên
        // máy có nhãn hệ thống tiếng Anh (*"Maps"*). *"bản đồ"* CHÍNH LÀ nhãn tiếng Việt của app này, nên đây
        // không phải một biệt danh bịa ra — và nhãn thật vẫn được xét TRƯỚC.
        VoiceAppTargets.GMAPS to listOf(
            "google map", "google maps", "ban do google", "ban do", "google",
            "gu go map", "gu go mep", "gu go", "cai ban do",
        ),
        VoiceAppTargets.WAZE to listOf("waze", "quay", "guay", "guey"),
        // [owner 2026-09-19] "vietmap" là tên tự chế, ASR tiếng Việt nghe "hên xui" (việt máp/mép/mốp/mụp/láp…)
        // ⇒ thêm biến thể phiên âm như các app tiếng Anh khác (xem GMAPS "gu go mep"). Toàn 2-từ nên không đụng
        // từ thường; APP_TARGETS chỉ dùng cho đường mở-app/chọn-app-nav nên không rớt vào vựng chung. (Biến thể
        // rụng-1-âm-cuối như "vietma" đã do VoiceAppTargets.bySpokenLoose lo — KHÔNG khai exact ở đây kẻo phá nó.)
        VoiceAppTargets.VIETMAP to listOf(
            "viet map", "vietmap", "viet mat", "ban do viet",
            "viet mep", "viet mop", "viet mup", "viet lap",
            // owner 2026-09-23: user đọc "việt máp lay" / "vietmap live" (tên đầy đủ) không nhận. Thêm dạng có "live/lay".
            "viet map lay", "viet map live", "vietmap live", "vietmap lay", "map lay", "map live",
        ),
    )
}
