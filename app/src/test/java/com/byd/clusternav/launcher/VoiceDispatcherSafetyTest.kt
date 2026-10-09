package com.byd.clusternav.launcher

import com.byd.clusternav.launcher.voice.VoiceIntent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.byd.clusternav.launcher.voice.VoiceRiskTable

/**
 * ═══ V1 · CỔNG XÁC NHẬN CỦA CÂU GHÉP — BÀI CHẠY THẬT, KHÔNG PHẢI QUÉT NGUỒN ════════════════════════════════════
 *
 * Spec `docs/specs/kachi-voice-command.html` R3 · R4 · §7 OQ4 (soát 2026-09-14 chốt theo hướng an toàn).
 *
 * ## Vì sao bài này dựng [VoiceDispatcher] THẬT trong khi các bài `:app` khác chỉ quét chuỗi nguồn
 * Thứ cần khoá ở đây là **thứ tự thời gian** (*"vế sau có chạy trước khi vế trước được đồng ý không"*), mà thứ tự
 * thì không đọc ra được từ một chuỗi ký tự — `SettingsScreenWiringContractTest` quét nguồn vì nó nói về **dây
 * nối**, còn đây nói về **hành vi**. Dựng được thật vì lớp này không chạm `android.*` trên đường đi của một việc
 * launcher/hồ sơ: các lambda do bài tự cấp.
 *
 * [ĐO] bệnh nó khoá: bản đầu bắn **mọi** vế ngay lập tức và chỉ *hỏi thêm* cho vế CONFIRM ⇒ câu *"mở khoá cửa rồi
 * mở hết kính"* hạ hết kính **trong lúc** hộp hỏi của vế mở khoá còn đang mở.
 */
class VoiceDispatcherSafetyTest {

    /**
     * Việc đã bắn — Android box B2 · W3: cổng xe giả (`CarControlPort`) gỡ cùng nút xe; ghi lại việc launcher / hồ sơ
     * mà dispatcher THẬT gọi tới (cùng mục đích: chứng minh thứ tự thời gian của cổng xác nhận).
     */
    private class Fired { val log = ArrayList<String>() }

    /** Hộp hỏi lại giả: **không** trả lời ngay — bài tự quyết định lúc nào bấm Đồng ý / Huỷ. */
    private class Ask {
        val asked = ArrayList<String>()
        private val yes = ArrayList<() -> Unit>()
        private val no = ArrayList<() -> Unit>()
        fun onConfirm(q: String, y: () -> Unit, n: () -> Unit) { asked += q; yes += y; no += n }
        fun agreeLast() = yes.removeAt(yes.size - 1).invoke()
        fun cancelLast() = no.removeAt(no.size - 1).invoke()
    }

    private class Rig {
        val port = Fired()
        val ask = Ask()
        val said = ArrayList<String>()
        val profiles = ArrayList<String>()

        /** Số lần câu lệnh yêu cầu mở một phiên NGHE (V1 pha nghe · `launcher_voice`). */
        var listens = 0
        val dispatcher = VoiceDispatcher(
            state = { HomeUiState(profiles = listOf("Mặc định", "Vợ")) },
            media = { error("bài này không chạm tới nhạc") },
            appsByLabel = { emptyMap() },
            openApp = { false },
            openAppList = { port.log += "apps" },
            openSettings = { port.log += "settings" },
            onSwitchProfile = { profiles += it; port.log += "profile:$it" },
            onListen = { listens++ },
            confirm = { q, y, n -> ask.onConfirm(q, y, n) },
            // ⚠ V3 · R7 (1.66): mặc định **không hỏi gì cả** (owner 2026-09-16). Bài này canh CƠ CHẾ của
            // cổng hỏi-lại, nên nó bật MỌI mã hỏi-được — không thì mọi ca dưới đây chạy thẳng và bài
            // trở thành một bài canh cho chính cái mặc định, không phải cho cổng.
            confirmIds = { VoiceRiskTable.askableIds().toSet() },
            say = { said += it },
            // V1.1 — bốn cổng mới; bài này không chạm tới chúng, nên chúng **nổ** nếu bị chạm. Một lambda trả
            // giá trị giả sẽ làm bài xanh trong khi một ý định đi nhầm đường.
            assignAppToSlot = { _, _ -> error("bài này không gắn app vào ô") },
            sendToApp = { error("bài này không giao việc cho app đích") },
            geocode = { error("bài này không tra toạ độ") },
            mediaPackage = { null },
            // Gói lệnh chạy NGAY trên thread gọi — bài cần kết quả tất định, không cần đo tính đa luồng.
            background = { it() },
        )
    }

    // ══ 1 · Vế CONFIRM DỪNG cả chuỗi ══════════════════════════════════════════════════════════════════════
    // Android box B2 · W3: câu mẫu cũ (mở cốp · bật đèn đọc) là lệnh xe đã gỡ ⇒ vế CONFIRM nay là ĐỔI HỒ SƠ, vế thường
    // là MỞ CÀI ĐẶT / ỨNG DỤNG. Cơ chế canh giữ nguyên: vế sau KHÔNG chạy trước khi vế CONFIRM được đồng ý.

