package com.byd.clusternav.launcher

import com.byd.clusternav.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ V-CLUSTER — nối dây tầng `:app` của *"Phần cụm lưu hết thành profile"* (owner 2026-09-30) ═══════════════════════
 *
 * Spec `docs/specs/kachi-profiles-are-everything.html` §11.3 VC-R5/R7/R9 · §11.5 A2–A6. Đọc source đã bỏ chú thích
 * ([SourceRoots.codeOf]) và cắt thân bằng [SourceRoots.body] — không `substringAfter/Before` (quét tràn = test giả).
 *
 * Android box B2 · W2c: chiếu cụm gỡ — còn canh đổi hồ sơ không chạm cụm/HUD/app khác (lời gọi BYD không mọc lại).
 */
class ClusterProfileSwitchWiringContractTest {

    private fun launcher(name: String) = SourceRoots.codeOf("src/main/java/com/byd/clusternav/launcher/$name")

    // ── VC-R5 — đổi hồ sơ KHÔNG dựng/gỡ phiên chiếu, KHÔNG lệnh wm/am mới lên cụm ─────────────────────────────

    /**
     * Mọi thứ [ClusterNavBridge.reapplyAll] được phép làm là gọi lại applier CÓ SẴN. Một lời gọi mở/đóng projection,
     * dispatch, resize, áp hình học hay một chuỗi `wm`/`am` trong thân này là cụm đổi trước mặt người lái vì một cú
     * chạm chip hồ sơ — đúng thứ VC-R5 cấm. `cast_enabled` đi đường HOÃN (bản chờ), không đi qua đây.
     */
    @Test
    fun `reapplyAll khong dung go phien chieu va khong ban lenh len cum`() {
        val body = SourceRoots.body(launcher("ClusterNavBridgeReapply.kt"), "internal fun ClusterNavBridge.reapplyAll()")
        listOf(
            "setCastEnabled", "applyCastPendingNow", "openProjection", "closeProjection", "closeOrphanProjection",
            "dispatch(", "applySplitRatioLive", "setSplitPct", "applyPinned", "applySavedProfile", "resizeActive",
            "setDensity", "setGeometry", "resetGeometry", "restoreCluster", "deepRescue", "FloatingBubbleService",
            "coordinator", "\"wm ", "\"am ",
        ).forEach { bad -> assertFalse(body.contains(bad), "reapplyAll chứa '$bad' — lượt đổi hồ sơ chạm phiên chiếu/cụm") }
        assertTrue(body.contains("AutomationService.sync(app)"), "A3: luật dẫn đường theo hồ sơ cần đồng bộ FGS")
    }

    /**
     * Review 2.89 Pass 3 · vietmap-dock-r2-2 — đổi hồ sơ giữa phiên phải chạy lại lượt điều kiện nền (lượt SẴN của hồ sơ trước có thể
     * đã TRẢ appop vẽ nổi của VietMap; hồ sơ mới bật bóng thì cần nó lại) — đúng lượt nền `AppPrereqs.onReady` (luồng riêng, không mở
     * app), không phải `VietMapAutostartService` (mở app — cố ý bỏ). Thử ĐỎ: xoá dòng `step("app.prereqs")`.
     */
    @Test
    fun `Pass 3 - doi ho so khong cham applier BYD, khong mo app`() {
        // Android box B2 · W1 — ĐỔI GHIM: lượt áp lại sau đổi hồ sơ không còn chạy applier chỉ-BYD (điều kiện nền VietMap ·
        // cụm/HUD · biển báo · bong bóng · ghế · lọc bụi · camera); chỉ còn đồng bộ automation + `:wake`.
        val body = SourceRoots.body(launcher("ClusterNavBridgeReapply.kt"), "internal fun ClusterNavBridge.reapplyAll()")
        listOf("AppPrereqs", "speedSign", "NavRepository", "VmOverlayPosition", "VmBubbleVisibility",
            "SeatComfortApplier", "Pm25FilterApplier", "CameraReapply").forEach { assertFalse(body.contains(it), "applier BYD '$it' còn trong reapplyAll") }
        assertTrue(body.contains("step(\"automation.sync\") { AutomationService.sync(app) }"), body)
        assertTrue(body.contains("step(\"voice.wake\") { VoiceWakeService.sync(app) }"), body)
        listOf("VietMapAutostartService", "AppPrereqs.ensure(", "startForAppOpen").forEach {
            assertFalse(body.contains(it), "đổi hồ sơ không được mở app (`$it`)")
        }
    }

    /** Lượt đổi hồ sơ ở tầng dữ liệu: chụp → trỏ → áp → reapplyAll, và KHÔNG một đường nào vào coordinator. */
    @Test
    fun `switchProfile khong cham coordinator`() {
        val body = SourceRoots.body(launcher("PrefsWorkspaceRepository.kt"), "override fun switchProfile(name: String): HomeUiState")
        listOf("SimpleCastRuntime", "coordinator", "setCastEnabled", "Projection", "dispatch(", "FloatingBubbleService")
            .forEach { bad -> assertFalse(body.contains(bad), "switchProfile chứa '$bad'") }
    }

    // Android box B2 · W2c — các bài VC-R7/VC-R9 (chốt bản chờ `cast_enabled`, *Áp ngay*, hình học/tỉ lệ phiên chiếu, vị trí
    // bong bóng VietMap) gỡ cùng mã chiếu cụm; ba bài trên còn canh rằng đổi hồ sơ không chạm cụm/HUD/app khác.
}
