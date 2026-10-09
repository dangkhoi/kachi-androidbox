package com.byd.clusternav.system

/**
 * Cổng "chỉ chạy khi màn nhà Kachi đang hiện trên display 0" — dựng chuỗi `sh` thuần (`:core`).
 *
 * Android box B2 · W2b (2026-10-09): tách từ `CameraGuard.onHomeUnlessCamera` (rào camera BYD, đã xoá) đúng PHẦN không
 * liên quan camera — Android box không có màn camera của hãng nào, nhưng lệnh đưa app lên trước display 0 (K7 *Toàn màn*
 * khi app ở ô · K10 *Mở bình thường* của chuyến) vẫn KHÔNG được giành màn hình với app khác người dùng vừa mở.
 *
 * ## Phép đo
 * `grep -A2 "displayId=0 "` lấy mọi stack của display 0 (tiêu đề · `configuration=` · dòng task đầu), grep thứ hai chỉ
 * giữ dòng `visible=true`. Cờ `visible` và `topActivity` là của STACK (A10 `RootActivityContainer.java:1276,1303-1304`)
 * và được in trên mọi dòng task ⇒ dòng task đầu là đủ. Đọc hỏng (chuỗi rỗng) ⇒ không chạy. Một dòng `visible=true` mang
 * BẤT KỲ dạng in nào của màn nhà ([onHome] `homeComps`) ⇒ chạy [onHome] `cmd`; khác ⇒ không.
 *
 * ## Ràng buộc chuỗi
 * Không có dấu `'` ở bất cứ đâu — chuỗi có thể nằm trong `sh -c '…'`. Component phải hợp lệ (chữ, số, `.`, `_`, `/`);
 * chuỗi khác ⇒ ném, không bao giờ trả một lệnh có thể chèn shell. `$` (lớp lồng) bị từ chối: trong `case` nó là biến.
 */
object HomeGate {

    private val SAFE = Regex("[A-Za-z0-9_./]+")

    private const val READ = "c=\$(am stack list | grep -A2 \"displayId=0 \" | grep \"visible=true\") ; "

    /** Chạy [cmd] chỉ khi một dòng `visible=true` của display 0 có một trong [homeComps] (mỗi cái `pkg/cls`). */
    fun onHome(homeComps: List<String>, cmd: String): String {
        require(homeComps.isNotEmpty() && homeComps.all { it.matches(SAFE) }) { "component HOME không hợp lệ: $homeComps" }
        require(cmd.isNotBlank() && !cmd.contains('\'')) { "lệnh rỗng hoặc có dấu ' — không nằm được trong sh -c" }
        // `"$comp "` có dấu cách cuối: dòng task in `<comp> bounds=…` ⇒ khớp ĐÚNG component, không dính tên dài hơn cùng
        // tiền tố (`…KachiHome` ≠ `…KachiHomeActivity`).
        return READ + "case \"\$c\" in \"\") ;; " + homeComps.joinToString("|") { "*\"$it \"*" } + ") $cmd ;; esac"
    }
}
