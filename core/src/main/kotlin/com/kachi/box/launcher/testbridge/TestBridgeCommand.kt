package com.kachi.box.launcher.testbridge

import com.kachi.box.launcher.voice.WakeSessionJournal
import com.kachi.box.modules.navaccess.A11yBindJournal

/**
 * ═══ T-BRIDGE · MỘT LỆNH ĐÃ PHÂN TÍCH ════════════════════════════════════════════════════════════════════════
 *
 * Spec `docs/specs/kachi-test-bridge.html` R3. Thuần Kotlin ⇒ kiểm off-device.
 *
 * @property name mã lệnh ([TestBridgeCommands.SAY]…), đã kiểm là có thật.
 * @property text câu lệnh chữ (`say`).
 * @property path đường dẫn tệp WAV (`wav`), rỗng = để bên Android tự dò chỗ quen thuộc.
 * @property pkg tên gói (`slot` · `open`).
 * @property arg tên hồ sơ / tên bố cục sẵn (`profile` · `preset`) — **không** đặt tên `name` vì trường đó đã là
 *   mã lệnh; hai thứ trùng tên trong cùng một data class là chỗ truyền nhầm không ai thấy.
 * @property file tên tệp prefs (`prefs`), đã kiểm nằm trong danh sách cho phép.
 * @property slot số ô **1-based** đúng như người ta nói/gõ (`slot` · `slot_clear`); phép đổi sang 0-based nằm ở
 *   tầng thi hành, đúng một chỗ (cùng luật `VoiceDispatcher.runOpenApp`).
 * @property autoConfirm `--ez auto_confirm true` — xem KDoc [TestBridgeCommands.EXTRA_AUTO_CONFIRM].
 * @property op `--es op` của `teach` / `teach_text` (rỗng hoặc `save` — [TestBridgeTeachCommands.validOp]).
 *   (Android box B2 · W3: trường `id` · `v` · `dev` · `method` · `halArgs` của lệnh `ctl` / `hal` gỡ cùng HAL xe.)
 * @property key tên khoá prefs cho lệnh `prefs_set`, đã kiểm nằm trong [TestBridgeCommands.WRITABLE_PREFS_KEYS].
 *   Giá trị đi trong [text] (một chuỗi cho MỌI kiểu — xem KDoc [TestBridgeCommands.PREFS_SET]).
 * @property tail số dòng cuối của nhật ký cho lệnh `a11ylog` / `wakelog`, **đã kẹp** ở [TestBridgeCommands.parse]
 *   (đọc từ `--ei n`, xem [TestBridgeCommands.a11yLogTail]); `0` với mọi lệnh khác. Trường riêng chứ không mượn
 *   [slot]: `slot` là số Ô 1-based, và một tầng thi hành đọc `cmd.slot` ra số dòng là chỗ đọc nhầm không ai thấy.
 */
data class TestBridgeCommand(
    val name: String,
    val text: String = "",
    val path: String = "",
    val pkg: String = "",
    val arg: String = "",
    val file: String = "",
    val slot: Int = 0,
    val autoConfirm: Boolean = false,
    val op: String = "",
    val key: String = "",
    val tail: Int = 0,
)

/** Kết quả phân tích: hoặc một lệnh dùng được, hoặc một **mã lỗi ASCII** cho script đọc. */
sealed interface TestBridgeParse {
    data class Ok(val cmd: TestBridgeCommand) : TestBridgeParse

    /**
     * @property code mã lỗi — **ASCII, không dịch** (cùng luật `PermissionReport.logLine`: hai lượt đo trên hai
     *   máy khác ngôn ngữ phải grep được bằng MỘT chuỗi).
     */
    data class Err(val code: String) : TestBridgeParse
}

