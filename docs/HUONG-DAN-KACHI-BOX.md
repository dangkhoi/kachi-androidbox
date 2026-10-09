# Hướng dẫn dùng Kachi Android box · User guide

> **Trạng thái**: Current · **Cập nhật**: 2026-10-09 · **Mục đích**: hướng dẫn cài, cấp quyền và dùng Kachi trên Android
> box / đầu Android (bản 1.0, `com.kachi.box`). Tên nhóm và tên hàng lấy theo app (`SettingsCatalogGroups.kt` ·
> `SettingsCatalogEntries.kt`). Chưa đăng bản nào và **chưa thử trên box thật** — mọi bước dưới đây đã chạy trên máy ảo
> Android 10. Hướng dẫn cũ của Kachi BYD: `HUONG-DAN-KACHI.md` (lịch sử).

## Tiếng Việt

### 1. Cài

1. Tải `apk/Kachi-box-<bản>-release.apk` từ nhánh `main` của repo `dangkhoi/kachi-androidbox` (khi đã có bản).
2. Mở tệp bằng trình cài của máy (cho phép *cài từ nguồn này* nếu máy hỏi), hoặc `adb install -r <tệp>`.
3. Máy cần Android 10 trở lên, CPU arm64. Kachi box (`com.kachi.box`) cài **song song** với Kachi BYD, không đè.
4. Lần mở đầu có hộp *Miễn trừ trách nhiệm* — đọc rồi đồng ý.

### 2. Cấp quyền — hai đường

Kachi cần: **Trợ năng** (phím vật lý) · **Đọc thông báo** (widget nhạc) · **Vẽ trên màn khác** · **Micro** (giọng nói) ·
**Định vị** (lịch dẫn đường) · **Màn hình chính**. Danh sách quyền còn thiếu: *Cài đặt › Hệ thống & quyền › Quyền còn thiếu*.

**a) Máy có adb mạng** (`localhost:5555` mở — tuỳ hãng box):

- Kachi tự nối kênh adb loopback trên chính máy. Lần đầu máy hiện *"Cho phép gỡ lỗi USB?"* — tích *Luôn cho phép* rồi
  *Cho phép*.
- Có kênh ⇒ Kachi **tự cấp** các quyền trên và **mở được app trong ô**. Lần mở sau nối lại trong nền, không hỏi nữa.

**b) Máy không có adb mạng**:

- Mỗi hàng quyền thiếu có nút **Mở cài đặt hệ thống** (hoặc **Cho phép** cho micro / định vị). Bấm ⇒ bật quyền ở màn hệ
  thống ⇒ bấm Back; hàng tự biến mất.
- Hàng *Kênh điều khiển cửa sổ* mở màn *Tuỳ chọn nhà phát triển*. Nếu tuỳ chọn ấy đang tắt, Kachi chỉ cách bật (ở màn
  *Giới thiệu*, chạm 7 lần vào *Số bản dựng*) rồi mở màn đó.
- Không có kênh thì **app không chạy trong ô được**: ô hiện thẻ app, chạm ⇒ thẻ có nút **Mở toàn màn hình**.
- Hàng *Cho phép cửa sổ tự do* có thể vẫn báo thiếu mà không có nút — chỉ cần cho app trong ô, bỏ qua được.

### 3. Đặt Kachi làm màn hình chính

*Cài đặt › Hệ thống & quyền › Màn hình chính* ⇒ **Đặt Kachi làm màn hình chính**. Có kênh: đặt luôn. Không kênh: máy mở màn
chọn *Ứng dụng màn hình chính mặc định* — chọn **Kachi**. Có thể bật thêm *Giữ Kachi làm màn hình chính khi nổ máy* và
*Tự mở Kachi khi nổ máy*.

### 4. Bố cục và ô

- *Cài đặt › Màn hình chính › Bố cục sẵn*: 1 ô · 2 cột · 2 hàng · 3 ô · 4 ô. **Vẽ bố cục riêng…** = lưới 12 × 6, tối đa 6 ô.
- Mỗi ô chứa một **widget** hoặc **một app**. Đặt app vào ô: kéo từ ngăn kéo *Ứng dụng*, hoặc nút ⇄ ở đầu ô.
- Đầu ô app có ba nút: **chạy nền** · **⇄ đổi** · **tắt** (tắt cần chạm hai lần). *Tự ẩn nút ⇄* ở cùng trang.
- *Hình nền & trình chiếu*: chọn hình nền; nút **Sao chép đường dẫn thư mục** cho biết chỗ bỏ ảnh vào.

