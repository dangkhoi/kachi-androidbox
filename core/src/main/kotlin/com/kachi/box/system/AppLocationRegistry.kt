package com.kachi.box.system

/** Vị trí DUY NHẤT của một app: trên [displayId], và nếu ở màn launcher thì ở ô [slot]. */
data class AppLocation(val pkg: String, val displayId: Int, val slot: Int?)

/**
 * Registry VỊ TRÍ APP (thuần JVM :core — KHÔNG android.*, KHÔNG dadb).
 *
 * Bất biến MỘT-VỊ-TRÍ: mỗi app tồn tại ở đúng MỘT nơi tại một thời điểm. [place] tự thực thi bất biến bằng cách GHI ĐÈ
 * vị trí cũ (bản đồ khóa theo `pkg`). Android box B2 · W2c: khái niệm "đã trên cụm" (`isCastable`) gỡ cùng chiếu cụm.
 *
 * Thread-safe: bản đồ vị trí giữ dưới SNAPSHOT bất biến `@Volatile`, cập nhật copy-on-write dưới lock; đọc
 * không cần khoá.
 */
class AppLocationRegistry {
    private val lock = Any()

    @Volatile
    private var locations: Map<String, AppLocation> = emptyMap()

    /**
     * Đặt [pkg] vào [displayId] (+ [slot] nếu ở màn launcher). GHI ĐÈ vị trí cũ ⇒ thực thi bất biến
     * MỘT-VỊ-TRÍ: không bao giờ nhân đôi.
     */
    fun place(pkg: String, displayId: Int, slot: Int? = null) {
        val loc = AppLocation(pkg, displayId, slot)
        synchronized(lock) { locations = locations + (pkg to loc) }
    }

    /** Gỡ [pkg] khỏi mọi vị trí (app đóng). Idempotent. */
    fun remove(pkg: String) {
        synchronized(lock) { locations = locations - pkg }
    }

    /** Vị trí hiện tại của [pkg], hoặc `null` nếu chưa đặt. */
    fun locationOf(pkg: String): AppLocation? = locations[pkg]

    /** Mọi app đang ở trên [displayId], sắp theo [AppLocation.slot] tăng dần rồi `pkg` (thứ tự ổn định). */
    fun onDisplay(displayId: Int): List<AppLocation> =
        locations.values
            .filter { it.displayId == displayId }
            .sortedWith(compareBy({ it.slot ?: Int.MAX_VALUE }, { it.pkg }))

    /** Snapshot mọi vị trí (đọc-only). */
    fun all(): List<AppLocation> = locations.values.toList()
}