/**
 * ═══ T-BRIDGE · DANH MỤC LỆNH + BỘ PHÂN TÍCH EXTRA ═══════════════════════════════════════════════════════════
 *
 * Spec `docs/specs/kachi-test-bridge.html` R3. Đây là **toàn bộ** phần "hiểu một lệnh" của cầu kiểm thử, tách
 * khỏi Android có chủ ý.
 *
 * ## Vì sao tách, khi nó chỉ có một chỗ gọi
 * Vì đây là phần **sai được mà không ai thấy**: thiếu một extra, số ô âm, tên tệp prefs lạ — cả ba đều cho ra
 * một lệnh "chạy được" nhưng làm sai việc, và trên xe thì hậu quả là một ô bị thay app trong lúc đang lái. Để
 * nó nằm trong `BroadcastReceiver` thì cách duy nhất kiểm là cắm máy rồi bắn `am broadcast` — tức không bao giờ
 * kiểm hết được 13 lệnh × các ca thiếu đối số. Ở `:core` thì một bài `TestBridgeCommandTest` đi hết bảng.
 *
 * ## Danh mục là DỮ LIỆU ([SPECS]), không phải một `when` dài
 * Nhờ vậy hai câu hỏi khác nhau dùng chung một nguồn: *"lệnh này cần đối số gì"* (phép kiểm ở [parse]) và
 * *"cầu có những lệnh nào"* (câu trả lời của lệnh `help`/tài liệu). Viết hai lần là hai bản sẽ lệch — đúng bẫy
 * hai-bản-sao mà dự án đã trả giá bốn lần.
 */
object TestBridgeCommands {

    // ── Tên extra (khớp cờ của `am broadcast`) ───────────────────────────────────────────────────

    const val EXTRA_CMD = "cmd"
    const val EXTRA_TEXT = "text"
    const val EXTRA_PATH = "path"
    const val EXTRA_PKG = "pkg"
    const val EXTRA_ARG = "name"
    const val EXTRA_FILE = "file"
    const val EXTRA_SLOT = "n"

    /** `--es op <op>` — thao tác phụ của `teach` / `teach_text` (rỗng hoặc `save`). */
    const val EXTRA_OP = "op"

    /** `--es key <tên khoá>` — khoá prefs cho lệnh [PREFS_SET] (bắt buộc, phải nằm trong [WRITABLE_PREFS_KEYS]). */
    const val EXTRA_KEY = "key"

    /**
     * `--ez auto_confirm true` — **chỉ** có tác dụng khi chế độ kiểm thử đang bật (bản thân cả cầu này cũng vậy).
     *
     * Nó KHÔNG phải một đường tắt "bỏ qua mọi câu hỏi": nó là cách nói *"lượt chạy này do máy điều khiển, không
     * có ai ngồi trước màn để bấm"*. Lệnh chạm mức rủi ro `CONFIRM` mà **không** có cờ này thì bị **từ chối**,
     * không phải im lặng chạy — xem spec §R5.
     */
    const val EXTRA_AUTO_CONFIRM = "auto_confirm"

    // ── Mã lệnh ─────────────────────────────────────────────────────────────────────────────────

    const val SAY = "say"
    const val WAV = "wav"
    const val KWS = "kws"

    // ⚠ Android box B2 · W1/W3 — sáu mã `ctl` · `hal` · `sweep` · `featmap` · `captest` · `ctllog` rời [SPECS] ở W1 (⇒ [parse]
    // trả `unknown_cmd`), hằng + mã thi hành mồ côi gỡ ở W3. Ba mã camera gỡ ở W2b.
    const val TTS = "tts"
    const val LISTEN = "listen"
    const val PROFILE = "profile"
    const val PROFILES = "profiles"
    const val PRESET = "preset"
    const val SLOT = "slot"
    const val SLOT_CLEAR = "slot_clear"
    const val OPEN = "open"
    const val STATE = "state"
    const val PREFS = "prefs"
    const val REAPPLY = "reapply"

