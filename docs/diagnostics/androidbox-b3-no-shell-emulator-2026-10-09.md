# B3 — Kachi Android box khi KHÔNG có kênh shell: QA máy ảo (2026-10-09)

**Trạng thái**: Current · **Cập nhật**: 2026-10-09 · **Mục đích**: bằng chứng QA cho spec `docs/specs/androidbox-plan.html` §4.2 (B3) — mọi quyền thiếu có đường tự cấp bằng tay, ô app có lối mở toàn màn, khi không có adb mạng.

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
