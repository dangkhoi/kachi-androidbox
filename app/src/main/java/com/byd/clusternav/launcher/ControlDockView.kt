package com.byd.clusternav.launcher

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.byd.clusternav.launcher.KachiTheme.c
import com.byd.clusternav.launcher.KachiTheme.dpi
import com.byd.clusternav.launcher.KachiBars as Bars
import com.byd.clusternav.launcher.KachiSpace as Sp

/**
 * Thanh nút — thẻ kính bo góc trên nền wall. Nhận các **việc của launcher** ([CapabilityKind.LAUNCHER]: *Ứng dụng* ·
 * *Cài đặt* · *Nói với xe*) và **khối lối tắt ứng dụng** ([LauncherActions.SHORTCUTS]).
 *
 * Android box B2 · W3 (2026-10-09): ô nút xe (`ControlTileFactory` — TOGGLE · STEP · COVER · SELECT · BUTTON), ô gói lệnh và
 * ô ĐỌC datum xe gỡ cùng lõi HAL BYDAuto. Mã của chúng còn trong `DockConfig.enabled` của hồ sơ cũ là mã lạ ⇒ bỏ qua (không
 * sập, không vẽ gì).
 *
 * ## 2.89 · B3 DOCK-SCALE — cỡ thanh theo % ([DockConfig.scalePct], 50–150, spec `kachi-289-field-fixes` §B3)
 * Mọi thứ của thanh dựng bằng [ui] = `DockScaleContext.wrap(context, %)` ⇒ ô, icon, chữ, lề, bo góc co/giãn đúng %. Ở 100 %
 * [ui] LÀ `context` và [place] đi [sized] như cũ ⇒ từng pixel + từng vùng chạm như 2.88. Khác 100 %: mỗi ô nằm trong một
 * [DockHitCell] (khung chạm ≥ 48 dp thật dọc trục thanh × trọn bề dày thanh), lề trong thanh chỉ còn ở hai đầu trục.
 */
class ControlDockView(context: Context) : LinearLayout(context) {

    /**
     * S4 · R12 — cú bấm một ô [CapabilityKind.LAUNCHER] (`launcher_apps` / `launcher_settings`).
     *
     * Mặc định **no-op** để mọi chỗ dựng cũ (kể cả test) không phải sửa; chỗ nối thật là
     * [Activity.controlDock] ở `KachiHomeWiring`, và nó gọi ĐÚNG hai đường mà thanh trên đang dùng
     * (`drawerController.openAppList()` · `panels.openSettings()`) — không mở đường thứ hai.
     */
    var onLauncherAction: (String) -> Unit = {}
    private var config = DockConfig()
    /**
     * B3 — `Context` dựng mọi thứ của thanh ở cỡ [uiPct]. 100 % ⇒ CHÍNH `context` (đường cũ). Giữ trong TRƯỜNG của view
     * (sống/chết cùng Activity) — không bộ đệm toàn cục (K1, KDoc `DockScaleContext`).
     */
    private var ui: Context = context
    private var uiPct = BarScale.DEFAULT

    init {
        gravity = Gravity.CENTER
        // WP1 · R1.1 — thanh nút **KHÔNG viền**. [ĐO ảnh `after-home-dark.png`] viền cũ là vạch 1px
        // `rgb(99,103,117)` từ x=50 tới x=1868 ở y=882 = đường kẻ dễ thấy thứ hai của màn chính. Thanh tách khỏi
        // vùng ô bằng nền `BAR` + khe [Sp.SLOT_GAP] mà `DockAreaLayout` đặt.
        // R-OP — nền theo độ đục nền chung ([KachiChrome.fade]; 0 % trong suốt ⇒ y như cũ, 100 % ⇒ không tô gì).
        background = KachiChrome.fade(GradientDrawable().apply {
            cornerRadius = dpi(ui, Sp.RADIUS_XL).toFloat(); setColor(c(KachiTheme.BAR))
        })
        rebuild()
    }

