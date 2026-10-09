package com.kachi.box.launcher

import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.kachi.box.launcher.KachiTheme.c
import com.kachi.box.launcher.KachiTheme.dpi

/**
 * ═══ Ô **HÀNH ĐỘNG CỦA CHÍNH LAUNCHER** trên thanh nút (*Ứng dụng* · *Cài đặt* · *Nói với xe*) ═══════════════════════
 *
 * Tách khỏi `ControlTileFactory.kt` ngày 2026-09-26 (UX4). Android box B2 · W3 (2026-10-09): `ControlTileFactory` (ô nút xe)
 * gỡ cùng lõi HAL BYDAuto ⇒ hai phép tô của nó (nền [applyBg] + mực [tint]) dời vào đây, giữ nguyên byte — ô launcher là ô
 * duy nhất còn dùng chúng. Ô là cú bấm một phát: nền nghỉ = tắt, nháy sáng [TAP_FLASH_MS] khi bấm.
 *
 * Ba tính chất mà `LauncherActionTileWiringContractTest` canh: không chạm cổng xe nào, không chấm *"chưa kiểm"*, cú bấm ra
 * [onTap].
 */
internal fun launcherTileOf(
    ctx: Context,
    size: TileSize,
    pick: CapabilityPick,
    onTap: () -> Unit,
): View {
    val tile = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        val p = dpi(ctx, size.padDp); setPadding(p, p, p, p)
    }
    val r = KachiIcons.res(pick.icon, size.iconDp)
    val icon = ImageView(ctx).apply { if (r != 0) setImageResource(r) }
    tile.addView(icon, LinearLayout.LayoutParams(dpi(ctx, size.iconDp), dpi(ctx, size.iconDp)))
    val label = TextView(ctx).apply {
        text = pick.displayLabel; setTextSize(TypedValue.COMPLEX_UNIT_SP, size.labelSp)
        gravity = Gravity.CENTER; maxLines = 2; ellipsize = TextUtils.TruncateAt.END
    }
    tile.addView(reserveTwoLines(label))
    fun dress(active: Boolean) { applyBg(ctx, size, tile, active); tint(size, icon, label) }
    // Nền NGHỈ: luôn tắt (cú bấm một phát).
    dress(false)
    tile.setOnClickListener {
        dress(true)
        onTap()
        tile.postDelayed({ dress(false) }, TAP_FLASH_MS)   // nháy sáng momentary, rồi về nền NGHỈ
    }
    return tile
}

/**
 * Nền ô: BẬT = [KachiTheme.gradientSoft] (mực `INK_ON_ACCENT` được `ThemePaletteContractTest` đo trên nền `tileOn*`), TẮT =
 * `surface` qua lớp mờ nền chung ([KachiChrome.fade], R-OP).
 */
private fun applyBg(ctx: Context, size: TileSize, v: View, active: Boolean) {
    v.background = if (active) KachiTheme.gradientSoft(ctx, size.radius) else KachiChrome.fade(KachiTheme.surface(ctx, size.radius))
}

/**
 * Mực ô launcher: icon + nhãn luôn tô kiểu ĐANG BẬT ([KachiTheme.INK_ON_ACCENT]) — ô việc là cú bấm, không có trạng thái tắt
 * để làm mờ (y như bản BYD: `tint(i, l, true)`). [T1 · ĐO TỪ PIXEL] `INK_ON_ACCENT`, không `ON_ACCENT`: nền nhấn ở đây BÁN
 * TRONG SUỐT, bảng sáng trộn ra tím nhạt và icon trắng chỉ còn 1.39:1.
 */
private fun tint(size: TileSize, icon: ImageView, label: TextView) {
    KachiIcons.tint(icon, size.iconDp, true, KachiTheme.INK_ON_ACCENT)
    label.setTextColor(c(KachiTheme.INK_ON_ACCENT))
}

/** Thời gian nháy sáng của một ô bấm-một-phát (đúng con số của nút BẤM xe ≤ 2.98). */
private const val TAP_FLASH_MS = 220L
