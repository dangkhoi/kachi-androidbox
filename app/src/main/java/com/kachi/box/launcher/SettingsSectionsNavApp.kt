package com.kachi.box.launcher

import android.content.Context
import android.widget.LinearLayout
import com.kachi.box.R

/**
 * Nhóm **Dẫn đường** · hàng *App dẫn đường mặc định* (Android box B2 · W1, 2026-10-09).
 *
 * Trước W1 hàng này nằm trong [SettingsNavSection] cùng khối dẫn đường lên cụm/HUD, biển báo tốc độ và bong bóng VietMap
 * — toàn bộ phần chỉ-BYD. W1 gỡ khối đó khỏi trang (lớp ấy còn trong cây tới W2, không ai dựng), nên hàng duy nhất còn ý
 * nghĩa trên Android box được dựng riêng ở đây: nói *"dẫn đường"* không nêu app ⇒ giọng nói và lịch tự dẫn dùng app này.
 * Ghi qua đúng lời gọi cầu cũ ([ClusterNavBridge.setNavDefaultApp]) — cùng khoá `voice_nav_default_app`, không đường thứ hai.
 */
class SettingsNavAppSection(
    private val context: Context,
    private val rows: SettingsRows,
    private val deps: SettingsDeps,
) {

    private val bridge get() = deps.bridge

    fun build(body: LinearLayout) {
        body.addView(rows.chipRow(
            label = context.getString(R.string.kachi_nav_default_app),
            options = bridge.navAppChoices().map { it to SettingsNavAutomationFormat.navAppLabel(context, it) },
            current = bridge.navDefaultApp(),
        ) { key -> bridge.setNavDefaultApp(key) })
    }
}
