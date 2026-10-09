# HANDOFF — Kachi Android box (repo `kachi-androidbox`)

> **Trạng thái**: Session handoff · **Cập nhật**: 2026-10-09 · **Chủ**: dangkhoi · **Đọc file này TRƯỚC KHI làm gì trong repo này.**
> Phiên mới mở tại thư mục `kachi-androidbox/` sẽ KHÔNG thấy bộ nhớ (memory) của phiên Kachi BYD — mọi điều cần biết nằm ở đây.

## 0. Một câu

Repo này là **Kachi cho xe KHÔNG phải BYD** (Android box / đầu Android): chỉ lấy phần **launcher** của Kachi (màn chính theo ô,
widget, hồ sơ, giọng nói offline, nhạc/YouTube phát tiếp, gán phím, OTA). Nó được **tách hẳn** khỏi Kachi BYD; hiện mới ở bước
**spec nháp, CHƯA có dòng mã nào đổi** so với Kachi BYD 2.98.

## 1. Hai repo — KHÔNG được nhầm

| | Kachi BYD (đang chạy trên xe, KHÔNG đụng) | Kachi Android box (repo này) |
|---|---|---|
| Thư mục máy | `…/byd/byd-launcher/` | `…/byd/kachi-androidbox/` |
| GitHub | `github.com/dangkhoi/byd-kachi` | `github.com/dangkhoi/kachi-androidbox` |
| Remote `origin` | `git@github.com:dangkhoi/byd-kachi.git` | `git@github.com:dangkhoi/kachi-androidbox.git` |
| Nhánh làm việc | `feat/key-source-split` (2.98 đang gom) · `main` = kênh OTA BYD (2.97) | `androidbox/main` → đẩy lên `main` |
| Phiên Claude | phiên riêng, chỉ phục vụ byd-kachi | **phiên của bạn** |

Owner 2026-10-09: *"không đụng gì vào 2.97 2.98 đang chạy OK nhé, có làm gì thì làm trên code mới clone qua cho android-box thôi"* ·
*"git này cho repo kachi-androidbox nhé, ko nhầm lẫn nhau nhé"* · *"session này chỉ phục vụ byd-kachi"* (⇒ Android box làm ở phiên khác).

**Luật cứng**: không bao giờ `git push` từ repo này lên `byd-kachi`, không sửa file trong `byd-launcher/`, không đăng APK vào kênh OTA BYD.

## 2. Repo đang ở đâu

- Clone từ `byd-launcher` tại commit **`bf54415`** (Kachi BYD **2.98 (201) WIP** — chưa đăng; bản BYD đang đăng là 2.97 (200)).
  Lịch sử kế thừa đã công khai sẵn trên `byd-kachi`.
- Commit riêng của repo này:
  - `7d9bd6d` — spec nháp `docs/specs/androidbox-plan.html`
  - `527fb40` — gỡ `apk/Kachi-2.97-release.apk` (APK BYD), ghi chú README + `apk/README.md` là repo riêng
  - `5b6c7c8` — "Initial commit" GitHub (LICENSE) · `15dd8e6` — merge giữ LICENSE MIT của dự án
  - (+ commit handoff này)
- `main` trên GitHub = `androidbox/main` ở máy.

## 3. ⚠ Những thứ CÒN là của BYD trong mã (chưa đổi — làm trước tiên)

1. **`applicationId` vẫn là `com.byd.launcher`** (`app/build.gradle.kts`) ⇒ bản dựng từ repo này **ĐÈ** lên Kachi BYD nếu cài lên
   xe BYD. **Không cài lên xe BYD** cho tới khi đổi (spec B1, đề xuất `com.kachi.box`).
2. **Kênh OTA `UpdateChecker` vẫn trỏ `dangkhoi/byd-kachi`** (`apk/Kachi-<ver>-release.apk`) ⇒ bản dựng từ đây sẽ tự "cập nhật"
   về bản BYD. Phải đổi sang repo này + khuôn tên riêng (vd `Kachi-box-<ver>-release.apk`) — tìm `byd-kachi` trong mã
   (`grep -rn "byd-kachi" app core`), cả gói giọng nói tải từ `raw.githubusercontent.com/dangkhoi/byd-kachi/main/voice/...`
   (`SherpaTtsCatalog`, `WakeModelCatalog`) — quyết: trỏ sang repo này (cần copy `voice/`) hay giữ dùng chung tài nguyên giọng.
3. **Khoá ký**: bản release BYD ký bằng keystore ngoài repo `~/.kachi/kachi-release.keystore` + `keystore.properties` (gitignored,
   KHÔNG có trong clone). Hỏi owner: dùng chung khoá hay khoá mới cho Android box (appId khác thì dùng chung không xung đột).
4. **`CLAUDE.md` + `.kiro/steering/`** trong repo là luật của dự án BYD (xe đang chạy, HAL, cụm…). Phần lớn quy trình vẫn đúng
   (spec trước · mức bằng chứng · test hồi quy · review · quét bảo mật) — phần riêng BYD (HAL, cụm, firmware) không áp dụng.
   Nên viết lại `CLAUDE.md` cho Android box ở bước đầu (owner duyệt).
5. README gốc vẫn là của BYD (đã thêm ghi chú đầu trang) — viết lại khi có bản đầu.

## 4. Kế hoạch (spec `docs/specs/androidbox-plan.html`) — chờ owner duyệt

- **B1** tách sạch: appId riêng, tên app, kênh OTA riêng (repo này).
- **B2** gỡ phần chỉ-BYD: HAL BYD (đọc/điều khiển xe), chiếu cụm, HUD, camera 360, biển tốc độ cụm, phím mã BYD, cài đặt tiện
  nghi xe, các sửa lỗi firmware BYD (HomeGuard chống launcher BYD…). Không hiện chức năng chết.
