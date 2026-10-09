package com.byd.clusternav.system

/**
 * Android box W0 (2026-10-09, spec `docs/specs/androidbox-plan.html` §4.1 · kiểm kê
 * `docs/diagnostics/androidbox-b2-inventory-2026-10-09.md` §2.1) — dấu hiệu màn camera của máy đang chạy.
 *
 * Kachi BYD đọc dấu này từ `ClusterProfile.cameraSignature` (đời xe ĐÃ đo có app camera `com.byd.avc/…`; đời chưa đo ⇒
 * `null` = "chưa biết" ⇒ CHẶN mọi lệnh đưa app lên trước màn nhà). Android box KHÔNG có màn camera của hãng nào ⇒
 * [SIGNATURE] = `null` mang nghĩa **"không có camera"** ⇒ lệnh HOME / K10 / K7 chạy TRẦN (không bọc `case` camera) —
 * ngữ nghĩa ngược với bản BYD, có chủ ý. Mã rào camera (`CameraGuard`, dấu `com.byd.avc/`) vẫn còn để đợt W2b xoá.
 *
 * Một chỗ duy nhất: mọi chỗ gọi trước đây đọc `ClusterProfile.resolveCached(..).cameraSignature` nay đọc hằng này.
 */
object CameraPresence {
    /** `null` = máy không có màn camera ⇒ không rào. */
    val SIGNATURE: String? = null
}
