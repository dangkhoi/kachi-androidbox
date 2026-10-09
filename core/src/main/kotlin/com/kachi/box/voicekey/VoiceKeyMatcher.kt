package com.kachi.box.voicekey

/**
 * LOGIC THUẦN (không Android): một sự kiện phím vật lý có (a) kích hoạt mở app đích không, (b) mở đích NÀO,
 * và (c) có "nuốt" (consume) sự kiện đó không.
 *
 * Rework 1.19 (owner 2026-08-14): **BỎ khái niệm cử chỉ (Nhấn/Nhấn-giữ) + mốc thời-gian-giữ.** Trên xe này
 * nút phát KEYCODE KHÁC NHAU cho nhấn-ngắn vs nhấn-giữ, nên phân biệt bằng thời gian giữ là thừa và gây lỗi
 * (giữ > 500ms từng làm PRESS từ chối bắn → phím lọt → hệ thống mở nhầm trợ lý Gemini). Giờ đơn giản:
 * đúng keycode đã cấu hình → **BẮN 1 lần** (trên DOWN đầu của mỗi lần nhấn) + **NUỐT trọn** phím đó
 * (DOWN/UP/repeat); keycode khác hoặc tính năng tắt → **pass-through hoàn toàn** (giữ chức năng gốc).
 *
 * F3 (owner 2026-08-24): đổi từ "so với MỘT mã" sang **tra DANH SÁCH gán** (nhiều phím → nhiều app).
 * Máy trạng thái chống-bắn-lặp GIỮ NGUYÊN (`CLAUDE.md §6` — không đảo đường đang chạy tốt ngoài hiện
 * trường), chỉ khoá theo **(keyCode, downTime)** thay vì downTime đơn lẻ: hai phím đã gán được bấm gần
 * nhau có thể trùng `downTime` ở độ phân giải ms, mà khoá chung một ô thì phím thứ hai bị nuốt mất lần bắn.
 *
 * Matcher NHỚ QUYẾT ĐỊNH của từng lần nhấn (mã phím → downTime, nuốt?):
 *  - DOWN đầu của mã có dòng gán ⇒ bắn + nuốt; mã không gán ⇒ không nhớ gì, để phím đi tiếp;
 *  - DOWN lặp / UP / OTHER của cùng lần nhấn DÙNG LẠI quyết định của DOWN đầu;
 *  - UP/OTHER không thấy DOWN nào ⇒ nuốt khi và chỉ khi mã có dòng gán.
 *
 * Android box B2 · W2f (2026-10-09): tra NGUỒN (2.88 KEY-SOURCE-SPLIT — núm bệ giữa / vô-lăng BYD, đọc HAL đồng bộ trên DOWN
 * đầu) gỡ cùng HAL BYD; quyết định nay y đường 2.87 cho mọi mã.
 *
 * CÓ TRẠNG THÁI (mỗi lần nhấn chỉ bắn 1 lần). Gọi tuần tự từ 1 luồng (onKeyEvent main).
 */
enum class VoiceKeyAction { DOWN, UP, OTHER }

/**
 * Cấu hình tối thiểu: bật/tắt + **danh sách gán** (mã phím → đích).
 *
 * Ý nghĩa của chuỗi đích (package name hay sentinel) do tầng app quyết định, KHÔNG ở `:core` — ở đây nó chỉ
 * là dữ liệu đi kèm được chuyển tiếp nguyên vẹn ra [VoiceKeyDecision.targetSpec].
 */
data class VoiceKeyConfig(val enabled: Boolean, val bindings: List<VoiceKeyBinding>) {
    /** Tra đích — tất định vì `VoiceKeyBindings` cưỡng chế mỗi mã phím chỉ gán MỘT đích. */
    fun targetFor(keyCode: Int): String? = VoiceKeyBindings.targetFor(bindings, keyCode)
}

