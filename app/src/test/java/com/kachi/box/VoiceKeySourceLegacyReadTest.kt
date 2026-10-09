package com.kachi.box

import com.kachi.box.modules.voicekey.VoiceKeyBindingStore
import com.kachi.box.modules.voicekey.VoiceKeyCustomButtonStore
import com.kachi.box.voicekey.VoiceKeyBinding
import com.kachi.box.voicekey.VoiceKeyCustomButton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ Android box B2 · W2f (2026-10-09) — đọc chuỗi phím của Kachi BYD 2.88+ có trường nguồn `"s"` ═══════════════════════
 *
 * Gán theo NÚT (2.88 KEY-SOURCE-SPLIT: núm bệ giữa / vô-lăng BYD cùng ra mã 291/292, HAL tách) gỡ cùng HAL BYD. Hai khoá prefs
 * `voicekey_bindings` · `voicekey_custom_buttons` của máy cũ / tệp hồ sơ nhập vẫn có thể mang `"s"`. Luật (thay R5 của 2.88):
 *  1. đọc lên được, KHÔNG ném, mã `"s"` gì cũng bị bỏ qua (kể cả mã lạ / sai kiểu) ⇒ dòng thường, bắt mọi nút;
 *  2. trùng mã ⇒ dòng KHÔNG `"s"` thắng (thứ 2.88 bắn khi không biết nguồn), không có thì dòng đầu; dòng đích rỗng không thắng;
 *  3. ghi ra không bao giờ có `"s"` — khuôn JSON 2.87 (so với bộ mã hoá 2.87 chép nguyên văn ở [Legacy287]).
 */
class VoiceKeySourceLegacyReadTest {

    private val kiki = "ai.zalo.kiki.car"
    private val maps = "com.google.android.apps.maps"

    /** Bộ mã hoá 2.87 (188) chép NGUYÊN VĂN — chuẩn so byte (thứ tự khoá là việc của `org.json`, không so chuỗi viết tay). */
    private object Legacy287 {
        fun bindings(list: List<VoiceKeyBinding>): String {
            val arr = org.json.JSONArray()
            list.forEach { arr.put(org.json.JSONObject().put("k", it.keyCode).put("t", it.targetSpec)) }
            return arr.toString()
        }
        fun buttons(items: List<Pair<String, Int>>): String {
            val arr = org.json.JSONArray()
            items.forEach { arr.put(org.json.JSONObject().put("n", it.first).put("k", it.second)) }
            return arr.toString()
        }
    }

    @Test
    fun `dong gan co s doc len thanh dong thuong, dong khong nguon thang khi trung ma`() {
        val raw = """[{"k":291,"t":"$maps","s":"knob"},{"k":291,"t":"$kiki","s":"wheel"},{"k":291,"t":"$kiki"},""" +
            """{"k":292,"t":"$maps","s":"wheel"},{"k":292,"t":"$kiki","s":"knob"}]"""
        assertEquals(
            listOf(VoiceKeyBinding(291, kiki), VoiceKeyBinding(292, maps)),
            VoiceKeyBindingStore.decode(raw),
            "291: dòng không nguồn thắng (giữ vị trí của nó); 292: chỉ có dòng có nguồn ⇒ dòng ĐẦU",
        )
    }

    @Test
    fun `ma nguon la hoac sai kieu khong lam mat dong, khong nem`() {
        val raw = """[{"k":291,"t":"$maps","s":"pedal"},{"k":292,"t":"$maps","s":""},{"k":328,"t":"$kiki","s":null},""" +
            """{"k":88,"t":"$kiki","s":{"x":1}},{"k":87,"t":"$kiki","s":[1]}]"""
        assertEquals(listOf(291, 292, 328, 88, 87), VoiceKeyBindingStore.decode(raw).map { it.keyCode })
    }

    @Test
    fun `dong khong nguon dich rong khong cuop cho dong co nguon hop le`() {
        val raw = """[{"k":291,"t":"","s":"knob"},{"k":291,"t":"  "},{"k":291,"t":"$maps","s":"wheel"}]"""
        assertEquals(listOf(VoiceKeyBinding(291, maps)), VoiceKeyBindingStore.decode(raw))
    }

    @Test
    fun `json hong van ra rong, khong nem`() {
        listOf(null, "", "{", "[1,2]", """{"k":1}""").forEach { assertEquals(emptyList<VoiceKeyBinding>(), VoiceKeyBindingStore.decode(it), "$it") }
    }

    @Test
    fun `ghi ra khong bao gio co s, y byte 2_87`() {
        val decoded = VoiceKeyBindingStore.decode("""[{"k":291,"t":"$maps","s":"knob"},{"k":328,"t":"$kiki"}]""")
        val enc = VoiceKeyBindingStore.encode(decoded)
        assertEquals(Legacy287.bindings(listOf(VoiceKeyBinding(291, maps), VoiceKeyBinding(328, kiki))), enc)
        assertTrue("\"s\"" !in enc)
    }

    @Test
    fun `nut tu hoc co s doc len, trung ma thi nut khong nguon thang, ghi ra khong s`() {
        val raw = """[{"n":"Núm lên","k":291,"s":"knob"},{"n":"Vô-lăng lên","k":291,"s":"wheel"},{"n":"Mic","k":328},""" +
            """{"n":"cũ","k":291},{"n":"Lạ","k":88,"s":"pedal"}]"""
        val got = VoiceKeyCustomButtonStore.decode(raw)
        assertEquals(listOf(VoiceKeyCustomButton("Mic", 328), VoiceKeyCustomButton("cũ", 291), VoiceKeyCustomButton("Lạ", 88)), got)
        assertEquals(Legacy287.buttons(listOf("Mic" to 328, "cũ" to 291, "Lạ" to 88)), VoiceKeyCustomButtonStore.encode(got))
    }

    @Test
    fun `nut tu hoc chi co ban co nguon thi nut dau thang`() {
        val raw = """[{"n":"Núm lên","k":291,"s":"knob"},{"n":"Vô-lăng lên","k":291,"s":"wheel"}]"""
        assertEquals(listOf(VoiceKeyCustomButton("Núm lên", 291)), VoiceKeyCustomButtonStore.decode(raw))
    }
}
