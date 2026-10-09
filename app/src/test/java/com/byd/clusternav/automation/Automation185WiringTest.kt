package com.byd.clusternav.automation

import com.byd.clusternav.launcher.SettingsNavAutomationFormat
import com.byd.clusternav.launcher.automation.ScheduledNavRules
import com.byd.clusternav.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ AUTOMATION 1.85 · BÀI CANH DÂY NỐI của `:app` (stage 3) ══════════════════════════════════════════════════
 *
 * Spec `docs/specs/kachi-automation.html` R1–R5. Hai loại bài trong một tệp, và ranh giới giữa chúng là **có đo
 * được off-car hay không**:
 *  • **thuần** — phép đọc/in giờ ([SettingsNavAutomationFormat]) chạy thật;
 *  • **quét nguồn** — mọi thứ chạm HAL / `LocationManager` / FGS thì off-car không chạy được, nên thứ canh được là
 *    *"đường dây có nối đúng chỗ không"*. Đây đúng khuôn `ClusterNavBridgeWiringContractTest` /
 *    `KachiAutostartServiceWiringTest` của dự án: chuỗi lời gọi **thật**, không phải nhãn.
 *
 * ⚠ Bài quét nguồn không thay được một lượt đo trên xe — nó chỉ chặn *"ai đó gỡ một mắt xích rồi build vẫn xanh"*.
 * Phần phải đo trên xe ghi ở `docs/_handoff/1.85-stage3.md`.
 */
class Automation185WiringTest {

    private fun app(relative: String): String = SourceRoots.codeOf("src/main/java/com/byd/clusternav/$relative")

    private val navApplier by lazy { app("automation/ScheduledNavApplier.kt") }
    private val service by lazy { app("automation/AutomationService.kt") }
    private val gps by lazy { app("automation/GpsAvailability.kt") }
    private val bridge by lazy { app("launcher/ClusterNavBridgeAutomation.kt") }
    private val prefs by lazy { app("PrefsAutomation.kt") }
    private val autostart by lazy { app("KachiAutostart.kt") }
    private val boot by lazy { app("BootSetupService.kt") }

    // ── Phần THUẦN: đọc/in giờ ─────────────────────────────────────────────────────────────────────

    @Test
    fun `in gio ra HH mm va kep gia tri rac`() {
        assertEquals("07:30", SettingsNavAutomationFormat.hhmm(7 * 60 + 30))
        assertEquals("00:00", SettingsNavAutomationFormat.hhmm(0))
        assertEquals("23:59", SettingsNavAutomationFormat.hhmm(ScheduledNavRules.MAX_MIN))
        // Giá trị rác trên đĩa KHÔNG được in ra "25:73" — kẹp về hai đầu dải.
        assertEquals("23:59", SettingsNavAutomationFormat.hhmm(99_999))
        assertEquals("00:00", SettingsNavAutomationFormat.hhmm(-5))
    }

    @Test
    fun `doc gio nhan ba dang nguoi dung go`() {
        assertEquals(7 * 60 + 30, SettingsNavAutomationFormat.parseHhmm("07:30"))
        assertEquals(7 * 60 + 30, SettingsNavAutomationFormat.parseHhmm("7:30"))
        assertEquals(7 * 60 + 30, SettingsNavAutomationFormat.parseHhmm("7h30"))
        assertEquals(7 * 60 + 30, SettingsNavAutomationFormat.parseHhmm(" 7 30 "))
        assertEquals(0, SettingsNavAutomationFormat.parseHhmm("0:00"))
        assertEquals(ScheduledNavRules.MAX_MIN, SettingsNavAutomationFormat.parseHhmm("23:59"))
    }

    /**
     * Giờ sai ⇒ `null`, để chỗ gọi đưa `-1` xuống `:core` và chính [ScheduledNavRules.of] từ chối rồi UI **nói ra**.
     * Lùi về một giá trị mặc định ở tầng đọc sẽ lưu một khung giờ người dùng KHÔNG gõ, im lặng.
     */
    @Test
    fun `gio sai tra null chu khong doan`() {
        listOf("", "  ", "abc", "7", "7:", ":30", "24:00", "23:60", "7:30:00", "-1:00", "7:-5")
            .forEach { assertNull(SettingsNavAutomationFormat.parseHhmm(it), "\"$it\" phải bị từ chối") }
    }

    @Test
    fun `vong tron in roi doc lai giu nguyen phut`() {
        (0..ScheduledNavRules.MAX_MIN step 7).forEach { m ->
            assertEquals(m, SettingsNavAutomationFormat.parseHhmm(SettingsNavAutomationFormat.hhmm(m)))
        }
    }

    // ── Phần QUÉT NGUỒN: đường dây ────────────────────────────────────────────────────────────────

