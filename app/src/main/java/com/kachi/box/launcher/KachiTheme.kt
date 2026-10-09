package com.kachi.box.launcher

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import com.kachi.box.R

/**
 * Ba trạng thái BỀ MẶT của [KachiTheme.surface] (VISUAL-REFRESH P1 · T2).
 *
 * Khai ở tầng trên cùng chứ không lồng trong `object KachiTheme` để chỗ gọi viết `SurfaceTone.ACTIVE` thay vì
 * `KachiTheme.SurfaceTone.ACTIVE` — cùng lối [ThemeMode]/[LayoutPreset] của `:core`.
 */
enum class SurfaceTone {
    /**
     * Thẻ nội dung thường — **chỉ** chuyển sắc DỌC, không viền, không mép.
     *
     * Pass 5 đã gỡ nét đỉnh đặc; WP1 (owner 2026-09-20: *"bị bug gạch trên đầu mỗi khung, bỏ viền đi luôn"*) gỡ
     * nốt mép kính 1px **và** hairline viền ngoài. Xem [KachiTheme.surface] về vì sao bỏ hẳn thay vì hạ cường độ.
     */
    NEUTRAL,

    /**
     * **KHAY** — thẻ ô làm việc ở màn chính, tức cái mặt mà [NEUTRAL] đứng lên (VISUAL-REFRESH P1 · soát Pass 4).
     *
     * Cùng cách dựng với [NEUTRAL] nhưng lấy cặp [KachiTheme.SLOT]/[KachiTheme.SLOT_TO] (**tối hơn** thẻ nội dung một
     * bậc). ⚠ WP1 (2026-09-20) **gỡ viền [KachiTheme.LINE_STRONG]** mà tone này từng có: owner *"bỏ viền đi luôn"*
     * ⇒ khay nay tách khỏi nền **chỉ bằng bậc sáng** của cặp vai trên, không bằng một đường kẻ.
     *
     * Vì sao phải là một tone RIÊNG chứ không dùng lại [NEUTRAL]: khay và thẻ mà cùng một sắc độ thì thẻ hết chỗ
     * nổi lên. Đây đúng là chỗ P1 hụt — ô làm việc dựng `GradientDrawable` thẳng tại chỗ nên nó **không nằm trong
     * bảng rà 26 chỗ gọi `card()`**, và [ĐO] điểm ảnh của nó TRƯỚC/SAU P1 giống nhau từng byte.
     */
    WELL,

    /** Thẻ/ô ĐANG BẬT — cùng cách dựng nhưng mang màu nhấn; chữ trên nó là `INK`, **không** phải `ON_ACCENT`. */
    ACTIVE,

    /** Ô LÕM (nhập liệu, rãnh, đoạn phân đoạn) — một tô đặc tối hơn, **không** gradient. */
    SUNKEN,
}

/**
 * Bảng màu + helper drawable của launcher. **Tra theo chủ đề đang chọn** (T1) — mã hex nằm ở [KachiPalette].
 *
 * ## ⚠ Trước T1 đây là 13 `const val` — và đó là lý do nút gạt chủ đề từng bị BỎ
 * `const val` là hằng **biên dịch**: chỗ gọi được nhúng thẳng chuỗi vào bytecode, nên đổi chủ đề lúc chạy không
 * thể có tác dụng. Phiên S1 đo đúng điều đó rồi kết luận nút gạt sẽ là **nút chết** và cố ý không làm
 * (`kachi-settings-screen.html` §4.5). T1 đổi 13 hằng thành **thuộc tính có getter** ⇒ tên gọi giữ nguyên (21 tệp
 * không phải sửa cách gọi) nhưng giá trị nay đọc lại mỗi lần vẽ.
 *
 * ## Một nơi ghi duy nhất
 * [applyTheme] là **chỗ duy nhất** đổi [palette]. Nó KHÔNG phải bản sao thứ hai của trạng thái: nguồn sự thật vẫn
 * là `HomeUiState.themeMode`, còn đây là **hình chiếu lúc vẽ** của nguồn đó. Bài canh
 * [com.kachi.box.launcher.ThemePaletteContractTest] đếm số chỗ gọi [applyTheme] trong `app/src/main` và đỏ nếu
 * có chỗ thứ hai — chính cái bẫy "hai bản sao cùng khoá" mà dự án đã sập vào ba lần.
 */
