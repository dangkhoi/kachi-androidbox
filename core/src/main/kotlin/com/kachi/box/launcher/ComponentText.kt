package com.kachi.box.launcher

/**
 * So hai chuỗi component `pkg/cls` theo NGHĨA (`ComponentName`), chịu cả dạng SHORT (`pkg/.Cls` = `pkg/pkg.Cls`) lẫn FULL.
 *
 * ## Vì sao cần (review Pass 3 · BOX-RENAME-PACKAGE, 2026-10-09)
 * Trước đổi tên, gói `com.byd.launcher` ≠ tiền tố lớp `com.byd.clusternav.…` ⇒ `flattenToShortString()` = `flattenToString()`
 * — so chuỗi trần là đủ. Nay gói = tiền tố lớp (`com.kachi.box/com.kachi.box.…`) ⇒ hệ thống in DẠNG NGẮN ở những chỗ
 * dùng `flattenToShortString()`:
 *  • [ĐO AOSP android-10.0.0_r47 `PackageManagerShellCommand.printResolveInfo` `:923`] `cmd package resolve-activity --brief`
 *    in `comp.flattenToShortString()` ⇒ đọc lại HOME ra `com.kachi.box/.launcher.KachiHome`;
 *  • [ĐO `AccessibilityManagerService.persistComponentNamesToSettingLocked` `:1598`] `enabled_accessibility_services`
 *    do hệ ghi lại cũng ở dạng ngắn.
 * Thuần, không `android.*` (`:core`).
 */
object ComponentText {

    /** Đưa về `pkg/lớp-đầy-đủ`. Không có `/` ⇒ trả nguyên (đã trim) — không bao giờ khớp một component thật. */
    fun normalize(s: String): String {
        val slash = s.indexOf('/')
        if (slash < 0) return s.trim()
        val pkg = s.substring(0, slash).trim()
        var cls = s.substring(slash + 1).trim()
        if (cls.startsWith(".")) cls = pkg + cls          // dạng short `/.Cls` → `pkg.Cls`
        else if (!cls.contains(".")) cls = "$pkg.$cls"    // dạng chỉ tên lớp trần → `pkg.Cls`
        return "$pkg/$cls"
    }

    /** Cùng một component không (cả hai vế đều chuẩn hoá). */
    fun same(a: String, b: String): Boolean = normalize(a) == normalize(b)
}