/**
 * @property fire true → tầng app phóng intent mở [targetSpec].
 * @property consume true → [android.accessibilityservice.AccessibilityService.onKeyEvent] trả true (chặn hệ
 *   thống xử lý phím). Chỉ true cho phím CÓ dòng gán khớp.
 * @property targetSpec đích của phím vừa bắn. **Bất biến: `fire` ⟺ `targetSpec != null`** — tầng app không
 *   phải tra lại danh sách (tra hai lần = hai kết quả nếu prefs đổi giữa chừng).
 */
data class VoiceKeyDecision(
    val fire: Boolean,
    val consume: Boolean,
    val targetSpec: String? = null,
) {
    companion object { val IGNORE = VoiceKeyDecision(fire = false, consume = false) }
}

class VoiceKeyMatcher {

    /** Quyết định của MỘT lần nhấn đang dở (DOWN đầu đã xử lý, chưa thấy UP). */
    private class Press(val downTime: Long, val consume: Boolean) {
        /** DOWN lặp / UP / OTHER của cùng lần nhấn: không bắn, nuốt y như DOWN đầu. */
        fun again() = VoiceKeyDecision(fire = false, consume = consume)
    }

    // (keyCode → quyết định của lần nhấn đang dở) — chống bắn lặp. Kích thước bị chặn bởi số mã phím ĐÃ GÁN từng được
    // bấm (mã không có dòng gán nào không bao giờ được nhớ).
    private val presses = HashMap<Int, Press>()

    fun onKey(cfg: VoiceKeyConfig, action: VoiceKeyAction, keyCode: Int, downTimeMs: Long): VoiceKeyDecision {
        if (!cfg.enabled) return VoiceKeyDecision.IGNORE
        val press = presses[keyCode]
        return when (action) {
            // Bắn 1 lần cho mỗi lần nhấn (theo downTime). DOWN có downTime khác ⇒ lần nhấn MỚI (UP cũ lạc mất) ⇒ quyết lại.
            VoiceKeyAction.DOWN ->
                if (press != null && press.downTime == downTimeMs) press.again() else begin(cfg, keyCode, downTimeMs)
            // #10 (deep-pass 2026-09-23): UP kết thúc lần nhấn ⇒ XOÁ entry. Chống-lặp dựa trên chu kỳ DOWN→UP,
            // KHÔNG chỉ dựa 'downTime khác' — nếu ROM TÁI DÙNG cùng downTimeMs cho lần nhấn mới thì (không xoá)
            // fire=false MÃI ⇒ phím chết trong khi service VẪN bound (status ACTIVE, watchdog không chữa được vì
            // không phải lỗi bind). Xoá ở UP: DOWN của lần nhấn kế luôn khác entry (đã trống) ⇒ fire lại đúng 1 lần.
            VoiceKeyAction.UP -> { presses.remove(keyCode); press?.again() ?: orphan(cfg, keyCode) }
            VoiceKeyAction.OTHER -> press?.again() ?: orphan(cfg, keyCode)
        }
    }

    /** DOWN đầu của một lần nhấn: chọn dòng → nhớ quyết định cho phần còn lại của lần nhấn. */
    private fun begin(cfg: VoiceKeyConfig, keyCode: Int, downTimeMs: Long): VoiceKeyDecision {
        val target = cfg.targetFor(keyCode)
        if (target == null) {
            // Mã không gán: không nhớ gì, để phím đi tiếp.
            presses.remove(keyCode)
            return VoiceKeyDecision.IGNORE
        }
        presses[keyCode] = Press(downTimeMs, consume = true)
        return VoiceKeyDecision(fire = true, consume = true, targetSpec = target)
    }

    /** UP/OTHER không thấy DOWN (vd service vừa nối lại): nuốt ⟺ mã có dòng gán. */
    private fun orphan(cfg: VoiceKeyConfig, keyCode: Int): VoiceKeyDecision =
        if (cfg.targetFor(keyCode) != null) VoiceKeyDecision(fire = false, consume = true) else VoiceKeyDecision.IGNORE

    /** Reset trạng thái (gọi khi service (re)connect) để một lần nhấn dở dang không dính sang phiên mới. */
    fun reset() { presses.clear() }
}