object KachiTheme {

    /** Bảng màu đang dùng. Mặc định TỐI để mọi đường vẽ trước lượt [applyTheme] đầu tiên vẫn ra bảng cũ. */
    var palette: KachiPalette = KachiPalette.DARK
        private set

    /** Bảng đang dùng là bảng TỐI? (hình chiếu của [applyTheme], chỉ để ô xem-trước màu ở Cài đặt chọn đúng hạt giống). */
    var night: Boolean = true
        private set

    /** Màu trội của ảnh nền đã đưa vào lượt [applyTheme] gần nhất (chỉ để xem trước ô *theo ảnh nền*). */
    var artDominant: IntArray? = null
        private set

    /**
     * Bảng **GỐC** của chủ đề đang dùng — tức bảng TRƯỚC khi suy theo lựa chọn màu của người dùng.
     *
     * ⚠ [SOÁT P1b] Ô xem-trước ở Cài đặt phải so với GỐC, không với [palette]: [palette] đã bị chuyển sắc theo lựa
     * chọn hiện hành, nên hỏi nó màu của ô *Xanh Kachi* sẽ trả về **màu đang chọn** (chọn Lục ngọc ⇒ ô "Xanh Kachi"
     * cũng vẽ lục ngọc, hai ô giống hệt nhau). Cùng một phép chọn với [applyTheme], không phải một đường thứ hai.
     */
    val basePalette: KachiPalette get() = if (night) KachiPalette.DARK else KachiPalette.LIGHT

    /**
     * Chọn bảng màu cho [mode] tại giờ [hour] (0..23, chỉ dùng khi mode = AUTO), rồi **suy** theo lựa chọn màu của
     * người dùng ([choice], P1b · R8) và màu trội của ảnh nền ([artDominant], chỉ khi chọn *theo ảnh nền*).
     *
     * Trả về `true` nếu **thứ đã áp đổi** (bảng màu, hoặc màu sơn hình xe) — chỗ gọi dùng để quyết định có dựng lại
     * màn hay không. Trả `false` thay vì dựng lại vô điều kiện vì `applyTheme` chạy mỗi nhịp trạng thái (1 Hz trên
     * xe) và dựng lại màn mỗi giây thì app trong ô bị nhả/gắn liên tục — họ lỗi P-bug1/R3; cùng lý do đó phép suy
     * chỉ chạy khi **đầu vào đổi** ([derivedFor]), mọi nhịp khác chỉ là một phép so.
     */
    fun applyTheme(
        mode: ThemeMode,
        hour: Int,
        choice: ColorChoice = ColorChoice.DEFAULT,
        artDominant: IntArray? = null,
    ): Boolean {
        val isNight = mode.isNight(hour)
        val art = if (choice.accent == AccentChoice.FROM_ART) artDominant?.toList() else null
        val key = Triple(isNight, choice, art)
        val next = if (key == derivedFor) derived else {
            val base = if (isNight) KachiPalette.DARK else KachiPalette.LIGHT
            base.derive(choice, art?.toIntArray(), isNight).also { derived = it; derivedFor = key }
        }
        night = isNight
        this.artDominant = artDominant
        if (next == palette) return false
        palette = next
        return true
    }

    private var derivedFor: Triple<Boolean, ColorChoice, List<Int>?>? = null
    private var derived: KachiPalette = KachiPalette.DARK

