package com.byd.clusternav.launcher

import android.widget.TextView
import com.byd.clusternav.launcher.KachiSpace as Sp

// [WP2 · 2026-09-20] Tách khỏi `ControlTileFactory.kt` (trần 500 dòng). Android box B2 · W3 (2026-10-09): bộ dựng ô nút xe
// (`ControlTileFactory`), tay cầm `ActionTile` và phép tra hình nút xe (`controlIconRes`) gỡ cùng lõi HAL BYDAuto; còn bảng
// cỡ ô thanh nút ([TileSize.DOCK]) cho ô việc launcher ([launcherTileOf]) và `reserveTwoLines`.

/**
 * Cỡ ô của thanh nút ([DOCK]). Android box B2 · W3: hai cỡ còn lại (`BIG` — ô nút xe giữa màn · `GROUP` — hàng nút của ô nhóm
 * xe) và các trường chỉ ô nút xe dùng (giá trị STEP · lựa chọn SELECT · ô hẹp) gỡ cùng `ControlTileFactory`.
 *
 * ⚠ T5/WP5 — số của [DOCK]: icon [Sp.ICON_S] (20), bo [Sp.RADIUS_L] (16), lề trong [KachiBars.DOCK_PAD] (4dp — ô dọc
 * chỉ chứa nổi nội dung nếu lề hạ cùng bề dày thanh, xem KDoc [KachiBars.DOCK_TILE_H_VERTICAL]).
 */
enum class TileSize(
    val iconDp: Int,
    val labelSp: Float,
    val padDp: Int,
    /** Bo góc, **dp dạng Int** từ T5 (họ `Sp.RADIUS_*`). */
    val radius: Int,
) {
    DOCK(Sp.ICON_S, 11.5f, KachiBars.DOCK_PAD, Sp.RADIUS_L),
}

/**
 * Nhãn của ô **CHỈ-BẬT-TẮT / BẤM-MỘT-PHÁT** luôn chiếm ĐÚNG hai dòng, dù chữ chỉ có một dòng.
 *
 * ## ⚠⚠ [KIỂM TOÁN UX mục 6] Vì sao phải cố định, không phải rút ngắn nhãn
 * [ĐO] trong 9 ô của thanh nút, *"Khoá / mở khoá"* là nhãn **duy nhất** xuống hai dòng ⇒ nội dung ô đó cao hơn
 * các ô khác một dòng, mà ô căn giữa dọc ⇒ **icon của nó lệch trục 4–7px** so với tám ô còn lại. Rút ngắn nhãn
 * chữa được ĐÚNG ô này và **không chữa nguyên nhân**: 64 nút, nhãn nào cũng có thể xuống dòng ở cỡ ô khác
 * (thanh dọc rộng 100dp vs ngang 84dp), và lần sau sẽ không ai nhớ luật này.
 *
 * Chốt chỗ cho hai dòng thì chiều cao nội dung **không còn phụ thuộc độ dài chữ** ⇒ icon nằm cùng trục do cấu
 * tạo. Chỉ áp cho hai kiểu ô mà nhãn là phần TỬ CUỐI: ô có thêm hàng giá trị (STEP/COVER/SELECT) thì thêm một
 * dòng nữa sẽ đẩy hàng giá trị ra ngoài trần 86dp của ô.
 */
internal fun reserveTwoLines(label: TextView): TextView = label.apply { minLines = 2 }
