package com.byd.clusternav.launcher

/**
 * QUYẾT ĐỊNH THUẦN của các điều khiển ClusterNav dựng lại trong Kachi Settings (IA v2 · §4.2, cuối mục).
 *
 * ## Vì sao tách ra `:core` thay vì để trong section/bridge ở `:app`
 * Mỗi hàm ở đây là một **luật đã có thật ở màn cũ** nhưng đang nằm lẫn trong tay một `View`: loại trừ hai công tắc
 * tự-chiếu nằm trong `setOnCheckedChangeListener` (CastAutostart.kt:32–61), ba trạng thái phím-thoại nằm trong
 * `refreshVoiceKeyStatus` (MainActivity.kt:998–1016), phép quy đổi chế độ cụm nằm trong `Prefs.navClusterScreenMode`.
 * Chép nguyên chúng sang section mới nghĩa là có **hai bản** của cùng một luật, ở hai màn, và bản thứ hai sẽ lệch —
 * đúng bẫy hai-bản-sao mà dự án đã trả giá bốn lần. Ở đây thì luật là dữ liệu/hàm thuần, **kiểm off-car**, và cả hai
 * màn có thể chỉ vào cùng một chỗ.
 *
 * ⚠ Mọi con số/mã chuỗi ở đây đều **đọc từ mã đang chạy**, không đoán: mã lưu của chủ đề lấy từ
 * `com.byd.clusternav.ThemeMode.Choice.code` (`"system"`/`"light"`/`"dark"` — KHÔNG phải `"auto"`), giá trị chế độ cụm
 * lấy từ `Prefs.NAV_SCREEN_*`. Android box B2 · W2c: nấc cỡ biển báo tốc độ gỡ cùng biển báo.
 */
object ClusterNavSettingsModel {

    // ── Chủ đề sáng/tối: MỘT công tắc, hai chỗ lưu (IA v2 · R3) ──────────────────────────────────

    /**
     * Mã lưu của `com.byd.clusternav.ThemeMode.Choice` tương ứng với [mode] của launcher.
     *
     * ⚠ [ĐO] `app/src/main/java/com/byd/clusternav/ThemeMode.kt:26–30` — ba mã thật là `"system"`, `"light"`,
     * `"dark"`. [ThemeMode.AUTO] của launcher ("theo giờ 18h–6h") ánh xạ sang `"system"` ("Theo xe") vì đó là ô
     * *"không ép gì cả"* của màn cũ: cả hai đều trả lời *"tôi không tự chọn, cứ để nó tự"*. Không có mã `"auto"` —
     * viết `"auto"` ở đây thì `Choice.fromCode` lùi im lặng về `SYSTEM`, tức đúng-kết-quả-vì-may, và ngày ai đó thêm
     * một mã mới thì nó sai mà không ai biết.
     *
     * KHÔNG khớp tuyệt đối về **hành vi**: `AUTO` của launcher xem đồng hồ, `SYSTEM` của màn cũ xem cờ `uiMode` của
     * xe. Đó là sai lệch có chủ ý và đã được nói rõ ở KDoc `SettingsSections.display` — launcher dựng view bằng mã
     * nên nó không nhận được thông báo khi xe đổi chế độ.
     */
    fun themeChoiceCode(mode: ThemeMode): String = when (mode) {
        ThemeMode.DAY -> "light"
        ThemeMode.NIGHT -> "dark"
        ThemeMode.AUTO -> "system"
    }

    // Android box B2 · W2c — tỉ lệ chia đôi + hai công tắc tự-chiếu (chiếu cụm) + nấc cỡ biển báo gỡ cùng chiếu cụm.

    // ── Phím vô-lăng: ba trạng thái ──────────────────────────────────────────────────────────────

    /** Trạng thái dịch vụ phím vô-lăng — ba ca của `refreshVoiceKeyStatus` (MainActivity.kt:998–1016). */
    enum class VkStatus { OFF, ACTIVE, DISCONNECTED }

    /**
     * [ĐO] MainActivity.kt:998–1016: tắt (xám) · bật + đã nối (xanh ✓) · bật + chưa nối (đỏ, gợi ý bấm "Sửa ngay").
     *
     * Thứ tự hai câu hỏi có nghĩa và không đảo được: **tắt thì không hỏi tiếp**. `bound` là ground-truth của dịch vụ
     * Hỗ trợ (`NavAccessibilitySource.connected`) và nó có thể còn `true` một lúc sau khi người dùng tắt công tắc —
     * hỏi `bound` trước sẽ báo "đang hoạt động" cho một tính năng vừa bị tắt.
     */
    fun voiceKeyStatus(enabled: Boolean, bound: Boolean): VkStatus = when {
        !enabled -> VkStatus.OFF
        bound -> VkStatus.ACTIVE
        else -> VkStatus.DISCONNECTED
    }

    // ── Chế độ hiện dẫn đường trên cụm ───────────────────────────────────────────────────────────

    /**
     * Hai lựa chọn THẬT của `nav_cluster_screen_mode`.
     *
     * ⚠ [ĐO] `Prefs.kt:60–78`: hằng có bốn giá trị (OFF 0 · SIMPLE 1 · SMALL 2 · FULL 3) nhưng **UI chỉ được ghi hai
     * cái**. SIMPLE/SMALL là số đoán, chưa bao giờ chứng minh được trên trim này (chúng đụng bức tường no-root), còn
     * FULL=3 là giá trị đã đo `rc=0`. `Prefs.navClusterScreenMode` còn **quy đổi lúc đọc**: mọi giá trị khác OFF đều
     * về FULL. Bày lại SIMPLE/SMALL ra bộ chọn là mời người dùng chọn một thứ mà lượt đọc kế tiếp sẽ âm thầm đổi.
     */
    enum class NavClusterMode(val value: Int, val label: String, val labelEn: String) {
        OFF(0, "Tắt", "Off"),
        ON(3, "Bật (Giữa + ETA)", "On (centre + ETA)"),
    }

    /** Hai nấc của bộ chọn, theo thứ tự hiện ra. */
    fun clusterModeOptions(): List<NavClusterMode> = listOf(NavClusterMode.OFF, NavClusterMode.ON)

    /**
     * Quy đổi giá trị ĐÃ LƯU → nấc hiện trên bộ chọn, **giống hệt** `Prefs.navClusterScreenMode`: chỉ `0` là Tắt,
     * mọi giá trị khác (kể cả SIMPLE/SMALL đời cũ) về Bật. Lệch phép quy đổi này thì bộ chọn hiện một nấc còn runtime
     * dùng nấc khác — sai im lặng, và chỉ lộ ra khi nhìn cụm.
     */
    fun clusterModeOf(stored: Int): NavClusterMode =
        if (stored == NavClusterMode.OFF.value) NavClusterMode.OFF else NavClusterMode.ON
}
