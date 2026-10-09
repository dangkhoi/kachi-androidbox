package com.byd.clusternav.modules.navaccess

/**
 * Cờ "dịch vụ Hỗ trợ đã `onServiceConnected`" — `NavAccessibilityService` GHI, `NavConnect.isAccessibilityBound` chỉ dùng
 * làm đường lùi khi `AccessibilityManager` không hỏi được (cờ có thể KẸT true — KDoc ở đó).
 *
 * Android box B2 · W2d: các trường đọc màn Google Maps (cự ly/đường/dòng đáy) gỡ cùng bộ đọc màn.
 */
object NavAccessibilitySource {
    @Volatile var connected = false
}
