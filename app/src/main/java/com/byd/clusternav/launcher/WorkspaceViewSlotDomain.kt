package com.byd.clusternav.launcher

import android.view.View

/**
 * ═══ Bắt đầu kéo ô + cỡ ô — hai hàm nhỏ tách khỏi [WorkspaceView] ═══
 *
 * Tách THUẦN khỏi `WorkspaceView.kt` (507 dòng → trần 500, L6-debt 2026-09-27): thân giữ nguyên byte, chỉ đổi `private fun`
 * thành hàm mở rộng `internal` cùng package (cùng khuôn `WorkspaceViewCards.kt`).
 */

// Android box B2 · W3: `slotDomain` (sắc lĩnh vực xe của khay ô) gỡ cùng `Domain`.

internal fun WorkspaceView.startSlotDrag(index: Int, v: View) {
    v.startDragAndDrop(null, View.DragShadowBuilder(v), index, 0)
}

/**
 * 2.93 `WIDGET-CAPACITY-HINT` — cỡ px THẬT của ô [i] (view ô đã đo; `null` = chưa đo / không có ô) cho câu sức chứa của bộ
 * chọn widget. `slotViews` mở `internal` cho đúng phép đọc này (không ghi).
 */
internal fun WorkspaceView.slotFrame(i: Int): Pair<Int, Int>? =
    slotViews.getOrNull(i)?.let { v -> (v.width to v.height).takeIf { it.first > 0 && it.second > 0 } }