    /**
     * Android box B2 · W2e (2026-10-09) — AUTOMATION #1 *tự sấy kính khi mưa* gỡ cùng HAL BYD (cảm biến mưa
     * `SETTING_FRONT_RAIN_WIPER_SPEED` + nút sấy). Ba bài canh dây nối của nó (đọc sấy từ xe · chỉ ghi kính đã chọn ·
     * bind theo tên hằng) xoá cùng mã; bài này chặn nó mọc lại nửa vời: không tệp, không lời gọi, không cổng prefs.
     */
    @Test
    fun `tu say kinh khi mua da go het`() {
        listOf(
            "automation/RainDefrostApplier.kt", "launcher/SettingsSectionsCar.kt",
            "launcher/automation/RainDefrostGlasses.kt", "launcher/automation/RainDefrostPolicy.kt",
        ).forEach { assertFalse(SourceRoots.exists("src/main/java/com/byd/clusternav/$it"), "$it phải đã xoá") }
        listOf(service, bridge, prefs, navApplier).forEach { src ->
            listOf("RainDefrost", "rainDefrost", "RAIN_EVERY_TICKS", "K_RAIN_DEFROST", "RainGlass").forEach {
                assertFalse(it in src, "'$it' còn trong mã automation (W2e)")
            }
        }
    }

    /**
     * ⚠⚠ R2.4 · usecase hầm: `GpsAvailability` trả **ba** giá trị (`true`/`false`/`null`), và `null` = *chưa biết*
     * phải xử như **CHỜ**. So `== true` là cách duy nhất làm điều đó; `!= false` sẽ biến *"chưa cấp quyền"* thành
     * *"có GPS"* ⇒ dẫn trong hầm, im lặng, đúng trên chiếc xe chưa cấp quyền.
     */
    @Test
    fun `gps chua biet thi CHO chu khong di luon`() {
        assertTrue(
            "GpsAvailability.isAvailable(app, nowMs) == true" in navApplier,
            "null (chưa biết) phải rơi về false = chờ; cấm dùng `!= false`",
        )
        assertTrue("Boolean?" in gps, "phải phơi được ca \"không đọc được\"")
        assertTrue("MAX_FIX_AGE_MS" in gps, "phải hỏi TUỔI fix, không chỉ hỏi provider có bật")
        assertTrue("elapsedRealtimeNanos" in gps, "đồng hồ đơn điệu: đầu xe VẶN đồng hồ tường lúc nổ máy")
    }

    /**
     * R2.4: đóng dấu **NGAY** sau khi giao được, và **KHÔNG** đóng dấu khi chưa giao được.
     *  • thiếu vế đầu ⇒ nhịp sau (60 s) mở app dẫn đường lần thứ hai, suốt cả khung giờ;
     *  • thêm vế sau sai ⇒ một lần hỏng tạm thời (app chưa cài) làm mất cả lượt đi của ngày hôm đó.
     */
    @Test
    fun `dong dau da-dan chi khi giao duoc`() {
        assertTrue("if (launched) {" in navApplier)
        assertTrue("NavAutomationFired.put(fired, rule.id, today)" in navApplier)
        assertTrue("setNavAutomationFired(app" in navApplier)
        val afterElse = navApplier.substringAfter("if (launched) {").substringAfter("} else {")
        assertTrue(
            "setNavAutomationFired" !in afterElse.substringBefore("\n        }"),
            "nhánh KHÔNG giao được tuyệt đối không được đóng dấu",
        )
    }

    /** Đi đúng đường mà một câu lệnh giọng nói đi — không dựng `Intent` thứ hai (xem KDoc `VoiceAppIntents`). */
    @Test
    fun `nav dung lai duong ban y-dinh cua voice`() {
        assertTrue("VoiceAppIntents.send(app, handoff)" in navApplier)
        assertTrue("Intent(Intent.ACTION_VIEW" !in navApplier, "cấm dựng Intent dẫn đường thứ hai ở đây")
        assertTrue("SavedPlaces.find(" in navApplier, "điểm đến tra từ sổ địa chỉ của hồ sơ đang dùng")
        assertTrue("PackageQueries.queryActivities(" in navApplier, "hỏi app đã cài qua cửa duy nhất của dự án")
    }

