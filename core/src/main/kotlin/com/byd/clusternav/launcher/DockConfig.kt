package com.byd.clusternav.launcher

/** Vị trí thanh điều khiển trên viền màn (owner: đặt được 4 viền). */
enum class DockEdge {
    BOTTOM, LEFT, RIGHT, TOP;

    /**
     * Nhãn cho người đọc (S1). Trước S1 viền chỉ đổi được bằng pill **"Thanh"** ở thanh trên xoay vòng 4 viền, nên
     * không nơi nào cần chữ; màn Cài đặt bày cả 4 viền để **chọn thẳng** (không phải bấm ba lần để tới viền mình
     * muốn) nên phải có chữ. Pill đó nay đã **bỏ hẳn** ⇒ đây là đường duy nhất, và nó cần nhãn.
     * Ở `:core` cùng lý do với [LayoutPreset.label].
     *
     * U5 · T2: nhãn sinh ra bằng `when` (không phải một dòng dữ liệu) ⇒ dịch tại chỗ bằng [Strings.t] — xem KDoc
     * [Strings] về hai cơ chế. Trả tiếng Việt khi [Strings.current] = [Lang.VI], nên test cũ giữ nguyên.
     */
    val label: String
        get() = when (this) {
            BOTTOM -> Strings.t("Dưới", "Bottom")
            LEFT -> Strings.t("Trái", "Left")
            RIGHT -> Strings.t("Phải", "Right")
            TOP -> Strings.t("Trên", "Top")
        }
}

/**
 * Cấu hình thanh (bền qua prefs): viền + danh sách id đang hiện (thứ tự = thứ tự hiển thị).
 *
 * **RW0 (2026-09-10)**: danh sách này nay nhận **cả thông tin ĐỌC lẫn HÀNH ĐỘNG** — xem [setEnabled].
 * Định dạng lưu KHÔNG đổi (vẫn là danh sách mã trần) ⇒ cấu hình người dùng đã lưu đọc lên nguyên vẹn (spec R4).
 */
