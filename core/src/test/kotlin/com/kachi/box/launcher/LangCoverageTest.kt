package com.kachi.box.launcher

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.kachi.box.launcher.LangCoverageFixtures.SHORT_CAP
import com.kachi.box.launcher.LangCoverageFixtures.SAME_ON_PURPOSE
import com.kachi.box.launcher.LangCoverageFixtures.localizedRows
import com.kachi.box.launcher.LangCoverageFixtures.hasVietnameseMark
import com.kachi.box.launcher.LangCoverageFixtures.snapshot
import com.kachi.box.launcher.LangCoverageFixtures.assertNotEquals

/**
 * ═══ U5 · T2 — BÀI CANH NHÃN TIẾNG ANH ════════════════════════════════════════════════════════════════════════
 *
 * ## Bệnh nó chữa
 * Nhãn tiếng Anh là **dữ liệu gắn vào từng dòng registry** ([Strings] KDoc giải thích vì sao). Dữ liệu thì **quên
 * được**: thêm một datum mới mà không điền `labelEn` sẽ không làm gì đỏ, launcher vẫn chạy, và cái sai chỉ lộ ra
 * dưới dạng *"một ô nói tiếng Việt giữa màn tiếng Anh"* — mà **chỉ người dùng English gặp**, tức owner sẽ không
 * thấy. Bài này biến chuyện quên đó thành **đỏ off-car**.
 *
 * ## Vì sao ĐẾM TUYỆT ĐỐI, không chỉ `forEach { assertNotNull }`
 * Vòng lặp trên một danh sách chỉ chứng minh *"những gì đang có đều có nhãn"*. Nó **không** bắt được ca danh sách bị
 * co lại (ai đó bỏ một bộ đăng ký khỏi phép quét, hoặc [Localized] bị tháo khỏi một lớp) — lúc đó vòng lặp quét ít
 * hơn và vẫn xanh. Ghim số là cách duy nhất để *"quét thiếu"* cũng đỏ. Đây đúng bài học của dự án về **dấu xanh
 * giả**: thêm tệp MỚI thì đỏ đúng, còn thứ **mất đi** thì im lặng.
 *
 * ## Ba luật về CHẤT bản dịch (không chỉ "có hay không")
 *  1. **Không được trùng y nguyên nhãn Việt** — vì chép nguyên là cách "điền cho xong". Trừ [SAME_ON_PURPOSE]: ký
 *     hiệu ngành (VIN · PM2.5 · EV/HEV) thì dịch mới là sai (spec §6 OQ2).
 *  2. **Không được chứa dấu tiếng Việt** — bắt ca dịch nửa vời (*"Tyre pressure trước-trái"*).
 *  3. **Nhãn ngắn phải THẬT ngắn** — chip thanh trạng thái cao ~24dp; nhãn ngắn dài bằng nhãn đầy thì `shortEn` vô
 *     nghĩa và chữ bị cắt (đúng lỗi [ĐO] 2026-09-10: *"Áp lốp trước-t…"* × 2 không phân biệt được).
 *
 * ## ⚠ TỰ DỌN [Strings.current] — bắt buộc
 * [Strings.current] là `var` toàn cục. Bài nào đổi nó mà không dọn sẽ làm **bài chạy sau** đọc nhãn tiếng Anh trong
 * khi nó assert tiếng Việt ⇒ đỏ ở một tệp **không liên quan**, và chạy riêng lẻ thì lại xanh. Đó là loại lỗi rất tốn
 * thời gian để lần ra, nên [dọn] chạy sau MỌI bài ở đây (kể cả bài không đổi gì) và có một bài riêng chứng minh việc
 * đổi-rồi-trả-về không để lại vết.
 */
class LangCoverageTest {

    /** Chạy sau MỌI bài — kể cả bài không chạm [Strings.current], để không phải nhớ bài nào có chạm. */
    @AfterEach
    fun `dọn`() {
        Strings.current = Lang.VI
    }