    /**
     * ═══ [SOÁT Pass 1 · P2] GHI một khoá prefs trong **danh sách trắng** — chỉ chế độ kiểm thử ═══════════
     *
     * `am broadcast … --es cmd prefs_set --es key voice_confirm_ids --es text "control:door"`
     *
     * ## Vì sao lệnh này phải tồn tại
     * 1.66 đổi mặc định của cổng xác nhận sang *"không hỏi gì cả"*, và tập việc-phải-hỏi (`voice_confirm_ids`)
     * do người dùng tích trong Cài đặt. Cầu kiểm thử **không có đường ghi prefs** ⇒ 8 ca `confirm=1` của
     * `voice-cases.tsv` phải bị hạ về 0 và **cả cổng an toàn quan trọng nhất của tính năng mất luôn lớp canh
     * E2E** (ghi lại ở §9 của spec: *"cầu kiểm thử không có đường ghi prefs"*). Một lệnh ghi có danh sách trắng
     * trả lại lớp canh ấy mà không mở một cửa chung vào SharedPreferences.
     *
     * ## Ba ràng buộc, và không cái nào là trang trí
     *  1. **Danh sách trắng cứng** ([WRITABLE_PREFS_KEYS]) — khai ở `:core`, kiểm ngay trong [parse]. Không có
     *     đường ghi *"khoá bất kỳ"*: receiver này `exported=true` (mọi app trên xe bắn vào được — KDoc
     *     `KachiTestBridge`), nên một lệnh ghi tuỳ ý là một đường sửa cấu hình xe cho bất cứ app nào, ngay cả khi
     *     nó vẫn phải qua công tắc chế độ kiểm thử.
     *  2. **Chỉ khoá của đường GIỌNG NÓI + nhãn chip** — năm khoá, tất cả đều đảo lại được bằng một cú chạm trong
     *     Cài đặt. Không có khoá nào chạm tới cast/cụm/phím vô-lăng.
     *  3. **Giá trị là MỘT CHUỖI** cho mọi kiểu (`"1"`/`"0"` cho công tắc, số cho `int`, danh sách ngăn phẩy cho
     *     tập). Tầng thi hành mới biết kiểu thật của từng khoá; nhét ba loại extra vào đây là ba đường phân tích
     *     cho cùng một việc.
     */
    const val PREFS_SET = "prefs_set"

    /**
     * ═══ H2 · ĐỔ NHẬT KÝ LƯỢT NÓI (`voice-log/`) RA MỘT TỆP ZIP TRÊN THẺ ════════════════════════════════════
     *
     * `am broadcast … --es cmd voice_dump` ⇒ lời đáp mang **đường dẫn tệp zip** để `adb pull` về máy soạn thảo.
     *
     * ## Vì sao là một lệnh của cầu, không chỉ một nút trong Cài đặt
     * Nút trong Cài đặt là đường của NGƯỜI trên xe (chụp màn, gửi Zalo — CLAUDE.md §11). Lệnh này là đường của
     * **máy**: một vòng đo E2E nói vài chục câu rồi muốn kéo cả chỗ tiếng ấy về host để chạy lại off-car
     * (`scripts/voice/replay-car-log.py`) — bắt người đo bấm tay giữa vòng là làm hỏng chính phép đo. Cả hai đi
     * qua **một** thân hàm (`VoiceUtteranceLog.exportZip`), không phải hai đường nén.
     *
     * Chỉ-ĐỌC đối với trạng thái XE — nhưng **xuất tiếng cabin (dữ liệu cá nhân) ra `Download/` công khai**, qua một
     * receiver `exported=true` mà app nào trên đầu xe cũng gửi được ⇒ [SCAN §6 1.69, W5] lệnh này **đi qua cổng
     * `auto_confirm`** như mọi lượt ghi thân xe: mức rủi ro ở đây là quyền riêng tư, không phải cơ khí. Không đối số.
     */
    const val VOICE_DUMP = "voice_dump"