data class DockConfig(
    val edge: DockEdge = DockEdge.BOTTOM,
    val enabled: List<String> = DEFAULT_ENABLED,
    /**
     * S1b (owner 2026-09-14: *"thêm chức năng cho ẩn hiện thanh luôn nhé, bên cạnh việc đặt ở trên dưới trái phải"*).
     * `false` ⇒ thanh nút KHÔNG hiện, vùng ô lấp trọn màn (xem [DockAreaLayout]). Là **lựa chọn tách khỏi [edge]**
     * (không phải viền thứ 5): ẩn rồi hiện lại giữ nguyên viền đã chọn thay vì quên mất về BOTTOM. Theo hồ sơ
     * ([ProfileScope] `dock_visible`) như [edge].
     */
    val visible: Boolean = true,
    /**
     * 2.89 · B3 DOCK-SCALE (owner 05/10 *"50-150% đi"*) — cỡ thanh nút theo %, [BarScale.MIN]..[BarScale.MAX] bước
     * [BarScale.STEP]; [BarScale.DEFAULT] = hôm nay từng pixel. Tham số CUỐI ⇒ mọi lời gọi vị trí cũ biên dịch nguyên, và
     * mọi `copy` sẵn có ([withEdge] · [withVisible] · [setEnabled] · [moveEnabled] · `DockSelection.apply`) tự giữ số này.
     * Theo hồ sơ ([ProfileScope] `dock_scale`).
     */
    val scalePct: Int = BarScale.DEFAULT,
) {
    fun withEdge(e: DockEdge): DockConfig = copy(edge = e)

    /** B3 — đổi cỡ thanh (đã [BarScale.snap]); giữ nguyên viền, nút, ẩn/hiện. */
    fun withScale(pct: Int): DockConfig = copy(scalePct = BarScale.snap(pct))

    /** Ẩn/hiện thanh nút — giữ nguyên [edge] và [enabled] để hiện lại đúng chỗ cũ. */
    fun withVisible(v: Boolean): DockConfig = copy(visible = v)

    /**
     * Bật/tắt một **khả năng** trong thanh.
     *
     * TRƯỚC RW0 chỗ này chặn: `if (ControlRegistry.byId(id) == null) return this` ⇒ mọi mã KHÔNG phải nút bị **bỏ
     * qua im lặng**, nên thông tin đọc (áp suất lốp, phần trăm pin…) không bao giờ vào được thanh. Đó là **cổng chặn
     * thật** của yêu cầu "đặt được ở cả 3 vùng" (spec Đ3/R2).
     *
     * NAY nhận mọi mã có trong [CapabilityCatalog] (đọc HOẶC hành động). Vẫn từ chối mã lạ — giữ nguyên tính chất
     * "không nhét rác vào cấu hình bền" của bản cũ.
     */
    fun setEnabled(id: String, on: Boolean): DockConfig {
        // Android box B2 · W3 (2026-10-09): thanh nút chỉ còn hành động LAUNCHER — `ControlDockView` bỏ qua mã
        // ĐỌC (widget), nên nhận chúng ở đây là tái tạo đúng lỗi "đặt 10 hiện 6" (`DockSelection.sanitize`).
        if (CapabilityCatalog.kindOf(id) != CapabilityKind.LAUNCHER) return this
        val cur = enabled.toMutableList()
        if (on) { if (id !in cur) cur.add(id) } else cur.remove(id)
        return copy(enabled = cur)
    }
    fun isVertical(): Boolean = edge == DockEdge.LEFT || edge == DockEdge.RIGHT

    /**
     * ═══ UX-OVERHAUL · WP4 — DỜI CHỖ MỘT NÚT TRONG THANH ════════════════════════════════════════════════════
     *
     * [enabled] đã là **danh sách có thứ tự** (thứ tự = thứ tự nút hiện trên thanh) từ RW0, nhưng tới 1.85 **không
     * có bề mặt nào sắp lại được nó**: [DockSelection.apply] cố ý giữ nguyên chỗ của phần cũ và nối phần mới vào
     * cuối (xem KDoc ở đó — xáo lại theo thứ tự catalog là một lỗi đã tránh). Nên nút vào thanh muộn thì **mãi mãi**
     * ở cuối. WP4 · R4.1 vá đúng chỗ ấy.
     *
     * Uỷ quyền [BarOrder.move] — CÙNG phép với thứ tự thanh trên ([HeaderLayout.move]), không viết bản thứ hai.
     * Không dời được (đụng biên / mã không có trong thanh) ⇒ trả về **chính** vật này, để chỗ gọi biết mà không
     * nhân đôi luật.
     */
    fun moveEnabled(id: String, delta: Int): DockConfig {
        val next = BarOrder.move(enabled, id, delta)
        return if (next === enabled) this else copy(enabled = next)
    }

    /** Dời được nút [id] theo [delta] hay không — CÙNG luật với [moveEnabled]. */
    fun canMove(id: String, delta: Int): Boolean = BarOrder.canMove(enabled, id, delta)

    companion object {
        /**
         * Android box W0 (2026-10-09, spec `androidbox-plan.html` §4.1) — thanh nút mặc định chỉ gồm **hành động của
         * launcher** ([LauncherActions]): mở ngăn kéo · Cài đặt · phiên nghe · khối lối tắt. Trước đó mặc định là các nút
         * xe `enabledByDefault` của `ControlRegistry` — trên Android box không có xe ⇒ thanh nút xe chết.
         * Cấu hình đã lưu (danh sách mã) không đổi; chỉ máy chưa từng lưu thanh mới nhận mặc định này.
         */
        val DEFAULT_ENABLED: List<String> = listOf(
            LauncherActions.APPS,
            LauncherActions.SETTINGS,
            LauncherActions.VOICE,
            LauncherActions.SHORTCUTS,
        )
    }
}
