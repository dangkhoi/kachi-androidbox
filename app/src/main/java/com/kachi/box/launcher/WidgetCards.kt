package com.kachi.box.launcher

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.kachi.box.launcher.KachiTheme.c
import com.kachi.box.launcher.KachiTheme.dpi
import com.kachi.box.launcher.KachiSpace as Sp

/**
 * ═══ Ô NÉN + THẺ CHỮ của widget dựng tay — khung dựng MỘT LẦN, số đổ lại TẠI CHỖ ═════════════════════════════════
 *
 * Android box B2 · W3 (2026-10-09): giữ lại đúng phần dùng chung của `WidgetTelemetry` (ô đọc datum xe theo `WidgetShape` —
 * gỡ cùng `TelemetryRegistry`): ô nén [miniCard] của đồng hồ / nhạc trong lưới nhiều widget, và thẻ chữ suy giảm
 * [labelCard] (ô trống, mã lạ). Thân giữ nguyên byte của bản BYD, bỏ chấm *"chưa kiểm trên xe"* và sắc lĩnh vực xe.
 */
internal object WidgetCards {

    /** Giá trị của một ô NÉN ở MỘT nhịp. */
    internal data class MiniValue(val big: String, val caption: String = "")

    /**
     * Ô NÉN: icon + số lớn + dòng phụ. Khung dựng ở hàm dựng, số đổ qua [set].
     *
     * ⚠ Thứ tự KHAI là thứ tự khởi tạo: [bigView]/[subView] phải khai **trước** [root] vì khối dựng của `root` gắn chúng.
     * Dòng phụ rỗng ⇒ [View.GONE] (không được `LinearLayout` đo nên bố cục y bản cũ, vẫn còn chỗ để đổ chữ về sau).
     * [free] = chữ dài vô hạn theo thiết kế (tên bài/nghệ sĩ) ⇒ khai tự do cho phép khớp của `FitGridLayout`.
     */
    internal class MiniCard(ctx: Context, icon: String, color: String, free: Boolean = false) {
        private val bigView: TextView = WidgetViews.tv(ctx, "", 17f, color, true)
            .apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END; if (free) FitScale.markFree(this) }
        private val subView: TextView = WidgetViews.tv(ctx, "", 10.5f, KachiTheme.MUT)
            .apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END; if (free) FitScale.markFree(this) }
        val root: LinearLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            KachiGlass.apply(this, Sp.RADIUS_M)   // P1b: kính khi có ảnh nền, surface() khi không
            val p = dpi(ctx, Sp.S); setPadding(p, p, p, p)
            val r = KachiTheme.iconRes(icon)
            if (r != 0) {
                addView(
                    ImageView(ctx).apply { setImageResource(r); setColorFilter(c(color)) },
                    LinearLayout.LayoutParams(dpi(ctx, Sp.ICON_S), dpi(ctx, Sp.ICON_S))
                        .also { it.bottomMargin = dpi(ctx, Sp.XS) },
                )
            }
            addView(bigView)
            addView(subView)
        }

        fun set(v: MiniValue) {
            bigView.text = v.big
            subView.text = v.caption
            subView.visibility = if (v.caption.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    /**
     * Ô NÉN **sống**: khung dựng một lần, [value] chạy lại mỗi lượt đổ và kết quả đổ vào chính khung đó.
     *
     * Giữ tên `miniCard` có chủ ý: `LauncherI18nContractTest.UI_SURFACE` coi `miniCard(` là một **bề mặt chữ**.
     * [ticks] = nội dung theo GIỜ (ô nén đồng hồ) ⇒ đổ lại cả theo nhịp đồng hồ ([WidgetRefreshers.liveTick]).
     */
    internal fun miniCard(
        ctx: Context,
        data: WidgetData,
        icon: String,
        color: String,
        free: Boolean = false,
        ticks: Boolean = false,
        value: (WidgetData) -> MiniValue,
    ): View {
        val card = MiniCard(ctx, icon, color, free)
        var last = data
        val fillCard = { d: WidgetData -> last = d; card.set(value(d)) }
        fillCard(data)
        if (ticks) WidgetRefreshers.liveTick(card.root) { fillCard(last) }
        return WidgetRefreshers.live(card.root, fillCard)
    }

    private fun eyebrow(ctx: Context, s: String) =
        WidgetViews.tv(ctx, s, 11f, KachiTheme.MUT2).apply { letterSpacing = 0.08f }

    /** Thẻ chữ suy giảm: eyebrow + số to + dòng phụ. Dùng cho ô trống và cho mã không tra ra được. */
    internal fun labelCard(ctx: Context, title: String, big: String, sub: String) = WidgetViews.col(ctx).apply {
        addView(eyebrow(ctx, title))
        addView(
            WidgetViews.tv(ctx, big, 30f, KachiTheme.INK, true)
                .apply { setPadding(0, dpi(ctx, Sp.XS), 0, dpi(ctx, Sp.XS)) },
        )
        if (sub.isNotEmpty()) addView(WidgetViews.tv(ctx, sub, 13f, KachiTheme.MUT))
    }
}
