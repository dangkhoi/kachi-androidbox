package com.kachi.box.launcher

/**
 * CATALOG cho các màn chọn của launcher (THUẦN → test JVM): widget dựng tay ([WidgetRegistry]) + hành động của chính
 * launcher ([LauncherActions]). Không kê tay: thêm một dòng ở hai bộ ấy ⇒ tự có một mục chọn được.
 *
 * ## Android box B2 · W3 (2026-10-09) — chỉ còn widget + hành động launcher
 * ≤ 2.98 BYD catalog gộp thêm nút xe (`ControlRegistry`), datum xe (`TelemetryRegistry`), nhóm khả năng
 * (`CapabilityGroups`), gói lệnh (`ActionMacros`) cùng lĩnh vực xe (`Domain`) và mức bằng chứng (`EvidenceTier`). Cả bộ ấy
 * gỡ cùng lõi HAL BYDAuto. Mã của chúng đã lưu trong ô / thanh nút của hồ sơ cũ là **mã lạ** ⇒ [CapabilityCatalog.kindOf]
 * trả `null` ⇒ `WorkspaceState.sanitized` cho ô rụng thành trống và thanh nút bỏ qua (không sập).
 */

/** Một widget dựng tay CHỌN ĐƯỢC. */
data class WidgetPick(
    val id: String,
    override val label: String,
    val icon: String,
    /** Nhãn tiếng Anh (U5 · T2) — chảy từ [WidgetDef.labelEn], không gõ lại ở đây. */
    override val labelEn: String? = null,
) : Localized

/** CATALOG WIDGET: widget dựng tay của [WidgetRegistry]. */
object WidgetCatalog {

    /** Widget dựng tay, theo thứ tự khai. */
    val CURATED: List<WidgetPick> = WidgetRegistry.ALL.map { WidgetPick(it.id, it.label, it.icon, it.labelEn) }

    /** Tra một widget theo id — cho UI dựng nhãn khi id đã nằm trong ô. */
    fun pick(id: String): WidgetPick? = CURATED.firstOrNull { it.id == id }
}

/**
 * Loại một khả năng chọn được: **ĐỌC** (widget dựng tay — xem, không bấm) hay **việc của launcher** (bấm được, không gửi
 * gì ra ngoài: mở ngăn kéo · Cài đặt · phiên nghe; khối lối tắt). (≤ 2.98 BYD còn `WRITE` = nút / gói lệnh xe.)
 */
enum class CapabilityKind {
    /** Widget dựng tay ([WidgetRegistry]) — hiển thị, KHÔNG bấm. */
    READ,

    /** Hành động của CHÍNH launcher ([LauncherActions]). */
    LAUNCHER,
}

/**
 * Một khả năng CHỌN ĐƯỢC để đặt vào ô / thanh nút — gộp widget lẫn việc launcher về MỘT hình dạng, để UI chỉ cần một danh
 * sách và một bộ dựng ô.
 *
 * @property curated `true` = widget dựng tay (có bố cục riêng).
 */
data class CapabilityPick(
    val id: String,
    override val label: String,
    val icon: String,
    val kind: CapabilityKind,
    val curated: Boolean = false,
    /** Nhãn tiếng Anh (U5 · T2) — **chảy từ bộ đăng ký gốc**, không gõ lại ở đây. */
    override val labelEn: String? = null,
) : Localized {

    override val displayLabel: String
        get() = Strings.pick(label, labelEn)

    /**
     * Gợi ý loại của ô này — `""` khi nhãn KHÔNG trùng, `"thẻ"`/`"bấm"` khi trùng ([CapabilityCatalog.collidingLabels],
     * phép phát hiện chạy trên nhãn GỐC tiếng Việt — xem lịch sử U6).
     */
    val typeHint: String get() = if (label in CapabilityCatalog.collidingLabels()) kindHint else ""

    /** 1.95 — LOẠI Ô luôn hiển thị (huy hiệu nhỏ ở góc ô picker). */
    val kindLabel: String get() = kindHint

    /** DÒNG PHỤ để hiển thị: gợi ý loại (khi trùng). (≤ 2.98 BYD còn nội dung nhóm khả năng — gỡ cùng nhóm.) */

    val displaySub: String get() = typeHint

    private val kindHint: String
        get() = when {
            curated -> Strings.t("thẻ", "card")
            kind == CapabilityKind.READ -> Strings.t("xem", "view")
            else -> Strings.t("bấm", "press")
        }
}

/**
 * TRA CỨU KHẢ NĂNG — lớp mỏng nằm TRÊN hai bộ đăng ký (widget dựng tay · việc launcher), KHÔNG trộn dữ liệu của chúng.
 *
 * Không gian mã PHẲNG: hai bộ giao nhau rỗng ([collisions] luôn rỗng, bị test khoá) ⇒ tra cứu chỉ cần `id`.
 */
object CapabilityCatalog {

    /** [CapabilityKind] của [id], hoặc `null` nếu mã không thuộc bộ nào (mã cũ đã xoá — kể cả nút/datum xe ≤ 2.98). */
    fun kindOf(id: String): CapabilityKind? = when {
        WidgetRegistry.byId(id) != null -> CapabilityKind.READ
        LauncherActions.byId(id) != null -> CapabilityKind.LAUNCHER
        else -> null
    }

    /** Một khả năng theo [id], hoặc `null` nếu mã lạ (⇒ chỗ gọi suy giảm an toàn, KHÔNG sập). */
    fun pick(id: String): CapabilityPick? {
        WidgetRegistry.byId(id)?.let { return widgetPick(it) }
        LauncherActions.byId(id)?.let { return launcherPick(it) }
        return null
    }

    private fun widgetPick(w: WidgetDef): CapabilityPick =
        CapabilityPick(w.id, w.label, w.icon, CapabilityKind.READ, curated = true, labelEn = w.labelEn)

    private fun launcherPick(a: LauncherActionDef): CapabilityPick =
        CapabilityPick(a.id, a.label, a.icon, CapabilityKind.LAUNCHER, labelEn = a.labelEn)

    /** MỌI khả năng: widget dựng tay → việc launcher (thứ tự khai trong từng bộ được giữ). */
    fun all(): List<CapabilityPick> = buildList {
        WidgetRegistry.ALL.forEach { add(widgetPick(it)) }
        LauncherActions.placeable.forEach { add(launcherPick(it)) }
    }

    /** Nhãn xuất hiện NHIỀU HƠN MỘT LẦN trên toàn bộ khả năng — tính một lần rồi giữ. */
    fun collidingLabels(): Set<String> = collidingLabelsCache

    private val collidingLabelsCache: Set<String> by lazy {
        (WidgetRegistry.ALL.map { it.label } + LauncherActions.placeable.map { it.label })
            .groupBy { it }.filterValues { it.size > 1 }.keys
    }

    /** Mã xuất hiện ở NHIỀU HƠN MỘT bộ đăng ký. **Phải luôn rỗng** — bị test khoá. */
    fun collisions(): List<String> {
        val w = WidgetRegistry.ALL.map { it.id }
        val l = LauncherActions.placeable.map { it.id }
        return (w + l).groupBy { it }.filterValues { it.size > 1 }.keys.sorted()
    }
}
