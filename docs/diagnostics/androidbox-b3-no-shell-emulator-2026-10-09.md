# B3 — Kachi Android box khi KHÔNG có kênh shell: QA máy ảo (2026-10-09)

**Trạng thái**: Current · **Cập nhật**: 2026-10-10 (lượt 3) · **Mục đích**: bằng chứng QA cho spec `docs/specs/androidbox-plan.html` §4.2 (B3) — mọi quyền thiếu có đường tự cấp bằng tay, ô app có lối mở toàn màn, khi không có adb mạng.

## Môi trường
- Máy ảo RIÊNG `emulator-5560` (AVD `kachi_box`, Android 10 / API 29, 1280×720), gói `com.kachi.box` 1.0 (1) bản debug dựng từ cây làm việc B3.
- Giả lập "box không có adb mạng": `adb -s emulator-5560 reverse --remove tcp:5555` (bỏ lối `localhost:5555` → adbd của máy chủ) · `pm clear com.kachi.box` · `settings put secure enabled_accessibility_services null` · `cmd notification disallow_listener com.kachi.box/com.byd.clusternav.NavNotificationListener` · `appops set com.kachi.box SYSTEM_ALERT_WINDOW default` · `pm revoke … RECORD_AUDIO` / `ACCESS_FINE_LOCATION` · `svc power stayon true`.
- Đọc kết quả bằng `uiautomator dump` + `dumpsys activity activities` (`mResumedActivity`) + `dumpsys accessibility` + `settings get` + `logcat -s KachiReady Preflight KachiNoShell`. Không đọc ảnh chụp.
- Xong: `adb -s emulator-5560 reverse tcp:5555 tcp:5561` đã TRẢ LẠI ([ĐO] `reverse --list` → `host-15 tcp:5555 tcp:5561`; mở lại Kachi ⇒ `KachiReady: up src=f4`, Preflight tự cấp trợ năng như cũ).

## Kết quả [ĐO]

| # | Bước | Kết quả |
|---|------|---------|
| 1 | Mở Kachi lần đầu (sau hộp Miễn trừ) | `KachiReady: env PORT_CLOSED src=f4` · `Preflight: thiếu: shell, freeform, default_home, overlay, notif_listener, accessibility, microphone, location` · `card show variant=ENVIRONMENT`. Thẻ: *"The device's debugging port (adb) is not open, so apps cannot go into a slot. Open the app full screen, or turn on debugging and tap Retry."* (Later · Retry — thẻ tự hiện không gắn ô ⇒ không có nút mở toàn màn, đúng thiết kế). |
| 2 | Cài đặt › Hệ thống & quyền | Mỗi hàng thiếu: *"No control channel for Kachi to grant this itself — turn it on in the device settings"* (trước B3 hàng này nói *"Kachi đang tự xin lại"* — sai khi không có kênh) + nút *Open system settings* / *Allow*. *Freeform windows* không có nút (cờ Global, chỉ shell ghi). |
| 3 | Vẽ trên màn khác → nút | `mResumedActivity = com.android.settings/.Settings$AppDrawOverlaySettingsActivity`; bật ⇒ `appops … SYSTEM_ALERT_WINDOW: allow`; Back ⇒ về Kachi, hàng biến mất (trang tự đọc lại). |
| 4 | Đọc thông báo → nút | `NotificationAccessSettingsActivity`; bật + Allow ⇒ `enabled_notification_listeners` có `com.kachi.box/com.byd.clusternav.NavNotificationListener`; hàng biến mất. |
| 5 | Trợ năng → nút | `AccessibilitySettingsActivity` → *Kachi — physical keys* → Allow ⇒ `enabled_accessibility_services = com.kachi.box/…NavAccessibilityService`, `dumpsys accessibility` *Bound services: {Kachi — physical keys}*; hàng biến mất. |
| 6 | Micro → *Allow* | Hộp hệ thống `GrantPermissionsActivity` *"Allow Kachi to record audio?"* → Allow ⇒ `RECORD_AUDIO: granted=true`; hàng biến mất (`onRequestPermissionsResult`). |
| 7 | Định vị → *Allow* | *"Allow Kachi to access this device's location?"* → *only while using the app* ⇒ `ACCESS_FINE_LOCATION: granted=true`; hàng biến mất. |
| 8 | Là màn hình chính → nút | Trước: alias `.launcher.KachiHome` TẮT. Bấm ⇒ `enabledComponents: com.byd.clusternav.launcher.KachiHome` + `DefaultAppActivity` *"Default home app"* liệt kê **Kachi** + Pixel Launcher; chọn Kachi ⇒ `resolve-activity HOME → com.kachi.box/com.byd.clusternav.launcher.KachiHome`. |
| 9 | Nút *Set Kachi as home screen* (khối Màn hình chính) khi không kênh | Mở cùng màn `DefaultAppActivity` (không thẻ "gỡ lỗi USB" như trước). Câu kết quả bị lượt dựng lại trang lúc quay về xoá (trang hiện trạng thái mới) — chấp nhận. |
| 10 | Gán app vào ô 1 (cầu kiểm thử `slot n=1 pkg=com.android.settings`) | `KachiFloat: … chưa có bộ chiếu → không mở cửa sổ nổi` · `card show variant=ENVIRONMENT`; ô hiện icon + *Settings* + *No control channel*; thẻ có **Later · Open full screen · Retry**. *Open full screen* ⇒ `mResumedActivity = com.android.settings/.Settings` (toàn màn). |
| 11 | Kênh điều khiển → nút (Tuỳ chọn nhà phát triển TẮT trên máy ảo, `development_settings_enabled = null`) | Lượt đầu: hệ thống chuyển `DevelopmentSettingsDisabledActivity` rồi đóng ngay — người dùng không thấy gì ⇒ **vá**: đọc `DEVELOPMENT_SETTINGS_ENABLED`, tắt ⇒ toast *"Developer options are off — on the About screen tap Build number 7 times…"* + mở `MyDeviceInfoActivity` [ĐO sau vá]. |
| 12 | Ổn định | Tiến trình Kachi sống suốt bước 1–10 (một pid), 0 dòng `FATAL`/`ANR` của Kachi. |

