package com.byd.clusternav.launcher

import com.byd.clusternav.Prefs
import com.byd.clusternav.automation.AutomationService
import com.byd.clusternav.automation.RainDefrostApplier
import com.byd.clusternav.automation.ScheduledNavApplier
import com.byd.clusternav.launcher.automation.NavAutomationBook
import com.byd.clusternav.launcher.automation.RainDefrostChoice
import com.byd.clusternav.launcher.automation.RainDefrostStatus
import com.byd.clusternav.launcher.automation.RainGlass
import com.byd.clusternav.launcher.automation.RainStatusLine
import com.byd.clusternav.launcher.automation.RainStatusWords
import com.byd.clusternav.launcher.automation.ScheduledNavRule
import com.byd.clusternav.navAutomationRules
import com.byd.clusternav.rainDefrostChoice
import com.byd.clusternav.setRainDefrostChoice
import com.byd.clusternav.autoUpdateEnabled
import com.byd.clusternav.setAutoUpdateEnabled
import com.byd.clusternav.setNavAutomationRules

/**
 * ═══ AUTOMATION trên cầu Settings (hàm mở rộng của [ClusterNavBridge]) ═══════════════════════════════════════
 *
 * Spec `docs/specs/kachi-automation.html` R1 · R2 · R4. Cùng khuôn `ClusterNavBridgeWake`/`…Home`/`…Keys`: màn
 * Cài đặt KHÔNG ghi `Prefs.set` trực tiếp (`SettingsScreenWiringContractTest` cấm — state trên màn và state bền
 * phải đi qua MỘT cửa). Tách tệp vì `ClusterNavBridge.kt` đã 499 dòng (trần 500, CLAUDE.md §4.1).
 *
 * ⚠ **V8 (owner 2026-09-25) thêm một khoá KHÔNG phải automation vào đây**: công tắc *"Tự động cập nhật"*. Nó ở
 * cùng tệp vì nó thuộc **cùng họ**: những việc chiếc xe tự làm hộ khi mở máy (sấy khi mưa · dẫn theo lịch · dò
 * bản mới), và khoá của nó cũng nằm ở `PrefsAutomation.kt`. Mở một tệp `ClusterNavBridgeUpdate.kt` cho đúng ba
 * hàm sẽ thêm một tệp nữa phải giải trình ở `LayeringRulesTest` mà không chia được việc gì.
 *
 * ## ⚠⚠ Mỗi lượt GHI phải kèm một lượt `AutomationService.sync` — đây là phần dễ quên nhất
 * [ĐO] S4 · T2: **0 chỗ nào trong toàn dự án** đăng ký `registerOnSharedPreferenceChangeListener`. Nghĩa là ghi
 * prefs xong là *"đúng trên đĩa mà không có gì đang chạy biết"*. Với hai automation này, hậu quả cụ thể:
 *  • bật công tắc mà không `sync` ⇒ động cơ nền **không lên** tới lần nổ máy sau (người dùng kết luận nó hỏng);
 *  • tắt công tắc mà không `sync` ⇒ vòng nhịp **vẫn chạy** và còn ghi HAL sau khi đã tắt;
 *  • thêm luật đầu tiên mà không `sync` ⇒ sổ có luật nhưng không ai đánh giá nó.
 * Vì thế `sync` nằm **trong** hai setter dưới đây, không phải một bước chỗ gọi phải nhớ.
 */

/**
 * AUTOMATION #1 — kính nào được tự sấy khi mưa (theo XE, mặc định KHÔNG kính nào). Một biểu thức cho hai hàng ô
 * tích của màn Cài đặt (`choice.front` · `choice.rear`) — đúng lựa chọn hiệu lực mà động cơ nền đang dùng.
 */
fun ClusterNavBridge.rainDefrostChoice(): RainDefrostChoice = Prefs.rainDefrostChoice(app)

