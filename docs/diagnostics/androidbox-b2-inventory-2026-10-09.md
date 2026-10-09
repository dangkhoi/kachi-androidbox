# Kiểm kê B2 — phần chỉ-BYD cần gỡ khỏi Kachi Android Box

> **Trạng thái**: Current · **Ngày**: 2026-10-09 · **Mục đích**: liệt kê mọi vùng mã chỉ dành cho xe BYD (file, LOC, lối vào, chỗ dính với phần giữ lại, test ghim) và đề xuất thứ tự gỡ theo đợt, mỗi đợt tự build/test được.
>
> Phương pháp: `find`/`wc -l`/`grep -rlw` trên cây làm việc ngày 2026-10-09 (chưa sửa mã). LOC = `wc -l` cả chú thích (mã repo này chú thích dày, LOC thực thi ≈ 40–50 %). Chỉ nghiên cứu, **chưa chạy build**. Viết tắt đường dẫn:
> `A/` = `app/src/main/java/com/byd/clusternav/` · `C/` = `core/src/main/kotlin/com/byd/clusternav/` · `CI/` = `car-integration/src/main/kotlin/com/byd/clusternav/` · `AT/`, `CT/` = thư mục test tương ứng (`app/src/test/java/…`, `core/src/test/kotlin/…`).

Tổng quy mô hiện tại: `:app` main ≈ 70 600 LOC, `:core` main ≈ 72 600 LOC, test: app 298 tệp · core 562 · car-integration 10 · vehicle-contracts 9 · offcar-planner 32.

---

## 1. Vùng chỉ-BYD → tệp, LOC, lối vào

### 1.1 Bảng tổng

| # | Vùng | Tệp chính | LOC (app / core / khác) |
|---|---|---|---|
| R1 | **Chiếu cụm (cluster cast)** + nút nổi | `A/modules/clustercast/**` (20 tệp + `simplified/SimpleCastRuntime.kt` + `T3Daemon.java`), `A/cast/platform/CastAppCatalog.kt`, `A/ui/ClusterPreviewView.kt`, `A/housekeeping/CastPrefsHousekeeping.kt`, `A/ClusterNavActivity.kt`, `A/TatMayCastHold.kt`, `A/HealCastWait.kt`, `A/launcher/ClusterNavBridgeCast*.kt`, `ClusterNavBridgeGeometry.kt`, `SettingsSectionsCast*.kt`, `SlotActionsCluster.kt`; `C/modules/clustercast/simplified/**` (41), `C/modules/clustercast/model/**`, `C/modules/navaccess/{HealCastDeferral,TatMayCastHoldPlan}.kt`, `C/launcher/{ClusterSnapshotPlan,ClusterImportSummary,ProfileScopeCluster}.kt` | 6 585 / 7 460 |
| R2 | **Camera** (xi-nhan, 360, theo yêu cầu, AVMCamera, HAL helper :19322) | `A/launcher/camera/**` (23), `A/launcher/SettingsSectionsCamera*.kt`, `ClusterNavBridgeCameraPerCam.kt`, `CameraSettingsLabels.kt`, `A/PrefsCamera{PerCam,Dewarp}.kt`, `A/launcher/testbridge/TestBridge{Camera,CameraFrame,PerCam,Synth}.kt`; `C/launcher/camera/**` (26), `C/launcher/voice/VoiceCamera{Phrases,Turn}.kt`; `hal-helper/` (`KachiHalMain.java` + stubs), `app/src/main/assets/kachi_hal.jar`, `scripts/build-hal-helper.sh` | 5 939 / 4 991 / 377 |
| R3 | **Dẫn đường lên cụm + HUD** (AMAP broadcast, `BydHal` nav frame, NLS nav) | `A/{NavRepository,NavNotifLog,NavNotifRawLog,NavLog,NavDiag,NavState,NotificationParser,AmapEmissionArbiter,AmapFrameBuilder,ClusterBroadcaster,HudMirrorController,NavigationHudOwner,NavigationSpeedSignOwner,NavSpeedLimitPusher,SpeedProvider,NlsHeal,BitmapPixelFrame}.kt`, `A/launcher/{ClusterNavBridgeHud,SettingsNavHudRows,ProfileNavNotice}.kt`; `C/navigation/**` (45) + `navigation/screencapture/**` (10), `C/launcher/ProfileNavSwitch.kt`; `CI/carexec/NlsHealShell.kt`; `assets/navopen.jar` | 2 512 / 6 636 |
| R4 | **Biển tốc độ trên cụm** + chip cảnh báo | `A/speedbadge/**` (6), `A/PrefsBadge.kt`; `C/speedbadge/BadgeLayout.kt` | 1 104 / 61 |
| R5 | **VietMap** (widget badge, bong bóng mod, autostart, tiền đề app) | `A/vietmapwidget/**` (10), `A/VietMapAutostart.kt`, `A/VietMapAutostartService.kt`, `A/VmBubbleVisibility.kt`, `A/VmBubblePlacementView.kt`, `A/VmOverlayPosition.kt`, `A/AppPrereqs.kt`; `C/vietmapwidget/**`, `C/system/AppPrereq{Plan,Read}.kt`, `C/core/FloatAppList.kt` | 2 673 / 1 020 |
| R6 | **Tiện nghi xe + tự sấy kính** | `A/comfort/**` (Seat, Pm25, Recirc + 2 view), `A/body/BodyworkControl.kt`, `A/automation/RainDefrostApplier.kt`; `C/comfort/**`, `C/body/Bodywork.kt`, `C/launcher/automation/RainDefrost*.kt` (4) | 1 322 / 1 109 |
| R7 | **HAL BYDAuto + registry nút/datum + widget xe + chip xe + đọc/ghi bằng giọng/phím** | `A/modules/hal/**` (`BydHal`, `BydHalRead`, `BydHalContentPush`), `A/launcher/{BydHalGateway,BydFeatureIds,ControlDockView*,ControlTileFactory,ControlTileState,ControlLevelBar,CarMiniView,CarImage{View,Layer,Store},CarStateLayout,TyreBoardView,DoorBoardView,GroupBoardBinder,GroupTiles,GroupTileViews,GroupTileParts,GroupFitFrame,WidgetTelemetry,ReadTile,DatumIconView,RingView,TyreRawLog,CtlJournalStore,VoiceClimateStep,VoiceControlDispatch,VoiceReadback,CapTestConsole,CapTestStore,SettingsSectionsCar,LauncherTile,TileResync}.kt`, `A/WakeOnWriteControl.kt`, `A/modules/voicekey/KeyCtlDispatch.kt`, `A/launcher/voice/ControlSentRelay.kt`, `A/launcher/testbridge/TestBridge{Ctl,CtlLog,Hal,Sweep,FeatMap,CapTest}.kt`; `C/launcher/{Telemetry*,Control{Registry,Def,Levels,Visuals,TileWrite,WriteFlow,LastSent},CarStatus*,CarData*,CarControlAdapter,CarCapabilities,CarLayout,CarStripFit,Hal*,CtlWriteJournal,CtlSafetyPolicy,WriteReleaseScheduler,ClimateAuto,Tyre{Board,Judge},GroupBoard*,CapabilityGroup*,CapabilityTest,CapabilityDots,ActionMacros,MacroExec,KeyCtl{Plan,Target},RegistryIndex,Units}.kt`, `C/launcher/voice/{VoiceControlParse,VoiceTelemetry,VoiceBareCover,VoiceHalfButton,VoiceWriteLane}.kt` | ≈ 7 800 / ≈ 9 300 |
| R8 | **Nguồn phím âm lượng** (`AUDIO_VOLUME_CTRL_MODE`) | `A/modules/voicekey/KeySourceRecorder.kt`, `A/launcher/SettingsKeySourceDetail.kt`; `C/voicekey/KeySource{Journal,Meter,Probe,Resolver}.kt`, `KeySample.kt` | 278 / 549 |
| R9 | **HomeGuard** (launcher BYD 5.7.5 giành HOME) | `A/launcher/HomeGuard.kt`; `C/launcher/HomeGuardPolicy.kt` (giữ lại `wantsKachiHome`) | 147 / 108 |
| R10 | **RE/đo xe, T10, CarExec** | `C/carexec/{CarExec*,T10*,VerdictLedger}.kt`; `CI/vehicleprobe/**` (5), `CI/carexec/{CarExecCli,CarExecShell}.kt`; module **`:vehicle-contracts`** (toàn bộ), **`:offcar-planner`** (toàn bộ); `app/src/vehicleTest/**` (`HalProbeReceiver`, `HalProbePresets`, `HudSignProbe*`); `scripts/vehicle/`, `scripts/re/`, `scripts/verify-*`, `scripts/on-car-verify.sh`, `docs/refactor-car-execution/` | core 3 202 / ci 1 101 / vc 1 781 / ocp 3 037 |
| R11 | **Màn/tài nguyên kế thừa ClusterNav** | `app/src/main/res/layout/activity_main.xml` (mồ côi, niêm phong T11), `activity_cluster_nav.xml`, 41 `res/drawable/ic_car_*.xml`, `assets/car/default-car.png`, `design/car/`, `scripts/design/gen-car.py` | — |

