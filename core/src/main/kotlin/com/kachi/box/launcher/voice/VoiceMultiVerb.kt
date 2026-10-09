package com.kachi.box.launcher.voice

import com.kachi.box.launcher.voice.VoiceLexicon.Token

/**
 * Tách câu MIX không liên từ theo ranh giới động từ hành động (*"mở youtube mở bản đồ"*).
 *
 * Android box B2 · W3 (2026-10-09): tách khỏi `VoiceControlParse` (diễn giải đối số nút xe — đã gỡ cùng
 * `ControlRegistry`); phép tách này không biết gì về xe nên giữ nguyên byte.
 */
internal object VoiceMultiVerb {

    /**
     * Từ nối AN TOÀN có thể dính đuôi một vế MIX — cắt trước khi parse. KHÔNG gồm "sau"/"do" đơn (là "kính sau").
     * 2.93 VOICE-ROI-CONNECTOR: khai CÓ DẤU — chữ mang dấu *"rơi"* (tên bài *"Lá rơi"*) không bị cắt như *"rồi"* ([VoiceHomograph]).
     */
    private val SEGMENT_TAIL_JOINERS = VoiceHomograph.Words("xong", "rồi", "và", "then")

    /**
     * Tách câu MIX không liên từ ở ranh giới ĐỘNG TỪ HÀNH ĐỘNG ("hạ kính lấy gió ngoài tắt máy lạnh" = 3 lệnh).
     * Trả `null` nếu < 2 động từ. Mỗi vế = [start_i, start_{i+1}), cắt từ nối AN TOÀN + cặp "sau đó" ở đuôi.
     *
     * ⚠ An toàn nằm ở CHỖ GỌI ([VoiceIntentParser]): chỉ nhận kết quả khi MỌI vế parse ra ý định hiểu được — nên
     * câu có động từ nằm giữa tên đối tượng ("mở bài Cỏ dại và hoa dành dành") không bị cắt bừa.
     */
    fun multiVerbSplit(t: List<Token>): List<List<Token>>? {
        val starts = t.indices.filter { i -> VoiceGrammar.actionVerbAt(t, i) }
        if (starts.size < 2) return null
        val out = ArrayList<List<Token>>()
        for (k in starts.indices) {
            val from = starts[k]
            val to = starts.getOrNull(k + 1) ?: t.size
            var end = to
            if (end - from >= 2 && t[end - 2].norm == "sau" && t[end - 1].norm == "do") end -= 2
            while (end > from && SEGMENT_TAIL_JOINERS.matches(t[end - 1])) end--
            if (end > from) out.add(t.subList(from, end))
        }
        return out.takeIf { it.size >= 2 }
    }
}
