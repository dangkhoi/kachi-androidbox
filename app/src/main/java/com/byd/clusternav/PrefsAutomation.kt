package com.byd.clusternav

import android.content.Context
import com.byd.clusternav.launcher.automation.RainDefrostChoice

/**
 * ═══ Khoá của hai AUTOMATION — tách khỏi [Prefs] theo VAI (1.85, trần 500 dòng) ═══════════════════════════════
 *
 * Spec `docs/specs/kachi-automation.html` R1 · R2 · R5. Cùng cách [PrefsVoiceV3] / `PrefsInputd` tách nhóm: hàm
 * mở rộng của [Prefs], **cùng tệp `clusternav_prefs`** (mở tệp thứ hai là dựng cửa thứ hai vào cùng chỗ lưu —
 * thứ [com.byd.clusternav.launcher.SettingsCatalog.PREFS_FILES] sinh ra để bắt).
 *
 * ## Cả ba khoá theo XE (`ProfileScope.DEVICE_KEYS`)
 * Automation là việc của **chiếc xe** (*"khi mưa thì sấy kính"*, *"7–9h thứ Hai thì dẫn đến công ty"*), không
 * phải sở thích đi theo người lái — cùng họ `cast_enabled` / `voice_wake_enabled`. Hệ quả **phải biết**: sổ địa
 * chỉ thì theo **hồ sơ** (`<hồ sơ>__saved_places`), nên một luật trỏ tới mục không có trong hồ sơ đang dùng sẽ
 * **bỏ lượt** (`ScheduledNavApplier.launch` ghi log rồi thôi) — degrade an toàn, không nổ, và không đóng dấu
 * đã-dẫn nên đổi lại hồ sơ trong khung giờ thì lượt đi vẫn còn.
 */

/**
 * `internal` (không `private`) từ 2.74: tệp prefs khác của cùng tệp `clusternav_prefs` dùng lại đúng hàm này.
 *
 * Mở một accessor thứ hai ở tệp kia sẽ chép **tên tệp prefs** lần thứ hai — đúng "cửa thứ hai vào cùng chỗ lưu" mà
 * KDoc trên cảnh báo, và bản chép ấy sẽ lệch vào đúng lần ai đó đổi tên tệp. Một hàm, một literal.
 */
internal fun autoPrefs(ctx: Context) =
    ctx.applicationContext.getSharedPreferences("clusternav_prefs", Context.MODE_PRIVATE)

// ── AUTOMATION #1 · Tự sấy kính khi mưa (R1 · §V8 hai kính độc lập) ─────────────────────────────
// MẶC ĐỊNH TẮT — cài mới KHÔNG đọc cảm biến, KHÔNG đụng nút sấy tới khi owner tự tích một kính. Có kính được
// chọn ⇒ `AutomationService` đọc `SETTING_FRONT_RAIN_WIPER_SPEED` ([ĐO xe 2026-09-20]: 1 khô / ≥2 mưa) mỗi ~5
// phút và bật/tắt TỪNG kính đã chọn, độc lập (`RainDefrostGlasses`).
// ⚠ Ký ức *"sấy này của tôi"* là cờ RAM trong `RainDefrostApplier`, KHÔNG ở đây — lý do đầy đủ ở KDoc
// `RainDefrostState` (nổ máy lại thì quên là hướng sai AN TOÀN).
//
// ## Ba khoá, hai ô (kachi-automation V8 · D2)
// `rain_defrost_enabled` là công tắc CHÍNH của 1.85/V7 (mặc định false); `rain_defrost_front`/`_rear` là hai ô
// con của V7 (mặc định true = hành vi 1.85 "bật cả hai"). V8 bỏ công tắc chính khỏi giao diện nhưng GIỮ khoá:
// lựa chọn hiệu lực = `enabled && con` (`RainDefrostChoice.fromKeys`) ⇒ không khoá mới, không bước di trú, hạ cấp
// về V7 vẫn đúng nghĩa. Hai ô con là hai khoá riêng, KHÔNG một khoá 3 giá trị ("front"/"rear"/"both"): chuỗi ba
// giá trị sinh ra trạng thái thứ tư không ai định nghĩa khi prefs bị sửa tay (`prefs_set` trên xe).
private const val K_RAIN_DEFROST = "rain_defrost_enabled"
private const val K_RAIN_DEFROST_FRONT = "rain_defrost_front"
private const val K_RAIN_DEFROST_REAR = "rain_defrost_rear"

/**
 * AUTOMATION #1 — kính nào được tự sấy khi mưa (lựa chọn HIỆU LỰC). Cổng ĐỌC duy nhất của ba khoá: cả động cơ
 * (`RainDefrostApplier.choice`, `AutomationService.anyEnabled`) lẫn màn Cài đặt (qua cầu) đều hỏi đây.
 */
fun Prefs.rainDefrostChoice(ctx: Context): RainDefrostChoice {
    val p = autoPrefs(ctx)
    return RainDefrostChoice.fromKeys(
        enabled = p.getBoolean(K_RAIN_DEFROST, false),
        front = p.getBoolean(K_RAIN_DEFROST_FRONT, true),
        rear = p.getBoolean(K_RAIN_DEFROST_REAR, true),
    )
}