Tổng ước lượng gỡ: **≈ 30 000 LOC app + ≈ 33 000 LOC core + 2 module Gradle + hal-helper** (≈ 45 % mã chính).

Giữ lại, có phán đoán (ghi để owner chốt):
- **`A11yLifecycleHeal` + `C/modules/navaccess/{AccessibilityRebind,AccessibilityHealGates,A11yBindJournal,KeyReadyPlan}`** — lỗ hổng framework AOSP 10 (mối nối Hỗ trợ kẹt), không phải BYD. GIỮ, gỡ phần dính cast (`TatMayCastHold`, `HealCastWait`, `CastRestartHazard`) và rào camera `com.byd.avc` trong `AccessibilityRebind.GO_HOME_UNLESS_CAMERA`.
- **`NavNotificationListener`** — GIỮ component (MediaBridge cần nó: `A/launcher/MediaBridge.kt:166,224` gọi `getActiveSessions(ComponentName(NavNotificationListener))`), nhưng gỡ toàn bộ thân nav/VietMap/speed-sign; nên đổi tên `MediaSessionListener` ở đợt cuối (đổi tên ⇒ phải cấp lại quyền, xem §2).
- **Lịch tự dẫn đường** (`A/automation/{AutomationService,ScheduledNavApplier,GpsAvailability}.kt`, `C/launcher/automation/{ScheduledNav*,NavAutomation*}.kt`) — không chỉ-BYD (mở Google Maps/Waze tới địa chỉ đã lưu). Đề xuất GIỮ; nếu owner bỏ thì bỏ luôn quyền định vị (§4).
- **Chuyến lên xe / mở app khi bật / nhạc khi bật / YouTube phát tiếp** (`A/launcher/trip/**`, `C/launcher/trip/**`) — GIỮ, nhưng dính camera (§2.1).
- **`NlsHeal`** (gắn lại bộ nghe thông báo cho HUD) — gỡ cùng R3; Preflight vẫn tự cấp `allow_listener` cho media.

### 1.2 Lối vào (entry points) cần cắt

**AndroidManifest (`app/src/main/AndroidManifest.xml`)**

| Component | Dòng | Vùng | Xử lý |
|---|---|---|---|
| `.modules.clustercast.DiagActivity` | 97 | R1 (chẩn đoán cụm) | xoá |
| `.vietmapwidget.VietMapWidgetDiagActivity` | 102 | R5 | xoá |
| `.modules.clustercast.ClusterBlackActivity` | 128 | R1 | xoá |
| `.ClusterNavActivity` (exported) | 186 | R3 (thẻ dự phòng cụm) | xoá + `activity_cluster_nav.xml` + style `ClusterTheme` |
| `.modules.clustercast.FloatingBubbleService` | 204 | R1 | xoá |
| `.modules.clustercast.CastAutomationService` | 211 | R1 | xoá |
| `.BootSetupService` | 220 | R3/R5/R6 (lượt nổ máy "headless") | rút gọn (chỉ a11y grant + voicekey + automation nav) hoặc xoá cùng `system_headless_autostart` |
| `.VietMapAutostartService` | 230 | R5 | xoá |
| `.automation.AutomationService` | 251 | R6 (sấy) + lịch dẫn đường | giữ nếu giữ lịch dẫn đường, gỡ nhịp mưa |
| `.NavNotificationListener` | 308 | R3 (+ media) | giữ, thân rỗng |
| `.RebindReceiver` action `REBIND_WATCHDOG` | 320–326 | a11y watchdog (giữ) + `NlsHeal.onWatchdog` + bubble/cast boot (gỡ) | sửa thân |
| `.modules.navaccess.NavAccessibilityService` | 352 | phím (giữ) + đọc màn GMaps (gỡ) | giữ, sửa `res/xml/nav_accessibility_config.xml` (bỏ `packageNames` nav, `flagRetrieveInteractiveWindows`, `flagReportViewIds`, `canRetrieveWindowContent`) |
| `app/src/vehicleTest/AndroidManifest.xml`: `HudSignProbeReceiver/Activity`, `HalProbeReceiver` | — | R10 | xoá cả source set `vehicleTest` java (giữ build type nếu còn dùng) |

**`KachiApplication.onCreate` (`A/KachiApplication.kt`)**: xoá `ControlSentRelay.receiveInMain` (R7), `CameraDemandDispatch.receiveInMain` (R2), `HomeGuard.install` (R9). Giữ `ShellReadiness`, `A11yLifecycleHeal`, `VoiceEngine/VoiceVad.preload`, `YoutubeResumeSampler`, `StartupHousekeeping` (bỏ `CastPrefsHousekeeping` bên trong), `EarlyShellChannel`.

**`AppContainer` (`A/AppContainer.kt`)**: bỏ `halGateway`/`halBindingTable`/`carDemand`/`carDataAdapter`/`carData`/`carControl`/`telemetryText`/`carStatusRepository`/`forgetCarDemand`/`refreshForRead`/`readFresh` (R7), `castRuntime` (R1), `cameraSignal*` (R2); tham số khởi tạo `carGatewayInit`, `cameraSignalInit`, `releaseSchedulerInit`, `ctlJournalInit`, `carScope`.

**`BootSetupService.onStartCommand` (`A/BootSetupService.kt:34–45`)**: `NavRepository.setOutputEnabled` ×2, `NavigationSpeedSignOwner`, `VietMapAutostartService.startForBoot`, `SeatComfortApplier/Pm25FilterApplier/RecircApplier.applyOnStart`; `forcedPrefs` (`Prefs.setHud`).