    /** R4: nhịp 60 s cho nav, một vòng duy nhất (nhịp mưa ~5 phút gỡ ở W1/W2e). */
    @Test
    fun `mot dong co, mot vong, chi nhip lich dan duong`() {
        assertEquals(60_000L, AutomationService.TICK_MS)
        // Android box B2 · W1 (2026-10-09) — ĐỔI GHIM có lý do: vòng chỉ còn LỊCH TỰ DẪN ĐƯỜNG. Nhịp mưa
        // (`RainDefrostApplier.tickIfDue`/`loopStarted`/`forgetAll`) và đồng bộ camera xi-nhan (`syncCamera`) gỡ: HAL BYD.
        val loop = SourceRoots.body(service, "private fun startLoop(")
        assertTrue("val nowMs = android.os.SystemClock.elapsedRealtime()" in loop, "nhịp theo đồng hồ đơn điệu của lượt thức")
        assertTrue("ScheduledNavApplier.tick(app)" in loop, "lịch tự dẫn chạy trong vòng")
        listOf("RainDefrostApplier", "syncCamera", "cameraSignal").forEach {
            assertFalse(it in service, "'$it' đã gỡ khỏi động cơ (Android box)")
        }
        assertFalse("RainDefrostApplier" in SourceRoots.body(service, "fun anyEnabled("), "mưa không còn giữ FGS sống")
        assertTrue("CAMERA_TICK_MS" !in service, "BG-13: không còn vòng 250 ms riêng cho camera")
        assertEquals(
            1,
            Regex("""Thread\(\{""").findAll(service).count(),
            "đúng MỘT vòng nhịp — vòng thứ hai là thứ thứ hai phải nhớ dừng lúc huỷ",
        )
    }

    /**
     * Guard vòng đúng cơ chế đã proven ở vòng poll lọc bụi đời BYD (gỡ ở W2e): `running` + **token thế hệ**. Thiếu token thì chuỗi
     * bật→tắt→bật để thread cũ sống cạnh thread mới, và `finally` của thread cũ xoá cờ của thread mới.
     */
    @Test
    fun `vong nhip co token the he va tu thoat`() {
        assertTrue("myGen = ++generation" in service)
        assertTrue("myGen == generation && anyEnabled(app)" in service, "vòng phải tự thoát khi hết việc")
        assertTrue("if (myGen == generation) running = false" in service, "chỉ thế hệ hiện tại được nhả cờ")
        assertTrue("generation++" in service, "tắt công tắc phải vô hiệu vòng NGAY")
    }

    /** Hết việc ⇒ service tự chết. Một FGS thường trú không làm gì là chi phí ròng + một dòng log gây hiểu sai. */
    @Test
    fun `khong con viec thi service tu dung`() {
        assertTrue("if (!anyEnabled(applicationContext))" in service)
        assertTrue("stopSelf" in service)
        assertTrue("startForegroundOnce()" in service, "phải lên foreground TRƯỚC mọi việc (hợp đồng ~5 s)")
    }

    /**
     * ⚠ Mỗi lượt GHI phải kèm một lượt `sync` — [ĐO] S4: **0** chỗ nào trong dự án đăng ký
     * `registerOnSharedPreferenceChangeListener`, nên ghi prefs xong là *"đúng trên đĩa mà không ai đang chạy biết"*.
     */
    @Test
    fun `moi setter cua cau deu dong bo dong co`() {
        // Android box B2 · W2e — cửa ghi tự sấy kính (`setRainDefrostGlass`) gỡ cùng tính năng; còn đúng cửa sổ luật.
        val setRules = SourceRoots.body(bridge, "fun ClusterNavBridge.setNavRules(")
        assertTrue("setNavAutomationRules(app, NavAutomationBook.encode(rules))" in setRules)
        assertTrue("ScheduledNavApplier.pruneFired(app)" in setRules, "phải dọn dấu đã-dẫn mồ côi")
        assertTrue("AutomationService.sync(app)" in setRules)
    }

    /**
     * ⚠⚠ Phải re-arm ở **CẢ HAI** đường boot: `KachiAutostart` gác sau `launcher_autostart`, còn `BootSetupService`
     * gác sau `headless_autostart` — hai công tắc RIÊNG. Gác automation sau một công tắc không liên quan là đúng
     * bẫy đã ăn ở P7 (cảnh khởi động chết theo `launcher_autostart`, im lặng).
     */
    @Test
    fun `hai duong boot deu dung lai dong co`() {
        assertTrue("AutomationService.sync(app)" in autostart, "KachiAutostart phải re-arm engine")
        assertTrue("AutomationService.sync(applicationContext)" in boot, "BootSetupService phải re-arm engine")
    }

    /** Vòng nhịp là RAM ⇒ không re-arm thì lịch tự dẫn chỉ chạy đúng phiên người dùng sửa luật. */
    @Test
    fun `engine khong tu nho vong qua lan no may`() {
        assertTrue(
            "@Volatile private var running" in service && "@Volatile private var generation" in service,
            "hai cờ vòng là RAM (có chủ ý) — đó là lý do phải re-arm ở boot",
        )
    }
}