    /**
     * ═══ 2.83 · ĐỌC NHẬT KÝ GẮN DỊCH VỤ HỖ TRỢ (phím vô-lăng) — CHỈ ĐỌC ════════════════════════════════════
     *
     * `am broadcast … --es cmd a11ylog [--ei n <số dòng>]` ⇒ N dòng cuối của `filesDir/diag/a11y-bind.log` + hai
     * mốc prefs của thang chữa (`a11y_forcestop_elapsed` · `a11y_deep_sleep_ms`).
     *
     * ## Vì sao phải có
     * [ĐO xe 29/09] trên bản PHÁT HÀNH nhật ký bền không đọc được bằng đường nào: màn Chẩn đoán đã gỡ nút (21/09)
     * và `exported=false` nên `am start` bị từ chối, `run-as` không có vì không debuggable. Tức đúng dữ liệu cần
     * để chốt *"phím chết từ lúc nào, sau đợt ngủ bao lâu"* nằm trên xe mà không lấy ra được.
     *
     * ## Ranh giới
     *  • **Không cần màn chính** (chỉ đọc đĩa + prefs) ⇒ chạy được ngay sau khi tắt máy vừa giết launcher — đúng
     *    lúc cần đọc nhất.
     *  • **Không `auto_confirm`**: không chạm xe, không đổi state, và nhật ký riêng tư theo thiết kế (chỉ mốc giờ ·
     *    hai đồng hồ · pid · trạng thái — KDoc `A11yBindJournal`), khác [VOICE_DUMP] xuất tiếng cabin.
     *  • `--ei n` **tuỳ chọn**, kẹp ở [a11yLogTail]: vắng/≤ 0 ⇒ [A11YLOG_DEFAULT_LINES], quá trần ⇒
     *    [A11yBindJournal.MAX_LINES] (tệp không bao giờ dài hơn thế). Kẹp ở đây, không ở tầng thi hành — cùng luật
     *    "một chỗ quyết định mặc định".
     */
    const val A11YLOG = "a11ylog"

    /** Số dòng mặc định của [A11YLOG] — gấp hơn hai lần 20 dòng màn Chẩn đoán từng in, vẫn dưới trần tệp. */
    const val A11YLOG_DEFAULT_LINES = 50

    /** Kẹp `--ei n` của [A11YLOG] về `1..MAX_LINES`; vắng/≤ 0 ⇒ mặc định (một lệnh đọc không đòi đối số). */
    fun a11yLogTail(requested: Int): Int =
        if (requested <= 0) A11YLOG_DEFAULT_LINES else requested.coerceAtMost(A11yBindJournal.MAX_LINES)

    /**
     * FIX286 · VK6 — `… --es cmd wakelog [--ei n <số dòng>]` ⇒ N dòng cuối của `filesDir/diag/wake-sessions.log` (mỗi
     * phiên nghe của `:wake`: lối vào · chế độ · mô hình sẵn · ms nạp · ms tới micro · kết cục — `WakeSessionJournal`).
     * `usage-*.log` không có dòng nào của `:wake` (lọc pid chính). Cùng ranh giới [A11YLOG]: chỉ đọc, không cần màn
     * chính, không `auto_confirm`; `n` kẹp ở [wakeLogTail].
     */
    const val WAKELOG = "wakelog"

    /** Kẹp `--ei n` của [WAKELOG] — cùng luật mặc định, trần theo tệp `wake-sessions.log`. */
    fun wakeLogTail(requested: Int): Int =
        if (requested <= 0) A11YLOG_DEFAULT_LINES else requested.coerceAtMost(WakeSessionJournal.MAX_LINES)

    /**
     * Khoá prefs mà [PREFS_SET] được phép ghi — **danh sách trắng**, xem KDoc [PREFS_SET] ràng buộc (1).
     *
     * Danh sách nằm ở [TestBridgeWritableKeys] (tách 2.74: tệp này đã sát trần 500 dòng của CLAUDE.md §4.1, và
     * *"khoá nào ghi được"* là một vai khác *"cú pháp một lệnh"*). Tên cũ giữ nguyên ở đây vì nó là **hợp đồng**
     * mà [parse] và mọi bài canh đang gọi — đổi tên ở 20 chỗ gọi để dời một danh sách là thay một việc cơ học
     * bằng 20 chỗ sai được.
     */
    val WRITABLE_PREFS_KEYS: Set<String> get() = TestBridgeWritableKeys.ALL

    // ── Mã lỗi (ASCII, không dịch) ──────────────────────────────────────────────────────────────

    const val ERR_NO_CMD = "no_cmd"
    const val ERR_UNKNOWN_CMD = "unknown_cmd"

    /** Thiếu một extra bắt buộc — nối thêm TÊN extra để script biết thiếu cái gì, không phải chỉ "sai cú pháp". */
    const val ERR_MISSING = "missing_extra:"
    const val ERR_BAD_SLOT = "bad_slot"
    const val ERR_BAD_PREFS_FILE = "bad_prefs_file"