**`KachiAutostart.runBoot` (`A/KachiAutostart.kt`)**: dòng 55 `HomeGuardPolicy.wantsKachiHome` (giữ hàm, chuyển sang `DefaultHome`/`WorkspacePrefs`), dòng 92–96 `logBootPlan` dùng registry castable (`LauncherBootPlan.plan(…isCastable)`).

**`RebindReceiver` (`A/RebindReceiver.kt:21,67–90`)**: `NlsHeal.onWatchdog`, `SimpleCastRuntime` boot, `CastAutomationService.recordAndEnqueue`, `startOptedInBubble`.

**`EarlyShellChannel.readyChain` (`A/EarlyShellChannel.kt:131–132`)**: `NlsHeal.onReady`, `AppPrereqs.onReady`. Giữ `BehindHomeRecovery`, `KeyReady.prepare`, `TripStart.onReady`.

**Màn chính** (`A/launcher/`):
- `KachiHomeActivity.kt:48,71,73,397` — `dock.setCarStatus`, `topStrip.refreshChips(carStatus…)`, `ensureCastBubble(bridge)`, `carControl` ×4, `cameraSignal` ×3, `forgetCarDemand`.
- `KachiHomeWiring.kt` — `carStatusRepository` ×5 (collect → `HomeViewModel`), `carDemand`, `ensureCastBubble`, `CameraDemandDispatch`.
- `KachiHomeRender.kt` — `carStatus` ×6, `setCarStatus` ×3, `cameraSignal` ×2.
- `WorkspaceView.kt` — `CarStatus` ×6, `SlotActionsCluster` (nút "chiếu ô lên cụm").
- `KachiTopStrip.kt` — `refreshChips(CarStatus…)`.
- `AppDrawer.kt:154,176,249` — `cameraSection` (5 nút camera theo yêu cầu).
- `KachiHomeSlots.kt:130`, `behind/BehindHomeRunner.kt:146`, `trip/TripStart.kt:183,397` — `ClusterProfile.resolveCached(app).cameraSignature`.
- `HomeViewModel` / `C/launcher/HomeUiState.kt:79` — trường `carStatus`.

**Cài đặt** — `C/launcher/SettingsCatalogGroups.kt` (enum `SettingsGroup`, 11 nhóm), `SettingsCatalogEntries.kt`, `SettingsCatalogClusterNav.kt`; dựng ở `A/launcher/SettingsSections.kt:64–84` (`when` không `else` ⇒ xoá enum là bắt buộc sửa ở đây).

| Nhóm | Mục gỡ | Mục giữ |
|---|---|---|
| `CAST` | **cả nhóm**: `cast_enabled cast_bubble cast_split cast_style cast_autostart cast_autostart_pkg cast_autostart_split cast_autostart_left cast_autostart_right cast_actions cast_rescue` | — |
| `CAR` | **cả nhóm**: `car_recirc_on_start car_seat_enabled car_seat_mode car_seat_levels car_pm25 car_pm25_clean car_rain_defrost` + khối camera (`SettingsSectionsCar.kt:70–77` → `SettingsSectionsCamera*.kt`, không có mục catalog riêng) | — |
| `NAV` | `nav_enabled nav_cluster_mode nav_marquee nav_reconnect badge_enabled badge_upcoming badge_alert_chip badge_size badge_center vm_bubble_enabled vm_bubble_autostart vm_bubble_pos` | `places_list places_add nav_default_app nav_automation` (đổi tên nhóm "Dẫn đường") |
| `BARS` | `bars_top_strip bars_top_strip_labels` (mọi chip là chip xe: `C/launcher/TopStrip.kt` `chip_energy/outside_temp/pm25/seat/seat_r/tyres`) | `bars_header_order bars_dock_* bars_app_shortcuts` |
| `DISPLAY` | `display_units` (`C/launcher/Units.kt` chỉ cho datum xe) | còn lại |
| `KEYS` | trang chi tiết nguồn phím (`SettingsKeySourceDetail`), đích `ctl:` / `cam:` trong `SettingsSectionsKeys.kt` | `keys_*` |
| `SYSTEM` | `system_nav_stop` (dừng dẫn đường cụm), `system_headless_autostart` (nếu gỡ `BootSetupService`) | còn lại |
| `VOICE` | các id CONFIRM của nút xe trong `voice_confirm_ids` (`A/launcher/voice/VoiceConfirmSettings.kt`) | còn lại |

**Widget** — `C/launcher/WidgetRegistry.kt`: gỡ `w_energy w_tire w_pm25 w_car w_speed w_board` (`WidgetKind.CAR/BOARD`); giữ `w_clock w_media w_photos w_apps`. Bảng nhu cầu `C/launcher/CarDataDemand.kt:62–84`. Nhóm khả năng `C/launcher/CapabilityGroups.kt` (bảng nhóm xe).

**Thanh nút** — `C/launcher/LauncherActions.kt`: gỡ `launcher_cam_front/left/right/rear/off`; giữ `launcher_apps launcher_settings launcher_voice launcher_shortcuts`. Nút xe = mọi id `ControlRegistry` trong `DockConfig.enabled` (mặc định `ControlRegistry.defaultEnabledIds()`).

**Giọng nói** — `C/launcher/voice/VoiceIntent.kt`: gỡ `Control`, `Macro`, `Read`; nhánh tương ứng ở `A/launcher/VoiceDispatcher.kt` (→ `VoiceControlDispatch`, `VoiceReadback`, `VoiceClimateStep`) và `onCamera` (`VoiceDispatcher.kt:166`). Từ vựng sinh từ registry: `VoiceGrammar`, `VoiceSynonyms`, `VoicePhrases`, `SherpaHotwords`, `SherpaPhraseHotwords`, `VoiceCommandCatalog`, `VoicePhoneticMatch`, `VoiceRisk`, `VoiceReply*`, `VoiceClarify`, `TtsPronunciation`, `VoiceGrammarSnapshot` (ảnh chụp `:wake`, mang `CarStatus`), `A/launcher/voice/VoiceWakeSessionFactory.kt`, `VoiceWiring.kt`. Giữ `Launcher`, `Profile`, `Nav`, `NavigateSaved`, `Media`, `OpenApp`, `Layout`, `EndSession`, `Unknown`. `VoiceAppTargets`/`VoiceTargetDispatch` chỉ cần `NavApps` (§2.1).

**Cầu kiểm thử** — `C/launcher/testbridge/TestBridgeCommand.kt`: gỡ `camera camera_frame camera_synth ctl hal sweep featmap captest ctllog`; `diag` (= `ClusterDiag.capture`, `A/launcher/testbridge/KachiTestBridge.kt:418`) thay bằng bản chẩn đoán chung hoặc gỡ; `diag_screen` (`TestBridgeScreenCommands.kt`: `diag`, `vietmap` đều là màn BYD) gỡ; `reapply` giữ (rút gọn `ClusterNavBridgeReapply`); `prefs_set` — `TestBridgeWritableKeys.kt` có 34 khoá camera/cast/badge/vm/seat/pm25/rain/nav cần gỡ; `state` — `TestBridgeState.kt:112,299` trường `camera`.

---

## 2. Điểm dính nguy hiểm

