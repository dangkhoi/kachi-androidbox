package com.kachi.box.launcher

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.kachi.box.R
import com.kachi.box.launcher.KachiTheme.c
import java.text.SimpleDateFormat
import java.util.Date
import com.kachi.box.launcher.KachiBars as Bars
import com.kachi.box.launcher.KachiSpace as Sp

/**
 * Thanh trạng thái trên cùng của HOME: đồng hồ + ngày + khoảng đệm co giãn + hai pill **chỉ-icon**
 * "Ứng dụng"/"Cài đặt" (S4 · R12 — xem [pill]) + **chip hồ sơ**. Tách khỏi [KachiHomeActivity] (B5b) để activity
 * còn là composition-root.
 *
 * THUẦN VIEW — KHÔNG giữ state launcher: mọi tương tác đẩy lên qua callback (một chiều), activity nối vào intent VM.
 * [setProfile]/[updateClock] do [KachiHomeActivity.render] / vòng tick gọi để phản chiếu state.
 *
 * Android box B2 · W3 (2026-10-09): hàng **chip trạng thái xe** (`refreshChips` · `fitChips` · `TopStripChips`) gỡ — mọi chip
 * là chip dữ liệu xe BYD. Vai *"phần co giãn của thanh"* mà hàng chip gánh (S4 · R11 b) nay là một [spacer] trơ đứng ngay
 * SAU vật đồng hồ, nên thanh mặc định giữ đúng hình 1.85 (đồng hồ bên trái, các nút bên phải).
 *
 * ## S1 — MỘT cửa vào cấu hình
 * Trước S1 thanh này có **ba** bề mặt cấu hình: pill "Tuỳ biến" (bảng khả năng + đơn vị + hình nền + quyền), pill
 * "Cài đặt" (nhảy thẳng sang màn ClusterNav cũ) và pill **"Thanh"** (xoay vòng 4 viền của thanh nút xe, ghi bền
 * `dock_edge`). Ba chỗ cho cùng một loại việc, và không chỗ nào bày đủ. Nay chỉ còn **"Cài đặt"** → [SettingsPanel];
 * màn ClusterNav là **một nhóm bên trong** đó.
 *
 * ## ⚠⚠ S4 · R7 — 5 NÚT BỐ CỤC ĐÃ RỜI KHỎI ĐÂY
 * §4.5 của IA v2 cố ý giữ chúng lại (*"đổi bố cục là việc hằng ngày"*). S4 đảo quyết định đó, và lý do là một quyết
 * định **sản phẩm** của owner chứ không phải một lỗi kỹ thuật: hồ sơ tài xế nay giữ **tất cả** lựa chọn (bố cục · ô ·
 * thanh nút · chip · chủ đề · đơn vị · ngôn ngữ · cấu hình ClusterNav), nên *"đổi cả bộ"* là việc hằng ngày còn
 * *"đổi riêng bố cục"* thì lùi về mức chỉnh-một-lần. Owner 2026-09-14: *"bỏ luôn các nút đổi bố cục trên header"*.
 * Đường chọn bố cục sẵn còn **đúng một** chỗ: Cài đặt → Màn hình chính ([SettingsHomeSection.layout]) — cùng intent
 * `onPreset` như trước, nên không mất chức năng nào.
 *
 * Thứ **cố ý ở lại**: chip hồ sơ — đang ở hồ sơ nào là thông tin phải thấy liên tục, và từ S4 nó còn là đường đổi
 * **cả bộ cấu hình**. Chạm nó KHÔNG xoay vòng nữa (xem [ProfileChip.picker]): nó mở bộ chọn có tên từng hồ sơ, và
 * cả lối *"Quản lý hồ sơ…"* sang Cài đặt → Hồ sơ tài xế. Việc *tạo/xoá* hồ sơ vẫn chỉ ở Cài đặt.
 *
 * Giới hạn của thanh này là **khoá lưu bền nào được phép chạm**, không phải "bao nhiêu pill": từ S4 chỉ còn
 * `active_profile` (chip hồ sơ) — xem [SettingsCatalog.TOP_STRIP_ALLOWED_KEYS], nơi khai lý do, và
 * `TopStripSurfaceContractTest` canh cả hai đầu.
 */
