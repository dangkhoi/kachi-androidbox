# Kachi Android box

> **(VI)** Màn hình chính (launcher) cho ô tô dùng **Android box / đầu Android** bất kỳ — không phụ thuộc hãng xe hay hãng box.
> Tách từ Kachi cho BYD (bản 2.98) và chỉ giữ phần launcher. Bản hiện tại **1.1** trên kênh OTA: [`apk/Kachi-box-1.1-release.apk`](apk/) (chưa thử trên box thật).
>
> **(EN)** A home-screen launcher for cars running any **Android box / Android head unit** — no car-brand or box-brand
> dependency. Split from Kachi for BYD (2.98), launcher part only. Current release **1.1** is on the OTA channel: [`apk/Kachi-box-1.1-release.apk`](apk/) (not yet tried on a real box).

## Tính năng

Kiểm theo mã hiện tại (`app/` · `core/` · `car-integration/`), không phải theo bản BYD.

- **Màn chính theo ô** — 5 bố cục sẵn (1 ô · 2 cột · 2 hàng · 3 ô · 4 ô) hoặc tự vẽ trên lưới 12 × 6, tối đa 6 ô. Mỗi ô
  chứa một widget hoặc **một app thật chạy trong ô** (màn ảo riêng) — việc này cần kênh adb loopback (xem *Cấp quyền*).
  Không có kênh ⇒ ô hiện thẻ app và nút *Mở toàn màn hình*.
- **Widget** — đồng hồ · đang phát (nhạc) · trình chiếu ảnh · lưới lối tắt app · widget Android của app khác.
- **Thanh nút + lối tắt** — thanh nút đặt ở 4 viền, đổi cỡ 50–150 %, khối lối tắt app; mỗi lối tắt mở vào ô, toàn màn
  hoặc chạy ngầm.
- **Hồ sơ tài xế** — mỗi hồ sơ giữ bố cục, nội dung ô, thanh nút, hình nền, giao diện, ngôn ngữ; xuất/nhập tệp `.kachi`
  (tệp của Kachi BYD nhập được, khoá chỉ-BYD bị bỏ qua).
- **Giọng nói offline tiếng Việt** cho lệnh launcher: mở app (kèm *"vào ô số N"*), nhạc, dẫn đường, đổi hồ sơ, đổi bố cục.
  Nhận dạng chạy tại máy (sherpa-onnx), mô hình tải một lần. **"Hey Kachi"** rảnh tay — thử nghiệm, mặc định tắt.
  Máy không micro ⇒ mọi lối vào giọng nói tự ẩn.
- **Nhạc / YouTube phát tiếp** khi lên xe; mở app khi nổ máy.
- **Sổ địa chỉ + lịch tự dẫn đường** theo hồ sơ — giao điểm đến cho Google Maps, Waze (hoặc VietMap nếu có cài).
- **Gán phím vật lý** cho app hoặc trợ lý (qua dịch vụ Trợ năng).
- **5 tiếng giao diện** — Tiếng Việt · English · 简体中文 · ไทย · Bahasa Melayu (giọng nói chỉ tiếng Việt).
- **Cập nhật OTA** từ thư mục `apk/` của repo này.

Không có: đọc/điều khiển xe, cụm đồng hồ, HUD, camera — đã gỡ cùng phần BYD. Dữ liệu xe cắm thêm (GPS · OBD2 · MCU) đang
hoãn (`BOX-B4`).

## Yêu cầu máy

Android **10 trở lên** (minSdk 29), CPU **arm64-v8a**. Giọng nói cần micro và khoảng 100 MB trống cho mô hình.

## Cài đặt

1. Tải `apk/Kachi-box-<bản>-release.apk` trên nhánh `main` (khi đã có bản), cài bằng trình cài của máy hoặc
   `adb install -r`.
2. `applicationId` = **`com.kachi.box`** ⇒ cài **song song** với Kachi BYD (`com.byd.launcher`), không đè lên nhau.
3. Đặt Kachi làm màn hình chính: *Cài đặt › Hệ thống & quyền › Màn hình chính*.

## Cấp quyền

Kachi cần: trợ năng (phím vật lý), đọc thông báo (widget nhạc), vẽ trên màn khác, micro, định vị (lịch dẫn đường),
màn hình chính.

- **Máy có adb mạng** (`localhost:5555`): Kachi tự nối qua kênh adb loopback, **tự cấp** các quyền trên và mở được app
  trong ô. Lần đầu máy hỏi *"Cho phép gỡ lỗi USB?"* — chọn luôn cho phép.
- **Máy không có adb mạng**: *Cài đặt › Hệ thống & quyền* hiện từng quyền thiếu kèm nút **mở đúng màn Cài đặt hệ thống**
  (hoặc hộp xin quyền); bật xong quay lại là hàng tự biến mất. App trong ô không chạy được — dùng *Mở toàn màn hình*.