    // ══ VAI MÀU — tên GIỮ NGUYÊN từ bản `const val` để 21 tệp không phải đổi cách gọi ═════════════════════
    val BG: String get() = palette.bg
    val INK: String get() = palette.ink
    val INK2: String get() = palette.ink2
    val MUT: String get() = palette.mut
    val MUT2: String get() = palette.mut2
    val ICON: String get() = palette.icon
    val CARD: String get() = palette.card
    val CARD2: String get() = palette.card2
    val CARD_FILL: String get() = palette.cardFill
    val PANEL: String get() = palette.panel
    val FIELD: String get() = palette.field
    val CELL: String get() = palette.cell
    val TILE: String get() = palette.tile
    val CHIP_OFF: String get() = palette.chipOff
    val DIM: String get() = palette.dim
    val TRACK: String get() = palette.track
    val SLOT: String get() = palette.slot
    val SLOT_TO: String get() = palette.slotTo
    val BAR: String get() = palette.bar
    val BAR_TOP: String get() = palette.barTop
    val LINE: String get() = palette.line
    val LINE_STRONG: String get() = palette.lineStrong
    val GRID_LINE: String get() = palette.gridLine
    // ⚠ WP1 · R1.1 — `EMPTY_LINE` XOÁ cùng vai `KachiPalette.emptyLine` (gạch đứt ô trống, mời mọc lại); FIX286 · ES7 —
    // `EMPTY_FILL` XOÁ cùng vai `emptyFill` (khung trống trong suốt, owner 03/10).
    val WASH: String get() = palette.wash
    val OVERLAY: String get() = palette.overlay
    val ACCENT: String get() = palette.accent
    val ACCENT_INK: String get() = palette.accentInk
    val ACCENT2: String get() = palette.accent2
    val GRAD_FROM: String get() = palette.gradFrom
    val GRAD_TO: String get() = palette.gradTo
    val ON_ACCENT: String get() = palette.onAccent
    val INK_ON_ACCENT: String get() = palette.inkOnAccent
    val ACCENT_SOFT: String get() = palette.accentSoft
    val ACCENT_LINE: String get() = palette.accentLine
    val ACCENT_WASH: String get() = palette.accentWash
    val TILE_ON_FROM: String get() = palette.tileOnFrom
    val TILE_ON_TO: String get() = palette.tileOnTo
    val TILE_ON_LINE: String get() = palette.tileOnLine
    val SCRIM_PANEL: String get() = palette.scrimPanel
    val SCRIM_BTN: String get() = palette.scrimBtn
    val WIDGET_BACKING: String get() = palette.widgetBacking
    val WALL_SCRIM: String get() = palette.wallScrim
    val SCRIM_HEAD: String get() = palette.scrimHead
    val GREEN: String get() = palette.green
    val AMBER: String get() = palette.amber
    val RED: String get() = palette.red
    val CYAN: String get() = palette.cyan
    val ORANGE: String get() = palette.orange
    val SLATE: String get() = palette.slate
    val AMBER_SOFT: String get() = palette.amberSoft
    val RED_SOFT: String get() = palette.redSoft
    val ART_TO: String get() = palette.artTo
    val GLOW1: String get() = palette.glow1
    val GLOW2: String get() = palette.glow2
    val CLEAR: String get() = palette.clear
    // ── VISUAL-REFRESH P1 · chất liệu bề mặt (KDoc của từng vai ở [KachiPalette]) ──
    val SURF_FROM: String get() = palette.surfFrom
    val SURF_TO: String get() = palette.surfTo
    val SURF_LINE: String get() = palette.surfLine
    val SURF_ON_FROM: String get() = palette.surfOnFrom
    val SURF_ON_TO: String get() = palette.surfOnTo
    val FIELD_SUNKEN: String get() = palette.fieldSunken
    val SURF_FROM_OVER_ART: String get() = palette.surfFromOverArt
    val SURF_TO_OVER_ART: String get() = palette.surfToOverArt
    // Android box B2 · W3: tám vai hình xe (PART_* · GLASS_* · LAMP_* · TAIL_ON · CAR_SHADOW) gỡ cùng hình xe.
    // ⚠ WP1 (2026-09-20) GỠ `GLASS_SHEEN`/`GLASS_SHADE`: hai vai mép kính sinh ra sáng 2026-09-20 rồi CHẾT cùng
    // ngày — owner xem ảnh và gọi đúng tên *"bug gạch trên đầu mỗi khung"*, y như `surfEdge`/`surfOnEdge` của
    // Pass-4/Pass-5. Ba lần cùng một họ lỗi ⇒ **bất biến**: mép ghim vào cạnh là một VẠCH ở mọi alpha; chiều nổi
    // phải do chuyển sắc gánh. Vai cũng gỡ khỏi [KachiPalette] (giữ getter trơ sẽ đỏ `ThemePaletteContractTest`).