    // ── 1 · ĐẾM TUYỆT ĐỐI: không mã nào thiếu nhãn EN ────────────────────────────────────────────

    @Test
    fun `moi widget co nhan EN, dung 4 widget`() {
        // F1 (2026-10-02): 9 → 10 = `w_apps` (lưới lối tắt ứng dụng, spec shortcuts-autostart R1.3).
        // Android box B2 · W3 (2026-10-09): 10 → 4 = sáu widget xe gỡ cùng lõi HAL BYDAuto.
        assertEquals(4, WidgetRegistry.ALL.size)
        val missing = WidgetRegistry.ALL.filter { it.labelEn.isNullOrBlank() }.map { it.id }
        assertTrue(missing.isEmpty(), "widget thiếu nhãn tiếng Anh: $missing")
    }

    @Test
    fun `moi muc cai dat co nhan EN — 9 nhom va 50 muc`() {
        // +VOICE (owner 2026-09-21 tách menu Giọng nói riêng) · Android box B2 · W1: −CAST −CAR ⇒ 11 → 9.
        assertEquals(9, SettingsCatalog.GROUPS.size)
        // 54 = 20 (IA v1) + 36 mục dựng lại từ màn ClusterNav (IA v2 §4.3: nav 11 · cast 9 · keys 5 · car 5 thêm ·
        // system 6 thêm · about 1 thêm), trừ `clusternav_open` (IA v2), trừ `system_advanced_screen` (S3 2026-09-13:
        // màn cũ gỡ hẳn), rồi S4 · R1/R6: **−3** mục cảnh (`home_scenes` · `home_scene_boot` · `home_scene_save`)
        // **+2** mục hồ sơ (`profiles_boot` · `profiles_add`), rồi T-BRIDGE **+1** (`system_test_bridge` — công
        // tắc chế độ kiểm thử qua adb, docs/specs/kachi-test-bridge.html), rồi S5 **+2** (`system_default_home` —
        // nút Đặt Kachi làm màn hình chính, và `system_keep_home_on_boot` — công tắc giữ khi nổ máy).
        // S1b (2026-09-14): **+1** (`bars_dock_visible` — công tắc ẩn/hiện thanh nút xe).
        // A1 (2026-09-15, docs/specs/kachi-voice-addresses.html): **+2** — `places_list` (khoá `saved_places`) và
        // `places_add` (nút, không khoá). Cả hai nằm ở nhóm NAV dù khoá thuộc phía launcher: nhóm chia theo thứ
        // người dùng nghĩ tới, không theo tệp lưu (xem KDoc `SettingsGroup`).
        // Voice pha 2 (2026-09-16, docs/specs/kachi-voice-feedback.html R4/T8): **+3** — `voice_speak_replies` và
        // `voice_prefer_offline` (hai khoá của `Prefs`, theo XE) + `voice_tts_pack` (nút tải gói giọng, không khoá).
        // V3 (1.66): +4 mục — `voice_confirm_ids` · `voice_ask_aloud` · `voice_mic_source` · `bars_top_strip_labels`.
        // H2/H6 (1.69): **+3** — `voice_keep_log` (ô tích giữ nhật ký lượt nói, khoá THEO XE), `voice_log_export`
        // (nút nén `voice-log/` ra `Download/`, không khoá) và `voice_model_light` (hai nút đổi/gỡ mô hình nghe,
        // không khoá — lựa chọn mô hình lưu ở tệp prefs riêng `kachi_voice` của `VoiceModelStore`).
        // VISUAL-REFRESH P1b · R8 (2026-09-17): **+1** — `display_color` (màu nhấn + tông thẻ, khoá `color_choice`
        // theo hồ sơ; docs/specs/kachi-visual-refresh.html §R8).
        // voice-clone T7/T8 (2026-09-17): **+1** — `voice_feedback_voice` (ô tích chọn giọng phản hồi Piper/giọng bé).
        // AUTOMATION (1.85, spec kachi-automation): **+2** — `car_rain_defrost` (công tắc tự sấy kính khi mưa,
        // khoá `rain_defrost_enabled`) + `nav_automation` (sổ luật tự dẫn đường theo lịch, khoá
        // `nav_automation_rules`). Cả hai theo XE.
        // UX-OVERHAUL WP1 (2026-09-20): **+1** — `display_glass_real` (công tắc "Kính thật (làm mờ nền)", khoá
        // `ui_glass_real`, nhóm Hiển thị, theo XE) ⇒ 75 → 76.
        // Release production (owner 2026-09-21): **81 → 77 (−4)** = dọn hết dev/debug UI khỏi màn Cài đặt, chỉ giữ
        // công tắc `system_test_bridge`. Bốn mục rời danh mục vì chúng không còn hàng nào trên màn:
        // `system_vietmap_data` · `system_diagnostics` (hai màn chẩn đoán) · `voice_keep_log` · `voice_log_export`
        // (ô tích + nút xuất nhật ký lượt nói). KHẢ NĂNG không mất — cả bốn vẫn chạy qua adb (`am start` cho hai màn,
        // `prefs_set`/`voice_dump` cho hai cái kia); khoá `voice_keep_log` chuyển sang HIDDEN_KEYS kèm lý do.
        // Một-mô-hình-nghe (owner 2026-09-21, cùng bản): **77 → 76 (−1)** = `voice_model_light` (hai nút *chuyển
        // sang mô hình nhẹ* / *gỡ bản nặng*). Danh mục mô hình nghe thu về ĐÚNG MỘT gói (`SherpaModelCatalog.ALL`,
        // owner: *"chỉ giữ model đang OK trên xe, không thử nghiệm gì nữa"*) ⇒ bề mặt chọn-mô-hình không còn gì để
        // chọn giữa, `VoiceModelSettings.lightModelRows` gỡ. Hàng *trạng thái + Tải/Gỡ* của gói duy nhất Ở LẠI
        // (nó chưa bao giờ là một mục danh mục — nó thuộc khối dựng tay cùng `voice_tts_pack`). 2.74 · R3: **75 → 76 (+1)** = `voice_commands`, mục THÔNG TIN *"Câu lệnh nói được"* / *"Spoken commands"*.
        // F1 lối tắt (2026-10-02): **76 → 77 (+1)** = `bars_app_shortcuts` *"Lối tắt ứng dụng"* / *"App shortcuts"*.
        // F2/F3 chuyến lên xe (2026-10-02, nhóm C): **77 → 79 (+2)** = `system_ignition_apps` *"Mở app khi nổ máy"* / *"Open
        // apps at ignition"* + `voice_ignition_music` *"Tự mở nhạc khi lên xe"* / *"Play music when you get in"*; EN tại chỗ khai.
        // 2.87 · R-AH3 (owner 03/10, spec kachi-287-look-and-keys): **79 → 80 (+1)** = `home_swap_autohide` *"Tự ẩn nút ⇄"* /
        // *"Auto-hide the ⇄ button"* (khoá `swap_button_autohide`, theo hồ sơ).
        // 2.89 · B1b (owner 05/10, spec kachi-289-field-fixes B1b): **80 → 81 (+1)** = `cast_style` *"Kiểu chiếu cụm"* /
        // *"Cluster cast style"* (Bo tròn / Chữ nhật, khoá `cast_style` theo hồ sơ).
        // 2.89 · B3 DOCK-SCALE (owner 05/10, spec kachi-289-field-fixes B3): **81 → 82 (+1)** = `display_bar_scale` *"Cỡ thanh
        // nút xe"* / *"Car bar size"* (khoá `dock_scale` theo hồ sơ).
        // 2.91 · F1 (spec kachi-291-small-fixes §4.1): **82 → 83 (+1)** = `vm_bubble_autostart` *"Tự mở VietMap cho bong bóng"* /
        // *"Auto-open VietMap for the bubble"* — tách nghĩa cũ của `vm_bubble_enabled` khỏi công tắc hiện bóng.
        // 2.91 VOICE-APP-NAMES (owner 06/10, spec kachi-290-voice-app-names §4.5): **83 → 84 (+1)** = `voice_app_names_list`
        // *"Dạy tên app"* / *"Teach app names"* (khoá `voice_app_names` theo hồ sơ).
        // Android box B2 · W1 (2026-10-09): **84 → 50 (−34)** = mục chỉ-BYD gỡ khỏi danh mục — BARS 2 (chip thanh trạng thái +
        // nhãn chip) · DISPLAY 1 (đơn vị) · NAV 12 (dẫn đường cụm/HUD 4 · biển báo 5 · bong bóng VietMap 3) · CAST 11 · CAR 7 ·
        // SYSTEM 1 (`system_nav_stop`). Danh sách + lý do: `SettingsCatalogRetired`, `SettingsCatalogRetiredTest`.
        assertEquals(50, SettingsCatalog.ENTRIES.size)
        val badGroups = SettingsCatalog.GROUPS.filter { it.labelEn.isBlank() || it.subEn.isBlank() }.map { it.id }
        assertTrue(badGroups.isEmpty(), "nhóm cài đặt thiếu labelEn/subEn: $badGroups")
        val badEntries = SettingsCatalog.ENTRIES.filter { it.labelEn.isNullOrBlank() }.map { it.id }
        assertTrue(badEntries.isEmpty(), "mục cài đặt thiếu nhãn tiếng Anh: $badEntries")
    }