### 2.1 Lớp dùng chung giữa phần giữ và phần gỡ (phải dời/tách TRƯỚC khi xoá)

| Lớp (nằm trong vùng gỡ) | Phần GIỮ đang dùng | Việc |
|---|---|---|
| `C/modules/clustercast/{StackParse,DisplayParse,WmParse,AnrDropbox}.kt` | `C/launcher/{SlotPresence,SlotLiveness,StackReads,FloatingOrphanPlan}.kt`, `C/launcher/behind/{BehindHomePlan,SlotReturn}.kt`, `C/launcher/trip/TripMusicView.kt`, `C/modules/navaccess/AccessibilityRebind.kt`, `A/launcher/behind/BehindHomeRecovery.kt`, `A/launcher/trip/TripStart.kt`, `A/NavConnect.kt` | Dời sang `C/system/` (đổi package), đợt W0. `ClusterOverlayDisplay`/`AppScale` đi theo cast/camera. |
| `C/launcher/camera/CameraGuard.kt` (rào "không HOME khi màn camera hiện") | `C/launcher/trip/TripPlan.kt:5,116,205` (`normalCmd`, `Why.CAMERA_UNKNOWN`), `C/launcher/behind/SlotReturn.kt`, `C/modules/navaccess/AccessibilityRebind.kt` | ⚠ `cameraSignature == null` hiện nghĩa là **"chưa biết" ⇒ CHẶN** (`TripPlan.kt:116`, `TripStart.kt:397` trả `CAMERA_UNKNOWN`). Android box không có camera BYD ⇒ phải đổi ngữ nghĩa thành "không có camera ⇒ cho qua" và lệnh K10/K7/HOME không bọc `case`. Làm ở W0, có test. |
| `A/modules/clustercast/ClusterProfile.kt` (`resolveCached().cameraSignature`, `supportsStyle`) | `KachiHomeSlots.kt:130`, `BehindHomeRunner.kt:146`, `TripStart.kt:183,397`, `SettingsSectionsTrip.kt:71`, `NavConnect.kt:359`, `C/launcher/FloatingOrphan{Plan,Sweep}.kt` | Thay bằng hằng "không camera" ở W0, xoá `ClusterProfile` ở W2. |
| `C/navigation/NavApps.kt` | `C/launcher/voice/VoiceAppTargets.kt`, `A/launcher/VoiceTargetDispatch.kt` | Dời gói id GMaps/Waze/VietMap(app ngoài) sang `C/launcher/voice/`. |
| `A/NavNotificationListener.kt` | `A/launcher/MediaBridge.kt:14,166,224`, `PermissionPreflight.kt:101,184` (`allow_listener`), `RebindReceiver.kt:236` | Giữ lớp, rút thân (W2). Đổi tên = component mới ⇒ quyền bộ nghe mất trên máy đã cài; nếu đổi tên phải cấp lại qua Preflight lúc khởi động (đã có đường `cmd notification allow_listener`). |
| `C/launcher/ControlRegistry.kt` chứa **`DockConfig`/`DockEdge`/`ControlKind`** | `WorkspacePrefs`, `DockAreaLayout`, `DockSelection`, `HomeUiState`, mọi thanh nút | Tách `DockConfig`/`DockEdge` ra tệp riêng (W0); mặc định `enabled` = `ControlRegistry.defaultEnabledIds()` ⇒ đổi sang danh sách launcher actions. |
| `CapabilityCatalog` / `LauncherCatalog` (`C/launcher/LauncherCatalog.kt`) | `AppDrawer`, `TopStripPicker`, `WidgetViews`, `WorkspaceState.sanitized`, `WorkspaceRenderPlan`, `DockSelection`, `AppWidgetLabels`, `IconRepeat`, `Lang`, voice grammar | Catalog gộp widget + launcher action + nút xe + datum + nhóm + macro. Phải viết lại thành catalog chỉ-launcher (W3). `WorkspaceState.sanitized` dùng nó để tự rụng ô lạ ⇒ ô xe đã lưu tự thành trống (đúng ý), cần test. |
| `C/launcher/CapabilityGroups.kt` | `A/launcher/KachiTheme.kt` (màu theo lĩnh vực `Domain`), `TopStrip`, `TopStripChips` | `Domain`/`domainTints` ở `KachiPalette` phải bỏ hoặc giữ enum rỗng (W3). |
| `CarStatus` | `HomeUiState.carStatus`, `WorkspaceView`, `KachiTopStrip`, `WidgetRefreshers`, `TileSize`, `VoiceWiring`, `VoiceWakeSessionFactory`, `VoiceGrammarSnapshot` | Gỡ trường; widget `w_clock` đọc `ext_temp` xe (`CarDataDemand.kt:81`) ⇒ đồng hồ mất nhiệt độ ngoài trời (thời tiết thật cần nguồn khác — ngoài phạm vi). |
| `WorkspaceState.DEFAULT` (`C/launcher/WorkspaceState.kt:244–247`) | lần chạy đầu | Mặc định có `w_board`, `w_energy` ⇒ đổi sang `w_clock`/`w_media`/`w_apps` ở W0. `TopStripConfig.DEFAULT_IDS` (`TopStrip.kt:233`) = toàn chip xe ⇒ rỗng. |
| `A/AppPrereqs.kt` + `C/system/AppPrereqPlan` | `SimpleCastRuntime`, `VietMapAutostart`, `PrefsBadge`, `EarlyShellChannel`, `ClusterNavBridgeReapply`, `C/carexec/WakeEpochPolicy.kt`, `ProfileScope` | Phạm vi = VietMap + app chiếu ⇒ gỡ cả; sửa `WakeEpochPolicy` + `ProfileScope` (khoá dấu `app_prereq_marks`). |
| `A/TatMayCastHold.kt`, `A/HealCastWait.kt` | `A/A11yLifecycleHeal.kt:57–58`, đường chữa phím | Gỡ móc trong heal (W2 cùng R1). |
| `C/carexec/` (gói hỗn hợp) | GIỮ `LocalShellAdmission`, `LocalShellFailure`, `ShellApprovalLedger`, `ShellReadinessPolicy`, `WakeEpochPolicy` (kênh shell) | Chỉ xoá `CarExec*`, `T10*`, `VerdictLedger`; `ShellOutcome` đang dẫn chiếu `CarExecCommands` trong chú thích — kiểm khi xoá. Nên dời phần giữ sang `C/system/shell/` (W4, tuỳ chọn). |
| `car-integration` | GIỮ `CI/carexec/{LocalDeviceShell,LocalShellRetryPolicy,FirstOpenApprovalPolicy}.kt` | Xoá `vehicleprobe/**`, `CarExecCli`, `CarExecShell`, `NlsHealShell`. |
| `:vehicle-contracts` | `core/build.gradle.kts:13` `api(project(":vehicle-contracts"))`; chỉ `CI/vehicleprobe/*` import `com.byd.clusternav.vehicle.*` | Bỏ `api(...)` sau khi xoá `vehicleprobe`. |
| `ClusterNavBridge*` (`A/launcher/`, 13 tệp) | Cài đặt dùng cầu cho cả phần giữ (`Home`, `System`, `Keys`, `Wake`, `Msg`) | Xoá `Cast`, `CastStyle`, `Geometry`, `CameraPerCam`, `Hud`; sửa `ClusterNavBridge.kt`, `Reapply.kt`, `Automation.kt` (bỏ sấy), `Msg.kt`. |
| `DisplayOwnershipRegistry` (`C/system/`) + `WindowCommandDispatcher.setCastDisplay` | Cổng chủ quyền display của slot | Giữ, gỡ nhánh `CAST`. |
| `C/core/DiagFiles.kt:28` `CASTLOG` | `DiagStorageCap` | Giữ hằng một bản để còn dọn thư mục cũ, hoặc bỏ. |
| Prefs file `clusternav_prefs`/`simple_cast_prefs` | `A/Prefs.kt`, `PrefsBadge`, `PrefsCamera*`, `PrefsAutomation`, `VmOverlayPosition` | `clusternav_prefs`, `clusternav_theme`, `clusternav_lang` vẫn chứa khoá GIỮ (voice, voicekey, theme, lang, update) ⇒ giữ tên tệp. `simple_cast_prefs` bỏ hẳn. |