    fun c(s: String): Int = Color.parseColor(s)
    fun dp(ctx: Context, v: Float): Float = v * ctx.resources.displayMetrics.density
    fun dpi(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    // ⚠ [P1b] Năm bộ dựng drawable KHÔNG-chất-liệu (`card` · `gradient` · `gradientSoft` · `topFade` · `pill`) nay là
    // hàm mở rộng ở `KachiThemeDrawables.kt` — cùng chữ ký, cùng cách gọi `KachiTheme.card(...)`; tách vì trần 500
    // dòng (CLAUDE.md §4.1) khi P1b thêm phép suy bảng màu + `surfacePair`. Bảng tra icon ở lại đây: bốn bài canh
    // icon đọc chính tệp này.

    /**
     * ═══ VISUAL-REFRESH P1 · T2 — BỀ MẶT CÓ CHẤT LIỆU ═══════════════════════════════════════════════════════
     *
     * Thay một lớp tô phẳng bằng **một–hai lớp**: (0) chuyển sắc DỌC, (1) lớp sắc lĩnh vực nếu [domain] khác
     * `null`. Thẻ lồi lên khỏi nền mà **không** bóng đổ, **không** viền, **không** mép.
     *
     * ## ⚠⚠ WP1 · R1.1 (owner 2026-09-20) — GỠ HẲN MÉP + VIỀN, lần thứ BA của cùng một họ lỗi
     * Owner xem ảnh bản WP1 đầu: *"better, bị bug gạch trên đầu mỗi khung, bỏ viền đi luôn"*. Bản đó có mép kính
     * 1px bán trong suốt (`glassSheen` đỉnh + `glassShade` đáy = bevel) trên nền gradient — tức đã "nhẹ" hơn nét
     * đỉnh đặc 1–2dp mà Pass-5 gỡ, **vẫn bị gọi đúng tên là gạch**. Cộng với `surfEdge`/`surfOnEdge` của Pass-4,
     * đây là lần thứ ba ⇒ ghi thành **bất biến**, không phải một lượt vá:
     *
     *  • Một hình chữ nhật ghim vào **cạnh** thẻ là một VẠCH ở **mọi** alpha và **mọi** độ dày. Cái sai là HÌNH
     *    DẠNG, không phải cường độ — nên cách chữa là BỎ, không phải hạ alpha (đã thử hạ, vẫn bị chê).
     *  • Chiều nổi do **chuyển sắc DỌC** gánh một mình; nó vì thế là **hợp đồng** (đỉnh sáng hơn đáy), có bài canh
     *    riêng (`dinh chuyen sac khong bao gio toi hon day`).
     *  • Bốn tone phân biệt nhau **CHỈ bằng MÀU FILL** qua [surfacePair] — không tone nào có `setStroke`.
     *
     * Hai bài canh đã ĐẢO CHIỀU theo đúng điều đó (`SurfaceMaterialContractTest.be mat loi khong con vien hay mep`
     * · `SurfaceContrastContractTest.khong con mep hay vien tren be mat`); ràng buộc WCAG giữ nguyên.
     *
     * ## Vì sao không có bóng/blur/elevation — và vì sao có bài canh riêng
     * Đầu máy DiLink chạy GPU TRINKET. `setShadowLayer`/`BlurMaskFilter`/`RenderEffect` và `elevation` đều bắt GPU
     * vẽ thêm một lượt off-screen mỗi khung. Cảm giác "lồi" ở đây do **chênh sáng trong chính gradient** tạo ra, tức
     * là 0 chi phí thêm so với một tô đặc. [SurfaceMaterialContractTest] quét tầng `launcher/` và đỏ nếu một trong
     * bốn thứ đó quay lại.
     *
     * ## Vì sao chuyển sắc DỌC chứ không chéo
     * [gradient] đang dùng `TL_BR` (chéo) cho **nút/pill đang chọn**. Giữ chéo = nhận diện của *"cái đang được
     * chọn"*, dọc = nhận diện của *"bề mặt"* ⇒ hai vai không lẫn nhau. Đây là lý do chức năng, không phải sở thích.
     * Sau Pass 5 nó còn gánh thêm một vai nữa: **nó là toàn bộ chiều nổi**, nên chiều của nó (đỉnh sáng hơn đáy)
     * là một hợp đồng, không phải một lựa chọn — có bài canh riêng.
     *
     * ## ⚠ Mỗi lần gọi dựng một `Drawable` MỚI — cố ý
     * `Drawable` dùng chung giữa nhiều View thì chúng chia nhau **một** `ConstantState`: đổi bounds/alpha ở một ô là
     * đổi cả những ô kia. Hàm này vì thế **không cache**. Ràng buộc AC5.3 (*"dựng một lần"*) nói về **nhịp trạng
     * thái** — chỗ gọi phải dựng lúc dựng View, không dựng lại mỗi giây trong `bind`/`onDraw`.
     *
     * @param tone [SurfaceTone.NEUTRAL] thẻ nội dung · [SurfaceTone.WELL] **khay** mà thẻ đứng lên (ô làm việc ở
     *   màn chính) · [SurfaceTone.ACTIVE] thẻ/ô đang bật (mang màu nhấn) · [SurfaceTone.SUNKEN] ô lõm — **giữ
     *   phẳng**: một tô đặc, không gradient. Lồi và lõm phải khác nhau ở CƠ CHẾ chứ không chỉ ở độ sáng, nếu
     *   không thì hai vai đọc như một.
     * @param overArtwork thẻ này nằm **trên ẢNH NỀN** ⇒ dùng bản bán trong suốt 80 % ([KachiPalette.surfFromOverArt])
     *   để ảnh lọt qua. Sinh ra cho P1b (spec §4.10) sau phản hồi owner 2026-09-16 kèm ảnh chụp trên xe: *"cái màu
     *   đen, xám của mình, khi nhét thêm hình nền vào, nó lại không đẹp nữa"* — thẻ đục trên ảnh đọc ra thành
     *   **miếng vá**, không thành **cửa sổ**.
     *   ⚠ P1 **chưa chỗ nào bật cờ này** (mặc định `false` ⇒ hành vi hôm nay không đổi một pixel). Nó có sẵn để
     *   P1b chỉ phải thêm **một lớp ảnh ở chỉ số 0** của một `LayerDrawable` chứ không phải viết lại hàm này — sau
     *   Pass 5 chồng lớp chỉ còn `base` (+ tint) nên chèn lớp đáy càng không lệch gì.
     *   ⚠⚠ Và ghi ra chỗ CHƯA ĐỦ: ở 80 %, [ĐO] trên hai nền tệ nhất (trắng tinh / đen tuyền) [INK] còn 7.27:1
     *   (tối) và 10.17:1 (sáng) — đạt; nhưng [MUT] chỉ còn 3.15–3.91:1. P1b **phải** kèm lớp che 35–50 % hoặc
     *   chọn mực theo độ chói đo được của vùng ảnh dưới thẻ. Không có bước đó thì cờ này chưa dùng được thật.
     */
    fun surface(
        ctx: Context,
        radius: Int = KachiSpace.RADIUS_XL,
        tone: SurfaceTone = SurfaceTone.NEUTRAL,
        overArtwork: Boolean = false,
    ): Drawable {
        val r = KachiSpace.dpf(ctx, radius)
        // Ô LÕM: một tô ĐẶC, KHÔNG gradient, KHÔNG viền (WP1 · R1.1). Lồi/lõm khác nhau ở CƠ CHẾ (gradient vs phẳng).
        if (tone == SurfaceTone.SUNKEN) return GradientDrawable().apply {
            cornerRadius = r
            setColor(c(FIELD_SUNKEN))
        }
        // Nền KÍNH: chuyển sắc DỌC theo tone. KHÔNG viền, KHÔNG mép/gạch đỉnh-đáy (WP1 · R1.1 — owner 2026-09-20:
        // "bỏ viền đi luôn"; gỡ hẳn sheen/shade bevel = "gạch trên đầu" + mọi setStroke). Trạng thái ACTIVE/WELL
        // phân biệt CHỈ bằng MÀU FILL qua surfacePair(tone), không stroke/edge nào.
        // (≤ 2.98 BYD còn một lớp sắc LĨNH VỰC xe phủ lên — gỡ ở Android box B2 · W3.)
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            surfacePair(tone, overArtwork),
        ).apply { cornerRadius = r }
    }

