package com.kachi.box.launcher.voice

import com.kachi.box.launcher.Lang
import com.kachi.box.launcher.LauncherActions
import com.kachi.box.launcher.Strings

/**
 * ═══ THÂN CÂU XEM-TRƯỚC của nút xe + nhạc — phần của [VoiceReply.preview], tách khỏi `VoiceReply.kt` ════════════
 *
 * Tách ở spec `kachi-i18n-zh-th-ms.html` T2 vì trần 500 dòng của mọi `Voice*.kt` (`VoiceListenWiringContractTest`):
 * mọi hàm dựng câu NÓI nay nhận `lang` tường minh (giao diện ZH/TH/MS ⇒ giọng nói tiếng Việt, xem `voiceLangOf`), và
 * `VoiceReply.kt` (498 dòng) không còn chỗ cho các tham số ấy. Tách theo VAI *"gọi tên một việc"* (động từ + nhãn +
 * giá trị); API công khai vẫn nằm ở [VoiceReply] nên không chỗ gọi nào đổi một ký tự.
 *
 * `internal` và [lang] **không có mặc định**: chỉ [VoiceReply] gọi tới, và tầng này không bao giờ tự đọc
 * [Strings.current] — một mặc định ở đây là đường để câu nói lặng lẽ lấy tiếng giao diện (chữ Hán cho giọng Việt).
 *
 * VI/EN y từng byte như trước khi tách: [lang] = [Strings.current] cho ra đúng `displayLabel`/`displayArgs`/`Strings.t`
 * cũ (`labelIn`/`argsIn` là chính phép đọc mà hai thuộc tính kia uỷ quyền).
 */
internal object VoiceReplyPreview {

    /** Câu xem-trước của một việc launcher: *"Mở …"* (Android box B2 · W2b gỡ các nhánh camera theo yêu cầu). */
    fun launcher(i: VoiceIntent.Launcher, lang: Lang): String =
        Strings.t("Mở ", "Open ", lang) + mid(VoiceReply.labelOf(i.id, lang))

    /**
     * 2.96 R12 — nhãn ĐỨNG SAU động từ: hạ chữ đầu (*"Đóng kính lái"*, không *"Đóng Kính lái"*). Chữ viết tắt giữ nguyên
     * ([VoiceFeedbackPhrase.decap] — *"EV / HEV"*); chữ Hán/Thái không có hoa/thường nên không đổi gì.
     */
    fun mid(label: String): String = VoiceFeedbackPhrase.decap(label)

    // Android box B2 · W3: câu xem-trước của nút xe (`control` · `select`) gỡ cùng `ControlRegistry`.

    /** Câu xem-trước của một lệnh nhạc. */
    fun media(i: VoiceIntent.Media, lang: Lang): String = when (i.op) {
        VoiceMediaOp.PLAY -> Strings.t("Phát nhạc", "Play", lang) + by(i.app, lang)
        VoiceMediaOp.PAUSE -> Strings.t("Dừng nhạc", "Pause", lang)
        // ⚠ [SOÁT chuỗi-lời-đáp 2026-09-17] Hai dòng này PHẢI là **cụm động từ**, không phải cụm danh từ.
        // `VoiceFeedbackPhrase.merge` dựng câu đọc bằng cách ghép *"Đã "*/*"Chưa "* + thân dòng — cụm danh từ
        // *"Bài tiếp theo"* vì thế ra *"Chưa bài tiếp theo, chưa có phiên nhạc nào"*, một câu không phải tiếng
        // Việt. Mọi vai khác (PLAY · PAUSE · QUERY) vốn đã là động từ; hai vai này là ngoại lệ duy nhất, và sửa
        // **tại nguồn** đúng hơn là dạy tầng đọc nhận diện cụm danh từ (CLAUDE.md §7).
        VoiceMediaOp.NEXT -> Strings.t("Chuyển bài tiếp theo", "Skip to the next track", lang)
        VoiceMediaOp.PREV -> Strings.t("Quay lại bài trước", "Go back to the previous track", lang)
        // V1.1 — đọc lại tên bài trong ngoặc kép nhọn. Phần này do nhận dạng **tự do** đọc ra (R16), tức chỗ dễ
        // sai nhất trong cả câu; để nó lẫn vào câu trơn thì người nghe không biết máy đang hỏi về đoạn nào.
        VoiceMediaOp.QUERY -> Strings.t("Tìm bài ", "Search ", lang) + "«" + i.query + "»" + by(i.app, lang)
    }

    /** Đuôi *"bằng &lt;app&gt;"* — rỗng khi câu không nêu app. */
    fun by(appKey: String?, lang: Lang): String =
        appKey?.let { Strings.t(" trên ", " on ", lang) + VoiceAppTargets.labelOf(it) } ?: ""

    /** Đuôi *"vào ô N"* — rỗng khi câu không nêu ô. Số giữ **đúng như người ta nói** (1-based). */
    fun inSlot(slot: Int?, lang: Lang): String =
        slot?.let { Strings.fIn(lang, " vào ô {0}", " in slot {0}", it) } ?: ""
}
