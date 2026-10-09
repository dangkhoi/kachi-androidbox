package com.byd.clusternav.launcher.testbridge

/**
 * ═══ T-BRIDGE · DANH SÁCH TRẮNG khoá prefs mà `prefs_set` được phép GHI ═══════════════════════════════════════
 *
 * Tách khỏi [TestBridgeCommands] ở 2.74 (R8-B) vì lý do cơ học: tệp đó đã **464 dòng** và mỗi lượt thêm một bộ núm
 * đo-trên-xe lại bồi thêm ~10 dòng danh sách + chú thích ⇒ trần 500 của CLAUDE.md §4.1 vỡ đúng ở lượt sau. Danh sách
 * là một **vai riêng** (*"khoá nào được ghi"*) khác hẳn vai của [TestBridgeCommands] (*"cú pháp một lệnh"*), nên
 * tách đúng đường khớp chứ không phải cắt bừa cho vừa trần.
 *
 * Ba ràng buộc của danh sách này nằm ở KDoc [TestBridgeCommands.PREFS_SET] — **đọc ở đó trước khi thêm một khoá**.
 * Nhắc lại đúng một điều quan trọng nhất: `KachiTestBridge` là receiver `exported=true`, nên đây là **cửa duy nhất**
 * vào `SharedPreferences` từ ngoài tiến trình, và mọi khoá trong đây phải đảo lại được bằng một cú chạm trong Cài đặt.
 */
object TestBridgeWritableKeys {

    /**
     * Toàn bộ khoá ghi được. [TestBridgeCommands.WRITABLE_PREFS_KEYS] trỏ thẳng vào đây (một tên, một chỗ khai).
     *
     * Bốn khoá đầu là khoá THEO XE của đường giọng nói (`PrefsVoiceV3.kt` + `Prefs.voiceAskAloud`). Android box B2 · W1:
     * `top_strip_labels` (nhãn chip xe) và mọi khoá `camera_*` đã rời danh sách.
     *
     * ## H5 (2026-09-16) — **bốn núm chỉnh bộ nghe**, tất cả đều mặc định = hằng đang chạy
     * `voice_endpoint_silence_ms` · `voice_endpoint_min_speech_ms` ([VoiceEndpointer]) và `voice_beam` ·
     * `voice_hotword_score` ([SherpaModelCatalog]). Chúng vào đây vì đúng câu hỏi chúng sinh ra để trả lời —
     * *"cabin 80 km/h thì 800 ms im là sớm hay muộn"*, *"beam 8 có nghe ra hơn không"* — chỉ đo được bằng cách
     * đổi giá trị **giữa hai lượt `wav`/`listen` trên xe**, tức bằng máy, không phải bằng một lượt build lại APK
     * cho mỗi con số. Mặc định của cả bốn **bằng đúng hằng hôm nay** ⇒ danh sách này dài ra mà hành vi không đổi
     * một ly; và cả bốn vẫn nằm trong đường GIỌNG NÓI, không chạm cast/cụm/phím (ràng buộc (2) của KDoc trên).
     */
    val ALL: Set<String> = setOf(
        "voice_confirm_ids",
        "voice_ask_aloud",
        "voice_follow_up_ms",
        "voice_mic_source",
        // Android box B2 · W1 — `top_strip_labels` (nhãn chip dữ liệu xe) ra khỏi danh sách cùng hàng Cài đặt của nó.
        "voice_endpoint_silence_ms",
        "voice_endpoint_min_speech_ms",
        "voice_endpoint_floor_cap",
        // Ba núm của Silero VAD — đường ngắt câu CHÍNH từ 1.69 (docs/diagnostics/voice-stream-eval-2026-09-16.md
        // §8). Cùng lý do với ba khoá trên: bộ tham số chốt bằng lưới trên host, còn cabin thật thì chỉ đo được
        // bằng cách đổi số **giữa hai lượt nói** trên xe.
        "voice_vad_threshold",
        "voice_vad_min_speech_ms",
        "voice_vad_min_silence_ms",
        "voice_beam",
        "voice_hotword_score",
        // Tốc độ đọc Piper (owner 2026-09-17 "nói nhanh quá") — chỉnh mức chậm đúng ý trên xe không cần build.
        "voice_tts_speed",
        // owner 2026-09-21 (bản release production) — công tắc GIỮ NHẬT KÝ lượt nói. Vào đây vì ô tích của nó vừa
        // bị gỡ khỏi Cài đặt cùng mọi bề mặt dev/log: không có dòng này thì khoá thành **bất khả chỉnh**, tức dọn
        // bề mặt hoá ra dọn luôn khả năng. Đây cũng là khoá DUY NHẤT của danh sách này không còn đường đảo lại
        // bằng một cú chạm trong Cài đặt (xem ràng buộc (3) ở KDoc trên) — nó vẫn nằm trong đường GIỌNG NÓI và vẫn
        // chỉ ghi được khi chế độ kiểm thử đang mở, nên hai ràng buộc còn lại không đổi.
        "voice_keep_log",
        // Android box B2 · W1 — toàn bộ khoá camera BYD (`camera_*`, cả bộ *Từng camera* `CameraCamConfig.NEW_KEYS`) RA KHỎI
        // danh sách trắng: lệnh camera đã rời cầu và hàng Cài đặt đảo lại được (ràng buộc 3) không còn ⇒ `prefs_set` phải trả
        // `bad_prefs_key` thay vì ghi một khoá không có cách nào đảo lại bằng tay.
    )
}
