package com.kachi.box.launcher

import android.view.View
import android.view.ViewGroup
import com.kachi.box.R

// 2.93 (nhóm WIDGET) — [WidgetData] dời THUẦN từ `WidgetViews.kt` (trần 500 dòng) về đây, cạnh sổ hàm đổ nhận chính nó
// (`live`/`refresh`). Không sang tệp riêng: tệp không chạm `android.*` ở :app là tệp "thuần" mới (`LayeringRulesTest`),
// mà [WidgetData] lại cần `MediaSnapshot` (:app) nên không xuống được :core. Thân giữ nguyên byte.

/**
 * Gói dữ liệu render cho widget: nhạc [media] (đọc live) + transport [onMedia] + ảnh trình chiếu.
 *
 * Android box B2 · W3 (2026-10-09): `car` (trạng thái xe) · `control` (cổng nút xe) · `units` (đơn vị datum xe) gỡ cùng lõi
 * HAL BYDAuto.
 */
class WidgetData(
    val media: MediaSnapshot? = null,
    val onMedia: (String) -> Unit = {},
    /**
     * U4(b) — nguồn ảnh cho widget trình chiếu. Chỗ gọi đọc thư mục MỘT LẦN rồi truyền vào; để mỗi ô tự đọc thư mục
     * là I/O lặp lại trên thread chính mỗi lần dựng ô.
     */
    val photos: List<String> = emptyList(),
    /** U4(b) — chu kỳ đổi ảnh của widget trình chiếu. */
    val photoIntervalSec: Int = Slideshow.DEFAULT_INTERVAL_SEC,
)

/**
 * ═══ SỔ ĐĂNG KÝ "ĐỔ GIÁ TRỊ TẠI CHỖ" CHO Ô GIỮA MÀN ═════════════════════════════════════════════════════════
 *
 * Hai đường `view → hàm đổ`, giữ **trên chính view** của ô con:
 *  • [live]/[refresh] — ô **ĐỌC** (widget dựng tay), hàm đổ nhận cả gói [WidgetData];
 *  • [liveTick]/[tickAll] — ô theo GIỜ (đồng hồ), đổ theo nhịp đồng hồ.
 * (≤ 2.98 BYD còn `liveAction`/`refreshAction`/`resyncActions` của ô nút xe — gỡ ở Android box B2 · W3.)
 *
 * ## ⚠⚠ Vì sao sổ này tồn tại — owner 2026-09-21
 * Nguyên văn: *"widget curated như Áp suất lốp refresh lấy số mới bị GIẬT"*. [WidgetViews.refreshRead] trước đây làm
 * mới một ô ĐỌC bằng cách **dựng lại view rồi thay vào chỗ cũ** (`removeViewAt` + `addView`). Trên xe trạng thái đổi
 * **1 nhịp/giây**, nên mỗi giây một ô curated bị tháo khỏi cây view và gắn lại: khung mới phải đo–đặt–vẽ từ đầu, và mắt
 * thấy đúng một cú **giật**.
 *
 * Ô **HÀNH ĐỘNG** và ô **NHÓM** đã tránh được chuyện này từ trước (chúng đổ chữ tại chỗ qua bảng riêng / `binders`);
 * sổ này mang cùng cách làm sang ô ĐỌC — tức là nó không phát minh cơ chế mới, nó **trải rộng cơ chế đã đúng**.
 *
 * ## ⚠⚠ Vì sao giữ trên VIEW (`setTag`) chứ KHÔNG phải một `WeakHashMap<View, …>`
 * [SOÁT 2026-09-21 · P1] Bản đầu của lượt này dùng `WeakHashMap<View, (WidgetData) -> Unit>` với lý do *"ô bị gỡ
 * khỏi cây là mục tự rụng, không rò Context"*. **Lý do đó sai**, và sai theo cách không nhìn thấy được: mọi hàm đổ
 * đều **bắt chính view** mà nó đổ vào (`fillEnergy` giữ `card`, `fillTyreBoard` giữ `boardView`, `fillSpeed` giữ
 * `number` → `mParent` → root…). Trong `WeakHashMap`, **giá trị được giữ MẠNH**; một giá trị trỏ về khoá của nó làm
 * khoá **không bao giờ** trở nên weakly-reachable ⇒ mục không bao giờ bị dọn. Đây là cảnh báo có sẵn trong tài liệu
 * `java.util.WeakHashMap` (*"value objects must not strongly refer to their own keys"*). Vì [WidgetViews] là `object`,
 * sổ sống cả tiến trình ⇒ **mỗi** ô từng dựng + `Context` của nó bị giữ vĩnh viễn, và launcher dựng lại cây ô ở mọi
 * lượt đổi bố cục / đổi hồ sơ / `recreate` khi đổi giao diện-ngôn ngữ. Trên đầu xe còn 56–94 MB trống thì đó là rò
 * thật, không phải rò lý thuyết.
 *
 * Giữ hàm đổ bằng `setTag(R.id.…)` đảo đúng chiều tham chiếu: **view giữ hàm đổ**, nên cả vòng (view → hàm đổ →
 * view) chết cùng lúc khi ô bị gỡ khỏi cây — không cần ai đi xoá sổ, và cũng không còn bảng dùng chung nào để lo
 * chuyện nhiều luồng. Đây là đúng khuôn [KachiGlass] đã dùng cho trạng thái-theo-view (`kachi_glass_spec`), tức
 * không phải một cơ chế thứ hai của dự án.
 *
 * ## Bất biến (vi phạm là mở lại đúng cú giật qua một cửa khác)
 *  1. Hàm đổ **KHÔNG được dựng View mới** — chỉ `setText` / `set(...)` / `invalidate` trên view đã có. Thứ gì cố
 *     định theo mã khả năng (nhãn · hình · dấu *"chưa kiểm"* · lưới ô con) phải dựng ở hàm dựng, KHÔNG ở hàm đổ.
 *  2. Ô chưa đăng ký ⇒ [refresh] trả `false` ⇒ chỗ gọi **lùi về** đường dựng lại cũ. Nhờ vậy thêm một bộ vẽ mà quên
 *     đăng ký thì ô đó chỉ mất tính mượt, **không bao giờ câm** — đúng hướng suy giảm mà dự án chọn ở mọi cổng khác.
 *  3. Hai khoá tag là **của riêng sổ này**. Đừng đọc/ghi chúng ở tệp khác: cả điểm đăng ký lẫn điểm đổ phải đi qua
 *     các hàm dưới đây, nếu không thì "ai đang đổ ô này" lại thành một câu hỏi phải đi tìm.
 */