    /**
     * Phép quét GỘP: mọi thứ mang [Localized] phải có nhãn EN, và **tổng số phải khớp**.
     *
     * Đây là bài bắt ca *"một bộ đăng ký lặng lẽ rơi khỏi tầm quét"*: sáu bài trên mỗi bài canh một bộ, nên bỏ hẳn
     * `Localized` khỏi một lớp sẽ làm lớp đó không còn ở đây mà **không bài nào đỏ** nếu không ghim tổng.
     */
    @Test
    fun `tong so nhan co ban EN dung 80`() {
        val all: List<Localized> = localizedRows()
        // 302 = 265 + 3 nhóm mới (nav · cast · keys — `bars` bù cho `clusternav` bị bỏ) + 36 mục mới của IA v2,
        // trừ 1 mục `system_advanced_screen` gỡ cùng màn ClusterNav cũ (S3 · 2026-09-13), rồi S4 · R1/R6 −3 mục
        // cảnh +2 mục hồ sơ.
        // S4 · R12: +2 hành động launcher ([LauncherActions]) — chúng mang [Localized] nên PHẢI nằm trong tầm quét
        // này, không thì một bộ đăng ký mới có nhãn chưa dịch mà không bài nào thấy.
        // V1 pha NGHE: +2 — `launcher_voice` *"Nói với xe" · "Talk to car"* và điều kiện `microphone`.
        // T-BRIDGE: +1 — mục `system_test_bridge` (công tắc "Chế độ kiểm thử qua adb").
        // S5: +2 — `system_default_home` (nút Đặt Kachi làm màn hình chính) + `system_keep_home_on_boot` (công tắc).
        // A1 (2026-09-15): +2 — `places_list` + `places_add` (sổ địa chỉ, docs/specs/kachi-voice-addresses.html).
        // ⚠ TÊN BÀI đã lệch số từ trước lượt này (tên nói 307 trong khi ghim 310); nay đặt lại cho khớp —
        // một cái tên nói sai con số nó đang canh là chỗ người sau đọc rồi tin nhầm.
        // Voice pha 2 (2026-09-16): +3 — `voice_speak_replies` · `voice_prefer_offline` · `voice_tts_pack`.
        // 288 (2026-09-16 owner gỡ ADAS/an toàn — trước đó 319).
        // VOICE-HOTFIX 1.69: **288 → 291 (+3)** = ba mục Cài đặt mới của đường giọng nói (`voice_keep_log` giữ
        // nhật ký lượt nói · `voice_log_export` xuất zip · `voice_model_light` đổi sang mô hình int8). Cả ba đã
        // có nhãn ở CẢ hai thứ tiếng — chính bài này là thứ ép điều đó, nên con số chỉ được ghim SAU khi dịch.
        // H1 · T2 (2026-09-16): **291 → 297 (+6)** = sáu datum mới cho đường ĐỌC của nút (`seat_vent_state`
        // `seat_heat_state` `defrost_front_state` `defrost_rear_state` `ac_mode_auto` `media_vol`). Cả sáu có nhãn
        // ở CẢ hai thứ tiếng + nhãn ngắn — chính bài này ép điều đó, nên số chỉ được ghim SAU khi đã dịch.
        // (V) FEATURE-FILTER (2026-09-17): **297 → 278 (−19)** = đúng 19 mã owner chấm NO (12 datum + 7 nút).
        // VISUAL-REFRESH P1b · R8 (2026-09-17): **278 → 279 (+1)** = mục Cài đặt `display_color` ("Màu sắc" / "Colours").
        // voice-clone T7/T8 (2026-09-17): **279 → 280 (+1)** = mục Cài đặt `voice_feedback_voice` ("Giọng phản hồi" / "Feedback voice").
        // nav-default-app (owner 2026-09-18): **280 → 281 (+1)** = mục `nav_default_app` ("App dẫn đường mặc định" / "Default navigation app").
        // 1.85 (2026-09-20): **282 → 283 (+1)** = +1 datum `ac_wind_auto`, +1 nút `child_lock_r`, −1 nút `hood`.
        // 1.85 AUTOMATION (2026-09-20): **283 → 286 (+3)** = +2 mục cài đặt (`car_rain_defrost` ·
        // `nav_automation`) +1 điều kiện quyền (`location`). Ba nhãn mới đều đã có bản EN tại chỗ khai.
        // UX-OVERHAUL WP1 (2026-09-20): **286 → 287 (+1)** = mục `display_glass_real` ("Kính thật (làm mờ nền)" /
        // "Real glass (blur the backdrop)"), đã có bản EN tại chỗ khai.
        // UX-OVERHAUL WP4 (2026-09-20): **287 → 295 (+8)** = +2 mục cài đặt (`bars_header_order` ·
        // `bars_dock_order`) **và +6 [HeaderItem]** — bộ đăng ký MỚI mang [Localized]. Đưa nó vào danh sách quét
        // ngay lượt này là đúng việc bài này sinh ra để làm (KDoc trên): một bộ mang nhãn mà không nằm trong tầm
        // quét thì nó có thể thiếu bản EN mà không bài nào đỏ.
        // UX-OVERHAUL WP6 (2026-09-20): **295 → 296 (+1)** = mục `cast_bubble` ("Hiện nút nổi chiếu cụm" / "Show
        // the floating cast button"), khoá `cast_bubble_visible` theo XE; đã có bản EN tại chỗ khai.
        // UX-OVERHAUL WP8 (2026-09-20): **296 → 258 (−38)** = −29 datum −8 nút (owner purge 37 mã BỎ) −1 nhóm
        // (`g_ambient` hết thành viên). Đây là lượt duy nhất con số này GIẢM; mọi lượt trước đều cộng.
        // Release production (owner 2026-09-21): **260 → 256 (−4)** = bốn mục Cài đặt rời danh mục cùng lượt dọn
        // dev/debug UI (`system_vietmap_data` · `system_diagnostics` · `voice_keep_log` · `voice_log_export`) — xem
        // lý do đầy đủ ở `tong so muc Cai dat…` phía trên. Lượt GIẢM thứ hai của con số này.
        // Một-mô-hình-nghe (owner 2026-09-21, cùng bản): **256 → 255 (−1)** = mục `voice_model_light`. Danh mục mô
        // hình nghe thu về đúng một gói ⇒ bề mặt chọn-mô-hình gỡ khỏi Cài đặt. Lượt GIẢM thứ ba.
        // 1.90: **255 → 244 (−11)** = −9 nút −2 datum (xe thuần điện). Lượt GIẢM thứ tư.
        // 2026-09-25: **245 → 238 (−7)** = 7 datum CHẾT bị gỡ. Lượt GIẢM thứ năm. 2.74 · R3: **238 → 239 (+1)** = mục `voice_commands`, đã có nhãn CẢ hai thứ tiếng tại chỗ khai (chính bài này ép điều đó).
        // UX5b (owner 2026-09-27): **239 → 241 (+2)** = hai datum ghế PHỤ (`seat_vent_state_r` ·
        // `seat_heat_state_r`), đều có nhãn ĐẦY + nhãn NGẮN ở cả hai thứ tiếng tại chỗ khai. Hai chip GỘP mới không
        // cộng vào đây: nhãn của chúng là `Strings.t(...)` inline ở `TopStripChips`/`TopStripConfig.choices` (không
        // phải dòng registry), đúng như ba chip dựng sẵn cũ.
        // F1 lối tắt ứng dụng (2026-10-02): **241 → 244 (+3)** = widget `w_apps` ("Lưới lối tắt app" / "App shortcut
        // grid") + mục Cài đặt `bars_app_shortcuts` + khối thanh nút `launcher_shortcuts` — bộ đăng ký MỚI
        // `LauncherActions.BLOCKS` (đặt được, không gọi bằng lời) đưa vào tầm quét ngay lượt này. Cả ba có EN tại chỗ khai.
        // F2/F3 chuyến lên xe (2026-10-02, nhóm C): **244 → 246 (+2)** = hai mục Cài đặt `system_ignition_apps` ·
        // `voice_ignition_music` (xem `tong so muc Cai dat…`), cả hai có EN tại chỗ khai.
        // 2.87 · R-AH3 (2026-10-03): **246 → 247 (+1)** = mục Cài đặt `home_swap_autohide` ("Tự ẩn nút ⇄" / "Auto-hide
        // the ⇄ button"), EN tại chỗ khai + dòng zh/th/ms trong `i18n/*.tsv`.
        // 2.88 (2026-10-04): **247 → 260 (+13)** = nhãn VI/EN của 13 mã trạng thái THÔ của lốp (màu cụm · trạng thái áp ·
        // rò khí ×4 + hệ thống TPMS) — cả 13 có bản dịch zh/th/ms (`I18nCoverageTest`).
        // 2.89 · B1b (2026-10-05): **260 → 261 (+1)** = mục Cài đặt `cast_style` ("Kiểu chiếu cụm" / "Cluster cast style"),
        // EN tại chỗ khai + dòng zh/th/ms trong `i18n/*.tsv`.
        // 2.89 · B3 (2026-10-05): **261 → 262 (+1)** = mục Cài đặt `display_bar_scale` ("Cỡ thanh nút xe" / "Car bar size"),
        // EN tại chỗ khai + dòng zh/th/ms trong `i18n/*.tsv`.
        // 2.91 · F1 (2026-10-06): **262 → 263 (+1)** = mục Cài đặt `vm_bubble_autostart` ("Tự mở VietMap cho bong bóng" /
        // "Auto-open VietMap for the bubble"), EN tại chỗ khai + dòng zh/th/ms trong `i18n/*.tsv`.
        // 2.91 VOICE-APP-NAMES (2026-10-06): **263 → 264 (+1)** = mục Cài đặt `voice_app_names_list` ("Dạy tên app" /
        // "Teach app names"), EN tại chỗ khai + dòng zh/th/ms trong `i18n/*.tsv`.
        // 2.93 · CAMERA-ON-DEMAND (2026-10-06): **264 → 269 (+5)** = bốn việc *"Camera sau/trái/phải/trước"* + *"Tắt camera"*
        // (`LauncherActions`), EN tại chỗ khai + dòng zh/th/ms trong `i18n/*.tsv`.
        // Android box B2 · W1 (2026-10-09): **269 → 233 (−36)** = −34 mục Cài đặt chỉ-BYD − 2 nhóm (CAST · CAR).
        // Android box B2 · W2b (2026-10-09): **233 → 227 (−6)** = năm việc camera theo yêu cầu (`launcher_cam_*`) + nút `cam`.
        // Android box B2 · W3 (2026-10-09): **227 → 80** = datum · nút · nhóm · gói lệnh · lĩnh vực · đại lượng ·
        // góc bánh gỡ cùng lõi HAL BYDAuto, −6 widget xe, −1 vật thanh trên (`CHIPS`).
        assertEquals(80, all.size, "số nhãn đổi — thêm mã mới thì phải dịch, rồi mới ghim số mới")
        val missing = all.filter { it.labelEn.isNullOrBlank() }.map { it.label }
        assertTrue(missing.isEmpty(), "còn nhãn chưa có bản EN: $missing")
    }