    /** `--es key` của [PREFS_SET] không nằm trong [WRITABLE_PREFS_KEYS] — nối tên khoá để script biết gõ sai đâu. */
    const val ERR_BAD_PREFS_KEY = "bad_prefs_key:"

    /** `--es op` không hợp lệ cho lệnh ([TestBridgeTeachCommands.validOp]) — nối op đã gõ. */
    const val ERR_BAD_OP = "bad_op:"

    /**
     * Một lệnh: cần extra gì, nhận thêm extra gì.
     *
     * [optional] có mặt để [parse] **từ chối extra lạ**? Không — nó chỉ để tài liệu/`help` liệt kê đúng. Từ chối
     * extra lạ sẽ làm mọi script cũ gãy khi cầu thêm một cờ mới, mà lợi ích thì bằng không: extra không ai đọc
     * thì không làm gì cả.
     */
    data class Spec(val name: String, val required: List<String>, val optional: List<String> = emptyList())

    /** Toàn bộ bảng lệnh, theo thứ tự dùng thật (chạy một câu → xem máy hiểu gì → đọc trạng thái → đổi bố cục). */
    val SPECS: List<Spec> = listOf(
        Spec(SAY, listOf(EXTRA_TEXT), listOf(EXTRA_AUTO_CONFIRM)),
        Spec(WAV, emptyList(), listOf(EXTRA_PATH)),
        Spec(KWS, emptyList(), listOf(EXTRA_PATH)),
        // Android box B2 · W1 — `camera` · `camera_frame` · `camera_synth` (camera BYD) gỡ khỏi bảng lệnh.
        Spec(TTS, listOf(EXTRA_TEXT)),
        Spec(LISTEN, emptyList()),
        Spec(STATE, emptyList()),
        Spec(PROFILES, emptyList()),
        Spec(PROFILE, listOf(EXTRA_ARG)),
        Spec(PRESET, listOf(EXTRA_ARG)),
        Spec(SLOT, listOf(EXTRA_SLOT, EXTRA_PKG)),
        Spec(SLOT_CLEAR, listOf(EXTRA_SLOT)),
        Spec(OPEN, listOf(EXTRA_PKG)),
        Spec(PREFS, listOf(EXTRA_FILE)),
        Spec(REAPPLY, emptyList()),
        // Android box B2 · W2c — `diag` (chụp `ClusterDiag` của chiếu cụm BYD) gỡ cùng mã chiếu cụm.
        // Android box B2 · W1 — `ctl` · `hal` · `sweep` · `featmap` (nút/HAL BYDAuto) gỡ khỏi bảng lệnh.
        Spec(VOICE_DUMP, emptyList(), listOf(EXTRA_AUTO_CONFIRM)),
        // `text` là **tuỳ chọn** có chủ ý: vắng ⇒ giá trị rỗng ⇒ *"trả khoá về mặc định"* (tập rỗng / tắt), đúng
        // thứ `trap` của harness cần để dọn sau mỗi ca mà không phải biết mặc định của từng khoá.
        Spec(PREFS_SET, listOf(EXTRA_KEY), listOf(EXTRA_TEXT)),
        // Android box B2 · W1 — `captest` (kiểm từng nút xe) gỡ khỏi bảng lệnh.
        // 2.83 — chỉ đọc, `n` (số dòng) tuỳ chọn và được kẹp trong [parse] qua [a11yLogTail].
        Spec(A11YLOG, emptyList(), listOf(EXTRA_SLOT)),
        // Android box B2 · W1 — `ctllog` (nhật ký lệnh ghi xe) gỡ khỏi bảng lệnh.
        Spec(WAKELOG, emptyList(), listOf(EXTRA_SLOT)),   // FIX286 · VK6 — chỉ đọc, `n` tuỳ chọn, kẹp qua [wakeLogTail]
        // Android box B2 · W1/W2c: `diag_screen` (hai màn chẩn đoán BYD) rời bảng ở W1, tệp `TestBridgeScreenCommands` gỡ ở W2c.
    ) + TestBridgeTeachCommands.SPECS