/**
 * kachi-automation V8 — tích/bỏ MỘT kính, kính kia giữ nguyên (owner 2026-09-30: *"tách auto này độc lập, không
 * constrain nhau"*).
 *
 * ## Thứ tự trong thân là một hợp đồng
 *  1. **Ghi prefs trước** (cả 3 khoá, `Prefs.setRainDefrostChoice`) rồi mới [RainDefrostApplier.forget]: nhịp nền
 *     chụp ký ức RỒI mới đọc lựa chọn, nên quên-sau-ghi đảm bảo một nhịp đang chạy dở hoặc đọc lựa chọn MỚI, hoặc
 *     bị bỏ commit (thế hệ đổi) — ký ức không bao giờ được ghi từ lựa chọn cũ (KDoc `RainDefrostGlasses.tick`).
 *  2. **Quên RIÊNG kính vừa đổi** (V8 · D4/D8): không ghi xe, ký ức kính kia còn nguyên. V7 gọi `reset()` xoá cả
 *     hai ⇒ bỏ tích "sau" làm Kachi quên luôn cái sấy trước nó đang giữ, hết mưa không tắt hộ được.
 *  3. [RainDefrostApplier.requestSoon] rồi [AutomationService.sync] (xem ⚠ ở KDoc tệp): engine đang chạy ⇒ nhịp
 *     mưa ở lượt thức kế (≤ 60 s, R-V8.5); engine đang dừng ⇒ `sync` dựng vòng mới, nhịp đầu chạy ngay; không còn
 *     kính nào (và không automation nào khác) ⇒ `sync` dừng engine.
 */
fun ClusterNavBridge.setRainDefrostGlass(glass: RainGlass, on: Boolean) {
    val after = Prefs.rainDefrostChoice(app).with(glass, on)
    Prefs.setRainDefrostChoice(app, after)
    RainDefrostApplier.forget(glass)
    RainDefrostApplier.requestSoon()
    RainDefrostApplier.logChoice(after, glass)
    AutomationService.sync(app)
}

/**
 * kachi-automation V8.1 · R-V8.7 — dòng tình trạng cho MỖI kính đang chọn (owner 30/09: app phải tự cho thấy nguyên
 * nhân, anh em chỉ cần chụp màn hình — CLAUDE.md §11).
 *
 * Chỉ đọc RAM của tiến trình (kết quả nhịp gần nhất từng kính + giờ nhịp kế — `RainDefrostApplier`, cùng tiến trình
 * với Cài đặt [ĐO manifest]) và prefs của lựa chọn — **không một lượt HAL** nên gọi được trên luồng vẽ. Luật chọn/ghép
 * chữ ở `:core` ([RainDefrostStatus]); chữ do màn Cài đặt đưa vào ([words], từ tài nguyên) cùng đồng hồ HH:mm.
 */
fun ClusterNavBridge.rainDefrostStatus(words: RainStatusWords, clock: (Long) -> String): List<RainStatusLine> =
    RainDefrostStatus.lines(rainDefrostChoice(), RainDefrostApplier::lastOf, RainDefrostApplier.nextCheckWallMs(), words, clock)

// ── V8 · TỰ CẬP NHẬT (owner 2026-09-25) ──────────────────────────────────────────────────────────

/** V8 — công tắc "Tự động cập nhật" (theo XE, mặc định TẮT). */
fun ClusterNavBridge.autoUpdate(): Boolean = Prefs.autoUpdateEnabled(app)

/**
 * Đặt công tắc V8. **Không** `AutomationService.sync`: lượt dò bản mới không chạy trong động cơ nền — nó đọc khoá
 * này ở [autoUpdateOnceIfEnabled] mỗi lần màn chính lên, nên giá trị mới ăn ngay mà không cần đánh thức gì.
 */
fun ClusterNavBridge.setAutoUpdate(on: Boolean) = Prefs.setAutoUpdateEnabled(app, on)