    // ── 2 · CHẤT bản dịch ───────────────────────────────────────────────────────────────────────

    @Test
    fun `nhan EN khong trung y nguyen nhan VI`() {
        val copied = localizedRows()
            .filter { it.labelEn == it.label && it.label !in SAME_ON_PURPOSE }
            .map { it.label }
        assertTrue(
            copied.isEmpty(),
            "nhãn EN chép y nguyên nhãn VI (điền cho xong?) — nếu là ký hiệu ngành thì khai vào SAME_ON_PURPOSE " +
                "kèm lý do: $copied",
        )
    }

    @Test
    fun `moi muc trong danh sach cho phep trung deu co ly do, va deu dung toi`() {
        SAME_ON_PURPOSE.forEach { (term, why) ->
            assertTrue(why.isNotBlank(), "'$term' được phép trùng thì phải nói LÝ DO, không thì đây là chỗ làm im bài")
        }
        // Danh sách cho phép mà không còn ai dùng = rác tích lại, và nó nới lỏng bài canh cho lần sau.
        val everyString = localizedRows().flatMap { listOf(it.label, it.labelEn ?: "") }
        val unused = SAME_ON_PURPOSE.keys.filterNot { it in everyString }
        assertTrue(unused.isEmpty(), "mục cho phép trùng không còn ai dùng ⇒ xoá đi: $unused")
    }

