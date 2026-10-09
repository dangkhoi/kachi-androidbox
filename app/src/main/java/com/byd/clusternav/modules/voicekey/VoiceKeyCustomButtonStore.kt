package com.byd.clusternav.modules.voicekey

import android.content.SharedPreferences
import com.byd.clusternav.voicekey.VoiceKeyCustomButton

/**
 * LƯU TRỮ **nút tự học** (`voicekey_custom_buttons`) — bộ chuyển đổi JSON ↔ [VoiceKeyCustomButton], cùng khuôn và cùng
 * lý do đặt ở `:app` với [VoiceKeyBindingStore] (`org.json` là thư viện nền tảng Android; luật thuần — khoá mã phím,
 * thay/xoá — ở `:core` `VoiceKeyCustomButtons`). Nhận [SharedPreferences] để là đúng tầng lưu trữ (`LayeringRulesTest`).
 *
 * Tách khỏi `Prefs.kt` ở 2.88 (spec `kachi-288-key-source-split` §4.4): `Prefs.kt` đã ~490/500 dòng; khoá + lượt ghi
 * (`putString(K_VK_CUSTOM, …)`) vẫn ở `Prefs.kt` — một khoá, một chỗ ghi.
 *
 * JSON mỗi nút: `{"n":tên,"k":mã}`. Android box B2 · W2f: trường `"s"` (nguồn nút BYD 2.88) bị BỎ QUA khi đọc, không bao giờ
 * ghi; trùng mã ⇒ nút không `"s"` thắng, không có thì nút ĐẦU (cùng luật [VoiceKeyBindingStore]);
 *  - một dòng hỏng (thiếu `"n"`/`"k"`) chỉ mất dòng đó. Trước 2.88 một dòng hỏng làm mất CẢ danh sách
 *    (`runCatching` bọc cả mảng) — JSON hỏng cả mảng thì vẫn ra rỗng như cũ;
 *  - ghi: danh sách không có nút nguồn nào mã hoá ra ĐÚNG từng byte như 2.87.
 */
object VoiceKeyCustomButtonStore {

    private const val K_NAME = "n"
    private const val K_KEYCODE = "k"
    private const val K_SOURCE = "s"

    fun read(sp: SharedPreferences, key: String): List<VoiceKeyCustomButton> = decode(sp.getString(key, null))

    /** JSON hỏng / null / sai kiểu ⇒ danh sách RỖNG (như trước 2.88). */
    fun decode(raw: String?): List<VoiceKeyCustomButton> {
        if (raw.isNullOrBlank()) return emptyList()
        val arr = try {
            org.json.JSONArray(raw)
        } catch (e: org.json.JSONException) {
            return emptyList()
        }
        // org.json ném JSONException khi "n"/"k" sai kiểu ⇒ chỉ bỏ dòng đó.
        val rows = (0 until arr.length()).mapNotNull { i ->
            try {
                row(arr.optJSONObject(i))
            } catch (e: org.json.JSONException) {
                null
            }
        }
        val winner = rows.groupBy { it.first.keyCode }.mapValues { (_, same) -> same.firstOrNull { !it.second } ?: same.first() }
        return rows.filter { winner[it.first.keyCode] === it }.map { it.first }
    }

    /** Nút + "dòng có trường nguồn `s` của Kachi BYD 2.88" (chỉ dùng để chọn nút thắng khi trùng mã). */
    private fun row(o: org.json.JSONObject?): Pair<VoiceKeyCustomButton, Boolean>? {
        if (o == null || !o.has(K_NAME) || !o.has(K_KEYCODE)) return null
        return VoiceKeyCustomButton(name = o.getString(K_NAME), keyCode = o.getInt(K_KEYCODE)) to o.has(K_SOURCE)
    }

    fun encode(items: List<VoiceKeyCustomButton>): String {
        val arr = org.json.JSONArray()
        items.forEach { arr.put(org.json.JSONObject().put(K_NAME, it.name).put(K_KEYCODE, it.keyCode)) }
        return arr.toString()
    }
}