### 2.2 Khoá hồ sơ (ProfileScope) dẫn chiếu prefs bị gỡ

- `C/launcher/SettingsCatalogClusterNav.kt` — bảng `CLUSTERNAV_KEYS` (nguồn duy nhất): khoá gỡ ở `clusternav_prefs`: `enabled nav_cluster_screen_mode marquee badge_* show_upcoming_badge show_alert_chip vm_bubble_* seat_comfort_* seat_level_0..3 pm25_filter_enabled recirc_on_start_enabled headless_autostart rain_defrost_* (nav_automation_rules giữ nếu giữ lịch)`; toàn bộ `simple_cast_prefs` (`cast_enabled split_ratio_left_pct cast_bubble_visible cast_style autostart_*`); khoá ẩn `interpolate acc_booster lane source_mode anim_opt hud`.
- `C/launcher/ProfileScope.kt` — `CLUSTERNAV_KEYS` sinh từ bảng trên + `ProfileScopeCluster.PROFILE_EXTRA_KEYS` (6 khoá camera + vị trí nút nổi), `DEVICE_KEY_PREFIXES` = `ProfileScopeCluster.FAMILY_PREFIXES` (khung app chiếu `config_*`), `SNAPSHOT_SUFFIXES` (ảnh chụp `simple_cast_prefs`).
- `C/launcher/ProfileScopeCluster.kt` — `CAST_GEOMETRY`, `DEFERRED` (`cast_enabled_pending`), `CAMERA_PROFILE_KEYS`, `CAMERA_DEVICE_KEYS`, `MOVED_TO_PROFILE`, `DECLARED_TYPES` — xoá cả tệp.
- `C/launcher/ClusterSnapshotPlan.kt` (`apply`, `mergeImport` — chỗ gọi duy nhất `importProfile` → `mergeImportedCast`) và `ClusterImportSummary.kt` (hộp thoại nhập báo khung chiếu).
- `C/launcher/ProfileSharePolicy.kt` (danh sách trắng chia sẻ), `ProfileTransfer.kt`, `ProfileScopeMigration.kt`, `WorkspacePrefsMigrations.kt`, `WorkspacePrefsSnapshot.kt`, sổ `profile_keys_filled_v1` trong `PrefsWorkspaceRepository.init`.
- ⚠ **Tương thích tệp hồ sơ**: tệp `.kachi` xuất từ Kachi BYD chứa ảnh chụp `simple_cast_prefs`/khoá camera. Sau khi gỡ, `ProfileScopeLauncher.check` + `PrefsTypedRead` phải **bỏ qua im lặng** khoá lạ (không ném) — cần một bài test nhập tệp BYD cũ.

### 2.3 Test ghim mã sẽ gỡ (xoá cùng đợt; số tệp đo từ cây test)

| Vùng | Test |
|---|---|
| R1 cast | `AT/modules/clustercast/**` + `CT/modules/clustercast/**` (25 + 59 `simplified/`), `AT/launcher/{ClusterNavBridgeWiringContractTest,ClusterNavBridgeCastKeysWiringContractTest,ClusterReopenWiringTest,ClusterProfileScopeCoverageTest,ClusterProfileSwitchWiringContractTest,ClusterNavSettingsWiringContractTest,BubblePanelAlwaysVisibleContractTest}`, `CT/launcher/{ClusterSnapshotPlanTest,ClusterImportMergeTest,ProfileScopeClusterTest,ClusterNavSettingsModelTest}`, `AT/{FloatingBubbleFirstLaunchContractTest,HealCastWaitWiringContractTest}`, `CT/modules/navaccess/{HealCastGateRecheckTest,HealCastDeferralTest,TatMayCastHoldPlanTest}` |
| R2 camera | `AT/launcher/camera/**` + `CT/launcher/camera/**` (45 tệp tổng), `AT/launcher/{CameraViewRowContractTest,VoiceCameraFallbackDispatchTest}`, `CT/launcher/voice/{CameraDemandVoiceTest,VoiceCameraBareNameTest,VoiceCameraTurnTest}`, `CT/modules/navaccess/A11yCameraSignatureTest`, `CT/core/SourcePathHygieneTest` (nhắc `hal-helper` — sửa, không xoá) |
| R3 nav/HUD | `CT/navigation/**` (42 + 12 `screencapture/`), `AT/{HudRoadCapabilityTest,NavArrivalClearContractTest,NavFunnelWiringContractTest,AmapFrameBuilderContractTest,AmapEmissionArbiterTest,NavNotifRawLogTest,NotificationParserTest,HudManeuverEncodingTest,SpeedSignSourceLifecycleTest,SourceArbiterTest,NaviStatePrimeVerificationTest,NavPackageRosterSyncTest,NavHudSurfaceSplitWiringTest,NavHalContentKeepAliveTest,NavNotificationListenerTest,NlsHealWiringContractTest,RoundaboutManeuverWiringTest,HudKeepAliveWiringTest,PhysicalHudOwnershipTest}`; `CT/resources/gmaps-content-golden.txt` |
| R4 badge | `AT/speedbadge/**`, `CT/speedbadge/BadgeLayoutTest`, `AT/{SpeedBadgeArmingWiringTest,AlertChipWiringContractTest}` |
| R5 VietMap | `AT/vietmapwidget/**`, `CT/vietmapwidget/**`, `AT/{VietMapAutostartServiceWiringTest,VietMapAutostartGateTest,VmFloatWhitelistWiringTest,VmOverlayPosLogGateTest,AppPrereqsWiringContractTest,AppPrereqsPackageAddedWiringTest}`, `AT/launcher/VmBubbleSwitchHonestyContractTest`, `CT/system/AppPrereq{Added,Revert,Plan,Read}Test`, `CT/core/FloatAppListTest` |
| R6 comfort | `CT/comfort/**`, `CT/body/BodyworkTest`, `AT/automation/{RainDefrostStatusWiringTest,RainDefrostV8WiringTest}`, `CT/launcher/automation/RainDefrost*Test`; `AT/automation/Automation185WiringTest` (sửa — giữ phần lịch dẫn) |
| R7 HAL/registry | ≈ 60 tệp: `CT/launcher/{Telemetry*,Control*,Car{Status,Data,Control}*,Hal*,Capability*,Group*,Tyre*,ActionMacro*,MacroExec*,KeyCtl*,ClimateAuto*,CtlSafetyPolicy*,Units*,TopStrip*,Dock*}Test`, `AT/launcher/{TyreNoThreshold,ControlHeight,ActionMacroWiring,VoiceStepReadback,GroupTile*,KeyCtlWiring,TopStrip*,CarImage*,VoiceActGateReadback,CarDataDemandRenderer,ControlStateUx,GroupPickerWiring,CapabilityTileWiring,BydHalGatewayLogOnce,SunroofFix286Wiring,ControlTileOffMainWiring,MsDockLabelFit}*`, `AT/{BydHalRejectionCacheTest,BydHalFeatureGetTest,BydHalCallNamedIntTest,WakeOnWriteDelegationTest}`, `AT/modules/voicekey/KeyCtlSafetyTest`, `AT/launcher/voice/ControlSentRelayWiringContractTest`, `CT/launcher/voice/{VoiceHalfButton,VoiceSeatTrunk0920,VoiceWindowScope,VoiceCarWav0926,VoiceCarWav0927,VoiceBareCover,VoiceTelemetry}Test`, `CT/launcher/testbridge/TestBridgeCtlLogCommandTest`, `AT/launcher/FakeHalGateway.kt` |
| R8 key source | `CT/voicekey/KeySource{Probe,Resolver,Journal,Meter}Test`, `CT/voicekey/VoiceKeyMatcherSourceTest`, `AT/launcher/{KeySourceSplitWiringContractTest,KeySourceL7WiringContractTest}`, `AT/VoiceKeySourceStoreTest` |
| R9 HomeGuard | `AT/launcher/HomeGuardWiringContractTest`, `CT/launcher/HomeGuardPolicyTest` (giữ ca `wantsKachiHome`) |
| R10 RE | `CT/carexec/**` trừ test của `LocalShell*`/`ShellApprovalLedger`/`ShellReadinessPolicy`/`WakeEpochPolicy` (18 tệp, lọc tay), `car-integration/src/test/**/vehicleprobe/**`, toàn bộ `vehicle-contracts/src/test`, `offcar-planner/src/test` (gồm **`ExpansionTransportFenceTest`** — niêm phong hash `activity_main.xml`/`strings.xml`), `app/src/testVehicleTest/**`, `AT/{MainProbeSurfaceAbsenceTest}` |