## Chưa đo được trên máy ảo này
- **Không micro**: máy ảo có `feature:android.hardware.microphone` ⇒ ẩn nút mic / ô *Nói với Kachi* / nhóm Giọng nói / Hey Kachi / nạp sẵn chỉ khoá bằng test (`NoShellFallbackTest`, `NoShellWiringContractTest`). Cần một box không micro (hoặc AVD `hw.audioInput=no` — [CHƯA BIẾT] có bỏ feature không).
- **OTA qua trình cài hệ thống**: kênh `dangkhoi/kachi-androidbox` chưa có `Kachi-box-<ver>` mới hơn 1.0 ⇒ chưa có bản để tải. Luật đường (`otaRoute`) + kiểm gói (`archiveMatches`) + provider/manifest khoá bằng test; lượt cài thật = khi đăng bản 1.1.
- Một Android box thật: adb mạng có/không, màn Cài đặt có bị hãng chặn không — [CHƯA BIẾT].

## Lượt 2 — BOX-FREEFORM-NOSHELL · BOX-HOME-RESULT-LOST · BOX-KEYS-NOSHELL-TOAST (2026-10-09, tối)

Bản debug dựng từ cây làm việc (chưa commit), `install -r` lên `emulator-5560`. Đọc bằng `uiautomator dump` + `dumpsys activity activities` + `dumpsys accessibility` + `dumpsys notification` + `logcat -s KachiReady Preflight KachiNoShell NavConnect`. Không đọc ảnh.

**Có kênh** (`reverse --list` → `host-15 tcp:5555 tcp:5561`):

| # | Bước | Kết quả [ĐO] |
|---|------|--------------|
| A1 | `settings put global enable_freeform_support 0`, mở lại Cài đặt › Hệ thống & quyền | Hàng *Freeform windows enabled — affects a core feature* VẪN hiện (như cũ). |
| A2 | Phím vật lý › *Check / Fix now* | `NavConnect: accessibility đã BOUND (AccessibilityManager) → bỏ dadb` — đường dadb như cũ, không mở màn hệ thống. |
| A3 | Khối Màn hình chính | Không câu kết quả nào khi chưa bấm (dấu chờ không có). |

**Không kênh** (`reverse --remove tcp:5555` · `pm clear com.kachi.box` · `settings put secure enabled_accessibility_services null`; cờ freeform vẫn `0`):