    /** Cỡ đổi ⇒ dựng lại [ui] + bộ dựng ô + nền (bo góc theo cỡ) rồi ô; không đổi ⇒ chỉ dựng lại ô như trước B3. */
    fun setConfig(cfg: DockConfig) {
        config = cfg
        if (BarScale.snap(cfg.scalePct) != uiPct) rescale() else rebuild()
    }

    /**
     * B3 — bề dày thanh (px) ở cỡ hiện tại: CÙNG số với `DockAreaLayout` (`BarScale.scaledDensity` = phép `ResourcesImpl`
     * dựng mật độ của [ui] — `BarScaleTest.mat do thanh trung bit…`). Dải xem trước của Cài đặt dùng để đặt khung.
     */
    fun thicknessPx(): Int = dpi(ui, if (config.isVertical()) Bars.DOCK_WIDE else Bars.DOCK_THICK)

    private fun rescale() {
        uiPct = BarScale.snap(config.scalePct)
        ui = DockScaleContext.wrap(context, uiPct)
        Log.i(TAG, "[bar-scale] pct=$uiPct dpi=${ui.resources.displayMetrics.densityDpi} base=${DockScaleContext.baseDpiOf(ui)}")
        restyle()
    }

    /**
     * Mật độ hệ thống đổi lúc chạy (`configChanges=density` ⇒ Activity không dựng lại): [ui] giữ `densityDpi` ghi đè TUYỆT
     * ĐỐI tính từ mật độ cũ ⇒ dựng lại theo mật độ mới. 100 % không có lớp ghi đè nên không cần gì (như trước B3).
     */
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        if (!BarScale.isIdentity(uiPct) && DockScaleContext.baseDpiOf(ui) != context.resources.configuration.densityDpi) rescale()
    }

    /** #10 (2026-09-23) — đổi màu theme MỚI: nền khay (BAR) + dựng lại nút (dock không giữ ô app nên an toàn). */
    fun restyle() {
        background = KachiChrome.fade(GradientDrawable().apply {
            cornerRadius = dpi(ui, Sp.RADIUS_XL).toFloat(); setColor(c(KachiTheme.BAR))
        })
        rebuild()
    }

    /**
     * WP5 · R5.1 — lề trong **[Bars.DOCK_PAD]** (trước WP5 là [Sp.S]): thanh mỏng lại 80 % mà ô chỉ nhỏ 85 % nên phần khung
     * phải nhường chỗ trước, nếu không ô 73/83dp không còn nằm trong thanh 93/99dp (số học ở KDoc [Bars.DOCK_PAD]).
     * B3 — khác 100 %: lề chỉ ở HAI ĐẦU trục; ngang trục là 0 để khung chạm ([DockHitCell]) lấp trọn bề dày thanh (hình ô
     * vẫn ở giữa ⇒ cùng chỗ như có lề).
     */
    private fun applyPad() {
        val p = dpi(ui, Bars.DOCK_PAD)
        when {
            BarScale.isIdentity(uiPct) -> setPadding(p, p, p, p)
            config.isVertical() -> setPadding(0, p, 0, p)
            else -> setPadding(p, 0, p, 0)
        }
    }

    private fun rebuild() {
        orientation = if (config.isVertical()) VERTICAL else HORIZONTAL
        applyPad()
        removeAllViews()
        config.enabled.forEach { id ->
            when (CapabilityCatalog.kindOf(id)) {
                // Widget dựng tay không có dạng một ô thanh nút ⇒ bỏ qua (≤ 2.98 BYD: ô ĐỌC datum/nhóm xe).
                CapabilityKind.READ -> Unit
                // S4 · R12: việc của CHÍNH launcher — ô vẽ như một cái nút, cú bấm đi ra ngoài qua [onLauncherAction].
                // F1 (spec shortcuts-autostart R1.2): khối LỐI TẮT không phải một nút mà một hàng icon dài theo số app
                // ⇒ cỡ do chính khối đặt theo [shortcutStripLength] (không qua [sized]); bề dày = bề dày một ô.
                CapabilityKind.LAUNCHER -> if (id == LauncherActions.SHORTCUTS) addView(shortcutStrip())
                else CapabilityCatalog.pick(id)?.let { pick ->
                    addView(place(launcherTileOf(ui, TileSize.DOCK, pick) { onLauncherAction(id) }))
                }
                null -> Unit   // mã lạ (rác prefs / mã đã xoá) → bỏ qua, KHÔNG sập
            }
        }
    }

    /**
     * Khối lối tắt (F1 · U3): bề DÀY = bề dày một ô của thanh (cùng số [Bars] với [sized]); bề DÀI do khối tự đặt theo
     * số app ([shortcutStripLength]) mỗi lần danh sách đổi — thanh dọc thì khối cao ra, ngang thì rộng ra (R1.2).
     * B3 — dựng bằng [ui] (icon + khe co/giãn); khác 100 %: khối lấp TRỌN bề dày thanh ([ShortcutIconsView.fillAcross]) và
     * mỗi khe ≥ 48 dp thật dọc trục ([shortcutSlotPx]) ⇒ mỗi icon là một đích chạm đủ cỡ dù hình nhỏ.
     */
    private fun shortcutStrip(): View = ShortcutIconsView(ui, grid = false).apply {
        val v = config.isVertical()
        val len = shortcutStripLength(ui, ShortcutHub.items().size)
        val fill = !BarScale.isIdentity(uiPct)
        val m = dpi(ui, Sp.XS)
        fillAcross = fill
        layoutParams = LayoutParams(
            if (v) (if (fill) MATCH else dpi(ui, Bars.DOCK_TILE_W_VERTICAL)) else len,
            if (v) len else (if (fill) MATCH else dpi(ui, Bars.DOCK_TILE_H)),
        ).also { if (!fill) it.setMargins(m, m, m, m) else if (v) it.setMargins(0, m, 0, m) else it.setMargins(m, 0, m, 0) }
        vertical = v
    }

    /** B3 — 100 % ⇒ [sized] y như trước; khác ⇒ ô nằm giữa một [DockHitCell] ([targets] = số đích dọc trục trong ô). */
    private fun place(tile: View, targets: Int = 1): View =
        if (BarScale.isIdentity(uiPct)) sized(tile) else hitCell(tile, targets)

    /** Cỡ ô của thanh nút (WP5: 83×60 khi dọc, 71×73 khi ngang, lề 4dp) — mọi số lấy từ [Bars]. */
    private fun sized(tile: View): View = tile.apply {
        layoutParams = LayoutParams(
            dpi(ui, if (config.isVertical()) Bars.DOCK_TILE_W_VERTICAL else Bars.DOCK_TILE_W),
            dpi(ui, if (config.isVertical()) Bars.DOCK_TILE_H_VERTICAL else Bars.DOCK_TILE_H),
        ).also { it.setMargins(dpi(ui, Sp.XS), dpi(ui, Sp.XS), dpi(ui, Sp.XS), dpi(ui, Sp.XS)) }
    }

    /**
     * B3 — khung chạm của một ô ở cỡ ≠ 100 %: hình = cùng số [Bars] với [sized] (trên [ui]); dọc trục =
     * [BarScale.cellAlongPx] (≥ 48 dp thật mỗi đích, trần = hình ở 100 % đo trên `context` GỐC); ngang trục = trọn bề dày.
     */
    private fun hitCell(tile: View, targets: Int): View {
        val v = config.isVertical()
        val w = dpi(ui, if (v) Bars.DOCK_TILE_W_VERTICAL else Bars.DOCK_TILE_W)
        val h = dpi(ui, if (v) Bars.DOCK_TILE_H_VERTICAL else Bars.DOCK_TILE_H)
        val len100 = dpi(context, if (v) Bars.DOCK_TILE_H_VERTICAL else Bars.DOCK_TILE_W)
        val along = BarScale.cellAlongPx(if (v) h else w, dpi(ui, Sp.XS), DockScaleContext.touchFloorPx(ui), targets, len100)
        return DockHitCell(ui, tile, w, h).apply {
            layoutParams = if (v) LayoutParams(MATCH, along) else LayoutParams(along, MATCH)
        }
    }

    private companion object {
        const val TAG = "KachiBar"
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    }
}