- **B3** tự đo năng lực máy: kênh adb mạng (ô chạy app thật) · trợ năng (phím) · GPS (tốc độ) · mic (giọng nói).
- **B4** nguồn dữ liệu xe cắm thêm: GPS (off-car được) · OBD2 Bluetooth ELM327 · broadcast MCU đầu Android (FYT/TS…) — hai cái
  sau cần thiết bị thật.
- **B5** song ngữ VI/EN, trang giới thiệu riêng.

**Câu hỏi owner CHƯA trả lời** (hỏi lại đầu phiên):
- OQ1 loại máy: (a) box cắm cổng CarPlay chạy Android trên màn zin (Carlinkit, Ottocast…) · (b) đầu Android thay màn zin (FYT/TS10…) · (c) cả hai.
- OQ2 có cần dữ liệu xe (tốc độ, nhiên liệu…) hay chỉ launcher?
- OQ3 tên app + applicationId + khoá ký.

## 5. Sự thật kỹ thuật kế thừa (đã đo, dùng được ngay)

- [ĐO máy ảo 08–09/10] Kachi chạy trên Android thường (A10): màn chính, ô app, widget, hồ sơ, giọng nói offline, YouTube phát
  tiếp; dữ liệu xe hiện "—" khi không có HAL BYD.
- **Ô chạy app thật** cần kênh shell: app tự nối dadb tới `localhost:5555` (loopback) và người dùng phải bấm "Luôn cho phép"
  hộp gỡ lỗi USB. Không có ⇒ ô chỉ widget / nút mở app. Nhiều Android box có thể không cho adb mạng [CHƯA BIẾT].
- **Máy ảo (macOS này)**: AVD `kachi_play` (Android 10 Play, có YouTube) chạy cổng `emulator-5556`. Kênh shell trên máy ảo cần:
  `adb -s emulator-5556 tcpip 5555` **và** `adb -s emulator-5556 reverse tcp:5555 tcp:5557` (5557 = cổng adb CỦA máy ảo đó).
  DNS hỏng sau khi máy Mac đổi mạng ⇒ chạy lại với `-dns-server 8.8.8.8,1.1.1.1`. `emulator-5554` (clusternav10) đôi khi treo.
  ⚠ Máy ảo đang dùng chung với phiên BYD — **chỉ một phiên chạm một máy ảo một lúc**; tốt nhất tạo AVD riêng cho Android box.
- **Build**: `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`; `local.properties` (gitignored) cần `sdk.dir=<Android SDK>` — clone
  chưa có, tạo trước khi build. adb/emulator ở `~/Library/Android/sdk/platform-tools/adb`, `~/Library/Android/sdk/emulator/emulator`
  (không có trong PATH). macOS không có lệnh `timeout`. Test: `./gradlew :core:test :app:testDebugUnitTest` — đếm kết quả từ XML
  `*/build/test-results`, đừng tin console.
- Bản BYD 2.98 đã có: tên màn ảo ô cố định (`SlotVdName` — `display_settings.xml` không phình), log shell chỉ-đọc theo thay đổi
  (`ShellLogGate`), dọn khung app đã gỡ, xoá APK OTA đã cài, vòng đọc dữ liệu xe dừng khi màn tắt (`ScreenLit`) — đều hữu ích cho box.

## 6. Quy trình bắt buộc (học từ dự án BYD)

1. **Spec trước, owner duyệt rồi mới code** (`docs/specs/<slug>.html`, khuôn `_template.html`).
2. Mọi khẳng định gắn **[ĐO] / [SUY] / [ĐOÁN] / [CHƯA BIẾT]**; chưa đo thì nói "chưa biết".
3. Mỗi lỗi đã tìm gốc ⇒ **một test hồi quy**. File ≤ 500 dòng. Hàm mới phải có chỗ gọi (`grep`).
4. Sau khi code: **senior review** bằng sub-agent (owner chọn Fable 5.1 — `claude-fable-5-1`), lặp tới khi 0 lỗi.
5. **Quét bảo mật TRƯỚC mọi commit/push** (sub-agent Fable): không bí mật, không thông tin cá nhân (chỉ handle `dangkhoi`, KHÔNG
   tên thật/email), không đường dẫn `/Users/<tên>`, không IP xe, không biển số. ⚠ Phiên trước đã **commit trước khi quét hai lần**
   trong repo này (đều đã quét bù sạch, chưa lộ gì) — đừng lặp lại.
6. Commit: `git -c user.name=dangkhoi -c user.email=dangkhoi@users.noreply.github.com commit …`, kết thúc message bằng
   `Co-Authored-By: Claude …` theo hướng dẫn của phiên.
7. Mỗi bản dựng đã báo owner = một số hiệu riêng (bump versionCode + versionName).
8. Hiệu năng là ưu tiên cao (owner): mỗi mục sửa ghi rõ chi phí (lệnh shell, luồng, nhịp định kỳ, bộ nhớ); không để thứ gì phình theo ngày tháng.

## 7. Việc đầu tiên cho phiên mới

1. Đọc file này + `docs/specs/androidbox-plan.html`.
2. Hỏi owner OQ1–OQ3; cập nhật spec; xin duyệt Requirements.
3. Sau khi duyệt: B1 (appId + OTA + giọng nói trỏ repo này) → build xanh → kiểm không còn chuỗi `byd-kachi`/`com.byd.launcher`
   ngoài chỗ có chủ đích → rồi mới B2.
