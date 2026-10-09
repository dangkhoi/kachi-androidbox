package com.byd.clusternav.launcher

import com.byd.clusternav.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Khoá DÂY NỐI của 3 bề mặt người dùng gói 2: bảng lốp 4 bánh (W4) · ô tick tự lấy gió (W3) · chọn đơn vị (R11–R13).
 *
 * Đây là test QUÉT SOURCE (không dựng được View trong JVM thuần). Mọi phép quét đi qua [code] để **bỏ chú thích
 * trước khi kiểm** — nếu không thì chỉ cần viết tên hàm vào một dòng comment là test xanh, tức là test tự lừa mình.
 */
class Goi2FeatureWiringContractTest {

    private fun code(relative: String): String = SourceRoots.codeOf(relative)

    private val widgets by lazy { code("src/main/java/com/byd/clusternav/launcher/WidgetViews.kt") }
    private val board by lazy { code("src/main/java/com/byd/clusternav/launcher/TyreBoardView.kt") }
    /** Nhóm "Tiện nghi xe" + "Hiển thị & đơn vị" của màn Cài đặt (S1·T3). */
    private val panel by lazy { code("src/main/java/com/byd/clusternav/launcher/SettingsSections.kt") }

    /** Nhóm "Màn hình chính" — lưới khả năng nằm ở đây (tách vì trần 500 dòng). */
    private val bridgeKt by lazy { code("src/main/java/com/byd/clusternav/launcher/ClusterNavBridge.kt") }
    private val bridgeSystemKt by lazy { code("src/main/java/com/byd/clusternav/launcher/ClusterNavBridgeSystem.kt") }
    private val drawerKt by lazy { code("src/main/java/com/byd/clusternav/launcher/AppDrawer.kt") }

    /** Dòng chọn đơn vị — chuyển sang bộ dựng dòng dùng chung (S1·T2). */
    private val rows by lazy { code("src/main/java/com/byd/clusternav/launcher/SettingsRows.kt") }
    private val panels by lazy { code("src/main/java/com/byd/clusternav/launcher/HomePanels.kt") }
    private val activity by lazy { code("src/main/java/com/byd/clusternav/launcher/KachiHomeActivity.kt") }
    private val boot by lazy { code("src/main/java/com/byd/clusternav/BootSetupService.kt") }
    private val prefs by lazy { code("src/main/java/com/byd/clusternav/launcher/WorkspacePrefs.kt") }

    // ── W4 · bảng áp suất lốp ────────────────────────────────────────────────────────────────────

    @Test
    fun `KHONG con chia 100 tai cho va KHONG con nguong cung 2 phay 2`() {
        // Đây là hai vết của bản cũ: registry khai kPa nhưng widget tự chia 100 ra bar, và ngưỡng "non" viết
        // thẳng vào bộ vẽ (< 2.2) — lệch với ngưỡng ở :core. Cả hai phải hết.
        assertFalse(widgets.contains("/ 100.0"), "không được tự đổi kPa→bar tại chỗ; phải đi qua UnitFormat")
        assertFalse(widgets.contains("< 2.2"), "không được có ngưỡng lốp viết cứng trong bộ vẽ")
        assertFalse(widgets.contains("fun tyreBars("), "hàm đổi đơn vị cũ phải bị xoá, không để song song")
    }

    // ── 2.74 · R2 · hình học ô giá trị: hằng ma thuật → hàm THUẦN ở :core ─────────────────────────

    // ── W3 · ô tick tự lấy gió trong — Android box B2 · W2e: gỡ cùng tiện nghi xe BYD ─────────────────────────────

    @Test
    fun `lay gio trong khi no may da go het`() {
        // Ba bài cũ (ô tick ở trang Tiện nghi xe · bật thì áp ngay · áp lúc nổ máy suy giảm an toàn) canh mã đã xoá.
        assertFalse(SourceRoots.exists("src/main/java/com/byd/clusternav/comfort/RecircApplier.kt"), "applier đã xoá")
        assertFalse(SourceRoots.exists("src/main/java/com/byd/clusternav/launcher/SettingsSectionsCar.kt"), "trang Tiện nghi xe đã xoá")
        listOf("RecircApplier", "Pm25FilterApplier", "SeatComfortApplier").forEach {
            assertFalse(boot.contains(it), "'$it' đã gỡ khỏi chuỗi khởi động")
        }
        listOf("recircOnStart", "setRecircOnStart").forEach { assertFalse(bridgeKt.contains(it), "cầu còn '$it'") }
        assertFalse(bridgeSystemKt.contains("applyRecircNow"), "cửa áp lấy gió ngay đã gỡ")
    }

    // ── R11–R13 · chọn đơn vị ────────────────────────────────────────────────────────────────────

    /**
     * Chữ THẬT sẽ hiện trên màn, đọc từ tệp tài nguyên bản Việt.
     *
     * ⚠ U5·T3 — trước đây bài này đọc chuỗi viết cứng trong `.kt`. Chữ nay nằm trong `res/values/strings_kachi.xml`,
     * nên phép kiểm phải đi tới đó: nếu chỉ kiểm *"mã có gọi khoá này không"* thì ai xoá nội dung câu cảnh báo vẫn
     * xanh. `app/build.gradle.kts` đã khai `inputs.dir("src/main/res")` nên đổi tệp đó là task chạy lại.
     */
    private fun res(name: String): String =
        Regex("""<string name="$name">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .find(SourceRoots.text("src/main/res/values/strings_kachi.xml"))
            ?.groupValues?.get(1)
            ?: error("không có chuỗi '$name' trong values/strings_kachi.xml — bài test đang quét vùng không tồn tại")

}
