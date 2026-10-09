# Project Context — Kachi Android box (luôn-bật)

> **Trạng thái**: Current · **Cập nhật**: 2026-10-09 (viết lại sau B1–B3; thay bản tóm tắt kế thừa của Kachi BYD) ·
> **Mục đích**: tóm tắt luôn-bật, *derived* từ repo. Chi tiết: `docs/README.md` (index) · `docs/PROJECT-BACKLOG.md`
> (khối ANDROIDBOX) · spec `docs/specs/androidbox-plan.html` (§9 nhật ký B1–B3). Lịch sử Kachi BYD (HAL, cụm, HUD, camera,
> firmware DiLink, 2.x): repo `dangkhoi/byd-kachi` — không áp dụng ở đây, không chép lại.

## Sản phẩm

- **Kachi** = launcher (HOME) cho ô tô dùng Android box / đầu Android bất kỳ (owner 09/10: *"loại nào cũng như nhau"*).
  Chỉ phần launcher; dữ liệu xe cắm thêm (B4) hoãn.
- `applicationId` **`com.kachi.box`** · bản **1.0 (1)** · minSdk 29 · compile/target 37 · JDK 17 · chỉ `arm64-v8a`.
  Namespace Kotlin vẫn `com.byd.clusternav.*` (đổi tên = `BOX-RENAME-PACKAGE`, chưa làm).
- Repo `dangkhoi/kachi-androidbox` (MIT), tách từ byd-launcher 2.98 `bf54415`. **Cấm** đụng `../byd-launcher/` hay đẩy lên
  `byd-kachi`.
- OTA: `apk/Kachi-box-<ver>-release.apk` trên `main` của repo này (`UpdateChecker.REPO`); khuôn BYD `Kachi-<ver>` bị bỏ qua.
  **Chưa đăng bản nào.**
- Khoá ký: dùng chung `~/.kachi` với bản BYD (OQ4) — cert SHA-256 `92:57…99:17`; `keystore.properties` gitignored.
- Gói giọng nói (Hey Kachi KWS, giọng đọc Piper) CỐ Ý tải từ `byd-kachi/voice/` (OQ5, ghim sha256). Mô hình nghe
  zipformer-vi int8 tải từ Hugging Face. VAD Silero + từ khoá KWS đóng trong APK (`app/src/main/assets/voice/`).

## Kiến trúc (3 module Gradle)

- `:core` — thuần Kotlin, cấm `android.*` (`LayeringRulesTest` + `docs/layering-rules.md`): luật bố cục/ô
  (`WorkspaceLayout`, `WorkspaceGrid` 12×6, `WorkspaceState.SLOT_CAP` = 6), hồ sơ (`ProfileScope*`, `ProfileTransfer`,
  `PrefSnapshotPlan`, `BydDeadPrefs`), giọng nói (`launcher/voice/*`: `VoiceIntent`, `VoiceIntentParser`, `VoiceGrammar`,
  `VoiceFeatureGone`), luật không kênh (`NoShellFallback`, `LauncherRequirements`), chuyến/nhạc (`launcher/trip/*`),
  lịch dẫn (`launcher/automation/*`), hệ thống (`system/*`: `StackParse`, `DisplayParse`, `HomeGate`,
  `DisplayOwnershipRegistry`, `FreeformSeedPolicy`, `PrioritySerialExecutor`).
- `:car-integration` — chỉ còn transport shell: `carexec/LocalDeviceShell.kt` · `LocalShellRetryPolicy.kt` ·
  `FirstOpenApprovalPolicy.kt` (dadb 2.0.0).
- `:app` — Android: `AppContainer` (DI tay), `KachiApplication`, màn chính `launcher/KachiHomeActivity` + `HomeViewModel`
  (UDF, `StateFlow<HomeUiState>`), `WorkspaceView` (view thuần), Cài đặt `SettingsPanel` + `SettingsSections*`.