    /**
     * Hai đầu chuyển sắc của một [tone] (đỉnh, đáy) — **một** chỗ tra cho cả [surface] lẫn lớp che của thẻ kính
     * (`KachiGlass`, P1b): lớp che phải biết đúng bề mặt sẽ nằm trên nó để chọn độ đục theo độ chói đo được.
     *
     * [overArtwork] ⇒ bán trong suốt 80 %: NEUTRAL dùng hai vai `surf*OverArt` đã đo; KHAY/BẬT hạ alpha của chính
     * cặp vai của mình bằng [ColorMath.scaleAlpha] — không mở thêm bốn vai `*OverArt` chỉ để lặp lại con số 0xcc.
     */
    fun surfacePair(tone: SurfaceTone, overArtwork: Boolean): IntArray {
        val (from, to) = when (tone) {
            SurfaceTone.ACTIVE -> SURF_ON_FROM to SURF_ON_TO
            SurfaceTone.WELL -> SLOT to SLOT_TO
            SurfaceTone.NEUTRAL -> if (overArtwork) SURF_FROM_OVER_ART to SURF_TO_OVER_ART else SURF_FROM to SURF_TO
            SurfaceTone.SUNKEN -> FIELD_SUNKEN to FIELD_SUNKEN
        }
        val f = if (overArtwork && tone != SurfaceTone.NEUTRAL) OVER_ART_OPACITY else 1.0
        return intArrayOf(ColorMath.scaleAlpha(c(from), f), ColorMath.scaleAlpha(c(to), f))
    }

