package com.kachi.box.launcher

/**
 * ═══ DI TRÚ khoá ClusterNav theo hồ sơ: *rót xuống*, không *bốc lên* ═══════════════════════════════════════════════
 *
 * Khoá vào phạm vi hồ sơ ở bản SAU không có trong ảnh chụp CŨ của mọi hồ sơ. Lượt áp KHÔNG chạm khoá vắng khỏi ảnh ⇒
 * giá trị đang sống được giữ ⇒ giá trị của hồ sơ vừa rời đi theo sang. Cách chữa: chép giá trị ĐANG SỐNG vào ảnh của mọi
 * hồ sơ **đã chụp**, **chỉ điền chỗ trống** (khoá vắng ở tệp sống ⇒ `null` tường minh = lượt áp XOÁ ⇒ mặc định).
 *
 * Android box B2 · W2c (2026-10-09): gỡ phần riêng chiếu cụm BYD — họ tiền tố có mốc (`cast_geometry`), khoá hoãn
 * `cast_enabled` và lượt rót một lần V-CLUSTER (`migrateClusterProfileOnce`). Còn sổ đã-rót 2.92 PROFILE-NEW-KEYS.
 */
object ProfileScopeMigration {

    /**
     * @param shots hồ sơ → ảnh đã giải mã của MỘT tệp (hồ sơ chưa có ảnh ⇒ map rỗng).
     * @param live tệp sống.
     * @param newKeys khoá vừa vào phạm vi hồ sơ, của tệp này.
     * @return CHỈ những hồ sơ có ảnh đổi → ảnh mới (ghi lại là việc của chỗ gọi, trong một `Editor`).
     */
    fun rotDown(
        shots: Map<String, Map<String, Any?>>,
        live: Map<String, Any?>,
        newKeys: Collection<String>,
    ): Map<String, Map<String, Any?>> {
        val out = LinkedHashMap<String, Map<String, Any?>>()
        shots.forEach { (profile, shot) ->
            val next = LinkedHashMap(shot)
            var touched = false
            newKeys.forEach { k -> if (k !in next) { next[k] = live[k]; touched = true } }
            if (touched) out[profile] = next
        }
        return out
    }

    // ── 2.92 · PROFILE-NEW-KEYS — khoá vào phạm vi hồ sơ ở bản SAU lượt rót của nó ─────────────────────────────────

    /**
     * Khoá `kachi_workspace` (theo XE) giữ **sổ đã-rót**: mỗi mục `tệp/khoá` là một khoá ClusterNav theo hồ sơ đã được
     * [fillNewKeys] rót xuống ảnh của mọi hồ sơ. Literal ở `:app` (`WorkspacePrefsMigrations.kt`) phải BẰNG hằng này.
     */
    const val FILLED_LEDGER_KEY = "profile_keys_filled_v1"

    /** Mục sổ đã-rót của khoá [key] trong tệp prefs [file] (`/` không có trong tên tệp prefs lẫn khoá ClusterNav). */
    fun ledgerEntry(file: String, key: String): String = "$file/$key"

    /** Sổ đã-rót ĐỦ cho bảng [scope] (`tệp → khoá`) — thứ chỗ gọi ghi lại sau lượt rót. */
    fun ledgerOf(scope: Map<String, List<String>>): Set<String> =
        scope.flatMap { (file, keys) -> keys.map { ledgerEntry(file, it) } }.toSet()

    /** Khoá của [scope] mà sổ [done] chưa có, theo tệp; tệp không còn khoá nào ⇒ vắng khỏi kết quả. */
    fun pendingKeys(scope: Map<String, List<String>>, done: Set<String>): Map<String, List<String>> =
        scope.mapValues { (file, keys) -> keys.filter { ledgerEntry(file, it) !in done } }.filterValues { it.isNotEmpty() }

    // ── 2.93 · PROFILE-NEW-FILE-FILL — "đã chụp" xét trên MỌI tệp ảnh chụp ─────────────────────────────────────────

    /**
     * Hồ sơ **ĐÃ TỪNG CHỤP** = có ảnh KHÁC RỖNG ở ít nhất MỘT tệp ([shots] = tệp → (hồ sơ → ảnh đã giải mã)).
     *
     * Vì sao xét trên MỌI tệp chứ không riêng tệp đang rót (senior review 2.92 F1 · review Pass 2 G3): khoá ở một tệp prefs
     * MỚI có ảnh VẮNG ở mọi hồ sơ (chưa bản nào chụp tệp đó) ⇒ phép lọc theo tệp coi mọi hồ sơ là *"chưa có ảnh"* ⇒ không
     * rót cho ai mà sổ vẫn ghi xong ⇒ rò y ca QA 2.92 (giá trị của hồ sơ vừa rời đi theo sang). Hồ sơ chưa chụp ở tệp NÀO
     * thì vẫn là *"bản sao của hiện tại"* (S4 · R5) — không đẻ ảnh cho nó.
     */
    fun captured(shots: Map<String, Map<String, Map<String, Any?>>>): Set<String> =
        shots.values.flatMap { byProfile -> byProfile.filterValues { it.isNotEmpty() }.keys }.toSet()

    /**
     * Rót khoá MỚI vào phạm vi hồ sơ xuống ảnh của mọi hồ sơ **đã chụp**, chỉ điền chỗ trống.
     *
     * ## Bệnh nó chữa (QA máy ảo 2.92, [ĐO])
     * Kachi BYD 2.92: lượt rót một lần cho một bảng khoá cố định không phủ khoá vào phạm vi hồ sơ ở bản SAU (2.90
     * `vm_bubble_hidden`, 2.92 `camera_projection`/`camera_zoom`) ⇒ chúng không bao giờ được rót ⇒ ảnh lưu bằng bản cũ không có
     * khoá ⇒ lượt áp không chạm ⇒ giá trị của hồ sơ vừa rời đi theo sang, rồi lượt rời hồ sơ đó chụp luôn giá trị lạc.
     *
     * Khác [rotDown]: hồ sơ CHƯA chụp ⇒ không chạm: chưa có ảnh = *"bản sao của hiện tại"* (S4 · R5) — lượt áp không ghi gì nên không có
     *    gì rò; đẻ ảnh cho nó là đổi nghĩa R5. *"Đã chụp"* = ảnh của tệp này khác rỗng HOẶC hồ sơ thuộc [captured] (2.93 ·
     *    PROFILE-NEW-FILE-FILL — có ảnh ở tệp khác; xem [ProfileScopeMigration.captured]). Ảnh tệp này VẮNG mà hồ sơ đã chụp
     *    ⇒ ảnh mới chỉ gồm các khoá rót.
     *
     * Khoá đang VẮNG ở tệp sống ⇒ `null` tường minh (lượt áp XOÁ ⇒ về mặc định của hồ sơ đó — với khoá mới tinh đây đúng
     * là giá trị hồ sơ cũ đang có, vì lúc nó được chụp khoá chưa tồn tại).
     *
     * @param newKeys mục sổ chờ của tệp này.
     * @param captured hồ sơ đã chụp ở BẤT KỲ tệp nào (mặc định rỗng = chỉ xét ảnh của tệp này, đúng hành vi 2.92).
     */
    fun fillNewKeys(
        shots: Map<String, Map<String, Any?>>,
        live: Map<String, Any?>,
        newKeys: Collection<String>,
        captured: Set<String> = emptySet(),
    ): Map<String, Map<String, Any?>> {
        val eligible = shots.filter { (profile, shot) -> shot.isNotEmpty() || profile in captured }
        return rotDown(eligible, live, newKeys)
    }
}