### 5. Widget

**Đồng hồ** · **Đang phát** (nhạc của app đang phát; cần quyền Đọc thông báo) · **Trình chiếu ảnh** (ảnh trong thư mục
`photos` của app) · **Lưới lối tắt app** · **widget Android của app khác**.

### 6. Thanh nút và lối tắt

*Cài đặt › Thanh trạng thái & thanh nút*: hiện/ẩn thanh nút, viền đặt (trên · dưới · trái · phải), nút trên thanh
(Ứng dụng · Cài đặt · Nói với Kachi · khối Lối tắt ứng dụng), thứ tự. Mỗi **lối tắt ứng dụng** chọn kiểu mở: *Ô n* ·
*Toàn màn* · *Chạy ngầm*. Cỡ thanh nút 50–150 %: *Cài đặt › Hiển thị › Cỡ thanh nút*.

### 7. Hồ sơ tài xế

*Cài đặt › Hồ sơ tài xế*: thêm hồ sơ (bản sao của hồ sơ đang dùng), đổi tên, xoá, chọn *Hồ sơ lúc nổ máy*. Mỗi hồ sơ giữ bố
cục, nội dung ô, thanh nút, hình nền, giao diện, ngôn ngữ, sổ địa chỉ, lịch dẫn đường.
**Xuất đầy đủ (sao lưu)** · **Xuất để chia sẻ** (không kèm địa chỉ và lịch) · **Nhập hồ sơ từ file** (`.kachi`). Tệp của
Kachi BYD nhập được; phần chỉ dành cho xe BYD bị bỏ qua.

### 8. Giọng nói (chỉ tiếng Việt)

1. *Cài đặt › Giọng nói › Nhận dạng giọng nói (tại máy)* ⇒ **Tải mô hình tiếng Việt** (~77 MB, một lần, cần mạng). Sau đó
   nhận dạng chạy ngay trên máy, không gửi tiếng nói ra mạng.
2. Bấm nút 🎤 / *Nói với Kachi* trên thanh nút rồi nói. Có thể gán một phím vật lý để gọi (mục 9).
3. **"Hey Kachi"** (gọi rảnh tay) — thử nghiệm, mặc định tắt, tốn pin/CPU hơn.
4. *Giọng đọc offline*: tải gói giọng để Kachi đọc câu trả lời. *Dạy tên app*: dạy Kachi cách bạn gọi tên một app.

Câu mẫu (chỉ lệnh launcher):

| Việc | Nói |
|---|---|
| Mở app | *"mở YouTube"* · *"mở YouTube vào ô số hai"* |
| Ngăn kéo / Cài đặt | *"mở ứng dụng"* · *"mở cài đặt"* |
| Nhạc | *"phát nhạc"* · *"tạm dừng"* · *"bài tiếp"* · *"bài trước"* · *"phát bài Diễm xưa bằng YouTube Music"* |
| Dẫn đường | *"dẫn đường tới chợ Bến Thành"* · *"… bằng Google Maps"* · *"về nhà"* · *"đến công ty"* (theo sổ địa chỉ) |
| Hồ sơ | *"đổi sang hồ sơ Mặc định"* |
| Bố cục | *"bố cục hai cột"* · *"đổi sang bố cục bốn ô"* |
| Kết thúc | *"tạm biệt"* · *"cảm ơn"* |

Kachi **không** điều khiển xe: câu như *"bật điều hoà"*, *"mở kính"* được trả lời là không có tính năng đó.
Máy không có micro ⇒ mọi nút giọng nói tự ẩn.

### 9. Phím vật lý

*Cài đặt › Phím vật lý*: bật **Nhận nút vật lý** (cần quyền Trợ năng) ⇒ **Học phím mới** (bấm phím cần gán) ⇒ chọn đích:
một app, trợ lý, hoặc *Kachi nghe (tại máy)*. Phím chết sau khi máy ngủ: **Kiểm tra và sửa ngay** (cần kênh adb; không kênh thì bật
lại Trợ năng ở *Hệ thống & quyền*).

### 10. Dẫn đường theo lịch, nhạc khi lên xe

- *Cài đặt › Dẫn đường*: **Sổ địa chỉ** · **App dẫn đường mặc định** (Google Maps · Waze · VietMap nếu có cài) ·
  **Tự dẫn đường theo lịch** (giờ + thứ trong tuần ⇒ tự giao điểm đến cho app dẫn đường).
