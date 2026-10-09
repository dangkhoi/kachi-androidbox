package com.byd.clusternav.launcher.voice

import com.byd.clusternav.launcher.voice.VoiceLexicon.Token

/**
 * ═══ 2.93 — ĐỘNG TỪ ĐẦU VẾ ĐỌC THEO DẤU: chữ CÓ dấu chỉ là động từ khi đúng một cách viết của động từ ═══════════════════
 *
 * Thuần Kotlin (`:core`) ⇒ kiểm off-car. Cùng luật [VoiceHomograph] của liên từ, áp cho [VoiceGrammar.VERBS] (khai không dấu):
 * bỏ dấu thì *"tất"* (tất cả) = `tat` = *"tắt"* ⇒ *"tất cả kính"* từng được đọc như một câu CÓ động từ tắt. Token **mang dấu**
 * chỉ là động từ khi chữ thô (chữ thường, NFC) đúng một cách viết có dấu của [SherpaSpokenWords.VERBS]; token **không dấu** (gõ,
 * nhật ký cũ) ⇒ không có dữ liệu để phân biệt ⇒ coi là động từ (hành vi cũ). Cụm của [VoiceGrammar.VERBS] chưa khai cách viết có
 * dấu nào (*"dẫn tới"* · *"đưa tôi đến"* · *"coi thử"* — bài canh của [SherpaSpokenWords] chỉ ép chiều *"mọi cách viết là một cụm
 * đã khai"*, không ép chiều ngược) cũng là *"không dữ liệu"* ⇒ hành vi cũ (senior review wave 2, [P3]).
 *
 * Chỗ hỏi phép này: động từ mang sang lượt trả lời của câu hỏi lại ([VoiceClarify] `leadVerb`). (≤ 2.98 BYD còn cổng tên bộ phận
 * xe trần và mạch mượn động từ của câu ghép — gỡ cùng nút xe ở Android box B2 · W3.)
 */
internal object VoiceVerbSpelling {

    /** Khoá bỏ dấu của một cụm động từ (cùng phép tách của bộ phân tích) → các cách viết có dấu đã khai cho nó. */
    private val SPELLINGS: Map<String, Set<String>> = SherpaSpokenWords.VERBS.values.flatten()
        .groupBy({ v -> VoiceLexicon.tokenize(v).joinToString(" ") { it.norm } }, { VoiceHomograph.spelling(it) })
        .mapValues { (_, forms) -> forms.toSet() }

    /** Số từ của cụm [VoiceGrammar.VERBS] khớp ở đầu [t] (dài trước), `0` = không có — CHƯA xét dấu. */
    fun verbWords(t: List<Token>): Int =
        VoiceGrammar.VERBS.firstOrNull { VoiceLexicon.phraseAt(t, 0, it.first) }?.first?.size ?: 0

    /**
     * [n] từ đầu của [t] — một cụm [VoiceGrammar.VERBS] ([verbWords]) — có là một động từ THẬT không (xem KDoc lớp). `n ≤ 0` ⇒
     * `false`.
     */
    fun isVerb(t: List<Token>, n: Int): Boolean {
        if (n <= 0 || n > t.size) return false
        val head = t.subList(0, n)
        val norm = head.joinToString(" ") { it.norm }
        val spelled = head.joinToString(" ") { VoiceHomograph.spelling(it.raw) }
        val forms = SPELLINGS[norm] ?: return true   // cụm chưa khai cách viết có dấu ⇒ không dữ liệu ⇒ như cũ
        return spelled == norm || spelled in forms
    }
    // Android box B2 · W3: `actionSpan` (động từ mượn cho vế tên nút xe trần) gỡ cùng `VoiceClauseEllipsis`/`VoiceBareCover`.
}
