# Brief chung cho mọi đợt B2 (Android box) — đọc trước khi làm

- Repo: thư mục gốc repo `kachi-androidbox` (KHÔNG BAO GIỜ đụng ../byd-launcher).
- Đọc: CLAUDE.md, docs/specs/androidbox-plan.html §4.1, docs/diagnostics/androidbox-b2-inventory-2026-10-09.md (bản đồ tệp + điểm dính + test ghim).
- Mục tiêu sản phẩm: launcher Android box chung (ô, widget không-xe, hồ sơ, giọng nói offline cho lệnh launcher, nhạc/YouTube, lối tắt, gán phím qua trợ năng, OTA, Cài đặt). Gỡ mọi thứ chỉ-BYD.
- Quyết định đã chốt: GIỮ lịch tự dẫn đường (ScheduledNav) + quyền định vị; GIỮ BootSetupService rút gọn; KHÔNG đổi tên NavNotificationListener / NavAccessibilityService; đồng hồ bỏ nhiệt độ ngoài trời; GIỮ A11yLifecycleHeal (lỗi AOSP) nhưng bỏ phần dính cast/camera.
- Build/test: `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`; `./gradlew :app:assembleDebug` và `./gradlew test --continue` phải xanh; đếm bằng `python3 scripts/count-tests.py` (đọc XML). Nếu module bị xoá thì task test của nó không còn — bình thường.
- Test ghim mã bị xoá: xoá cùng. Test quét nguồn/manifest của phần GIỮ: sửa cho đúng ý định mới, KHÔNG làm yếu bài để cho qua. Một lỗi tìm ra ⇒ một test hồi quy.
- Tệp ≤ 500 dòng. Sửa bằng chuỗi khớp chính xác; CẤM regex nhiều dòng để sửa test hàng loạt. Sau khi xoá: `grep` chắc không còn tham chiếu treo.
- Hiệu năng: không thêm luồng/nhịp/lệnh shell mới.
- KHÔNG commit, KHÔNG push, KHÔNG đổi versionCode.
- Cuối việc: append một mục vào §9 "Nhật ký triển khai" của docs/specs/androidbox-plan.html (ngày 2026-10-09, đợt Wx: đã xoá/sửa gì, quyết định/sai lệch, số test [ĐO]). Trả về báo cáo ngắn: tệp xoá/sửa, quyết định tự đưa ra, test cuối (tests/failures), việc để đợt sau.