    /** Tên mọi lệnh — cho tài liệu và cho bài canh "mã lệnh không trùng nhau". */
    val NAMES: List<String> = SPECS.map { it.name }

    private fun specOf(name: String): Spec? = SPECS.firstOrNull { it.name == name }

    /**
     * Phân tích một tập extra thành lệnh.
     *
     * @param extras giá trị đã được tầng Android gỡ khỏi `Intent` — chỉ ba kiểu: `String` · `Int` · `Boolean`.
     *   Nhận `Map` chứ không nhận `Intent` chính là chỗ khiến hàm này kiểm được off-device.
     * @param prefsFiles tên tệp prefs được phép ĐỌC. Truyền vào chứ không viết cứng: danh sách thật nằm ở
     *   [com.kachi.box.launcher.SettingsCatalog] và nó còn dài ra; chép lại ở đây là bản sao thứ hai.
     */
    fun parse(extras: Map<String, Any?>, prefsFiles: Set<String>): TestBridgeParse {
        val name = (extras[EXTRA_CMD] as? String)?.trim().orEmpty()
        if (name.isEmpty()) return TestBridgeParse.Err(ERR_NO_CMD)
        val spec = specOf(name) ?: return TestBridgeParse.Err(ERR_UNKNOWN_CMD)

        spec.required.forEach { key ->
            val v = extras[key]
            val missing = when (key) {
                EXTRA_SLOT -> v !is Int
                else -> (v as? String)?.isNotBlank() != true
            }
            if (missing) return TestBridgeParse.Err(ERR_MISSING + key)
        }

        val slot = extras[EXTRA_SLOT] as? Int ?: 0
        // Trần TRÊN không kiểm ở đây có chủ ý: chỉ tầng thi hành biết bố cục đang dùng có mấy ô (bố cục tự vẽ đổi
        // được giữa hai lệnh). Cùng phân công với `VoiceDispatcher.runOpenApp`.
        if (EXTRA_SLOT in spec.required && slot < 1) return TestBridgeParse.Err(ERR_BAD_SLOT)

        val file = (extras[EXTRA_FILE] as? String)?.trim().orEmpty()
        if (EXTRA_FILE in spec.required && file !in prefsFiles) return TestBridgeParse.Err(ERR_BAD_PREFS_FILE)

        // Danh sách trắng kiểm ở TẦNG PHÂN TÍCH, không ở tầng thi hành: một khoá lạ không bao giờ được dựng
        // thành một lệnh "chạy được" rồi mới bị từ chối — xem KDoc [PREFS_SET] ràng buộc (1).
        val key = (extras[EXTRA_KEY] as? String)?.trim().orEmpty()
        if (name == PREFS_SET && key !in WRITABLE_PREFS_KEYS) return TestBridgeParse.Err(ERR_BAD_PREFS_KEY + key)

        val op = (extras[EXTRA_OP] as? String).orEmpty().trim().lowercase()
        // Op lạ bị chặn ở TẦNG PHÂN TÍCH (cùng luật danh sách trắng của `prefs_set`). Android box B2 · W1: nhánh `captest`
        // (op mặc định `list`, `id` bắt buộc với op đóng dấu) và cổng tên màn của `diag_screen` gỡ cùng hai lệnh ấy.
        if (!TestBridgeTeachCommands.validOp(name, op)) return TestBridgeParse.Err(ERR_BAD_OP + op)

        return TestBridgeParse.Ok(
            TestBridgeCommand(
                name = name,
                text = (extras[EXTRA_TEXT] as? String).orEmpty(),
                path = (extras[EXTRA_PATH] as? String).orEmpty().trim(),
                pkg = (extras[EXTRA_PKG] as? String).orEmpty().trim(),
                arg = (extras[EXTRA_ARG] as? String).orEmpty().trim(),
                file = file,
                slot = slot,
                autoConfirm = extras[EXTRA_AUTO_CONFIRM] as? Boolean ?: false,
                op = op,
                key = key,
                tail = when (name) {
                    A11YLOG -> a11yLogTail(slot)
                    WAKELOG -> wakeLogTail(slot)
                    else -> 0
                },
            ),
        )
    }
}
