package com.byd.clusternav.voicekey

/**
 * Một **nút tự học** (Cài đặt › Phím vô-lăng › Học phím mới): tên người dùng đặt + mã phím.
 *
 * [name] là chữ của NGƯỜI DÙNG (kèm hậu tố mã do tầng Settings ghép từ tài nguyên lúc lưu) — không dịch.
 * Android box B2 · W2f (2026-10-09): trường nguồn (2.88, núm bệ giữa / vô-lăng BYD) gỡ cùng HAL BYD — nút đã lưu có nguồn
 * đọc lên thành nút thường (`VoiceKeyCustomButtonStore`).
 */
data class VoiceKeyCustomButton(val name: String, val keyCode: Int)

/**
 * LUẬT THUẦN của danh sách nút tự học (`:app` lo JSON — `VoiceKeyCustomButtonStore`). Khoá của một nút là **mã phím**:
 * học lại cùng mã ⇒ thay tên.
 */
object VoiceKeyCustomButtons {

    /** Thêm nút, thay nút cùng mã nếu có. Nút mới/được thay xuống CUỐI danh sách (hành vi trước 2.88). */
    fun put(current: List<VoiceKeyCustomButton>, button: VoiceKeyCustomButton): List<VoiceKeyCustomButton> =
        current.filterNot { it.keyCode == button.keyCode } + button

    /** Xoá nút của [keyCode]. */
    fun remove(current: List<VoiceKeyCustomButton>, keyCode: Int): List<VoiceKeyCustomButton> =
        current.filterNot { it.keyCode == keyCode }
}
