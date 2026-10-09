package com.kachi.box.modules.voicekey

import android.content.SharedPreferences
import com.kachi.box.voicekey.VoiceKeyBinding
import com.kachi.box.voicekey.VoiceKeyBindings

/**
 * LƯU TRỮ **danh sách gán phím** (F3) trong SharedPreferences, mã hoá bằng JSON.
 *
 * ── VÌ SAO Ở `:app` MÀ KHÔNG PHẢI `:core` ───────────────────────────────────────────────────────
 * `org.json` là thư viện của **nền tảng Android**; `:core` là module Kotlin thuần (`java-library`,
 * không có android.jar). Đưa codec xuống `:core` sẽ phải kéo một bản `org.json` thứ hai vào APK, chồng
 * lên bản của hệ thống — cái giá lớn hơn nhiều so với việc để phần mã hoá ở đúng tầng biết-Android.
 * **Luật thuần vẫn nằm ở `:core`** (`VoiceKeyBindings`: put/remove/targetFor/sanitize/migrateLegacy);
 * ở đây chỉ có mã hoá + đọc/ghi. Đây cũng là lý do file này nhận thẳng [SharedPreferences] chứ không
 * nhận `Context`: nó là **bộ chuyển đổi lưu trữ**, không phải nơi quyết định gì.
 *
 * (Ghi chú xếp chỗ, 2026-08-24: bản đầu tên `VoiceKeyBindingCodec` chỉ có `encode`/`decode` nên KHÔNG
 * chạm Android ⇒ `LayeringRulesTest > so file thuan con nam trong app chi duoc giam` bắt đúng — một file
 * thuần bị đặt nhầm vào `:app`. Cách sửa là cho nó nhận `SharedPreferences` để thành đúng tầng lưu trữ,
 * KHÔNG phải nâng con số ghim của luật đó lên.)
 *
 * Test off-device chạy được vì `:app` đã ghim `org.json:json` trên classpath test (app/build.gradle.kts —
 * `android.jar` chỉ có bản stub ném "Stub!"); [encode]/[decode] không cần `SharedPreferences`.
 *
 * MỌI đường đọc đều đi qua [VoiceKeyBindings.sanitize]: file prefs có thể hỏng, bị sửa tay, hoặc đến từ
 * bản trước ⇒ không được tin nội dung của nó giữ bất biến "một mã phím một đích".
 *
 * ── Trường nguồn `"s"` của Kachi BYD 2.88 — Android box B2 · W2f (2026-10-09) ─────────────────────
 * JSON mỗi dòng: `{"k":mã,"t":đích}`. Kachi BYD 2.88+ thêm `"s":"knob"|"wheel"` cho dòng gán theo nút (núm bệ giữa / vô-lăng);
 * gán theo nguồn đã gỡ cùng HAL BYD, nhưng chuỗi đã lưu (máy cũ, tệp hồ sơ nhập) phải đọc lên được, KHÔNG ném:
 *  - `"s"` (mã gì cũng vậy, kể cả mã lạ / sai kiểu) bị BỎ QUA ⇒ dòng thành dòng thường, bắt MỌI nút ra mã đó;
 *  - nhiều dòng cùng mã (2.88 cho phép dòng không nguồn + dòng núm + dòng vô-lăng cùng mã) ⇒ giữ dòng KHÔNG nguồn nếu có
 *    (đúng thứ 2.88 bắn khi không biết nguồn), không có thì dòng ĐẦU; giữ vị trí dòng thắng;
 *  - ghi: không bao giờ ghi `"s"` ⇒ lượt ghi đầu sau khi nâng cấp ra đúng khuôn JSON 2.87.
 */
object VoiceKeyBindingStore {

    private const val K_KEYCODE = "k"
    private const val K_TARGET = "t"
    /** Trường nguồn của Kachi BYD 2.88 — chỉ còn được nhận ra khi đọc, không bao giờ ghi. */
    private const val K_SOURCE = "s"

    /**
     * `null` ⇒ khoá CHƯA tồn tại (chưa migrate bao giờ) — khác hẳn `"[]"` (đã migrate, danh sách rỗng).
     *
     * ⚠ CỐ Ý KHÔNG có hàm tiện lợi `read(sp, key) = decode(rawOrNull(sp, key))`. Bản đầu 2026-08-24 có,
     * và nó **0 call site** (`CLAUDE.md §8`) — nhưng nguy hiểm hơn là nó *trông giống* hàm đọc đúng trong
     * khi nuốt mất phân biệt `null` ↔ `"[]"`. Ai gọi nhầm nó thay cho `Prefs.voiceKeyBindings` sẽ **bỏ qua
     * migrate** ⇒ máy nâng cấp từ 1.19 mất sạch cấu hình gán của owner mà không có lỗi nào nổi lên.
     */
    fun rawOrNull(sp: SharedPreferences, key: String): String? = sp.getString(key, null)

    fun write(sp: SharedPreferences, key: String, bindings: List<VoiceKeyBinding>) =
        sp.edit().putString(key, encode(bindings)).apply()

    /** JSON hỏng / null / sai kiểu ⇒ danh sách RỖNG (fail-safe: không gán bừa phím nào cho app nào). */
    fun decode(raw: String?): List<VoiceKeyBinding> {
        if (raw.isNullOrBlank()) return emptyList()
        val parsed = runCatching {
            val arr = org.json.JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                if (!o.has(K_KEYCODE) || !o.has(K_TARGET)) return@mapNotNull null
                // W2f: "s" (gán theo nút BYD) chỉ còn dùng để chọn dòng thắng khi trùng mã — giá trị không được đọc.
                Row(VoiceKeyBinding(keyCode = o.optInt(K_KEYCODE), targetSpec = o.optString(K_TARGET)), hadSource = o.has(K_SOURCE))
            }
        }.getOrDefault(emptyList())
        return VoiceKeyBindings.sanitize(preferSourceless(parsed))
    }

    private class Row(val binding: VoiceKeyBinding, val hadSource: Boolean)

    /** Trùng mã ⇒ dòng không `"s"` thắng, không có thì dòng đầu; dòng đích rỗng không bao giờ thắng. Giữ thứ tự dòng thắng. */
    private fun preferSourceless(rows: List<Row>): List<VoiceKeyBinding> {
        val valid = rows.filter { it.binding.targetSpec.isNotBlank() }
        val winner = valid.groupBy { it.binding.keyCode }
            .mapValues { (_, same) -> same.firstOrNull { !it.hadSource } ?: same.first() }
        return valid.filter { winner[it.binding.keyCode] === it }.map { it.binding }
    }

    fun encode(bindings: List<VoiceKeyBinding>): String {
        val arr = org.json.JSONArray()
        VoiceKeyBindings.sanitize(bindings).forEach {
            arr.put(org.json.JSONObject().put(K_KEYCODE, it.keyCode).put(K_TARGET, it.targetSpec))
        }
        return arr.toString()
    }
}