## Bản đồ tệp chính (đã kiểm tồn tại 2026-10-09)

| Vai | Tệp |
|---|---|
| Chủ dadb `localhost:5555` duy nhất | `app/…/system/ShellTransport.kt` · cổng `system/WindowCommandDispatcher.kt` |
| Kênh sớm / sẵn sàng | `EarlyShellChannel.kt` · `ShellReadiness.kt` · `launcher/ShellChannelGate.kt` · `ShellApprovalStore.kt` |
| App trong ô (màn ảo) | `launcher/VdAppHost.kt` · `launcher/SlotLiveProbe.kt` · `launcher/ParkedApps.kt` · `launcher/LauncherWindows.kt` |
| Quyền | `launcher/PermissionPreflight.kt` · `launcher/NoShellUi.kt` · `:core LauncherRequirements` · `NoShellFallback` |
| Mở app toàn màn | `launcher/AppOpener.kt` |
| Widget | `launcher/MediaWidgetView.kt` · `PhotoWidgetView.kt` · `ShortcutIconsView.kt` · `AppWidgetSlotHost.kt` · `:core WidgetRegistry` |
| Nhạc | `launcher/MediaBridge.kt` (phiên qua `NavNotificationListener`) · `launcher/trip/TripStart.kt` |
| Phím vật lý | `modules/navaccess/NavAccessibilityService.kt` · `modules/voicekey/AssistantLauncher.kt` · `:core voicekey/VoiceKeyMatcher` |
| Trợ năng kẹt | `A11yLifecycleHeal.kt` · `NavConnect.kt` · `:core modules/navaccess/AccessibilityRebind` |
| Giọng nói | `launcher/voice/VoiceSession.kt` · `VoiceRecognizer.kt` · `VoiceModelStore.kt` · `VoiceWakeService.kt` (`:wake`) · `PiperTtsService.kt` (`:tts`) · `launcher/VoiceDispatcher.kt` |
| Lịch dẫn đường | `automation/AutomationService.kt` · `automation/ScheduledNavApplier.kt` |
| OTA | `UpdateChecker.kt` · `UpdateFlow.kt` · `OtaSystemInstall.kt` (trình cài hệ thống khi không kênh) |
| Prefs | `Prefs.kt` · `launcher/WorkspacePrefs.kt` · `launcher/PrefsWorkspaceRepository.kt` · `launcher/WorkspacePrefsBydCleanup.kt` |
| Cầu kiểm thử adb | `launcher/testbridge/KachiTestBridge.kt` (bật tay ở Cài đặt, tự tắt 60 phút) |
| Log | `launcher/KachiLog.kt` (`<ngoài>/kachi-logs/`) · `DiagStorageCap.kt` (chỉ dọn danh sách cho phép) |

## Sự thật kỹ thuật còn đúng

- **Kênh dadb loopback**: Kachi tự nối `localhost:5555` của adbd trên chính máy (uid shell, không root) để `am`/`pm`/
  `appops`/`settings`. Lần đầu adbd hỏi *"Cho phép gỡ lỗi USB?"* — F4 `ShellChannelGate` chỉ dò khi màn có tiêu điểm +
  1,5 s để hộp không bị màn Kachi đè. Dấu bền `kachi_shell_approval` cho phép nối nền ở lần sau.
- **Máy ảo QA riêng**: AVD `kachi_box` = `emulator-5560` (A10, 1280×720). Bật kênh: `adb -s emulator-5560 tcpip 5555`
  + `adb -s emulator-5560 reverse tcp:5555 tcp:5561` (5561 = cổng adb của chính máy ảo đó). Giả lập "box không adb":
  `reverse --remove tcp:5555`. Luôn `-s emulator-5560`; `svc power stayon true` trước khi bật test mode.
- **Nhúng app vào ô**: mỗi ô app = một màn ảo PRIVATE của Kachi; app mở bằng `am start --display <id>` qua shell (uid
  app thường bị chặn bởi `ActivityStackSupervisor.isCallerAllowedToLaunchOnDisplay`). Không kênh ⇒ không có ô app.