    @Test
    fun `nhan EN khong chua dau tieng Viet`() {
        val dirty = ArrayList<String>()
        localizedRows().forEach { row ->
            row.labelEn?.let { if (hasVietnameseMark(it)) dirty.add("${row.label} → $it") }
        }
        SettingsCatalog.GROUPS.forEach { g ->
            if (hasVietnameseMark(g.subEn)) dirty.add("${g.id}.subEn → ${g.subEn}")
        }
        LauncherRequirements.ALL.forEach { r ->
            listOfNotNull(r.losesWhatIfMissingEn, r.userActionEn)
                .filter { hasVietnameseMark(it) }.forEach { dirty.add("${r.id} → $it") }
        }
        assertTrue(dirty.isEmpty(), "bản EN còn dấu tiếng Việt (dịch nửa vời): $dirty")
    }

    /**
     * Bản dịch KHÔNG được sinh ra cặp nhãn trùng MỚI.
     *
     * [ĐO] khi làm T2: ba nhãn `Đèn viền` (nhóm) / `Đèn viền cabin` (datum) / `Đèn viền cabin` (nút) — bản dịch đầu
     * của tôi cho **cả ba** thành `"Ambient lighting"`. Bản Việt chỉ trùng MỘT cặp (datum↔nút, và cặp đó đã có gợi ý
     * loại `· xem`/`· bấm`), nên tiếng Anh sinh ra một cặp trùng thứ hai **không ai gợi ý** — vì phép phát hiện trùng
     * chạy trên nhãn Việt (xem KDoc [CapabilityPick.displayLabel]). Hậu quả: hai ô chữ y hệt nhau, chỉ người dùng
     * English gặp.
     *
     * ## ⚠⚠ [ĐO] BÀI NÀY TỪNG KHÔNG BẮT ĐƯỢC CHÍNH CA NÓ SINH RA ĐỂ BẮT
     * Bản đầu hỏi *"trong nhóm trùng ở EN, có mục nào ĐÃ trùng ở VI không"* — nếu có thì tha cả nhóm. Dựng lại đúng
     * lỗi cũ (nhóm `g_ambient` mang nhãn của datum) thì bài **VẪN XANH**, vì hai mục kia đúng là đã trùng ở VI.
     * Phép đúng phải là **theo TỪNG CẶP**: trong một nhóm trùng ở EN thì **mọi** nhãn VI phải giống nhau — tức chúng
     * vốn đã là cùng một cặp trùng, không phải "có họ hàng với một cặp trùng". Đây là lời nhắc rằng thử-phá không chỉ
     * kiểm mã sản phẩm, nó kiểm cả bài canh.
     */
    @Test
    fun `ban dich khong sinh ra nhan trung MOI`() {
        val rows = WidgetRegistry.ALL.map { it.id to (it.label to it.labelEn) } +
            LauncherActions.placeable.map { it.id to (it.label to it.labelEn) }
        val newlyColliding = rows
            .groupBy { it.second.second ?: it.second.first }         // gom theo nhãn EN thực dùng
            .filterValues { group -> group.size > 1 && group.map { it.second.first }.distinct().size > 1 }
            .map { (en, group) -> "$en ← ${group.map { it.first }} (VI: ${group.map { it.second.first }.distinct()})" }
        assertTrue(
            newlyColliding.isEmpty(),
            "bản dịch làm hai mục KHÁC NHAU thành cùng một chữ (tiếng Việt chúng khác nhau ⇒ không có gợi ý loại " +
                "nào phân biệt): $newlyColliding",
        )
    }

