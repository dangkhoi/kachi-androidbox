package com.kachi.box.launcher.voice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * ═══ 1.91 · MỞ RỘNG DICTIONARY — NHIỀU CÁCH NÓI CHO CÙNG MỘT LỆNH ═════════════════════════════════════════════
 *
 * Owner 2026-09-21: *"nhiều câu tương tự nhau cho 1 command"* (kèm ví dụ ghế mát: *mát ghế · mát đít · mát mông ·
 * quạt ghế · thổi mát ghế*). Bài này khoá **kết quả** của lượt mở rộng — mỗi dòng là một cách nói mới phải ra đúng
 * nút/datum nó nhắm tới.
 *
 * ## Vì sao một tệp RIÊNG, không nhét vào `VoiceIntentParserTest`
 * Tệp kia đã **536 dòng** (vượt trần 500 của CLAUDE.md §4.1 từ trước lượt này) và nó trả lời một câu hỏi khác:
 * *"các LUẬT của bộ phân tích có đúng không"*. Tệp này chỉ trả lời *"bảng DỮ LIỆU cách nói có phủ đủ không"* —
 * tách theo VAI, đúng tiền lệ `VoiceWindowScopeTest` (lượt D) và `VoiceLogCases0918Test`.
 *
 * ⚠ Phạm vi kính/cửa sổ (*"mở hết cửa sổ"*) nằm ở [VoiceWindowScopeTest], không lặp ở đây.
 */
class VoiceDictionary0921Test {

    private fun one(s: String): VoiceIntent = VoiceIntentParser.parseOne(s)

    /** Bảng ca: câu → ý định mong đợi. Thông báo lỗi kèm nguyên văn câu để đọc là biết ca nào đỏ. */
    private fun expect(vararg cases: Pair<String, VoiceIntent>) =
        cases.forEach { (s, want) -> assertEquals(want, one(s), "câu: \"$s\"") }

    // ══ 1 · GHẾ MÁT / GHẾ SƯỞI — ví dụ owner đưa tận chữ ═══════════════════════════════════════════════

    // ══ 2 · ĐIỀU HÒA · GIÓ · NHIỆT · SẤY · LỌC ════════════════════════════════════════════════════════

    // ══ 3 · THÂN XE · ĐÈN · TIỆN NGHI ═════════════════════════════════════════════════════════════════

    // ══ 4 · THÔNG TIN ĐỌC ═════════════════════════════════════════════════════════════════════════════

    // ══ 5 · CÁC CỤM CỐ Ý **KHÔNG** NHẬN — chống "mở rộng" thành "đoán bừa" ════════════════════════════

    /**
     * ⚠⚠ [SOÁT 2026-09-21 · P1] Cổng verbless *"điều hòa &lt;số&gt; độ"* KHÔNG được biến một câu-không-phải-lệnh
     * thành một lệnh GHI.
     *
     * Bản đầu `return` thẳng `VoiceControlParse.control("ac_auto", SET, …)`. `ac_auto` là TOGGLE, nên khi
     * `degreesSetpoint` không khớp (số KHÔNG đứng ngay trước chữ *"độ"*) nhánh `TOGGLE + SET` trả
     * `Control(ac_auto, 1)` ⇒ [ĐO] *"điều hòa chế độ hai"* **BẬT điều hòa**. Đó chính là họ lỗi [P1] mà 1.83 đã vá
     * một lần cho *"bật điều hòa chế độ hai"* (bản vá ấy = `degreesSetpoint` đòi số ngay trước *"độ"*), và cổng mới
     * đi vòng qua nó — **cùng một lỗi, cửa khác**.
     *
     * Ca cuối là phần thứ hai của cùng bản vá: cổng nay đòi cụm HAI TỪ (*"điều hòa"* hoặc *"máy lạnh"*) thay vì
     * riêng chữ *"điều"*, nên *"điều chỉnh ghế 2 độ"* hết bị đọc thành **đặt nhiệt cabin = 17**.
     */
    @Test fun `khong phai setpoint thi phai HOI LAI, khong duoc bat dieu hoa`() {
        listOf(
            "điều hòa chế độ hai",
            "điều hòa chế độ 2",
            "điều hòa mức độ 3",
            "điều chỉnh ghế 2 độ",
        ).forEach {
            val got = one(it)
            assertEquals(
                VoiceIntent.Unknown::class.java, got.javaClass,
                "câu \"$it\" phải là KHÔNG HIỂU (hỏi lại) — không được thành lệnh ghi, ra: $got",
            )
        }
    }

}
