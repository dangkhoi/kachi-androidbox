package com.byd.clusternav.launcher


/**
 * ═══ TỪ VỰNG của [ClusterNavBridge] — mã thông điệp, KHÔNG phải câu chữ ═════════════════════════
 *
 * ## Vì sao cầu không mang chữ
 * Tầng `launcher/` có luật cứng: **mọi chữ trên màn phải đi qua tài nguyên** (`strings_kachi.xml` +
 * `values-en/`), canh bởi `LauncherI18nContractTest`. Nhánh ClusterNav thì ngược lại — nó đặt nhãn
 * lúc chạy bằng `Lang.t(vi, en)` vì `res/values/strings.xml` đang **byte-seal** (T11) nên không thêm
 * khoá vào đó được.
 *
 * Cầu nằm ở giữa hai luật, nên nó **không chọn bên nào**: nó chỉ trả về **mã** ([BridgeMsg]) và
 * **dữ liệu thô** (tên gói, mã phím, trạng thái `:core`). Tầng Settings dịch mã sang câu bằng tài
 * nguyên của launcher. Nhờ vậy cầu không giữ một chuỗi ngôn ngữ nào, và khi màn cũ bị gỡ (OQ1) thì
 * không có câu nào phải dời chỗ.
 *
 * ## KDoc = hợp đồng dịch
 *
 * ⚠ **2026-09-13 — màn cũ đã GỠ HẲN** (`docs/specs/kachi-remove-legacy-screen.html` R1/R3). Mọi chỉ dẫn
 * `MainActivity.kt:<dòng>` dưới đây là **vết lịch sử**, không phải một tệp còn đọc được: chúng trỏ vào bản trước
 * commit gỡ màn (tra bằng `git log -- app/src/main/java/com/byd/clusternav/MainActivity.kt`). Giữ số dòng vì đó là
 * cách duy nhất còn lại để so hành vi của cầu với bản gốc; cầu nay là **nguồn duy nhất** của những hành vi đó.
 * Mỗi giá trị dưới đây ghi **nguyên văn VI/EN của màn cũ** kèm dòng gốc. T4 chép đúng hai câu đó vào
 * `strings_kachi.xml` / `values-en/strings_kachi.xml` — hai màn phải nói **cùng một lời**, nếu không
 * người dùng thấy hai câu khác nhau cho cùng một việc.
 */
enum class BridgeMsg {
    // Android box B2 · W2c/W2d — mã thông điệp của dẫn đường cụm (cấp quyền thông báo · kết nối lại) và chiếu cụm (bật/tắt ·
    // trả app · cứu hộ cụm) gỡ cùng mã của chúng.

    // ── Phím vô-lăng ────────────────────────────────────────────────────────────────────────────

    /** VI "Đang bật dịch vụ Hỗ trợ…" · EN "Enabling accessibility service…" — `MainActivity.kt:848`. */
    ENABLING_ACCESSIBILITY,

    /**
     * VI "Đã bật. Bấm nút đã gán để mở app." · EN "Enabled. Press the mapped button to open the app."
     * — `MainActivity.kt:852`.
     */
    ACCESSIBILITY_ENABLED,

    /**
     * VI "Chưa bật được Hỗ trợ — bấm Allow USB debugging trên xe rồi thử lại, hoặc bật tay ở Cài đặt >
     * Hỗ trợ." · EN "Couldn't enable accessibility — tap Allow USB debugging on the car and retry, or
     * enable it in Settings > Accessibility." — `MainActivity.kt:853`.
     */
    ACCESSIBILITY_FAILED,

    /**
     * dadb CHẠY nhưng service chưa BIND — KHÔNG phải lỗi USB debugging.
     *
     * ⚠ ĐÍNH CHÍNH 2026-09-28: câu này (và KDoc này) từng quy cho "xe tải cao". [ĐO xe 2026-09-28] xe đứng yên,
     * tiến trình launcher sống liên tục 10 g 13 ph, nguyên nhân là lỗ hổng framework (`serviceDisconnectedLocked`
     * park ngược vào `mBindingServices`). Ca KẸT đó nay đi nhánh [ACCESSIBILITY_RESTARTING]; mã này còn lại cho
     * ca "chưa bind nhưng KHÔNG kẹt" — thử lại vài giây là ăn (CLAUDE.md §2: cấm trộn cơ chế với quy kết).
     */
    ACCESSIBILITY_NOT_BOUND,

    /** Đã phát hiện mối nối KẸT và đang tự chữa: giao diện sẽ khởi động lại một nhịp. */
    ACCESSIBILITY_RESTARTING,

    /** VI "Đang kiểm tra…" · EN "Checking…" — `MainActivity.kt:873`. */
    CHECKING,

    /** VI "Phím-thoại đã sẵn sàng." · EN "Voice key ready." — `MainActivity.kt:880`. */
    VOICE_KEY_READY,

    /** VI "Đã xoá gán" · EN "Binding removed" — `MainActivity.kt:798`. */
    BINDING_REMOVED,

    /**
     * VI "Đã lưu nút. Chọn app rồi bấm “Thêm gán”." · EN "Button saved. Pick an app, then tap
     * “Add binding”." — `MainActivity.kt:824–827`.
     */
    BUTTON_SAVED,

    /** VI "Đã xoá nút" · EN "Button removed" — `MainActivity.kt:903`. */
    BUTTON_REMOVED,

    /**
     * VI "Giữ màn hình này mở rồi bấm nút vật lý muốn dùng…" · EN "Keep this screen open, then press
     * the physical button…" — `MainActivity.kt:913`.
     */
    LEARN_PRESS_BUTTON,

