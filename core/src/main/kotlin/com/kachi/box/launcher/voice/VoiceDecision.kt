package com.kachi.box.launcher.voice

/**
 * Mô tả **một dòng** quyết định của bộ phân tích cho một lượt nói (1.70) — cùng chuỗi đi vào logcat
 * (`KachiVoiceSession: quyết định: "…" ⇒ …`) và vào tệp JSON của nhật ký lượt nói (`decision`).
 *
 * [ĐO xe 2026-09-17] log xe có *"nghe được: chỉnh lại hai mươi lăm độ nhiệt độ"* rồi… không gì cả: không biết
 * bộ phân tích ra `Control(temp, 25)` hay `Unknown(MISMATCH)`, nên không biết lỗi ở tai hay ở đầu. Thuần để test.
 */
object VoiceDecision {
    /** `Launcher(launcher_apps)` · `OpenApp(YouTube→ô 2)` · `không hiểu: MISMATCH` — nhiều vế nối bằng ` + `. */
    fun describe(intents: List<VoiceIntent>): String =
        if (intents.isEmpty()) "không hiểu: rỗng" else intents.joinToString(" + ") { one(it) }

    private fun one(i: VoiceIntent): String = when (i) {
        is VoiceIntent.Launcher -> "Launcher(${i.id})"
        is VoiceIntent.Profile -> "Profile(${i.name})"
        is VoiceIntent.Nav -> "Nav(${i.query}${i.app?.let { " bằng $it" } ?: ""})"
        is VoiceIntent.NavigateSaved -> "NavigateSaved(${i.placeName})"
        is VoiceIntent.Media -> "Media(${i.op}${if (i.query.isNotEmpty()) " ${i.query}" else ""})"
        is VoiceIntent.OpenApp -> "OpenApp(${i.appName}${i.slot?.let { "→ô $it" } ?: ""})"
        is VoiceIntent.Layout -> "Layout(${i.preset})"
        VoiceIntent.EndSession -> "EndSession"
        is VoiceIntent.Unknown -> "không hiểu: ${i.reason}"
    }
}