    @Test
    fun `ve sau KHONG chay truoc khi ve CONFIRM duoc dong y`() {
        val r = Rig()
        r.dispatcher.submit("đổi sang hồ sơ Vợ và mở cài đặt")
        assertEquals(1, r.ask.asked.size, "phải hỏi đúng một lần, cho vế đổi hồ sơ")
        assertEquals(emptyList<String>(), r.port.log, "CHƯA đồng ý mà đã chạy là cổng vô nghĩa")
        r.ask.agreeLast()
        assertEquals(listOf("profile:Vợ", "settings"), r.port.log, "đồng ý ⇒ chạy đúng thứ tự nói")
    }

    @Test
    fun `huy thi ca chuoi dung, va noi ro con may viec khong chay`() {
        val r = Rig()
        r.dispatcher.submit("đổi sang hồ sơ Vợ và mở cài đặt")
        r.ask.cancelLast()
        assertEquals(emptyList<String>(), r.port.log, "huỷ mà vẫn bắn là mất trắng cổng an toàn")
        assertTrue(r.said.any { it.contains("1 việc sau không chạy") }, "phải nói rõ còn việc không chạy: ${r.said}")
    }

    @Test
    fun `hai ve CONFIRM hoi lan luot, khong chong hop`() {
        val r = Rig()
        r.dispatcher.submit("đổi sang hồ sơ Vợ rồi đổi sang hồ sơ Mặc định")
        assertEquals(1, r.ask.asked.size, "hai hộp hỏi chồng nhau thì người lái không biết đang trả lời cho vế nào")
        r.ask.agreeLast()
        assertEquals(2, r.ask.asked.size, "đồng ý vế một mới hỏi vế hai")
        r.ask.agreeLast()
        assertEquals(listOf("Vợ", "Mặc định"), r.profiles)
    }

    @Test
    fun `viec khong nguy hiem chay thang, khong hoi lai`() {
        val r = Rig()
        r.dispatcher.submit("mở cài đặt và mở ứng dụng")
        assertEquals(emptyList<String>(), r.ask.asked, "đừng hỏi lại những việc nói ngược lại là xong")
        assertEquals(listOf("settings", "apps"), r.port.log)
    }

    // ══ 3 · Không có đường nào bắn mà bỏ qua cổng ═════════════════════════════════════════════════════════

    /**
     * [VoiceDispatcher.preview] là cửa *"đã hiểu là…"* của màn thử — nó **chỉ được phân tích**. Một ngày nào đó ai
     * đó cho nó chạy luôn cho tiện thì mọi câu nguy hiểm sẽ bắn **trước cả khi** người dùng bấm "Chạy câu lệnh".
     */
    @Test
    fun `preview khong thi hanh bat cu thu gi`() {
        val r = Rig()
        val intents = r.dispatcher.preview("đổi sang hồ sơ Vợ và mở cài đặt")
        assertEquals(2, intents.size)
        assertTrue(intents.first() is VoiceIntent.Profile)
        assertEquals(emptyList<String>(), r.port.log)
        assertEquals(emptyList<String>(), r.ask.asked)
        assertEquals(emptyList<String>(), r.said)
    }

    /** Android box B2 · W3 — câu xe ra "đã gỡ": dispatcher nói ra, KHÔNG gọi việc nào, KHÔNG hỏi gì. */
    @Test
    fun `cau xe noi da go va khong chay gi`() {
        val r = Rig()
        r.dispatcher.submit("mở kính")
        assertEquals(emptyList<String>(), r.port.log)
        assertEquals(emptyList<String>(), r.ask.asked)
        assertTrue(r.said.any { it.contains("điều khiển xe") }, "phải nói lý do: ${r.said}")
    }

    // ══ 4 · V1 pha NGHE — `launcher_voice` là một việc THẬT, không phải một mã trơ ═════════════════════════

    /**
     * Nói *"nói với xe"* phải mở một phiên nghe.
     *
     * Bài này chạy [VoiceDispatcher] **thật** (lớp này không chạm `android.*` trên đường đi của một hành động
     * launcher), nên nó canh đúng thứ mà một phép quét nguồn không canh được: mã `launcher_voice` đi tới đúng
     * lambda, đúng **một** lần, và không rơi vào nhánh `else -> failed` như một mã lạ.
     */
    @Test
    fun `noi voi xe mo dung mot phien nghe`() {
        val r = Rig()
        r.dispatcher.submit("nói với xe")
        assertEquals(1, r.listens, "câu `nói với xe` phải mở đúng một phiên nghe")
        assertTrue(r.said.isNotEmpty(), "phải nói lại là đã làm gì")
    }

    /** Và nó nối được vào câu ghép như mọi việc khác — thứ tự nói là thứ tự làm. */
    @Test
    fun `noi voi xe ghep duoc vao cau ghep`() {
        val r = Rig()
        r.dispatcher.submit("mở cài đặt rồi nói với xe")
        assertEquals(1, r.listens)
        assertEquals(listOf("settings"), r.port.log, "vế đầu vẫn phải chạy")
    }
}