| # | Bước | Kết quả [ĐO] |
|---|------|--------------|
| B1 | Mở Kachi | `KachiReady: env PORT_CLOSED src=f4` · `Preflight: thiếu: shell, default_home, notif_listener, accessibility, microphone, location` — **không còn `freeform`** (lượt 1 bước 1 có). |
| B2 | Hệ thống & quyền | Không hàng *Freeform windows*; mọi hàng thiếu còn lại có nút *Open system settings*. |
| B3 | *Set Kachi as home screen* → màn `DefaultAppActivity` → Back (không chọn) | Trang dựng lại lúc quay về, dưới nút: *"Not yet — Kachi is not the home screen. Tap the button again and choose Kachi in the picker."* (lượt 1 bước 9: câu bị xoá). |
| B4 | Bấm lại → chọn **Kachi** | `resolve-activity HOME → com.kachi.box/…KachiHome`; hệ thống mở Kachi ở task HOME mới (#37, task cũ #34 vẫn còn — có từ B3). Mở Cài đặt › Hệ thống: *"Kachi is the home screen."* + *"Done — press Home to return to Kachi."* |
| B5 | Đóng/mở lại Cài đặt | Câu *Done…* KHÔNG hiện lại (dấu một lần). |
| B6 | Phím vật lý › *Check / Fix now* (trợ năng chưa bật) | `KachiNoShell: mở android.settings.ACCESSIBILITY_SETTINGS` · `mResumedActivity = …AccessibilitySettingsActivity` · `Toast Queue` một bản ghi của `com.kachi.box` (chữ toast không đọc được qua dump; mã: `kachi_keys_manual`) · 0 dòng `NavConnect` (không dadb). |
| B7 | Bật *Kachi — physical keys* ở màn đó → Allow → Back ×2 | `Bound services:{… Kachi — physical keys …}`. |
| B8 | Bật công tắc *Listen to physical buttons* | *Voice key: ACTIVE ✓*, 0 dòng `NavConnect`/`KachiNoShell`, 0 toast (đã bound ⇒ không mở lại màn, không dadb). *Check / Fix now* lần nữa: y vậy. |
| B9 | `enabled_accessibility_services null` → công tắc tắt rồi bật | Mở `AccessibilitySettingsActivity` (`KachiNoShell: mở …ACCESSIBILITY_SETTINGS`), không dadb. |
| B10 | Ổn định | `logcat -b crash` 0 dòng Kachi. |

**Trả lại**: `reverse tcp:5555 tcp:5561` ([ĐO] `reverse --list` → `host-15 tcp:5555 tcp:5561`) · `enable_freeform_support 1` · mở lại Kachi ⇒ `Preflight: … sau khi tự cấp: đủ quyền`.

**Phát hiện thêm (lượt 2 không sửa — đã sửa 10/10 ở 1.1, xem Lượt 3)** [ĐO giao diện + log; nguyên nhân SUY từ mã `ShellReadinessPolicy.admit`]: lần đầu bỏ `reverse` mà KHÔNG `pm clear`, dấu duyệt `kachi_shell_approval` còn tươi ⇒ `KachiReady: env PORT_CLOSED src=early` nhưng `ShellAccessUi.usableNow()` = `ShellReadinessPolicy.usable(ENVIRONMENT, ledgerFresh = true)` = **true** ⇒ trang quyền hiện *"Kachi is re-requesting it"* + *"An environment limitation"* không nút tay, và nút Phím vật lý vẫn đi dadb. Ca thật: box từng có adb mạng rồi mất. Hàng *Freeform* thì ẩn đúng (đọc `shell != null` của màn chính).

## Lượt 3 — `BOX-STALE-LEDGER-NOSHELL` đã sửa (2026-10-10, release 1.1 (2))

`emulator-5560`, cài đè `adb install -r` bản release 1.1 (2) lên 1.0 (giữ prefs + dấu duyệt).

| # | Bước | Kết quả [ĐO] |
|---|---|---|
| C1 | `tcpip 5555` + `reverse tcp:5555 tcp:5561`, mở Kachi | `early mode=LEDGER … -> UP`, `ledger markUp … ok=true`, tự cấp trợ năng. |
| C2 | `reverse --remove tcp:5555` + `enabled_accessibility_services null` + force-stop, mở lại (dấu còn tươi) | `early mode=LEDGER try=1..3 -> PORT_CLOSED` ⇒ `env PORT_CLOSED src=early`. |
| C3 | Cài đặt › Hệ thống & quyền | 3 hàng thiếu (kênh điều khiển · màn chính · Trợ năng) đều có câu *"No control channel for Kachi to grant this itself…"* + nút *Open system settings* — hết câu *"Kachi is re-requesting it"* của lượt 2. |
| C4 | Phím vật lý › *Check / Fix now* | `KachiNoShell: mở android.settings.ACCESSIBILITY_SETTINGS`, màn trên cùng `Settings$AccessibilitySettingsActivity`, 0 dòng dadb. |
| C5 | Về HOME, trả `reverse tcp:5555 tcp:5561`, chờ 30 s | `up src=f4` (F4 dò lại 20 s) ⇒ `Preflight: sau khi tự cấp: thiếu: default_home`, trợ năng = `KachiKeyService`. |
| C6 | Ổn định | `logcat -b crash` 0 dòng Kachi. |

