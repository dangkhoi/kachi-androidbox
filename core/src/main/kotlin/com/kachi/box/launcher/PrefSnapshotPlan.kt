package com.kachi.box.launcher

/**
 * Kiểu giá trị mà `SharedPreferences` phân biệt ở tầng ĐỌC. `getBoolean` trên một giá trị ghi bằng `putString`
 * **ném** `ClassCastException` — và chỗ đọc là dịch vụ đang chạy (xem KDoc [PrefSnapshot]).
 */
enum class PrefType {
    BOOLEAN, INT, LONG, FLOAT, STRING, STRING_SET;

    companion object {
        /** Kiểu của [v]; `null` = `null` hoặc kiểu lạ (không phải một trong sáu kiểu prefs biết). */
        fun of(v: Any?): PrefType? = when (v) {
            is Boolean -> BOOLEAN
            is Int -> INT
            is Long -> LONG
            is Float -> FLOAT
            is String -> STRING
            is Set<*> -> if (v.all { it is String }) STRING_SET else null
            else -> null
        }
    }
}

/**
 * ═══ ÁP · LÀM SẠCH ảnh chụp ClusterNav theo hồ sơ — phép thuần, tầng `:app` chỉ đổ vào `Editor` ═══════════════════════
 *
 * Android box B2 · W2c (2026-10-09): phần CHUNG còn lại của `ClusterSnapshotPlan` (Kachi BYD V-CLUSTER) sau khi gỡ chiếu
 * cụm — bỏ họ tiền tố `cast_geometry` (mốc `@family:`), khoá hoãn `cast_enabled` và lượt merge khung chiếu lúc nhập.
 * Giữ hai hợp đồng có từ S4 / VC-R8, áp cho MỌI khoá ClusterNav theo hồ sơ (giọng nói, phím, giao diện, lịch dẫn…):
 *  1. **Khoá cố định**: vắng lúc chụp ⇒ `null` tường minh ⇒ lượt áp XOÁ khoá đó (không giữ giá trị của hồ sơ vừa rời).
 *  2. **Kiểm kiểu** trước khi ghi: kiểu khai sẵn thắng; không khai thì phải bằng kiểu của giá trị SỐNG. Sai kiểu ⇒ bỏ.
 *
 * Khoá ngoài phạm vi (vd khoá chiếu cụm / mốc họ trong ảnh do Kachi BYD xuất) không bao giờ được ghi — [inScope] lọc ở
 * cả hai đầu, [sanitize] bỏ chúng IM LẶNG (không vào [Shot.dropped]: đó là dữ liệu của tính năng đã gỡ, không phải lỗi).
 */
object PrefSnapshotPlan {

    /** Ảnh đã làm sạch + các khoá đã bỏ vì hỏng / sai kiểu — chỗ gọi GHI LOG, không ném. */
    data class Shot(val values: Map<String, Any?>, val dropped: List<String>)

    /** Lượt ghi vào tệp sống: `writes[k] = null` ⇒ XOÁ `k`; còn lại ⇒ ghi đúng kiểu. [dropped] để ghi log. */
    data class Edit(val writes: Map<String, Any?>, val dropped: List<String>)

    /** Khoá [key] có thuộc phạm vi ảnh chụp của một tệp không. */
    fun inScope(key: String, fixedKeys: Collection<String>): Boolean = key in fixedKeys

    /** Lượt áp ảnh [shot] lên tệp sống [live]: chỉ khoá trong [fixedKeys], kiểu đúng [declaredTypes] / kiểu sống. */
    fun apply(
        live: Map<String, Any?>,
        shot: Map<String, Any?>,
        fixedKeys: Collection<String>,
        declaredTypes: Map<String, PrefType>,
    ): Edit {
        val writes = LinkedHashMap<String, Any?>()
        val dropped = mutableListOf<String>()
        shot.forEach { (k, v) ->
            if (k !in fixedKeys) return@forEach
            if (v == null) { writes[k] = null; return@forEach }
            val actual = PrefType.of(v)
            val expected = declaredTypes[k] ?: PrefType.of(live[k])
            if (actual == null || (expected != null && actual != expected)) {
                dropped += "$k (kiểu $actual, cần $expected)"
                return@forEach
            }
            writes[k] = v
        }
        return Edit(writes, dropped)
    }

    /**
     * Làm sạch ảnh chụp đến từ **tệp nhập** trước khi nó chạm đĩa. Giữ khoá cố định có kiểu đúng [declaredTypes] (hoặc
     * không khai). Khoá cố định sai kiểu ⇒ [Shot.dropped]; khoá NGOÀI phạm vi ⇒ bỏ im lặng (xem KDoc lớp).
     */
    fun sanitize(shot: Map<String, Any?>, fixedKeys: Collection<String>, declaredTypes: Map<String, PrefType>): Shot {
        val out = LinkedHashMap<String, Any?>()
        val dropped = mutableListOf<String>()
        shot.forEach { (k, v) ->
            if (k !in fixedKeys) return@forEach
            val ok = v == null || PrefType.of(v).let { t -> t != null && (declaredTypes[k] ?: t) == t }
            if (ok) out[k] = v else dropped += k
        }
        return Shot(out, dropped)
    }
}
