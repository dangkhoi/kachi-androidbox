package com.byd.clusternav.launcher

import android.util.Log
import com.byd.clusternav.automation.AutomationService
import com.byd.clusternav.launcher.voice.VoiceWakeService

/**
 * ═══ S4 · R5 — ÁP LẠI cấu hình ClusterNav cho dịch vụ ĐANG CHẠY, sau một lượt đổi hồ sơ ═══════════════════════
 *
 * Hàm mở rộng của [ClusterNavBridge] ở tệp riêng (trần 500 dòng — CLAUDE.md §4.1), cùng cách `ClusterNavBridgeKeys.kt` đã tách.
 *
 * ## ⚠⚠ Vì sao BẮT BUỘC phải có bước này
 * `WorkspacePrefsProfile.applyClusterNav` ghi giá trị của hồ sơ mới vào đúng tệp `SharedPreferences` mà runtime
 * ClusterNav đang đọc — nhưng [ĐO] **không một chỗ nào trong dự án đăng ký
 * `registerOnSharedPreferenceChangeListener`** (grep `app/src/main`, `core/`, `car-integration/`,
 * `vehicle-contracts/`, `offcar-planner/` — 0 kết quả; hit duy nhất là một fake trong
 * `app/src/test/.../VoiceKeyBindingMigrationTest.kt:56`). Nghĩa là ghi prefs xong thì **trên đĩa đã đúng mà không
 * có gì đang chạy biết** — biển báo vẫn cỡ cũ, ghế vẫn mức cũ, lọc bụi vẫn chạy theo hồ sơ trước.
 *
 * ## Ba loại khoá, và vì sao chỉ MỘT loại cần gọi lại
 * [ĐO] đọc từng consumer:
 *  1. **Tự áp** — consumer đọc lại khoá ở **mỗi sự kiện** (mỗi thông báo dẫn đường, mỗi khung HUD, mỗi lần bấm
 *     phím vô-lăng). Chúng tự đúng ở nhịp kế tiếp, gọi thêm chỉ là nhiễu: `voicekey_enabled`/`voicekey_bindings`
 *     (`modules/navaccess/NavAccessibilityService.kt`, mỗi phím). Android box B2 · W2c: khoá chiếu cụm (`cast_*`,
 *     `config_*`, nút nổi) gỡ cùng mã — tệp `simple_cast_prefs` rời ảnh chụp hồ sơ.
 *  2. **Có applier sống** — phải GỌI LẠI, và đó là toàn bộ nội dung của [reapplyAll] dưới đây.
 *  3. **Chỉ đọc lúc khởi động / lúc dựng màn** — không có gì để gọi, và cố gọi là **đổi nghĩa của khoá**. Danh
 *     sách + lý do ở KDoc từng dòng bị bỏ qua, cuối hàm.
 *
 * ## Luật CLAUDE.md §6 áp ở đây
 * *Đường mới luôn xuống cuối, không đảo thứ tự/cơ chế đang chạy tốt trên xe.* Hàm này **không** dựng applier mới,
 * không đổi thứ tự bên trong applier nào: nó chỉ gọi đúng những hàm mà các `bridge.set*` tương ứng vẫn gọi.
 */