internal object WidgetRefreshers {

    /**
     * Bọc hàm đổ trong một lớp riêng thay vì đặt thẳng lambda vào tag: `getTag` trả `Any?`, nên `as?` xuống một
     * kiểu hàm đã bị xoá generic (`(WidgetData) -> Unit`) là một phép ép **không kiểm được lúc chạy** — hai sổ sẽ
     * nhận nhầm nhau nếu ai đó dùng sai khoá. Hai lớp riêng làm phép ép ấy thành thật.
     */
    private class ValueFill(val fn: (WidgetData) -> Unit)

    private class TickFill(val fn: () -> Unit)

    /**
     * Đăng ký đường đổ giá trị cho ô [view] rồi trả lại **chính nó**, để bộ vẽ `return` thẳng một dòng.
     *
     * ⚠ Cố ý **KHÔNG** tự gọi [fill] một lượt mồi: mọi bộ vẽ đã có sẵn dữ liệu của lượt dựng (nó vừa nhận
     * `car`/`data` làm tham số), nên bắt nó dựng thêm một gói [WidgetData] chỉ để mồi lần đầu là thêm một đối tượng
     * rác cho mỗi ô mỗi lượt. Bộ vẽ tự gọi hàm đổ của nó **trước** khi đăng ký.
     */
    fun <V : View> live(view: V, fill: (WidgetData) -> Unit): V {
        view.setTag(R.id.kachi_widget_fill, ValueFill(fill))
        return view
    }

    /** Đổ lại giá trị cho ô [view]. `false` = ô chưa có đường đổ tại chỗ ⇒ chỗ gọi lùi về dựng lại cả ô con. */
    fun refresh(view: View, data: WidgetData): Boolean {
        val fill = view.getTag(R.id.kachi_widget_fill) as? ValueFill ?: return false
        fill.fn(data)
        return true
    }

    /**
     * Ô có nội dung theo GIỜ (đồng hồ) đăng ký thêm một hàm đổ theo nhịp đồng hồ — chạy ở [tickAll], không cần trạng
     * thái xe đổi. QA 04/10 ([ĐO] máy ảo `clock-check-1.png` · `clock-check-2.png` (bằng chứng phiên, ngoài repo): widget đứng 02:05 suốt 02:30–02:31): đồng hồ chỉ đổ
     * lại khi `CarStatus` đổi, mà máy ảo/xe đỗ thì trạng thái không đổi. [fill] chỉ được đổi CHỮ trên view có sẵn
     * (bất biến 1 ở KDoc lớp) và tự dùng dữ liệu lần đổ gần nhất của nó.
     */
    fun liveTick(view: View, fill: () -> Unit) {
        view.setTag(R.id.kachi_widget_tick_fill, TickFill(fill))
    }

    /**
     * Nhịp đồng hồ (10 s, `KachiHomeActivity.tick` — dùng LẠI nhịp đồng hồ thanh trên, không thêm vòng đếm): đổ lại mọi
     * ô đã [liveTick] dưới [root], KHÔNG dựng view, KHÔNG dựng lại ô; báo lưới khớp ([FitGridLayout.contentChanged]) như
     * mọi đường đổ tại chỗ (giờ 9:59 → 10:00 dài ra). Trả số ô đã đổ. Luồng chính.
     */
    fun tickAll(root: View): Int {
        val fill = root.getTag(R.id.kachi_widget_tick_fill) as? TickFill
        if (fill != null) { fill.fn(); FitGridLayout.contentChanged(root); return 1 }
        if (root !is ViewGroup) return 0
        var n = 0
        for (i in 0 until root.childCount) n += tickAll(root.getChildAt(i))
        return n
    }
}
