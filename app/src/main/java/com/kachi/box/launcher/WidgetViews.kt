package com.kachi.box.launcher

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import com.kachi.box.R
import com.kachi.box.launcher.KachiTheme.c
import com.kachi.box.launcher.KachiTheme.dpi
import com.kachi.box.launcher.WidgetCards.MiniValue
import com.kachi.box.launcher.WidgetCards.labelCard
import com.kachi.box.launcher.WidgetCards.miniCard
import com.kachi.box.launcher.KachiSpace as Sp

/**
 * Dựng View cho 1 widget dựng tay (`w_*` trong [WidgetRegistry]): đồng hồ · nhạc · trình chiếu ảnh · lưới lối tắt.
 *
 * Android box B2 · W3 (2026-10-09): ba họ ô của bản BYD còn một — widget xe (năng lượng · lốp · PM2.5 · trạng thái xe ·
 * tốc độ · bảng tổng hợp), ô đọc datum xe (`WidgetTelemetry`), ô nút xe (`ControlTileFactory`) và ô NHÓM khả năng gỡ cùng lõi
 * HAL BYDAuto. Mã lạ (ô cũ chưa được `WorkspaceState.sanitized` dọn) ⇒ [labelCard] trơ, không sập. Đồng hồ bỏ dòng nhiệt độ
 * ngoài trời (nguồn là HAL xe — owner chốt).
 *
 * ## ⚠⚠ Bất biến từ 2026-09-21: MỖI bộ vẽ đăng ký một đường ĐỔ GIÁ TRỊ TẠI CHỖ
 * Mọi bộ vẽ **dựng khung một lần** rồi đăng ký hàm đổ vào [WidgetRefreshers]; [refreshRead] gọi hàm đó thay vì tháo/gắn
 * view. Mỗi hàm đổ mang **tên riêng** (`fillClock`/`fillMedia`/…).
 */
object WidgetViews {


    fun build(ctx: Context, id: String, data: WidgetData, scrollKey: String? = null): View = when (id) {
        "w_clock" -> clock(ctx, data)
        "w_media" -> MediaWidgetView.build(ctx, data)
        "w_photos" -> PhotoWidgetView(ctx).apply { bind(data.photos, data.photoIntervalSec) }
        "w_apps" -> ShortcutIconsView(ctx, grid = true, scrollKey = scrollKey)   // F1 R1.3 — tự nghe danh sách lối tắt (ShortcutHub)
        // Mã lạ (widget xe ≤ 2.98 chưa dọn, việc launcher) ⇒ thẻ chữ trơ — có hàm đổ RỖNG để lượt làm mới không dựng lại.
        else -> WidgetRefreshers.live(labelCard(ctx, id.uppercase(), "—", "")) { }
    }

    /**
     * Nội dung ô widget: **1** widget → to, lấp ô; **2..8** → lưới ô đều nhau. L5 WIDGET-FIT-ALL (2.87): số cột/hàng,
     * dạng ô (dọc/ngang/chỉ-icon) và cỡ chữ/icon khớp theo khung THẬT qua [FitGridLayout] + `GridFit` (`:core`) —
     * không còn chia hàng theo số mục (6 ⇒ 3+3 bất kể khung, ảnh 03/10 nhãn bị cắt nửa dưới). [scrollKey] — wave 2A ·
     * SHORTCUT-SCROLL-REBUILD: khoá nhớ vị trí cuộn của `w_apps` theo ô ([ShortcutScrollMemory.slotKey]).
     */
    fun buildGrid(ctx: Context, ids: List<String>, data: WidgetData, scrollKey: String? = null): View {
        val list = ids.take(8)
        if (list.isEmpty()) return FitGridLayout.single(ctx, labelCard(ctx, ctx.getString(R.string.kachi_widget_none), "—", ""))
        if (list.size == 1) return FitGridLayout.single(ctx, build(ctx, list[0], data, scrollKey).also { it.tag = WidgetTag(list[0], compact = false) })
        return FitGridLayout.grid(ctx, IconRepeat.ofIds(list)).apply {
            list.forEach { id -> addView(mini(ctx, id, data, scrollKey).also { it.tag = WidgetTag(id, compact = true) }) }
        }
    }

    /** Thẻ gắn lên mỗi view con: mã khả năng + nó được dựng bằng bộ vẽ đầy-đủ hay bộ vẽ nén. */
    private data class WidgetTag(val id: String, val compact: Boolean)

