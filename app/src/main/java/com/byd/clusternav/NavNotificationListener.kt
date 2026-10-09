package com.byd.clusternav

import android.service.notification.NotificationListenerService

/**
 * Bộ nghe thông báo của Kachi — Android box B2 · W2d (2026-10-09): CHỈ còn là COMPONENT được cấp quyền đọc thông báo.
 *
 * ## Vì sao còn giữ lớp (và giữ đúng TÊN)
 * `MediaBridge` (widget nhạc, nhạc lên xe, YouTube phát tiếp) gọi `MediaSessionManager.getActiveSessions(
 * ComponentName(NavNotificationListener))` — [ĐO nguồn] `MediaSessionService.java` r47 `:499-508,532-549` · r34
 * `:596-606,644-657`: hệ thống chỉ trả phiên nhạc cho component ĐANG được cấp quyền bộ nghe thông báo. Đổi tên lớp =
 * component mới ⇒ máy đã cài mất quyền (chốt: không đổi tên — `docs/_handoff/androidbox-b2-brief.md`). `PermissionPreflight`
 * vẫn tự cấp `cmd notification allow_listener` cho component này.
 *
 * ## Đã gỡ
 * Toàn bộ thân dẫn đường (đọc thông báo Google Maps → cụm/HUD BYD qua AMAP broadcast + HAL), máy đo thông báo thô, cầu
 * widget VietMap / biển tốc độ (W2c). Không ghi đè callback nào: lớp gốc của Android không làm gì với thông báo.
 */
class NavNotificationListener : NotificationListenerService() {

    companion object {
        /** `true` khi hệ thống đã gắn bộ nghe ([onListenerConnected]) — `NavConnect.selfGrant` chờ cờ này sau lệnh cấp quyền. */
        @Volatile var connected = false
    }

    override fun onListenerConnected() {
        connected = true
    }

    override fun onListenerDisconnected() {
        connected = false
        runCatching { requestRebind(android.content.ComponentName(this, NavNotificationListener::class.java)) }
    }

    override fun onDestroy() {
        connected = false
        super.onDestroy()
    }
}