    /**
     * VI "Đang đặt Gemini làm trợ lý hệ thống…" · EN "Setting Gemini as system assistant…"
     * — `MainActivity.kt:966`.
     */
    SETTING_GEMINI_ASSISTANT,

    /**
     * VI "Đã đặt trợ lý = Google/Gemini. Giữ nút mic để NÓI (không mở app)." · EN "Assistant set to
     * Google/Gemini. Long-press mic to TALK (not open app)." — `MainActivity.kt:971`.
     */
    GEMINI_ASSISTANT_SET,

    /**
     * Màn cũ hiện **nguyên chuỗi lỗi** mà `AssistantLauncher.setSystemAssistant` trả về
     * (`MainActivity.kt:972`). Chuỗi đó do module khác dựng bằng `Lang.t`, cầu KHÔNG mang nó qua —
     * xem `ClusterNavBridgeKeys.addBinding`: bridge báo mã này và ghi chuỗi gốc vào `Log.w` để còn
     * grep được trên xe. VI "Chưa đặt được trợ lý hệ thống." · EN "Couldn't set the system assistant."
     */
    GEMINI_ASSISTANT_FAILED,

    // ── Hệ thống ─────────────────────────────────────────────────────────────────────────────────
    // Android box B2 · W2e — `CLEANING_AIR` (nút *Lọc ngay* bụi mịn PM2.5) gỡ cùng tiện nghi xe.

    /**
     * Không có Activity để chạy luồng cập nhật (xem `ClusterNavBridge.checkUpdate`).
     * VI "Mở màn hình để kiểm tra cập nhật." · EN "Open the screen to check for updates."
     */
    UPDATE_NEEDS_SCREEN,

    // 2.93 wave 2C · DIAG-BRIDGE-DEAD-OPENERS: `SCREEN_OPEN_FAILED` (chỉ hàm `launch` của hai cửa chẩn đoán 0 chỗ gọi dùng) đã
    // gỡ cùng chuỗi `kachi_bridge_screen_open_failed` ở 5 thứ tiếng — xem `ClusterNavBridgeSystem.kt`.
}

/**
 * Ba trạng thái của dòng chữ phím-thoại — lặp lại `MainActivity.kt:928–945`, nhưng trả **mã**:
 *  - [OFF] VI "Phím-thoại: đang tắt" · EN "Voice key: off" (xám);
 *  - [ACTIVE] VI "Phím-thoại: ĐANG HOẠT ĐỘNG ✓" · EN "Voice key: ACTIVE ✓" (xanh);
 *  - [DISCONNECTED] VI "Phím-thoại: MẤT KẾT NỐI — bấm Sửa ngay" · EN "Voice key: DISCONNECTED — tap
 *    Fix now" (đỏ).
 */
enum class VoiceKeyStatus { OFF, ACTIVE, DISCONNECTED }

/**
 * Một mục trong danh sách NÚT (preset + nút tự học).
 *
 * [customName] = tên **người dùng tự đặt** lúc học phím (`Prefs.voiceKeyCustomButtons`) — chuỗi của
 * chính họ, không phải chữ của dự án nên không dịch. `null` ⇒ đây là **preset**: tầng Settings tra
 * tên theo [code] trong tài nguyên (bảng preset gốc ở `MainActivity.kt:707–717`).
 *
 * (Trường nguồn 2.88 — núm bệ giữa / vô-lăng BYD — gỡ ở Android box B2 · W2f.)
 */
data class ButtonOption(
    val code: Int,
    val customName: String? = null,
) {
    val isPreset: Boolean get() = customName == null
}

/**
 * 2.93 · KEY-LABEL-PRESET-SHADOW — mục NÚT mang nhãn cho một dòng gán (mã phím), `null` = không mục nào khớp.
 *
 * [ĐO máy ảo QA 04/10] học phím 88 đặt tên *"MEDIA PREVIOUS"* ⇒ dòng gán hiện *"Bài trước (PREVIOUS · 88)"*: danh sách là
 * preset TRƯỚC + nút tự học SAU ([buttonOptions]) và phép tra cũ lấy mục khớp ĐẦU TIÊN ⇒ preset cùng mã che tên người dùng
 * tự đặt. Luật: trong các mục khớp, nút TỰ HỌC thắng preset (tên người dùng đặt là thứ họ nhận ra); nhiều nút tự học cùng
 * mã ⇒ mục học trước (thứ tự danh sách giữ nguyên). Không ẩn preset khỏi hộp chọn — chọn bản nào cũng ra CÙNG một dòng gán,
 * nên chỉ phép tra nhãn phải đổi. Bài: `ButtonOptionLabelTest`.
 */
fun List<ButtonOption>.labelOwner(code: Int): ButtonOption? {
    val hits = filter { it.code == code }
    return hits.firstOrNull { !it.isPreset } ?: hits.firstOrNull()
}

/**
 * Một ĐÍCH gán được cho phím.
 *
 * [spec] là chuỗi bền ghi vào `voicekey_bindings`: tên gói, hoặc một trong ba sentinel
 * `Prefs.VK_TARGET_ASSIST` / `VK_TARGET_GEMINI_KEY` / `VK_TARGET_RECOGNIZER`. [appLabel] là tên ứng
 * dụng do **hệ thống** dịch (`PackageManager`); `null` ⇒ sentinel, tầng Settings tra tên trong tài
 * nguyên theo [spec].
 */
data class TargetOption(val spec: String, val appLabel: String? = null)
