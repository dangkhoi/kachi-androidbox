# Kachi Android box — Rule bắt buộc cho mọi phiên làm việc

> Viết lại 2026-10-09 (spec `docs/specs/androidbox-plan.html` B1) từ luật của Kachi BYD. Giữ nguyên **quy trình**
> (mỗi rule ở bản gốc đều sinh từ một lỗi CÓ THẬT trên xe), bỏ phần chỉ đúng cho BYD (HAL BYDAuto, cụm đồng hồ,
> HUD, camera 360, firmware DiLink, nguồn RE BYD). Đọc trước khi sửa dòng code đầu tiên.

Repo này là **Kachi cho Android box / đầu Android bất kỳ** (owner 09/10: *"androidbox bình thường, có màn android
vậy thôi, loại nào cũng như nhau"*) — chỉ phần launcher. Repo **tách hẳn** khỏi Kachi BYD (`dangkhoi/byd-kachi`):

- `applicationId` = `com.kachi.box` · kênh OTA = `apk/Kachi-box-<ver>-release.apk` trên `main` của
  `dangkhoi/kachi-androidbox`. **Cấm** push lên `byd-kachi`, sửa file trong `../byd-launcher/`, đăng APK vào kênh BYD.
- Gói giọng nói CỐ Ý vẫn tải từ `byd-kachi/voice/` (OQ5) — chỉ đọc, ghim sha256.

App vẫn chạy trong xe đang lăn bánh. Ưu tiên: **đúng > an toàn > nhanh**. Không có ngoại lệ vì "gấp".

---

## 1. Trước khi sửa: phải có spec

Task mới hoặc thay đổi lớn → **viết spec trong `docs/specs/` trước (khuôn `_template.html`), owner duyệt rồi mới
code** (rule global §1). Vá nóng < 20 dòng, một file, không đổi hành vi → được bỏ spec.

## 2. Phân biệt CƠ CHẾ và QUY KẾT — không được trộn

Mọi khẳng định gắn mức bằng chứng: **[ĐO]** (đọc source / dump thật / chạy lệnh) · **[SUY]** (khớp hiện tượng,
chưa đo trực tiếp — nói "nghi là", kèm cách chốt) · **[ĐOÁN]** · **[CHƯA BIẾT]**. Chứng minh được một cơ chế
KHÔNG cho phép quy luôn nó cho một app/máy cụ thể khi chưa đo app/máy đó. Không có dữ liệu ⇒ nói "chưa biết" và
nêu đúng một lệnh để chốt.

## 3. Framework Android: đọc source TRƯỚC khi ship, không dựa trí nhớ

Mọi khẳng định về hành vi framework phải trích `file:line` từ AOSP đúng bản (box thường chạy Android 10–13; đo
`getprop ro.build.version.release` trên máy thật). Không bao giờ gate một đường phục hồi bằng dữ liệu mà chỉ
chính đường đó mới làm mới được.

## 4. Lệnh đổi state hệ thống phải có phạm vi TƯỜNG MINH

Trước mỗi lệnh `am`/`wm`/`settings`/`cmd`, trả lời đủ bốn câu: nhắm **display nào** (không quét mù) · **app nào**
(allow-list) · **loại stack nào** (chỉ `standard`; `home`/`recents`/`pinned` là vùng cấm) · **hoàn tác kiểu gì**
nếu hỏng giữa chừng.

## 5. State đổi ngoài tiến trình thì SỐNG DAI hơn tiến trình

`wm density/size/overscan`, app-op, settings… sống qua reboot. Mỗi thứ đổi ra ngoài phải có **đường trả lại** chạy
được cả khi tiến trình đã chết (ghi marker vào prefs *trước* khi đổi). **Cấm** quyết định bằng cờ RAM — kiểm bằng
sự thật (`am stack list`, `dumpsys`). Guard cứng đặt ở tầng **thi hành**, không ở UI.

## 6. Không đảo thứ tự đường đã chạy tốt

Đường mới **luôn xuống cuối** và phải tự đo đường cũ có hụt thật không rồi mới leo.

## 7. Generic, không case-by-case — kể cả theo hãng box

Không `if (pkg == …)`, không `if (hãng box == …)`. Khác biệt giữa app/máy phải lộ ra qua **đo năng lực** (có kênh
adb mạng? có trợ năng? có mic?) rồi rẽ nhánh theo kết quả đo (spec B3). Không có năng lực ⇒ ẩn chức năng, không
hiện nút chết.

## 8. Sau khi sửa: kiểm HÀM MỚI CÓ ĐƯỢC GỌI KHÔNG

`grep -rn "<tênHàmMới>" app/src/main core/src/main` phải thấy ít nhất một chỗ gọi ngoài định nghĩa. Compile xanh
không có nghĩa là code chạy. Ưu tiên sửa bằng chuỗi khớp chính xác, không theo số dòng/regex nhiều dòng.

## 9. Phiên bản: mỗi bản build đã báo owner = một số hiệu riêng

Box đánh số từ **1.0 (1)**. Sửa code sau khi đã báo APK ⇒ bump `versionCode` + `versionName`. Không bao giờ đoán
máy đang chạy bản nào — đọc `dumpsys package com.kachi.box | grep versionName`.

## 10. Test là thứ khoá lại bài học

Mỗi lỗi đã tìm gốc ⇒ **một test hồi quy** kèm comment nói nó khoá cái gì. Parser/policy ở `:core` là code thuần,
test off-device; hành vi phụ thuộc output `dumpsys`/`am` phải có fixture lấy nguyên văn từ dump thật.

## 11. Lấy log: app tự chụp, không bắt người dùng gõ adb

Cần dữ liệu gì thì cho app tự ghi (`kachi-logs/` dưới thư mục ngoài của app) — người dùng chỉ chụp màn hình/gửi tệp.

## 12. Tính năng mới: chứng minh bằng shell thô trên máy thật TRƯỚC, nối dây theo tầng SAU

Bốn tầng theo thứ tự, tầng sau chỉ bắt đầu khi tầng trước xanh với bằng chứng MỚI: (1) shell/adb thô trên máy
thật (máy ảo nếu cơ chế không phụ thuộc phần cứng), lưu evidence vào `docs/diagnostics/` · (2) `car-integration`
(transport/shell) · (3) `:core` policy thuần · (4) `:app`/UI. Dump/doc cũ chỉ để định hướng đo, không thay phép đo.

## 13. Có adb + source thì debug bằng dữ liệu, không dò UI

Thứ tự: đọc source (gate quyết định hành vi) → đọc state bền (`run-as … cat`) → `logcat`/`dumpsys` → UI là
phương án cuối (mỗi thao tác một ảnh chụp mới, đọc ảnh qua sub-agent).

## 14. Quy trình mỗi lần chạm code

0. Đọc `.kiro/steering/project-context.md` + `docs/README.md` + `docs/PROJECT-BACKLOG.md` + spec liên quan.
1. Root-cause tới source, ghi mức bằng chứng.
2. Sửa generic; thêm test hồi quy; tệp ≤ 500 dòng.
3. Build + test: `export JAVA_HOME=/opt/homebrew/opt/openjdk@17` · `local.properties` = `sdk.dir=<Android SDK>`
   (gitignored) · `./gradlew :app:assembleRelease test --continue` · đếm kết quả bằng `python3 scripts/count-tests.py`
   (đọc XML, đừng tin console). Release cần `keystore.properties` (gitignored, khoá `~/.kachi`, dùng chung bản BYD — OQ4).
4. Grep chỗ gọi hàm mới; bump version nếu đã báo APK.
5. Senior review sub-agent (Fable 5.1 — `claude-fable-5-1`), lặp tới 0 lỗi.
6. **Quét bảo mật TRƯỚC mọi commit/push** (sub-agent Fable): không bí mật, không tên thật/email (chỉ handle
   `dangkhoi`), không `/Users/<tên>`, không IP/biển số. Commit:
   `git -c user.name=dangkhoi -c user.email=dangkhoi@users.noreply.github.com commit …`.
7. Cập nhật spec (§Nhật ký triển khai) + INDEX + BACKLOG + project-context trong CÙNG phiên.
8. Hiệu năng là ưu tiên cao: mỗi thay đổi ghi chi phí (lệnh shell, luồng, nhịp định kỳ, bộ nhớ); không để thứ gì
   phình theo ngày tháng.

## 15. Luật bền ở `.kiro/steering/` — BẮT BUỘC đọc

`project-context.md` · `documentation-and-backlog.md` · `product-team-workflow.md` cùng hiệu lực với file này
(`trace-den-tan-cung.md` · `conversation-protocol.md` của bản BYD là tệp CỤC BỘ — `.kiro/` bị gitignore cho tệp mới —
và CHƯA có trong repo này; máy nào có thì cũng áp); mâu thuẫn thì lấy cái **nghiêm hơn** và ghi vào backlog để
owner chốt. ⚠ `project-context.md` hiện vẫn là tóm tắt của Kachi BYD (kế thừa) — phần HAL/cụm/HUD/camera chỉ là lịch
sử, không áp dụng cho box; viết lại khi xong B2.