internal fun ClusterNavBridge.reapplyAll() {
    // ⚠ Android box B2 · W1 (2026-10-09) — gỡ khỏi lượt áp lại mọi applier chỉ-BYD: dẫn đường lên cụm/HUD (`nav.master` ·
    // `nav.clusterMode`), biển báo tốc độ (`badge.*`), bong bóng VietMap (`bubble.*`), điều kiện nền cho VietMap/app chiếu
    // (`app.prereqs`), ghế (`seat`), lọc bụi (`pm25`) và camera theo yêu cầu (`camera.demand`). Khoá còn đi theo hồ sơ
    // (`SettingsCatalogRetired`), chỉ không còn gì đang chạy để áp — đổi hồ sơ không chạm HAL / cụm / app khác nữa.

    // ── Tự động hoá (luật dẫn đường theo lịch) ──────────────────────────────────────────────────
    // `nav_automation_rules` (theo hồ sơ từ 2026-09-28) quyết việc FGS tự động hoá có sống không. Đổi hồ sơ mà không đồng
    // bộ thì hồ sơ B không lịch vẫn để engine của A chạy (và ngược lại). Idempotent, tự gác theo `anyEnabled`, không ném.
    step("automation.sync") { AutomationService.sync(app) }

    // ── Giọng nói: chế độ `:wake` (FIX286 · VK2/VK4) ─────────────────────────────────────────────────
    // `voicekey_bindings` · `voicekey_enabled` · `voice_music_default_app` theo HỒ SƠ ⇒ đổi hồ sơ đổi `keyHold` (hồ sơ B
    // gán phím Kachi nghe, A thì không) và đổi prefs mà `:wake` đọc. `sync` công bố ảnh chụp tươi rồi bật/tắt FGS theo
    // chế độ — đúng hàm mà công tắc/gán phím gọi; idempotent (đang chạy đúng chế độ ⇒ chỉ một lượt `onStartCommand`).
    step("voice.wake") { VoiceWakeService.sync(app) }

    // ── ⚠ CỐ Ý KHÔNG gọi lại — mỗi dòng là một quyết định, không phải một chỗ quên ───────────────
    //
    //  • `recirc_on_start_enabled` — nghĩa của khoá là *"lấy gió trong khi NỔ MÁY"* (`RecircApplier.applyOnStart`,
    //    đọc một lần ở `BootSetupService.kt:97`). `applyNowAsync` thì **KHÔNG gate** theo công tắc
    //    (`RecircApplier.kt:56-58`) nên gọi nó ở đây là bật quạt lấy gió trong mỗi lần đổi hồ sơ.
    //  • `headless_autostart` — chỉ rẽ nhánh một quyết định của `RebindReceiver` lúc nhận BOOT_COMPLETED
    //    (`RebindReceiver.kt:42,64`). Không có dịch vụ nào đang chạy để báo.
    //  • `theme_choice` — `ThemeMode.setChoice` đọc ở `attachBaseContext`, và KDoc của nó
    //    (`ThemeMode.kt:47`) nói rõ *"caller chịu trách nhiệm recreate Activity đang hiện"*. Màn ClusterNav đang
    //    mở là ca hiếm (người dùng đang ở màn chính để chạm chip hồ sơ); lần mở sau đã đúng.
    //  • `voicekey_custom_buttons` — [ĐO] không có consumer sống: chỉ `ClusterNavBridgeKeys.kt:159,162` đọc để đổ
    //    danh sách trong màn Cài đặt.
    //
    // (Chú thích cũ *"`seat_level_1..3` KHÔNG theo hồ sơ"* đã hết đúng từ S4-SEAT 2026-09-23: ba ghế đã khai ở
    // `SettingsCatalogClusterNav.KEYS` nên vào ảnh chụp, và `SeatComfortApplier.applyNow` ở trên áp cả bốn.)
}

/**
 * Chạy một applier, **ghi log khi nó ném**, và đi tiếp.
 *
 * ## Vì sao bắt ở đây chứ không để nó nổ lên
 * Mọi applier trong [reapplyAll] đều chạm lớp ngoài app (dịch vụ nền, tiến trình `:wake`). Chúng **được phép hỏng**
 * (quyền vừa bị thu hồi, dịch vụ không bật được từ nền). Để một cái ném ra ngoài thì `switchProfile` chết giữa chừng: hồ sơ đã
 * đổi trên đĩa, ảnh chụp đã áp, nhưng những applier **sau nó** không bao giờ chạy — tức người dùng nhận một nửa
 * cấu hình mà không ai nói gì.
 *
 * Đây KHÔNG phải `catch (e: Exception) {}` trần mà CLAUDE.md §4.1 cấm: mỗi lượt bắt có **tên** applier trong log,
 * nên một applier hỏng luôn truy được về đúng dòng ở đây (`adb logcat -s ClusterNavReapply`).
 */
private inline fun step(name: String, block: () -> Unit) {
    runCatching(block).onFailure { Log.w("ClusterNavReapply", "applier '$name' failed during profile switch", it) }
}