class KachiTopStrip(
    private val activity: Activity,
    private val onOpenSettings: () -> Unit,
    private val onProfileTap: () -> Unit,
    private val onOpenAppList: () -> Unit = {},   // U3: lối vào "Mở ứng dụng" (mở app toàn màn, không gắn ô)
    /**
     * V1 pha NGHE (R12 b) — chạm nút mic. CÙNG lambda mà ô *Nói với xe* trên thanh nút dùng
     * (`KachiHomeWiring.controlDock`), không đường thứ hai.
     */
    private val onVoice: () -> Unit = {},
    /**
     * Có vẽ nút mic không — hỏi **mỗi lần dựng**, không nhận một `Boolean` chụp sẵn.
     *
     * Hai điều kiện, và cả hai đổi được sau khi thanh này đã dựng: người dùng bật/tắt ở *Cài đặt › Thanh trạng
     * thái*, và **mô hình đã tải hay chưa**. Vẽ một nút mic trên máy chưa có mô hình là hứa một việc mà bấm vào
     * chỉ nhận được câu *"chưa tải mô hình"* — thà chưa có nút.
     */
    private val voicePillEnabled: () -> Boolean = { false },
    /**
     * UX-OVERHAUL · WP4 — **THỨ TỰ các vật trên thanh**, hỏi mỗi lần dựng (không nhận một [HeaderLayout] chụp sẵn).
     *
     * Cùng lẽ [voicePillEnabled] ngay trên: [view] là `by lazy` nên thanh dựng một lần cho mỗi vòng đời màn, mà
     * thứ tự thì đổi được **sau** đó (người dùng bấm ◀/▶ ở Cài đặt, hoặc đổi hồ sơ). Lượt đổi đi qua [setLayout];
     * lambda này chỉ trả lời câu *"lúc dựng thì đang ở thứ tự nào"*.
     *
     * ⚠ KHÔNG phải một cổng `on*`: thanh này **không ghi** thứ tự đi đâu cả (`TopStripSurfaceContractTest` canh
     * đúng điều đó). Đường ghi là màn Cài đặt → `HomeViewModel.setHeaderLayout` → prefs.
     */
    private val header: () -> HeaderLayout = { HeaderLayout.DEFAULT },
) {
    private lateinit var clock: TextView
    private lateinit var dateText: TextView
    /** Khoảng đệm co giãn (`0dp + weight 1`) — vật duy nhất hút chỗ trống của thanh (xem [place]). */
    private lateinit var spacer: View
    private lateinit var profileInitialView: TextView
    private lateinit var profileNameView: TextView

    /** Chính CHIP hồ sơ (cái NÚT) — giữ tham chiếu vì nhãn TalkBack và lề trong của nó đổi theo [setProfile]. */
    private lateinit var profileChipView: View

    /** Hàng ngang của thanh — giữ tham chiếu vì [place] gắn/tháo con của nó khi thứ tự đổi (WP4). */
    private lateinit var stripRow: LinearLayout

    /**
     * Một view cho MỖI vật của thanh, dựng đúng một lần ở [build].
     *
     * Bảng này (chứ không phải thứ tự `addView`) là chỗ *"vật nào tồn tại"*; [place] quyết *"nó đứng đâu"*. Tách
     * hai câu đó ra là toàn bộ nội dung kỹ thuật của WP4: `getValue` sẽ **nổ** nếu [HeaderItem] có thêm thành viên
     * mà [build] chưa dựng view cho nó — thà nổ ở lượt dựng đầu còn hơn thiếu im lặng một vật trên thanh.
     */
    private val items = HashMap<HeaderItem, View>()

    /** Thứ tự đang ĐẶT trên thanh — để [setLayout] bỏ qua lượt gọi không đổi gì (nó tháo/gắn cả hàng). */
    private var placed: HeaderLayout? = null

    /**
     * QA 2.87 [P2] — MỌI lượt tô màu chủ đề của view thanh này dựng ([themed]); [restyle] chạy lại hết. [ĐO máy ảo QA
     * `topright-light.png` (bằng chứng phiên, ngoài repo)] đổi Tối→Sáng tại chỗ (`applyThemeInPlace`, cũng là đường *Tự động* 06:00/18:00): nút mic + ứng
     * dụng giữ filter/nền lúc DỰNG ⇒ 1,13:1 tới lần khởi động lại. [restyle] cũ liệt kê tay từng view và quên bốn nút; nay không
     * còn danh sách tay (`TopStripSurfaceContractTest`: mọi dòng đọc màu chủ đề trong tệp đều qua [themed]).
     */
    private val painters = ArrayList<() -> Unit>()

    /** Tô ngay + nhớ để [restyle] tô lại theo bảng màu MỚI (đọc `KachiTheme.*` lúc CHẠY, không chụp màu lúc dựng). */
    private fun themed(paint: () -> Unit) { paint(); painters += paint }

    /** View thanh trạng thái (dựng lười một lần). */
    val view: View by lazy { build() }

    private fun build(): View {
        val strip = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            // WP1 · R1.1 — thanh mờ bo góc, **KHÔNG viền**. [ĐO ảnh `after-home-dark.png`] viền cũ là vạch 1px
            // `rgb(100,106,121)` chạy từ x=45 tới x=1874 ở y=107 — một trong hai đường kẻ dễ thấy nhất màn chính
            // (owner: *"KHÔNG còn viền ở BẤT CỨ ĐÂU hết"*). Thanh vẫn tách khỏi nền bằng chính nền `BAR_TOP` của nó.
            background = KachiChrome.fade(KachiTheme.card(context, Sp.RADIUS_L, KachiTheme.BAR_TOP))   // R-OP: độ đục nền chung
            setPadding(dp(Sp.L), dp(Sp.XS), dp(Sp.L), dp(Sp.XS))
        }
        stripRow = strip
        clock = TextView(activity).apply {
            themed { setTextColor(c(KachiTheme.INK)) }; KachiType.apply(this, KachiType.SECTION, bold = true); letterSpacing = 0.02f
        }
        dateText = TextView(activity).apply { themed { setTextColor(c(KachiTheme.MUT)) }; KachiType.apply(this, KachiType.CAPTION); setPadding(dp(Sp.M), 0, 0, 0) }
        // WP4 — đồng hồ + ngày là MỘT vật ([HeaderItem.CLOCK], xem KDoc ở đó): chúng đọc liền nhau, tách ra chỉ
        // mời người dùng dựng những thứ tự không ai muốn.
        items[HeaderItem.CLOCK] = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(clock); addView(dateText)
        }
        // S4 · R7 — hàng 5 nút bố cục đã BỎ ở đây. S4 · R11 (b): mọi vật khác `WRAP_CONTENT`, chỉ [spacer] co giãn ⇒ không
        // tổ hợp thứ tự nào làm ba vật bên phải bị ép về 0 / đẩy khỏi mép.
        spacer = View(activity)
        // V1 pha NGHE — nút mic. Thứ tự **DỰNG** ở đây là thứ tự mặc định (nói → ứng dụng → cài đặt); thứ tự
        // **ĐẶT** trên thanh do [HeaderLayout] quyết (WP4). Chỉ-icon + đích chạm [Bars.HEADER_BTN] (xem [pill]).
        //
        // [SOÁT Pass 2 · P2] Luôn GẮN, ẩn/hiện bằng `visibility` — xem [refreshVoicePill].
        voicePill = pill("ic-mic", R.string.kachi_pill_voice, false) { onVoice() }
            .also { items[HeaderItem.VOICE] = it }
        refreshVoicePill()
        items[HeaderItem.APPS] = pill("ic-apps", R.string.kachi_pill_apps, false) { onOpenAppList() }   // U3
        items[HeaderItem.SETTINGS] = pill("ic-settings", R.string.kachi_pill_settings, true) { onOpenSettings() }
        items[HeaderItem.PROFILE] = profileChip()
        place(header())
        return strip
    }

    /**
     * UX-OVERHAUL · WP4 — **ĐẶT LẠI CHỖ** các vật theo [layout]. Do `KachiHomeActivity.render` gọi khi state đổi.
     *
     * Chỉ **sắp lại** view đã dựng (`removeAllViews` + gắn lại), KHÔNG dựng lại chúng. Dựng lại sẽ mất tên trên chip hồ sơ, và mỗi lần bấm ◀/▶ lại tra + tint lại từng drawable — đúng việc mà
     * bản vá [SOÁT P2-9] vừa dọn khỏi đường nóng.
     */
    fun setLayout(layout: HeaderLayout) {
        if (!this::stripRow.isInitialized || layout == placed) return
        place(layout)
    }

    private fun place(layout: HeaderLayout) {
        placed = layout
        stripRow.removeAllViews()
        layout.order.forEach { item ->
            stripRow.addView(items.getValue(item), lpFor(item))
            // Đệm ngay SAU đồng hồ: thứ tự mặc định (đồng hồ đầu) ⇒ đồng hồ trái, các nút phải — đúng hình 1.85.
            if (item == HeaderItem.CLOCK) stripRow.addView(spacer, LinearLayout.LayoutParams(0, WRAP, 1f))
        }
    }

    /**
     * Cách đặt của từng vật — **chỉ phụ thuộc LOẠI vật, không phụ thuộc chỗ nó đứng**.
     *
     * Đó là điều khiến WP4 an toàn: đổi thứ tự không đổi *cách* một vật chiếm chỗ, nên không có tổ hợp thứ tự nào
     * làm thanh tràn ([spacer] là phần duy nhất co giãn, mọi vật khác vẫn `WRAP_CONTENT`).
     */
    private fun lpFor(item: HeaderItem): LinearLayout.LayoutParams = when (item) {
        HeaderItem.CLOCK -> LinearLayout.LayoutParams(WRAP, WRAP)
        // UX1 · R1 — chip hồ sơ ĐI CHUNG khe với ba pill ([pillLp]). Khe rộng hơn ([Sp.SLOT_GAP]) là của thời chip
        // còn VẼ chữ tên nên cần tách khỏi hàng icon; từ 2.55 chữ tên `GONE` ⇒ nó là nút chỉ-icon thứ tư, và giữ
        // một khe riêng chỉ làm hàng nút lệch nhịp mà không nói lên điều gì.
        else -> pillLp()
    }

    /** Nút mic — giữ tham chiếu vì nó ẩn/hiện theo hai thứ đổi được lúc đang chạy (xem [refreshVoicePill]). */
    private var voicePill: View? = null

    /**
     * Ẩn/hiện nút mic theo [voicePillEnabled] — gọi lại mỗi khi lớp phủ Cài đặt đóng/mở và mỗi lần màn quay lại.
     *
     * ## [SOÁT Pass 2 · P2] Vì sao không thể quyết một lần lúc dựng
     * [view] là `by lazy` ⇒ thanh trên dựng **một lần cho mỗi vòng đời màn chính**. Nhưng hai điều kiện của nút
     * mic đều đổi được **sau** lúc dựng, và đổi ngay trong màn Cài đặt đang phủ lên chính thanh này: người dùng
     * bấm *Tải mô hình tiếng Việt* (xong sau ~1 phút), hoặc gạt công tắc *Nút mic* ở **Thanh trạng thái**. Quyết
     * một lần lúc dựng thì cả hai thao tác đó **không đổi gì trên màn** cho tới lần dựng lại màn chính — tức
     * đúng hình dạng "bấm nút mà màn hình không đổi gì" mà P9 đã trả giá một lần (bố cục sẵn).
     */
    /** #10 (2026-09-23) — re-áp bảng màu theme MỚI lên các view đã dựng (thanh trên không giữ ô app). */
    fun restyle() {
        stripRow.background = KachiChrome.fade(KachiTheme.card(activity, Sp.RADIUS_L, KachiTheme.BAR_TOP))   // R-OP
        painters.forEach { it() }             // QA 2.87 [P2]: đồng hồ · ngày · 3 pill · chip hồ sơ — mọi lượt tô của [build]
    }

    fun refreshVoicePill() {
        voicePill?.visibility = if (voicePillEnabled()) View.VISIBLE else View.GONE
    }

    /**
     * R11 (2.76) — **BỐN nút cùng MỘT hộp** [Bars.HEADER_BTN]², khai `LayoutParams` chứ không `WRAP_CONTENT`.
     * [ĐO máy ảo 27/09, 1,5 px/dp] ba pill 60×51 px (40×34 dp) vs chip hồ sơ 51×51 (owner 26/09: *"cùng bề ngang?"*):
     * 40 không phải số của thang — `WRAP` để `ImageView` đo = hình gốc 24dp + 2×[Bars.HEADER_BTN_PAD], bề cao bị thanh
     * kẹp về 34. Giữ 34 (owner 20/09 *"nút 70 %"*): lên 40 là kéo [Bars.HEADER_H] 42→48 (86 %, không 75 %).
     */
    private fun pillLp() = LinearLayout.LayoutParams(dp(Bars.HEADER_BTN), dp(Bars.HEADER_BTN))
        .also { it.marginStart = dp(Sp.S) }

    /**
     * Pill bấm được của thanh trên ("Ứng dụng" · "Cài đặt") — **CHỈ ICON** từ S4 · R12.
     *
     * ## Vì sao bỏ chữ (owner 2026-09-14: *"đổi chữ Ứng Dụng, Cài Đặt thành icon luôn cho gọn"*)
     * [ĐO] hai pill chữ chiếm ≈ 200dp bề ngang của thanh trên (≤ 2.98 BYD: chỗ ấy nhường cho hàng chip xe).
     *
     * ## Chữ không mất, nó chuyển vai
     * `contentDescription` lấy **đúng khoá tài nguyên cũ** (`kachi_pill_apps` / `kachi_pill_settings`, VI+EN) ⇒
     * TalkBack và phép kiểm giao diện vẫn đọc ra "Ứng dụng"/"Settings"; chỉ phần VẼ là hình. Dùng lại khoá cũ
     * thay vì thêm khoá `*_desc` mới: hai chuỗi cho cùng một cái tên sẽ lệch nhau ở đúng lần ai đó sửa một chỗ.
     *
     * ## Đích chạm KHÔNG được co theo icon
     * **T5** đã nới pill từ ~30dp lên [Sp.TOUCH]; bỏ chữ đi thì bề NGANG cũng tụt xuống cỡ icon (20dp) nếu chỉ
     * dựa vào lề trong ⇒ đặt cả `minimumWidth` lẫn `minimumHeight`.
     *
     * ## ⚠⚠ WP5 · R5.2 — đích chạm nay là [Bars.HEADER_BTN] (34dp), KHÔNG còn [Sp.TOUCH] (48dp)
     * Owner 2026-09-20: *"header … nút App+Voice+profile 70 %"*. Tính chất được canh **không đổi**: khai TƯỜNG MINH
     * cả hai chiều, không để bề ngang co theo hình. Nhưng con số thì xuống dưới mức tối thiểu 48dp — đánh đổi này
     * ghi đầy đủ ở KDoc [Bars.HEADER_BTN] (tóm lại: không nút nào ở đây bắn lệnh xe, bấm nhầm hoàn lại được bằng
     * Back). `FIT_CENTER` + lề trong [Bars.HEADER_BTN_PAD] cho hộp hình 18dp — hình co theo nút, không phải nút co
     * theo hình.
     */
    private fun pill(icon: String, descRes: Int, primary: Boolean, onClick: () -> Unit) = ImageView(activity).apply {
        KachiTheme.iconRes(icon).let { if (it != 0) setImageResource(it) }
        contentDescription = activity.getString(descRes)
        scaleType = ImageView.ScaleType.FIT_CENTER
        minimumWidth = dp(Bars.HEADER_BTN); minimumHeight = dp(Bars.HEADER_BTN)
        dp(Bars.HEADER_BTN_PAD).let { setPadding(it, it, it, it) }
        themed { if (primary) { background = KachiTheme.gradient(context, Sp.RADIUS_PILL); setColorFilter(c(KachiTheme.ON_ACCENT)) } else { background = KachiTheme.pill(context); setColorFilter(c(KachiTheme.INK)) } }
        setOnClickListener { onClick() }
    }

    // ── Hồ sơ tài xế: ĐĨA chữ-cái-đầu, chạm = mở bộ chọn hồ sơ ──
    /**
     * Chip hồ sơ — **đĩa chữ-cái-đầu**; từ V5 (owner 2026-09-25: *"chỉ icon hồ sơ"*) thì **chỉ còn đĩa**: chữ TÊN vẫn
     * dựng nhưng `GONE` để header đỡ chật.
     *
     * ## ⚠⚠ ĐẢO CHIỀU — S4 · R7 gài *"phải có cả cái TÊN"*, V5 gỡ phần VẼ
     * Lý lẽ cũ vẫn đúng và giữ lại: hai hồ sơ *"Đi làm"* / *"Đường trường"* cùng chữ `Đ` nên một chữ cái không trả
     * lời nổi *"đang ở hồ sơ nào"*. V5 không bác nó — V5 **đổi chỗ trả lời**: tên đi vào nhãn TalkBack của chính nút
     * ([setProfile]) và vào bộ chọn mà một cú chạm mở ra ([ProfileChip], có tên từng hồ sơ + dấu hồ sơ đang dùng +
     * lối *"Quản lý hồ sơ…"*). Cử chỉ: **chạm = MỞ BỘ CHỌN**, không xoay vòng (`ProfileBar.cycle` đã xoá) — xoay
     * vòng trên một bộ giữ TOÀN BỘ cấu hình là cú chạm nguy hiểm nhất của launcher.
     *
     * ## ⚠ UX1 · R1 — ĐĨA PHẢI ĐỒNG TÂM VỚI NÚT, và chốt bằng phép CỘNG chứ không bằng con mắt
     * [ĐO ảnh owner 2.70 + mã] chữ nằm đúng tâm ĐĨA (khe trái = khe phải), nhưng **đĩa lệch trái 4dp trong nền
     * pill**: lề trong từng là `(XS, 0, M, 0)` — 12dp bên phải là **khe dẫn sang chữ tên**, mà chữ tên `GONE` từ
     * 2.55 và không ai trả lại lề của nó. Chữ không hỏng; hình học của chip hỏng.
     *
     * Chữa: khai TƯỜNG MINH cả hai chiều bằng [Bars.HEADER_BTN] như ba pill (xem [pill]), `Gravity.CENTER` để chỗ
     * DƯ chia đều, lề trong do [syncProfilePad] tự ĐO theo vật cuối còn hiện. [ĐO AOSP `android-10.0.0_r47`
     * `core/java/android/widget/LinearLayout.java`] `:1325` `mTotalLength` cộng cả lề trong · `:1330`
     * `widthSize = max(mTotalLength, getSuggestedMinimumWidth())` · `:1736` `childLeft = mPaddingLeft +
     * (right - left - mTotalLength) / 2` ⇒ `4 + (34 − 30) / 2 = 6dp`, đĩa chiếm 6..28dp trong nút 34dp ⇒ tâm đĩa
     * 17 = tâm nút 17; trục dọc cùng phép cộng ở `:1785` (CENTER_VERTICAL) ⇒ 6..28dp.
     *
     * Đích chạm: [Bars.HEADER_BTN] (34dp = 70 % của [Sp.TOUCH]), **không phải** [Sp.TOUCH] — đánh đổi ghi ở KDoc
     * [Bars.HEADER_BTN] (WP5 · R5.2). Chữ tên (nếu owner bật vẽ lại) kẹp ở [Sp.LABEL_COL] + một dòng + `…`: tên tự
     * đặt dài tuỳ ý, mà thanh trên không được đẩy pill "Cài đặt" ra khỏi mép.
     */
    private fun profileChip(): View = LinearLayout(activity).apply {
        profileChipView = this
        // `CENTER` (không phải `CENTER_VERTICAL`): chỗ DƯ ngang phải chia ĐỀU hai bên — đĩa là vật duy nhất còn vẽ
        // nên tâm của nó PHẢI là tâm của nút (UX1 · R1).
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
        themed { background = KachiTheme.pill(context) }
        // WP5 · R5.2 — chip hồ sơ là một NÚT ⇒ cùng đích chạm với ba pill ([Bars.HEADER_BTN], 70 % của [Sp.TOUCH]),
        // và khai CẢ HAI chiều đúng như [pill]: chỉ-icon thì bề NGANG không được để lề trong quyết.
        minimumWidth = dp(Bars.HEADER_BTN); minimumHeight = dp(Bars.HEADER_BTN)
        profileInitialView = TextView(activity).apply {
            themed { setTextColor(c(KachiTheme.ON_ACCENT)) }
            KachiType.apply(this, KachiType.BODY, bold = true); gravity = Gravity.CENTER
            // WP5 — đĩa chữ-cái-đầu 70 % ([Bars.HEADER_AVATAR]): giữ 32dp trong một chip cao 34dp thì đĩa ăn gần
            // trọn bề cao và chip đọc ra như một nút tròn dính hai mép.
            val s = dp(Bars.HEADER_AVATAR); width = s; height = s
            themed { background = KachiTheme.gradient(activity, Sp.RADIUS_PILL) }
        }
        profileNameView = TextView(activity).apply {
            themed { setTextColor(c(KachiTheme.INK)) }; KachiType.apply(this, KachiType.BODY, bold = true)
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; maxWidth = dp(Sp.LABEL_COL)
            setPadding(dp(Sp.S), 0, 0, 0)
            visibility = View.GONE   // V5 (owner 2026-09-25): chỉ icon hồ sơ, bỏ chữ tên (đỡ chật header)
        }
        addView(profileInitialView); addView(profileNameView)
        setOnClickListener { onProfileTap() }
        syncProfilePad()
    }

    /**
     * Lề trong của chip = lề của **vật CUỐI CÒN CHIẾM CHỖ** — chip tự ĐO cái nó đang chứa, không giả định có chữ tên.
     *
     * Chữ tên còn chiếm chỗ ⇒ [Sp.M] là khe *chữ–mép*; chỉ còn đĩa ⇒ [Sp.XS] **đối xứng** ⇒ đĩa đồng tâm với nút.
     * Đó chính là lỗi UX1 · R1: chữ tên ẩn từ 2.55 mà khe 12dp của nó còn lại ⇒ đĩa lệch trái 4dp. Bật/tắt chữ tên
     * lần sau không phải sửa lại chỗ này lần nữa.
     *
     * Chốt bằng `!= GONE`, KHÔNG bằng `== VISIBLE`: [ĐO AOSP `android-10.0.0_r47` `LinearLayout.java:1148`] phép đo
     * bỏ qua ĐÚNG con `GONE`, nên `INVISIBLE` **vẫn chiếm chỗ** (đúng hợp đồng `View.INVISIBLE`) và vẫn cần khe của
     * nó. Lấy `== VISIBLE` là ẩn tạm chữ tên kiểu `INVISIBLE` thì lề co lại trong khi chữ vẫn được đo ⇒ lệch lần nữa.
     */
    private fun syncProfilePad() {
        val end = if (profileNameView.visibility != View.GONE) dp(Sp.M) else dp(Sp.XS)
        profileChipView.setPadding(dp(Sp.XS), 0, end, 0)
    }

    /**
     * Chữ cái đầu lên ĐĨA, còn **tên** thì lên nhãn TalkBack của nút (do render / init gọi).
     *
     * [SOÁT P3-4] Cả hai lấy theo **NHÃN** (đã dịch), không theo khoá lưu: máy tiếng Anh hiện `D` / *Default*, không
     * phải `M` / *Mặc định*. Tên GỐC vẫn là thứ duy nhất đi vào prefs — xem KDoc [ProfileNames].
     */
    fun setProfile(profileName: String) {
        val label = ProfileNames.display(profileName)
        profileInitialView.text = ProfileNames.initial(profileName)
        profileNameView.text = label
        // Nhãn TalkBack đặt lên chính NÚT, không lên chữ tên: view `GONE` không vào cây a11y ⇒ nhãn để ở đó không
        // bao giờ đọc được; nút chỉ còn MỘT chữ cái của đĩa để đọc thay [SUY] (lệ ba pill chỉ-icon, xem [pill]).
        profileChipView.contentDescription = activity.getString(R.string.kachi_profile_chip_desc, label)
        syncProfilePad()
    }

    /** Cập nhật đồng hồ + ngày (do vòng tick / onResume gọi). */
    fun updateClock() {
        clock.text = SimpleDateFormat("HH:mm", LangHost.locale()).format(Date())
        dateText.text = SimpleDateFormat(LangHost.datePattern(), LangHost.locale()).format(Date())
    }

    private fun dp(v: Int): Int = (v * activity.resources.displayMetrics.density).toInt()

    private companion object {
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
