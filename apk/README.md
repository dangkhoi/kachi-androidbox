# apk/ — kênh OTA của Kachi Android box

| Bản | Tệp | Kích thước | SHA-256 | Đăng |
|---|---|---|---|---|
| **1.1 (2)** | `Kachi-box-1.1-release.apk` | 41 862 617 B | `956c3a15f4dc885918c886508e137ed1152199d69107d90b4d3c97df85c90dd8` | 2026-10-10 |
| 1.0 (1) | *(đã gỡ)* | 41 862 621 B | `953348c6c55665ffe4294610f9ee28e6b5aa641fdc168ce3a6dabbc5c422fc2f` | 2026-10-09 |

`applicationId` `com.kachi.box` · Android 10+ · arm64-v8a · chứng chỉ ký SHA-256 `92:57:49:9b:…:26:bb:99:17` · không debuggable.

App tự dò tệp `Kachi-box-<ver>-release.apk` trong thư mục này trên nhánh `main`. Mỗi lần đăng bản mới: thêm tệp mới, gỡ tệp cũ trong cùng commit. Kachi cho BYD dùng kênh riêng ở `github.com/dangkhoi/byd-kachi` — repo này KHÔNG phục vụ bản BYD, và tệp tên `Kachi-<ver>-release.apk` (khuôn BYD) bị bỏ qua.

The app polls this folder on `main` for `Kachi-box-<ver>-release.apk`. Version 1.1 (2) is current; 1.0 (1) was the first release (`com.kachi.box`, Android 10+, arm64-v8a). Kachi for BYD has its own channel at `github.com/dangkhoi/byd-kachi`; files named with the BYD pattern are ignored.

## Nội dung từng bản / Release notes

- **1.1 (2)** — 2026-10-10: box từng có adb mạng rồi mất (dấu duyệt cũ còn hạn) ⇒ trang *Hệ thống & quyền*, *Phím vật lý*, *Màn hình chính* và OTA đi đường tay (nút mở đúng màn Cài đặt hệ thống) thay cho câu "Kachi đang tự xin lại"; adb mạng quay lại ⇒ Kachi tự nối lại như cũ. · *A box that once had network adb and lost it now gets the manual system-settings buttons instead of "Kachi is re-requesting it"; reconnects by itself when adb comes back.*
- **1.0 (1)** — 2026-10-09: bản đầu. · *First release.*