/**
 * Ghi lựa chọn: CẢ BA khoá trong MỘT `edit()` (V8 · D2) — ghi lẻ `enabled` sẽ làm ô con cũ `true` "sống lại" (cài
 * mới là `(false, true, true)`: tích "sau" chỉ bằng cách bật `enabled` ⇒ kính trước cũng chạy). Chỗ gọi phải
 * `AutomationService.sync` sau khi ghi (xem `ClusterNavBridge.setRainDefrostGlass`).
 */
fun Prefs.setRainDefrostChoice(ctx: Context, choice: RainDefrostChoice) {
    val k = choice.toKeys()
    autoPrefs(ctx).edit()
        .putBoolean(K_RAIN_DEFROST, k.enabled)
        .putBoolean(K_RAIN_DEFROST_FRONT, k.front)
        .putBoolean(K_RAIN_DEFROST_REAR, k.rear)
        .apply()
}

// ── V8 (owner 2026-09-25) — TỰ CẬP NHẬT khi mở app ───────────────────────────────────────────────
// Owner: *"tách auto-update thành 1 toggle riêng ở Hệ thống, KHÔNG gắn với Nav+HUD"*. Trước V8 lượt dò bản mới
// chỉ đi kèm đường Nav+HUD / nút bấm tay, tức ai tắt dẫn đường thì không bao giờ được cập nhật mà không có gì
// nói ra điều đó.
//
// MẶC ĐỊNH TẮT: nó mở một kết nối HTTPS ra GitHub mỗi lần mở launcher và có thể dựng hộp thoại *"cài bản mới?"*
// trước mặt người đang lái. Một tính năng tự-tải-về-rồi-cài-đè phải do chủ xe bật tường minh — cùng lẽ
// `rain_defrost_enabled` / `voice_wake_enabled` mặc định TẮT.
private const val K_AUTO_UPDATE = "auto_update_enabled"

/** V8 — "Tự động cập nhật": mở launcher thì tự dò bản mới trong `apk/`. Mặc định **false**. */
fun Prefs.autoUpdateEnabled(ctx: Context): Boolean = autoPrefs(ctx).getBoolean(K_AUTO_UPDATE, false)

/** Xem [autoUpdateEnabled]. Không có tác dụng phụ nào phải đồng bộ: lượt dò đọc khoá này mỗi lần màn chính lên. */
fun Prefs.setAutoUpdateEnabled(ctx: Context, v: Boolean) =
    autoPrefs(ctx).edit().putBoolean(K_AUTO_UPDATE, v).apply()

// ── CAMERA theo xi-nhan (owner 2026-09-22) — mặc định TẮT ("đang phát triển") ────────────────────
// Android box B2 · W2b (2026-10-09): mọi khoá camera (xi-nhan · cụm · góc · xoay · lật · dải · nắn · kết xuất) gỡ cùng camera
// BYD — mã đọc/ghi không còn; khoá cũ trên đĩa vô hại, phạm vi hồ sơ giữ tên tới đợt dọn W4.

// ── AUTOMATION #2 · Tự dẫn đường theo lịch (R2) ───────────────────────────────────────────────────
// Hai khoá, hai VAI khác nhau — cố ý KHÔNG gộp:
//  • `nav_automation_rules` = CẤU HÌNH (sổ luật người dùng đặt trong Cài đặt › Dẫn đường), mã hoá bởi
//    `NavAutomationBook` (`:core`); rỗng = chưa có luật nào ⇒ engine không làm gì.
//  • `nav_automation_fired` = TRẠNG THÁI CHẠY (`id luật` → ngày đã dẫn, `NavAutomationFired`), thứ thi hành luật
//    "1 lần / khung / ngày" (R2.4). Nó KHÔNG phải cấu hình ⇒ khai ở `SettingsCatalog.NOT_SETTINGS`.
// Gộp hai vai vào một khoá thì một lượt SỬA luật sẽ xoá sạch dấu đã-dẫn: sửa giờ lúc 8h05 ⇒ dẫn lại ngay lần thứ
// hai, ngay trước mặt người đang lái.
private const val K_NAV_AUTOMATION = "nav_automation_rules"
private const val K_NAV_AUTOMATION_FIRED = "nav_automation_fired"

/** AUTOMATION #2 — sổ luật, dạng chuỗi của `NavAutomationBook.encode`. Rỗng = chưa có luật nào. */
fun Prefs.navAutomationRules(ctx: Context): String =
    autoPrefs(ctx).getString(K_NAV_AUTOMATION, "").orEmpty()

/** Xem [navAutomationRules]. Nhận chuỗi ĐÃ mã hoá — phép thêm/sửa/xoá là hàm thuần ở `:core`. */
fun Prefs.setNavAutomationRules(ctx: Context, encoded: String) =
    autoPrefs(ctx).edit().putString(K_NAV_AUTOMATION, encoded).apply()

/** Sổ ĐÃ-DẪN (`id=ngày`), dạng chuỗi của `NavAutomationFired.encode`. Rỗng = chưa dẫn lần nào. */
fun Prefs.navAutomationFired(ctx: Context): String =
    autoPrefs(ctx).getString(K_NAV_AUTOMATION_FIRED, "").orEmpty()

/** Xem [navAutomationFired]. */
fun Prefs.setNavAutomationFired(ctx: Context, encoded: String) =
    autoPrefs(ctx).edit().putString(K_NAV_AUTOMATION_FIRED, encoded).apply()