Test **SỬA** (không xoá) vì quét văn bản manifest/mã và sẽ đỏ khi gỡ component: `AT/KachiAutostartServiceWiringTest`, `AT/HeadlessAutostartContractTest`, `AT/KachiApplicationHardeningWiringTest`, `AT/StartupContentionR11ContractTest`, `AT/ReadyAtHomeWiringContractTest`, `AT/A11yLifecycleHealWiringContractTest`, `AT/A11yFixReturnHomeWiringTest` (rào camera), `AT/DiagStorageCapWiringContractTest`, `AT/DeadReckonRetirementTest` (quyền định vị), `AT/LayeringRulesTest` (+ `docs/refactor-car-execution/layering-rules.md`), `AT/AppContainerTest`, `AT/launcher/{SingleLauncherIconContractTest,OwnPackageLiteralContractTest,L4TripBehindContractTest,LegacyScreenAbsenceContractTest,SettingsCatalogControlContractTest,DevSurfaceGateContractTest,TestBridgeSafetyContractTest,TripWiringContractTest,DockPickerContractTest,DockAreaLayoutContractTest,ClusterNavKeysContractTest}`, `AT/launcher/voice/{VoiceTtsIsolationContractTest,VoiceWakeIsolationContractTest}`, `AT/modules/navaccess/AccessibilityBindingStuckTest`, `CT/modules/navaccess/{ForceStopReturnHomeTest,AccessibilityRebindTest}`, `CT/launcher/testbridge/{TestBridgeCommandTest,TestBridgeScreenCommandsTest}`, `CT/launcher/{CapabilityReachabilityTest,WorkspaceStateTest,ProfileScope*Test,LauncherProfileTypesCoverageTest}`.

Kho câu giọng nói: `CT/launcher/voice/{VoiceGoldenCoverageTest,VoiceGrammarCoverageTest,SherpaBiasingCoverageTest}` đọc `scripts/voice/data/{variants,misspell,mix,templates,extra}.tsv` có sàn % (85 / 64). Câu lệnh xe biến mất khỏi ngữ pháp ⇒ sàn tụt ⇒ phải lọc corpus bằng `scripts/voice/gen-variants.py` (luật repo: không sinh lại cả tệp — `VOICE-CORPUS-REFRESH`). Bảng dịch `core/src/main/resources/i18n/{zh,th,ms}.tsv` + `values*/strings_kachi.xml` chứa nhãn xe — dọn ở W4, bài `I18n*`/`LangCoverage` sẽ báo chuỗi mồ côi.

Gradle: `app/build.gradle.kts:280–288` khai `car-integration/src/main/kotlin`, `docs/refactor-car-execution/layering-rules.md`, `scripts/vehicle` làm input test ⇒ sửa khi xoá thư mục; `settings.gradle.kts` `include(":vehicle-contracts", ":offcar-planner")`.

---

## 3. Thứ tự gỡ theo đợt

Mỗi đợt kết thúc bằng: `./gradlew test --rerun-tasks --continue` xanh · `:app:lintDebug` 0 lỗi · `:app:assembleDebug` · E2E máy ảo (mở HOME, ô app, widget, voice `say`, đổi/nhập hồ sơ). Một đợt = một commit (hoặc vài commit con) để bisect.

### W0 — Dời phần dùng chung, đổi mặc định (0 tính năng mất)
- **Dời**: `C/modules/clustercast/{StackParse,DisplayParse,WmParse,AnrDropbox}.kt` → `C/system/`; `C/navigation/NavApps.kt` → `C/launcher/voice/`; `DockConfig`/`DockEdge` ra khỏi `ControlRegistry.kt` (tệp mới `C/launcher/DockConfig.kt`); `HomeGuardPolicy.wantsKachiHome` → `DefaultHome`/`WorkspacePrefs`. Test của các lớp này đi theo (`CT/modules/clustercast/{StackParse,DisplayParse,WmParse}*`).
- **Đổi ngữ nghĩa camera**: `CameraGuard` → `C/system/HomeGuardShell.kt` với "không có dấu camera ⇒ lệnh trần"; `TripPlan` bỏ `Why.CAMERA_UNKNOWN`; `ClusterProfile.cameraSignature` ở 6 chỗ gọi → hằng `null` mang nghĩa "không camera". Sửa `ForceStopReturnHomeTest`, `CT/launcher/trip/TripPlanTest`, `A11yCameraSignatureTest`.
- **Mặc định**: `WorkspaceState.DEFAULT` (bỏ `w_board`, `w_energy`), `TopStripConfig.DEFAULT_IDS = []`, `DockConfig.enabled` mặc định = launcher actions.
- Sửa: `WorkspaceStateTest`, `TopStripTest`, `DockSelectionTest`.