    /**
     * Làm mới **TẠI CHỖ** những ô con là **mục ĐỌC**, giữ nguyên view của nút HÀNH ĐỘNG và của widget tự-lo-nội-dung.
     * Trả về số ô con đã đổ lại (0 ⇒ chỗ gọi tự dựng lại cả ô cho chắc).
     *
     * ## [SOÁT P1-1] Vì sao phải có hàm này
     * Ô TRỘN (vd trình chiếu ảnh + nhạc) mà dựng lại cả ô mỗi lượt là đặt lại vòng quay ảnh. Mỗi view con mang [WidgetTag]
     * nên đổi được **đúng con cần đổi**; ô tự-lo-nội-dung ([WorkspaceRenderPlanner.selfDriven]) giữ nguyên; ô ĐỌC đổ số qua
     * [WidgetRefreshers]; đường LÙI dựng lại (ô chưa đăng ký hàm đổ) — không bao giờ để ô câm.
     */
    fun refreshRead(root: View, data: WidgetData): Int {
        var changed = 0
        fun walk(v: View) {
            val tag = v.tag as? WidgetTag
            if (tag != null) {
                if (WorkspaceRenderPlanner.selfDriven(tag.id)) return   // ô tự lo nội dung: không thay view (C5)
                // Đường CHÍNH của ô ĐỌC: đổ số mới vào CHÍNH view đang hiện.
                if (WidgetRefreshers.refresh(v, data)) {
                    FitGridLayout.contentChanged(v)
                    changed++
                    return
                }
                // Đường LÙI — ô chưa đăng ký hàm đổ (bộ vẽ mới, hoặc bộ vẽ cố ý không làm tại chỗ): dựng lại rồi
                // thay vào ĐÚNG chỉ số cũ. Giật như bản trước, nhưng không bao giờ để ô câm.
                val parent = v.parent as? ViewGroup ?: return
                val at = parent.indexOfChild(v)
                val lp = v.layoutParams
                val fresh = (if (tag.compact) mini(v.context, tag.id, data) else build(v.context, tag.id, data))
                    .also { it.tag = tag }
                parent.removeViewAt(at)
                parent.addView(fresh, at, lp)
                changed++
                return      // thẻ đánh dấu một ô con hoàn chỉnh — không đi sâu hơn
            }
            if (v is ViewGroup) for (i in v.childCount - 1 downTo 0) walk(v.getChildAt(i))
        }
        walk(root)
        return changed
    }

    // ── Compact (lưới nhiều widget) ────────────────────────────────────────────────────────────────────
    /**
     * Ô NÉN. Mỗi nhánh truyền một **hàm sinh giá trị** cho [miniCard]: khung dựng một lần, hàm ấy chạy lại mỗi nhịp.
     * Nhờ vậy ô nén cũng hết giật — cùng một cơ chế với ô to, không phải hai đường.
     */
    private fun mini(ctx: Context, id: String, data: WidgetData, scrollKey: String? = null): View = when (id) {
        "w_clock"  -> miniCard(ctx, data, "ic-sun", KachiTheme.INK, ticks = true) { MiniValue(SimpleDateFormat("HH:mm", LangHost.locale()).format(Date()), SimpleDateFormat("dd/MM", LangHost.locale()).format(Date())) }
        "w_media"  -> miniCard(ctx, data, "ic-music", KachiTheme.AMBER, free = true) { d -> MiniValue(d.media?.title ?: "—", d.media?.artist ?: "") }
        "w_photos" -> PhotoWidgetView(ctx).apply { bind(data.photos, data.photoIntervalSec) }
        "w_apps"   -> ShortcutIconsView(ctx, grid = true, compact = true, scrollKey = scrollKey)
        else       -> WidgetRefreshers.live(labelCard(ctx, id.uppercase(), "—", "")) { }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────────────────────────────
    /**
     * Khối dọc căn giữa của widget một cột (đồng hồ · tốc độ · trạng thái xe · nhạc · vòng năng lượng/PM2.5 · thẻ đọc
     * chung). 2.92 (spec `kachi-292-shortcut-widget.html` R4 — owner 06/10 *"margin 2 bên nhiều quá phí … check thêm các
     * widget khác"*): lề trong [Sp.L] 16 dp → [Sp.S] 8 dp, cùng mép 8 dp của lưới lối tắt. [ĐO máy ảo 06/10, trước → sau]
     * dải rộng thấp 1558×123 px (lề 16 dp ăn 48 px của 123 px chiều cao): vòng năng lượng 63 → 81 px, hình xe 31 → 52 px,
     * ảnh bìa nhạc 75 → 99 px; ô hẹp 301×804: vòng 209 → 229 px. Lề này nằm TRONG khung kính của ô (khe giữa hai ô vẫn
     * [Sp.SLOT_GAP]).
     */
    internal fun col(ctx: Context): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        val p = dpi(ctx, Sp.S); setPadding(p, p, p, p)
    }
    internal fun tv(ctx: Context, s: String, sp: Float, color: String, bold: Boolean = false) = TextView(ctx).apply {
        // [type scale] ngoại lệ: cỡ đã là THAM SỐ của hàm — bậc do chỗ GỌI quyết. Các chỗ gọi trong tệp này còn
        // truyền số tay (34/30/22/14/13/12.5/11f: giá trị hero + eyebrow của ô widget, phần lớn nằm ngoài 5 bậc) ⇒
        // việc chuyển chúng là một lượt riêng, không thuộc phạm vi lượt này.
        text = s; setTextColor(c(color)); setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); gravity = Gravity.CENTER
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    // ── Curated widgets ───────────────────────────────────────────────────────────────────────────────

    /**
     * Đồng hồ + ngày. Android box B2 · W3: dòng ☀ nhiệt độ ngoài trời (HAL xe `outsideTempC`) gỡ — owner chốt; thời tiết
     * thật cần nguồn khác (ngoài phạm vi).
     */
    private fun clock(ctx: Context, data: WidgetData): View {
        val time = tv(ctx, "", 50f, KachiTheme.INK, true)
        val date = tv(ctx, "", 14f, KachiTheme.MUT)
        val root = col(ctx).apply { addView(time); addView(date) }
        var last = data
        fun fillClock(d: WidgetData) {
            last = d
            time.text = SimpleDateFormat("HH:mm", LangHost.locale()).format(Date())
            date.text = SimpleDateFormat(LangHost.datePattern(), LangHost.locale()).format(Date())
        }
        fillClock(data)
        // QA 04/10: giờ theo NHỊP ĐỒNG HỒ (10 s), không chỉ khi trạng thái xe đổi — xem WidgetRefreshers.liveTick.
        WidgetRefreshers.liveTick(root) { fillClock(last) }
        return WidgetRefreshers.live(root, ::fillClock)
    }
}