## Build từ nguồn

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17   # JDK 17
echo "sdk.dir=<Android SDK>" > local.properties
./gradlew :app:assembleDebug test --continue
python3 scripts/count-tests.py                  # đếm kết quả từ XML
```

Bản release cần `keystore.properties` (không có trong repo). compileSdk/targetSdk 37.

## Quan hệ với Kachi BYD

Repo này (`dangkhoi/kachi-androidbox`) tách hẳn khỏi `dangkhoi/byd-kachi`: **không chung mã, không chung kênh OTA,
không chung APK**. App box chỉ nhận tệp `Kachi-box-<bản>`; tệp khuôn BYD bị bỏ qua. Ngoại lệ có chủ ý: gói giọng nói
(Hey Kachi, giọng đọc) vẫn tải từ `byd-kachi/voice/` (chỉ đọc, ghim sha256). Tài liệu BYD trong `docs/` là lịch sử.

## Miễn trừ · Giấy phép · Ghi công

Thử nghiệm cá nhân của **dangkhoi**, không liên kết với hãng xe hay nhà sản xuất thiết bị nào. Không bảo đảm về an toàn
lái xe, tương thích hay độ ổn định — dùng với rủi ro của riêng bạn. Đừng thao tác màn hình khi đang lái.

Giấy phép [MIT](LICENSE). Thư viện và mô hình bên thứ ba: [CREDITS.md](CREDITS.md). Hướng dẫn dùng:
[docs/HUONG-DAN-KACHI-BOX.md](docs/HUONG-DAN-KACHI-BOX.md). Mục lục tài liệu: [docs/README.md](docs/README.md).

---

## Features (EN)

Checked against the current code, not the BYD build.

- **Slot-based home** — 5 preset layouts or a custom one on a 12 × 6 grid, up to 6 slots. Each slot holds a widget or
  **a real app running inside the slot** (its own virtual display) — this needs the adb loopback channel (see
  *Permissions*). Without it the slot shows an app card with an *Open full screen* button.
- **Widgets** — clock · now playing · photo slideshow · app shortcut grid · other apps' Android widgets.
- **Button bar + shortcuts** — dock on any edge, 50–150 % size, an app-shortcut block; each shortcut opens into a slot,
  full screen, or in the background.
- **Driver profiles** — layout, slot contents, bar, wallpaper, theme, language per profile; export/import `.kachi` files
  (Kachi BYD files import; BYD-only keys are dropped).
- **Offline Vietnamese voice** for launcher commands: open apps (incl. *"into slot N"*), music, navigation, switch profile,
  switch layout. Recognition runs on-device (sherpa-onnx); the model is downloaded once. Hands-free **"Hey Kachi"** —
  experimental, off by default. No microphone ⇒ every voice entry point hides itself.
- **Music / YouTube resume** when you get in; open apps at ignition.
- **Address book + scheduled navigation** per profile — hands the destination to Google Maps, Waze (or VietMap if installed).
- **Physical key binding** to apps or the assistant (via an accessibility service).
- **5 UI languages** — Vietnamese · English · Simplified Chinese · Thai · Malay (voice is Vietnamese only).
- **OTA updates** from this repo's `apk/` folder.

Not included: car data/control, instrument cluster, HUD, cameras — removed with the BYD part. Plug-in car data
(GPS · OBD2 · MCU) is deferred (`BOX-B4`).

## Requirements · Install · Permissions (EN)

- Android **10+** (minSdk 29), **arm64-v8a**. Voice needs a microphone and ~100 MB free for the model.
- Install `apk/Kachi-box-<ver>-release.apk` from `main` (once published). `applicationId` **`com.kachi.box`** installs
  **side by side** with Kachi BYD (`com.byd.launcher`). Set it as home in *Settings › System & permissions › Home screen*.
- **Device with network adb** (`localhost:5555`): Kachi connects over the adb loopback, **grants its own permissions**
  and can host apps in slots. Accept *"Allow USB debugging?"* (always allow) the first time.
- **Device without network adb**: *Settings › System & permissions* lists each missing permission with a button that
  **opens the matching system settings screen** (or the permission dialog). Apps cannot run inside slots — use
  *Open full screen*.

## Relation to Kachi BYD · Licence (EN)

This repo (`dangkhoi/kachi-androidbox`) is fully separate from `dangkhoi/byd-kachi`: **no shared code, OTA channel or
APK**. Deliberate exception: voice packs (Hey Kachi, reply voice) are still downloaded from `byd-kachi/voice/`
(read-only, sha256-pinned). BYD documents under `docs/` are history.

Personal hobby project by **dangkhoi**, not affiliated with any car or device maker; no warranty of driving safety,
compatibility or stability. [MIT](LICENSE) · [CREDITS.md](CREDITS.md) · guide
[docs/HUONG-DAN-KACHI-BOX.md](docs/HUONG-DAN-KACHI-BOX.md).