### W1 — Tắt khởi động + ẩn UI BYD (mã còn, không ai gọi)
- **Sửa**: `KachiApplication.kt` (bỏ 3 móc §1.2), `BootSetupService.kt` (bỏ nav/VietMap/comfort), `RebindReceiver.kt` (bỏ cast/bubble/`NlsHeal`), `EarlyShellChannel.kt:131–132`, `KachiHomeActivity.kt` (`ensureCastBubble`), `KachiHomeWiring.kt` (`ensureCastBubble`, `CameraDemandDispatch`), `AppDrawer.kt` (`cameraSection`), `WorkspaceView.kt` (`SlotActionsCluster`), `A11yLifecycleHeal.kt:57–58` (`TatMayCastHold`), `StartupHousekeeping` (`CastPrefsHousekeeping`).
- **Cài đặt**: xoá `SettingsGroup.CAST`, `SettingsGroup.CAR` + mục NAV/BARS/DISPLAY/SYSTEM ở §1.2 trong `SettingsCatalogGroups.kt`/`SettingsCatalogEntries.kt`/`SettingsCatalogClusterNav.kt` (`CLUSTERNAV_KEYS` GIỮ nguyên ở đợt này để hồ sơ không mất dữ liệu); `SettingsSections.kt:73–80`.
- **Manifest**: xoá 8 component §1.2 (`DiagActivity`, `VietMapWidgetDiagActivity`, `ClusterBlackActivity`, `ClusterNavActivity`, `FloatingBubbleService`, `CastAutomationService`, `VietMapAutostartService`; `BootSetupService` tuỳ quyết định).
- **Cầu kiểm thử**: gỡ lệnh `camera* ctl hal sweep featmap captest ctllog diag_screen` khỏi `TestBridgeCommand.kt` + bảng dispatch `KachiTestBridge.kt`.
- **Test sửa**: nhóm "Test SỬA" ở §2.3 có quét manifest/khởi động (`KachiAutostartServiceWiringTest`, `HeadlessAutostartContractTest`, `KachiApplicationHardeningWiringTest`, `StartupContentionR11ContractTest`, `SettingsCatalogControlContractTest`, `TestBridgeCommandTest`, `TestBridgeSafetyContractTest`, `DevSurfaceGateContractTest`); test wiring của chính các component bị gỡ khỏi manifest (`VietMapAutostartServiceWiringTest`, `FloatingBubbleFirstLaunchContractTest`, `ClusterBlackInProcessRemovalContractTest`) xoá luôn ở đợt này.

### W2 — Xoá lá (không còn ai gọi sau W0/W1), từng khối một
Thứ tự con (mỗi khối build xanh riêng):
1. **W2a · RE/đo xe**: module `:vehicle-contracts`, `:offcar-planner` (+ `settings.gradle.kts`, `core/build.gradle.kts:13`), `CI/vehicleprobe/**`, `CI/carexec/{CarExecCli,CarExecShell}.kt`, `C/carexec/{CarExec*,T10*,VerdictLedger}.kt`, `app/src/vehicleTest/java/**` + manifest probe, `app/src/testVehicleTest/**`, `scripts/{vehicle,re}/`, `scripts/{verify-*,on-car-verify.sh,analyze-nav-distance-log.py}`, `res/layout/activity_main.xml` + `LegacyScreenAbsenceContractTest`. Sửa `app/build.gradle.kts:280–288`, `LayeringRulesTest`.
2. **W2b · Camera**: `A/launcher/camera/**`, `C/launcher/camera/**` (trừ phần đã dời ở W0), `hal-helper/`, `assets/kachi_hal.jar`, `scripts/build-hal-helper.sh`, `PrefsCamera*`, `SettingsSectionsCamera*`, `ClusterNavBridgeCameraPerCam`, `CameraSettingsLabels`, `TestBridge{Camera,CameraFrame,PerCam,Synth}`, `VoiceCamera*`, `LauncherActions` id `launcher_cam_*`, đích phím `cam:`; `AppContainer.cameraSignal*`; `TestBridgeState` trường `camera`.
3. **W2c · Cast + VietMap + biển tốc độ**: `A/modules/clustercast/**`, `C/modules/clustercast/{simplified,model}/**`, `ClusterOverlayDisplay`, `AppScale`, `A/cast/`, `A/ui/`, `CastPrefsHousekeeping`, `ClusterNavBridge{Cast,CastStyle,Geometry}`, `SettingsSectionsCast*`, `SlotActionsCluster`, `TatMayCastHold`, `HealCastWait`, `C/modules/navaccess/{HealCastDeferral,TatMayCastHoldPlan}`, `ProfileScopeCluster`, `ClusterSnapshotPlan`, `ClusterImportSummary`; `A/vietmapwidget/**`, `C/vietmapwidget/**`, `VietMapAutostart*`, `Vm*`, `AppPrereqs`, `C/system/AppPrereq*`, `C/core/FloatAppList`; `A/speedbadge/**`, `C/speedbadge/**`, `PrefsBadge`. Sửa `ProfileScope.kt` (bỏ `ProfileScopeCluster.*`, `SNAPSHOT_SUFFIXES` của `simple_cast_prefs`), `WorkspacePrefsProfile`/`importProfile` (bỏ `mergeImportedCast`), `DisplayOwnershipRegistry`/`WindowCommandDispatcher.setCastDisplay`, `KachiAutostart.logBootPlan`, `WakeEpochPolicy`, `ClusterNavBridge.kt`, `ClusterNavBridgeReapply.kt`, `DiagStorageCap`/`DiagFiles` (`castlog`).
4. **W2d · Dẫn đường cụm/HUD**: `C/navigation/**` (trừ `NavApps` đã dời), `A/{NavRepository,NavNotif*,NavLog,NavDiag,NavState,NotificationParser,Amap*,ClusterBroadcaster,HudMirrorController,NavigationHudOwner,NavigationSpeedSignOwner,NavSpeedLimitPusher,SpeedProvider,NlsHeal,BitmapPixelFrame,BitmapUtil}.kt`, `ClusterNavBridgeHud`, `SettingsNavHudRows`, `ProfileNavNotice`, `C/launcher/ProfileNavSwitch`, `CI/carexec/NlsHealShell.kt`, `assets/navopen.jar`. **Sửa** `NavNotificationListener.kt` (thân rỗng), `NavAccessibilityService.kt` (bỏ `NavScreenScan`/`NavApps.GMAPS`), `res/xml/nav_accessibility_config.xml`, `NavConnect.kt`, `SettingsSectionsNav.kt` (chỉ còn app dẫn đường mặc định), `PermissionPreflight` (bỏ cổng `NlsHeal`).
5. **W2e · Tiện nghi + sấy**: `A/comfort/**`, `A/body/**`, `C/comfort/**`, `C/body/**`, `A/automation/RainDefrostApplier.kt`, `C/launcher/automation/RainDefrost*`; sửa `AutomationService` (chỉ nhịp lịch dẫn), `ClusterNavBridgeAutomation.kt`, `PrefsAutomation.kt`.
6. **W2f · HomeGuard + nguồn phím**: `A/launcher/HomeGuard.kt`, phần còn của `HomeGuardPolicy`; `KeySourceRecorder`, `SettingsKeySourceDetail`, `C/voicekey/KeySource*`, `KeySample`; sửa `NavAccessibilityService` (bỏ `keySource`), `VoiceKeyMatcher`/`VoiceKeyBindings` (bỏ gán theo nguồn — kiểm tương thích chuỗi `voicekey_bindings` đã lưu).

