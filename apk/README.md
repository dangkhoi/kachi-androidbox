# apk/ — kênh OTA của Kachi Android box

| Bản | Tệp | Kích thước | SHA-256 | Đăng |
|---|---|---|---|---|
| **1.0 (1)** | `Kachi-box-1.0-release.apk` | 41 862 621 B | `953348c6c55665ffe4294610f9ee28e6b5aa641fdc168ce3a6dabbc5c422fc2f` | 2026-10-09 |

`applicationId` `com.kachi.box` · Android 10+ · arm64-v8a · chứng chỉ ký SHA-256 `92:57:49:9b:…:26:bb:99:17` · không debuggable.

App tự dò tệp `Kachi-box-<ver>-release.apk` trong thư mục này trên nhánh `main`. Mỗi lần đăng bản mới: thêm tệp mới, gỡ tệp cũ trong cùng commit. Kachi cho BYD dùng kênh riêng ở `github.com/dangkhoi/byd-kachi` — repo này KHÔNG phục vụ bản BYD, và tệp tên `Kachi-<ver>-release.apk` (khuôn BYD) bị bỏ qua.

The app polls this folder on `main` for `Kachi-box-<ver>-release.apk`. Version 1.0 (1) is the first release (`com.kachi.box`, Android 10+, arm64-v8a). Kachi for BYD has its own channel at `github.com/dangkhoi/byd-kachi`; files named with the BYD pattern are ignored.