    /** Độ đục của thẻ trên ảnh nền — cùng con số với alpha `0xcc` của hai vai `surf*OverArt` (P1). */
    private const val OVER_ART_OPACITY = 0.8

    /** Ánh xạ tên icon (ControlDef.icon / WidgetDef.icon) → vector drawable. 0 = không có. */
    fun iconRes(icon: String): Int = when (icon) {
        // Android box B2 · W3 (2026-10-09): bảng còn đúng các tên icon mà mã đang dùng (widget · hành động launcher ·
        // nút đầu ô · nút nhạc). Mọi dòng của nút/datum/nhóm/gói lệnh xe (60 dòng — ghế · kính · điều hòa · lốp · đèn ·
        // bộ hình xe U7 · icon nhóm…) gỡ cùng lõi HAL BYDAuto; tệp vector của chúng xoá cùng lượt (gen-icons --check).
        "ic-sun" -> R.drawable.ic_sun
        "ic-music" -> R.drawable.ic_music
        "ic-prev" -> R.drawable.ic_prev
        "ic-play" -> R.drawable.ic_play
        "ic-next" -> R.drawable.ic_next
        "ic-swap" -> R.drawable.ic_swap
        "ic-close" -> R.drawable.ic_close
        "ic-to-back" -> R.drawable.ic_to_back   // L6 — nút *chạy nền* cạnh ⇄ (SlotActionsCluster)
        "ic-photo" -> R.drawable.ic_photo
        // `ic-apps` → `ic_grid` (⊞ bốn ô = lưới ứng dụng trên mọi launcher Android).
        "ic-apps" -> R.drawable.ic_grid
        // `ic-settings` → `ic_gear` (bánh răng THẬT — [ĐO] ảnh 2026-09-14: `ic_sys_g` là mặt trời 8 tia).
        "ic-settings" -> R.drawable.ic_gear
        // V1 pha NGHE — `ic_mic` vẽ theo chuẩn bộ v2 (nét 1.6); KHÔNG dùng `ic_mic_g` (ô cockpit ClusterNav cũ).
        "ic-mic" -> R.drawable.ic_mic
        else -> 0
    }

    /**
     * 9 tên icon NHÓM mà `:core` được phép khai (spec kachi-capability-groups §4.1).
     *
     * Khai ở đây thay vì rải trong test: nó là **danh sách hợp đồng**, và [iconRes] phải tra ra được từng tên.
     * Thứ tự = thứ tự nhóm trong spec.
     *
     * ⚠ Ba tên `ic-group-adas` · `ic-group-occupants` · `ic-group-parking` đã gỡ 2026-09-16 cùng ba nhóm ADAS/an
     * toàn (owner) — tệp vector của chúng cũng xoá khỏi `res/drawable`.
     */
    val GROUP_ICON_NAMES: List<String> = listOf(
        "ic-group-tyres", "ic-group-windows", "ic-group-doors", "ic-group-lights",
        "ic-group-climate", "ic-group-energy", "ic-group-battery", "ic-group-trip",
    )
}