### W3 — Gỡ lõi HAL/registry (rủi ro cao nhất, cuối cùng)
- **Xoá**: toàn bộ R7 ở §1.1 (`modules/hal`, `BydHalGateway`, `BydFeatureIds`, registry, `CarStatus*`, `CarData*`, `HalBindingTable` …, widget xe, `ControlTileFactory`/`ControlTileState`, `CapTest*`, `KeyCtl*`, `ActionMacros`/`MacroExec`, `ControlSentRelay`, `WakeOnWriteControl`, `CarImage*`, 41 `ic_car_*` + `assets/car/`, `design/car/`, `scripts/design/gen-car.py`).
- **Sửa (viết lại)**: `ControlDockView` (chỉ launcher actions + khối lối tắt), `LauncherCatalog`/`CapabilityCatalog`/`CapabilityPicker`/`CapabilityIcons`/`CapabilityDescriptions`/`CapabilityModel` (chỉ widget + launcher action), `WidgetRegistry` (bỏ CAR/BOARD), `WidgetViews`, `WidgetRefreshers`, `TopStrip`/`TopStripChips`/`TopStripPicker`/`KachiTopStrip` (bỏ chip; hoặc bỏ hẳn khối chip), `HomeUiState`/`HomeViewModel` (bỏ `carStatus`), `KachiHomeActivity`/`KachiHomeWiring`/`KachiHomeRender`/`WorkspaceView`, `KachiTheme`/`KachiPalette` (`Domain` tint), `Lang`/`I18nCatalog`, `AppContainer`.
- **Giọng nói**: bỏ `VoiceIntent.Control/Macro/Read` và mọi chỗ sinh từ vựng từ `ControlRegistry`/`TelemetryRegistry`/`ActionMacros` (danh sách §1.2); `VoiceDispatcher` (bỏ nhánh), `VoiceRisk` (còn `Profile`), `VoiceGrammarSnapshot` (bỏ `CarStatus`), `VoiceWakeSessionFactory`; lọc corpus `scripts/voice/data/*.tsv` + hạ/giữ sàn coverage có lý do; sinh lại `voice/kws` hotword nếu bảng tĩnh đổi.
- **Phím**: `ClusterNavBridgeKeys`/`SettingsSectionsKeys` bỏ đích `ctl:`; `voicekey_bindings` đã lưu có `ctl:…` ⇒ đọc lên phải bỏ qua (test).
- **Test**: xoá nhóm R7 ở §2.3; sửa `CapabilityReachabilityTest`, `WorkspaceStateTest` (ô `w_board` đã lưu tự rụng), voice coverage tests.

### W4 — Dọn prefs, hồ sơ, tài liệu
- `A/Prefs.kt` + `SettingsCatalogClusterNav.kt` + `ProfileScope.kt`: xoá khoá §2.2; giữ tên tệp prefs. Thêm một lượt dọn khoá chết một lần (dấu `migrated_androidbox_v1`) hoặc để mặc — `ProfileScope.unclassified` sẽ đỏ nếu còn khoá không xếp loại.
- Bài test nhập tệp hồ sơ `.kachi` từ Kachi BYD 2.97 (khoá lạ bị bỏ, không ném).
- Tuỳ chọn: dời phần giữ của `C/carexec/` sang `C/system/shell/`; đổi `NavNotificationListener` → `MediaSessionListener`, `NavAccessibilityService` → `KachiKeyService` (cả hai đổi component ⇒ phải cấp lại quyền; làm cuối và có E2E).
- Tài liệu: `docs/README.md` index, `docs/PROJECT-BACKLOG.md`, `.kiro/steering/project-context.md`, `README.md`, `docs/HUONG-DAN-KACHI.md` (gỡ mọi mục camera/cụm/tiện nghi).

---

## 4. Quyền manifest không còn cần

| Quyền / khai báo | Dòng | Sau khi gỡ | Ghi chú |
|---|---|---|---|
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `uses-feature android.hardware.location.gps` | 46–51 | **Chỉ còn nếu giữ lịch tự dẫn** (`GpsAvailability` là người dùng duy nhất; `PermissionPreflight` `LauncherRequirements.LOCATION`). Bỏ lịch ⇒ bỏ cả ba + `DeadReckonRetirementTest`. | |
| `FOREGROUND_SERVICE_SPECIAL_USE` | 53 | Vẫn cần (`KachiAutostartService`, `VoiceKeyKeepAliveService`, `AutomationService`) | FGS cast/VietMap/Bubble hết |
| `SYSTEM_ALERT_WINDOW` | 15 | Vẫn cần (tấm chữ giọng nói, ngăn kéo app overlay) — chú thích "bubble GMaps lên cluster" phải sửa | |
| `QUERY_ALL_PACKAGES` | 58 | Vẫn cần (ngăn kéo app, lối tắt) — sửa chú thích "picker chiếu app lên cụm" | |
| `BIND_APPWIDGET` | 62 | Vẫn cần (widget app khác); `VietMapAppWidgetHost` hết | sửa chú thích "BYD privileged" |
| `INTERNAL_SYSTEM_WINDOW`, `MANAGE_ACTIVITY_STACKS`, `ACTIVITY_EMBEDDING`, `REAL_GET_TASKS`, `ADD_TRUSTED_DISPLAY` | 70–74 | Giữ (nhúng app vào ô) | chỉ cấp khi priv-app; trên Android box sideload = không cấp, vô hại |
| `RECEIVE_BOOT_COMPLETED`, `INTERNET`, `RECORD_AUDIO`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE` | — | Giữ | |
| Intent action `com.byd.clusternav.REBIND_WATCHDOG` | 326 | Giữ (watchdog a11y) | |
| `res/xml/nav_accessibility_config.xml`: `packageNames` nav, `flagRetrieveInteractiveWindows`, `flagReportViewIds`, `canRetrieveWindowContent` | — | **Bỏ** — chỉ cần `flagRequestFilterKeyEvents` + `canRequestFilterKeyEvents` cho phím | giảm hẳn chi phí theo dõi cửa sổ của system_server |
| `app/src/vehicleTest/AndroidManifest.xml` (receiver `HAL_PROBE` exported, activity `HudSignProbe` exported) | — | **Bỏ** | |

Không có quyền BYD riêng (`android.car`, `uses-library`, `<queries>`) trong manifest — kiểm `grep '<queries\|uses-library'` = 0 kết quả. Đường HAL BYD đi bằng reflection (`BydHal`) + shell uid 2000 (`kachi_hal.jar`, `navopen.jar`), không qua quyền manifest.

---

## 5. Việc owner cần chốt trước khi bắt đầu

1. Giữ hay bỏ **lịch tự dẫn đường** (quyết định quyền định vị).
2. Giữ hay bỏ **`BootSetupService`/`system_headless_autostart`** (khởi động nền không mở HOME) — sau khi gỡ, nó chỉ còn cấp lại a11y + giữ phím sống.
3. Có đổi tên component **`NavNotificationListener`/`NavAccessibilityService`** không (đổi ⇒ máy đã cài phải cấp lại quyền một lần).
4. Widget đồng hồ mất **nhiệt độ ngoài trời** (nguồn là HAL xe) — chấp nhận, hay cần nguồn thời tiết khác (việc mới).
5. Thanh trạng thái khi không còn chip xe: giữ khối chip rỗng (để mở rộng sau) hay gỡ hẳn `TopStrip` chip.