- **Trợ năng kẹt (lỗi AOSP 10)**: mối nối đứt bị park vĩnh viễn trong `mBindingServices`
  (`AccessibilityManagerService.java:4114-4117`, `:1630-1631`, r47); đường chữa duy nhất = `am force-stop` gói mình rồi
  lắp lại, lệnh chạy tách rời. Giữ `A11yLifecycleHeal` (không phải lỗi BYD). A12 xử khác (`mCrashedServices`).
- **B3 không kênh** (`NoShellFallback`): mỗi quyền thiếu có nút mở đúng màn Cài đặt hệ thống / `requestPermissions`;
  HOME = bật alias `.launcher.KachiHome` + màn chọn hệ thống; ô app có *Mở toàn màn hình*; OTA đi trình cài hệ thống
  (FileProvider `${applicationId}.ota`, kiểm gói + người ký trên luồng nền); không micro (`FEATURE_MICROPHONE`) ⇒ ẩn mọi
  lối vào giọng nói, Hey Kachi OFF. Tuỳ chọn nhà phát triển tắt ⇒ màn đó tự đóng [ĐO] ⇒ chỉ cách bật + mở Giới thiệu máy.
- **Lượt dọn prefs BYD một lần**: `BydDeadPrefs` + `WorkspacePrefsBydCleanup` ở `PrefsWorkspaceRepository.init`; dấu
  `migrated_androidbox_v1` ghi `commit()` SAU lượt dọn. Lần sau 0 chi phí.
- **Nhập `.kachi` của Kachi BYD**: `ProfileTransfer.planImport` lọc khoá trong ảnh chụp theo phạm vi, khoá lạ bỏ IM LẶNG;
  phím gán đích `ctl:`/`cam:` cũ không còn khớp (`AssistantLauncher.liveBindings`), dòng vẫn xoá được ở Cài đặt.
- Giọng nói về xe (*"bật điều hoà"*, *"mở kính"*…) ⇒ `Unknown(FEATURE_GONE)` — không bao giờ thành lệnh khác.
- "App đang chạy" = có TASK trong `am stack list`, không phải `pidof`. `am stack list` đọc hỏng ≠ trống.
- Component `NavNotificationListener` / `NavAccessibilityService` giữ TÊN (đổi ⇒ mất quyền đã cấp); thân đã mỏng: bộ
  nghe thông báo chỉ để `MediaBridge.getActiveSessions`, dịch vụ trợ năng chỉ lọc phím, không đọc cửa sổ.

## Trạng thái (2026-10-09)

- **B1 xong** (tách gói, OTA riêng) · **B2 xong** W0–W4 (gỡ toàn bộ phần BYD: commit `b4e69b1`…`0b1e248`) · **B3 xong**
  (`9a71aa4`, QA máy ảo `docs/diagnostics/androidbox-b3-no-shell-emulator-2026-10-09.md`) · **B5** README + hướng dẫn +
  trang giới thiệu viết lại (chưa commit) · **B4 hoãn** (owner).
- Test cây hiện tại [ĐO lượt soát Pass 2]: 3 844 / 0, lint 0 lỗi.
- **Chưa đăng OTA**, **chưa thử trên Android box thật**.

## Việc mở (backlog ANDROIDBOX)

- `BOX-REL-1.0` đăng bản đầu (chờ owner) · `BOX-ONDEVICE` thử box thật (adb mạng có không, máy không micro, màn Cài đặt
  có bị chặn không) · `BOX-RENAME-PACKAGE` · `BOX-FREEFORM-NOSHELL` (cờ cửa sổ tự do hiện "thiếu" không nút khi không
  kênh) · `BOX-HOME-RESULT-LOST` (câu kết quả đặt HOME mất khi trang dựng lại) · `BOX-KEYS-NOSHELL-TOAST` · `BOX-B4`.
