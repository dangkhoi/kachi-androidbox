package com.byd.clusternav.system

/**
 * Chủ sở hữu một display. Android box B2 · W2c: chỉ còn [LAUNCHER] — nhánh chiếu cụm BYD (`CAST`, id màn ảo cụm dò live)
 * gỡ cùng mã chiếu cụm. Giữ enum (không thay bằng boolean) để chữ ký [DisplayOwnershipRegistry.validate] và log REJECT
 * vẫn nói rõ AI phát lệnh.
 */
enum class DisplayOwner { LAUNCHER }

/** Kết quả [DisplayOwnershipRegistry.validate]. */
sealed class ValidationResult(val allowed: Boolean) {
    /** Cho phép dispatch. */
    object Allow : ValidationResult(true)

    /** Từ chối (display không chủ) kèm [reason] để log. */
    data class Reject(val reason: String) : ValidationResult(false)
}

/**
 * Registry SỞ HỮU DISPLAY (thuần JVM :core — KHÔNG android.*, KHÔNG dadb).
 *  - [DisplayOwner.LAUNCHER] sở hữu display [MAIN_DISPLAY] (`0`, màn chính) + mọi VirtualDisplay ĐÃ ĐĂNG KÝ (ô app,
 *    màn ảo giữ chỗ — id ≥ 1 bất kỳ, do CHÍNH launcher tạo).
 *  - Mọi display khác (của app khác, màn phụ của hệ) KHÔNG có chủ ⇒ [validate] TỪ CHỐI (fail-safe deny).
 *
 * B4 · DISPLAY-OWNER-DYNAMIC (2.89, Kachi BYD) — [ĐO nguồn AOSP] id display logic là bộ đếm tăng dần, display phụ ĐẦU TIÊN
 * sau khởi động nhận `1` (A10 r47 `DisplayManagerService.java:218,1004,1033-1034`; A12 r34 `Layout.java:37,44-45`) ⇒ KHÔNG
 * có hằng "display 1 là của ai": VD launcher đã đăng ký là sự thật mạnh nhất.
 *
 * Thread-safe: tập VD giữ dưới SNAPSHOT bất biến `@Volatile`, cập nhật copy-on-write dưới lock; đọc không cần khoá.
 */
class DisplayOwnershipRegistry {
    private val lock = Any()

    @Volatile
    private var virtualDisplays: Set<Int> = emptySet()

    /**
     * Đăng ký VirtualDisplay [id] do launcher tạo (ô app, màn giữ chỗ) → thuộc [DisplayOwner.LAUNCHER]. Nhận MỌI id ≥ 1
     * (sau khởi động nguội ô đầu tiên có thể là display 1 — KDoc lớp).
     */
    fun registerVirtualDisplay(id: Int) {
        require(id > MAIN_DISPLAY) { "virtual display id phải ≥ 1 (0 = màn chính): $id" }
        synchronized(lock) { virtualDisplays = virtualDisplays + id }
    }

    /** Gỡ đăng ký VirtualDisplay [id] (khi ô đóng / host release). Idempotent. */
    fun unregisterVirtualDisplay(id: Int) {
        synchronized(lock) { virtualDisplays = virtualDisplays - id }
    }

    /** Snapshot các VirtualDisplay đang đăng ký (đọc-only). */
    fun registeredVirtualDisplays(): Set<Int> = virtualDisplays

    /** Chủ của [displayId], hoặc `null` nếu không có chủ đã biết. */
    fun ownerOf(displayId: Int): DisplayOwner? = when {
        displayId == MAIN_DISPLAY -> DisplayOwner.LAUNCHER
        displayId in virtualDisplays -> DisplayOwner.LAUNCHER
        else -> null
    }

    /**
     * Validate [mutation] do [issuer] phát. ALLOW nếu:
     *  - mutation không nhắm display ([WindowMutation.NO_DISPLAY], vd force-stop), HOẶC
     *  - display đích do CHÍNH [issuer] sở hữu.
     * REJECT (kèm lý do) nếu display không có chủ (fail-safe: deny).
     */
    fun validate(mutation: WindowMutation, issuer: DisplayOwner): ValidationResult {
        val target = mutation.targetDisplayId
        if (target == WindowMutation.NO_DISPLAY) return ValidationResult.Allow
        return if (ownerOf(target) == issuer) {
            ValidationResult.Allow
        } else {
            ValidationResult.Reject(
                "display $target không có chủ đã đăng ký (issuer=$issuer, mutation=${mutation::class.simpleName})",
            )
        }
    }

    companion object {
        /** Màn chính launcher. VD của ô KHÔNG có hằng — nó là sự thật launcher tự đăng ký. */
        const val MAIN_DISPLAY: Int = 0
    }
}