    // ── 3 · Cơ chế chọn ngôn ngữ ─────────────────────────────────────────────────────────────────

    @Test
    fun `t tra ve theo ngon ngu dang chon`() {
        Strings.current = Lang.VI
        assertEquals("xin chào", Strings.t("xin chào", "hello"))
        Strings.current = Lang.EN
        assertEquals("hello", Strings.t("xin chào", "hello"))
        // Dạng có tham số tường minh KHÔNG phụ thuộc trạng thái toàn cục (đây là dạng test nên dùng).
        assertEquals("xin chào", Strings.t("xin chào", "hello", Lang.VI))
    }

    @Test
    fun `pick tu lui ve tieng Viet khi chua dich`() {
        Strings.current = Lang.EN
        assertEquals("gốc", Strings.pick("gốc", null))
        assertEquals("gốc", Strings.pick("gốc", ""), "chuỗi rỗng = CHƯA dịch, không phải 'nhãn trống'")
        assertEquals("gốc", Strings.pick("gốc", "   "))
        assertEquals("root", Strings.pick("gốc", "root"))
        // Một dòng chưa dịch thì hiện tiếng Việt — KHÔNG ném, vì launcher trên xe không được sập vì một nhãn.
        val undone = WidgetPick("x", "Nhãn Việt", "ic-sun", labelEn = null)
        assertEquals("Nhãn Việt", undone.displayLabel)
    }

    @Test
    fun `doi ngon ngu roi tra ve VI thi moi nhan y nhu cu`() {
        Strings.current = Lang.VI
        val before = snapshot()
        Strings.current = Lang.EN
        val english = snapshot()
        assertNotEquals(before, english)
        Strings.current = Lang.VI
        assertEquals(before, snapshot(), "đổi ngôn ngữ để lại vết ⇒ bài chạy sau sẽ đỏ ở tệp không liên quan")
    }

}