- *Cài đặt › Hệ thống & quyền*: **Mở app khi nổ máy** · **Tự mở nhạc khi lên xe** (YouTube / YouTube Music phát tiếp bài cuối).

### 11. Cập nhật

*Cài đặt › Hệ thống & quyền › Kiểm tra cập nhật*. Có kênh: Kachi tự cài. Không kênh: Kachi tải rồi mở **trình cài của
máy** — bấm *Cài đặt* (cho phép cài từ Kachi nếu máy hỏi). Kachi chỉ nhận tệp `Kachi-box-<bản>` cùng gói, cùng chữ ký.

### 12. Lấy log khi có lỗi

Kachi tự ghi log vào `Android/data/com.kachi.box/files/kachi-logs/` (log của app trong phiên + báo lỗi khi app chết).
Lấy về bằng máy tính: `adb pull /sdcard/Android/data/com.kachi.box/files/kachi-logs/ ./kachi-logs/`. Trình quản lý tệp
trên Android 11 trở lên thường không mở được `Android/data`. Gửi kèm ảnh chụp màn hình + giờ xảy ra lỗi.

---

## English

**1. Install** — download `apk/Kachi-box-<ver>-release.apk` from `main` of `dangkhoi/kachi-androidbox` (once published),
open it with the system installer or `adb install -r`. Android 10+, arm64. `com.kachi.box` installs side by side with
Kachi BYD. Accept the disclaimer on first launch.

**2. Permissions** — Accessibility (physical keys) · Notification access (music widget) · Draw over other apps ·
Microphone · Location (scheduled navigation) · Home app. See *Settings › System & permissions › Missing permissions*.

- *Device with network adb* (`localhost:5555`): accept *"Allow USB debugging?"* (always allow) once; Kachi then grants its
  own permissions and can run apps inside slots.
- *Device without network adb*: each missing row has **Open system settings** (or **Allow**); turn it on, press Back, the
  row disappears. The *Window control channel* row opens Developer options (or explains how to enable them). Apps cannot run
  inside slots — tap the slot and use **Open full screen**. The *Freeform windows enabled* row may stay without a button; ignore it.

**3. Home screen** — *Settings › System & permissions › Home screen* ⇒ **Set Kachi as home screen** (without the channel,
pick Kachi in the system chooser).

**4. Layouts & slots** — *Settings › Home screen*: 5 presets or *Draw your own layout…* (12 × 6 grid, up to 6 slots). Put
an app in a slot from the app drawer or the ⇄ button; app slots have [background] [⇄] [close] (close needs two taps).

**5. Widgets** — clock · now playing · photo slideshow (app's `photos` folder; *Copy folder path* shows where) · app
shortcut grid · other apps' Android widgets.

**6. Button bar** — *Settings › Status bar & button bar*: edge, buttons, order, app shortcuts (open in slot n / full
screen / background). Size 50–150 % under *Settings › Display › Button bar size*.

**7. Profiles** — *Settings › Driver profiles*: add (copy), rename, delete, profile on engine start; export full / export
to share (no addresses or schedule) / import `.kachi` (Kachi BYD files import; BYD-only keys are dropped).

**8. Voice (Vietnamese only)** — *Settings › Voice*: download the Vietnamese model once (~77 MB); recognition then runs
on-device. Tap 🎤 / *Talk to Kachi*. "Hey Kachi" is experimental and off by default. Example commands are in the table in
§8 above (open apps, *"into slot N"*, music, navigation, saved places, profile, layout). Car commands are answered as not
supported. No microphone ⇒ voice entry points hide.

**9. Physical keys** — *Settings › Physical keys*: enable, *Learn a new key*, bind to an app or the assistant. *Check and
fix now* needs the adb channel.

**10. Navigation & trips** — *Settings › Navigation*: address book, default app (Google Maps · Waze · VietMap if
installed), scheduled navigation. *System & permissions*: open apps at ignition, play music when you get in.

**11. Updates** — *Settings › System & permissions › Check for updates*. Without the channel Kachi hands the APK to the
system installer. Only same-package, same-signer `Kachi-box-<ver>` files are accepted.

**12. Logs** — `adb pull /sdcard/Android/data/com.kachi.box/files/kachi-logs/ ./kachi-logs/`; send with a screenshot and
the time it happened.
