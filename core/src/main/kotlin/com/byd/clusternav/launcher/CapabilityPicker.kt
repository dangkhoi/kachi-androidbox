package com.byd.clusternav.launcher

/**
 * Phần THUẦN của màn chọn khả năng (ngăn kéo cho ô giữa màn · Cài đặt cho thanh nút) — `:core`, kiểm off-device.
 *
 * Android box B2 · W3 (2026-10-09): mục NHÓM khả năng xe (G1 · T4: `groupPicks` · `singlesOf` · `groupHint` · tiêu đề
 * nhóm/mục rời) và đếm mục *"chưa kiểm trên xe"* gỡ cùng `CapabilityGroups` / `EvidenceTier`. Còn khối việc launcher + số cột.
 */
object CapabilityPicker {

    /** Tiêu đề khối **Launcher** (S4 · R12) — xem [launcherPicks]. */
    val LAUNCHER_TITLE: String
        get() = Strings.t("Launcher — việc của chính màn hình này", "Launcher — this screen's own actions")

    /** Câu phụ của khối Launcher: nói VIỆC, không nói kiến trúc (cùng luật [GROUPS_NOTE]). */
    val LAUNCHER_NOTE: String
        get() = Strings.t(
            "Không gửi lệnh nào xuống xe: mở danh sách ứng dụng, mở Cài đặt, nói với xe, khối lối tắt ứng dụng.",
            "These send nothing to the car: open the app list, open Settings, talk to the car, the app-shortcut block.",
        )

    /**
     * ═══ S4 · R12 — HAI Ô *Ứng dụng* · *Cài đặt* CHO BỘ CHỌN NÚT THANH XE ═════════════════════════════════════
     *
     * Lọc theo **loại** ([CapabilityKind.LAUNCHER]) trên [CapabilityCatalog.all], không kê tay hai mã: thêm một
     * hành động launcher về sau là tự có ô, đúng tinh thần registry-driven của cả tệp này.
     *
     * ## Vì sao khối này đứng ĐẦU bộ chọn nút, trước cả mục Nhóm
     * §4.2 (*"xếp Nhóm lên trước"*) nói về **khả năng của XE**: nó kéo 12 nhóm lên trước 187 mục rời để không ai
     * phải cuộn qua cả trăm ô mới thấy cái bảng lốp. Khối này chỉ **2 ô** (nửa hàng) nên nó không đẩy Nhóm xuống
     * theo nghĩa có thật, trong khi để nó ở CUỐI thì muốn đặt nút *Ứng dụng* lại phải cuộn qua trọn 187 ô — đúng
     * cái bệnh §4.2 sinh ra để chữa, chỉ ở đầu kia của trang.
     *
     * ## Vì sao KHÔNG có ở ngăn kéo gán-ô (ô giữa màn)
     * Ô giữa màn là khung lớn nhất của HOME; dùng nó chỉ để mở ngăn kéo là đổi chỗ đắt lấy việc rẻ, mà ngăn kéo
     * thì đã mở được từ thanh trên. Không cần phép lọc nào ở đó: hành động launcher khai `domain = null` nên
     * [CapabilityCatalog.byDomain] vốn đã không bày chúng.
     */
    fun launcherPicks(): List<CapabilityPick> =
        CapabilityCatalog.all().filter { it.kind == CapabilityKind.LAUNCHER }

    /**
     * SỐ CỘT của mọi lưới ô khả năng — **một con số cho cả hai màn chọn**.
     *
     * ## ⚠⚠ Vì sao nó ở `:core` chứ không ở mỗi màn
     * Đây đúng bệnh mà KDoc lớp này nói: có HAI màn chọn ([AppDrawer] cho ô giữa màn · màn Cài đặt cho thanh nút xe)
     * và chúng bày **chính những ô ấy**. Khi mỗi màn tự chọn số cột thì chúng lệch nhau, và [ĐO] 2026-09-12 đã lệch
     * thật: ngăn kéo để Nhóm **3** cột rồi các phần dưới **4** cột **trong CÙNG một vùng cuộn** ⇒ cuộn xuống là tâm
     * cột nhảy (owner báo *"chọn app vào ô lệch loạn"*), còn màn Cài đặt lại để mục lẻ **5** cột. Ba con số cho một
     * quyết định = ba chỗ phải sửa, và lần này chỉ hai chỗ được sửa.
     *
     * Một vùng cuộn phải có MỘT lưới cột. Danh sách **app** thì khác loại (icon nhỏ, không phải ô khả năng) nên nó
     * giữ số cột riêng ở tầng vẽ — cố ý không gộp vào đây.
     *
     * Không phải số dp (nó là một phép ĐẾM, không phải khoảng cách) nên nó không thuộc thang `KachiSpace`.
     */
    const val COLS = 4
}