/**
 * V8 — lượt dò bản mới TỰ ĐỘNG, gọi từ `KachiHomeActivity.onResume`. Im lặng khi đã ở bản mới nhất.
 *
 * ## Ba cổng, mỗi cổng một lý do
 *  1. **công tắc TẮT ⇒ không làm gì** — mặc định TẮT, và nó mở một kết nối ra Internet;
 *  2. **đúng MỘT lần cho mỗi tiến trình** ([checkedThisProcess]) — `onResume` chạy lại mỗi lần người lái đóng một
 *     app toàn màn, tức hàng chục lần một chuyến; không có cổng này thì mỗi lần quay về HOME là một lượt tải
 *     `apk/` từ GitHub, và nếu **có** bản mới thì hộp thoại *"cài đè?"* dựng lại mỗi lần người ta bấm "Để sau";
 *  3. **đi qua [ClusterNavBridge.checkUpdate]** (không gọi `UpdateFlow.start` trực tiếp) — nó là chỗ DUY NHẤT
 *     biết lấy `Activity` ở đâu, và `UpdateFlow` bắt buộc phải có `Activity` thật (dialog + `startActivity` cài
 *     APK). Mở đường thứ hai tới `UpdateFlow` là mở đường thứ hai giữ Activity.
 *
 * "Im lặng" = bỏ chuỗi trạng thái (`onText` no-op): hai nhánh *đang kiểm* và *đã mới nhất* không có gì để nói với
 * người lái. Nhánh **có bản mới** vẫn hỏi — tải ~40 MB rồi cài đè + khởi động lại app **không** phải việc được
 * làm sau lưng chủ xe (đó cũng là hợp đồng của `UpdateFlow.confirm`, giữ nguyên).
 *
 * ⚠ Cờ nằm ở **companion của tiến trình** (`@Volatile` trong [AutoUpdateOnce]) chứ không phải một field của cầu:
 * cầu được dựng lại mỗi lần Activity dựng lại (đổi ngôn ngữ ⇒ `recreate()`), mà *"đã kiểm chưa"* là câu hỏi của
 * **tiến trình**. Để nó theo cầu thì mỗi lượt `recreate` lại là một lượt tải mới.
 */
fun ClusterNavBridge.autoUpdateOnceIfEnabled() {
    if (!Prefs.autoUpdateEnabled(app)) return
    if (!AutoUpdateOnce.claim()) return
    checkUpdate { /* im lặng: không có bề mặt nào để hiện chuỗi trạng thái ở màn chính */ }
}

/** Cờ *"tiến trình này đã dò bản mới chưa"* — xem KDoc [autoUpdateOnceIfEnabled]. */
private object AutoUpdateOnce {
    @Volatile private var done = false

    /** `true` đúng MỘT lần cho mỗi tiến trình. `synchronized` vì `onResume` của hai màn có thể chen nhau. */
    fun claim(): Boolean = synchronized(this) {
        if (done) false else { done = true; true }
    }
}

// Android box B2 · W2b (2026-10-09): mọi cửa đọc/ghi camera (xi-nhan · dải · góc · nắn · kiểu hình · thu phóng) gỡ cùng
// camera BYD. Khoá prefs cũ còn trên đĩa; phạm vi hồ sơ giữ tên ở `RetiredCameraKeys` tới đợt dọn W4.

/** AUTOMATION #2 — sổ luật dẫn-đường-theo-lịch, đã giải mã (rỗng = chưa có luật nào). */
fun ClusterNavBridge.navRules(): List<ScheduledNavRule> =
    NavAutomationBook.decode(Prefs.navAutomationRules(app))

/**
 * Ghi **cả sổ** luật đã chốt, rồi dọn dấu đã-dẫn mồ côi + đồng bộ động cơ.
 *
 * Một cổng nhận cả danh sách (không phải cặp `onAdd`/`onDelete`) vì phép thêm/sửa/xoá là hàm **thuần** ở `:core`
 * ([NavAutomationBook.upsert]/[NavAutomationBook.remove]) — cùng khuôn `onSavedPlaces`. Hai đường ghi cho cùng
 * một bảng là chỗ để hai đường lệch nhau (bài học `unitPrefs` ×4 bản).
 *
 * `pruneFired` chạy **sau** lượt ghi: `NavAutomationBook.newId` cấp lại `id` đã rảnh, nên một dấu đã-dẫn mồ côi
 * (`r1=<hôm nay>` sau khi xoá `r1`) sẽ đóng dấu cho **luật mới vừa tạo** ⇒ luật ấy không chạy hôm nay, im lặng.
 */
fun ClusterNavBridge.setNavRules(rules: List<ScheduledNavRule>) {
    Prefs.setNavAutomationRules(app, NavAutomationBook.encode(rules))
    ScheduledNavApplier.pruneFired(app)
    AutomationService.sync(app)
}

/**
 * Bật/tắt NHANH một lịch (owner 2026-09-24) — user active/inactive tuỳ trường hợp mà không phải mở hộp Sửa.
 * Đi qua [NavAutomationBook.setEnabled] (thuần) rồi [setNavRules] (cổng ghi CHUNG — không mở đường ghi thứ hai).
 */
fun ClusterNavBridge.setNavRuleEnabled(id: String, enabled: Boolean) =
    setNavRules(NavAutomationBook.setEnabled(navRules(), id, enabled))
