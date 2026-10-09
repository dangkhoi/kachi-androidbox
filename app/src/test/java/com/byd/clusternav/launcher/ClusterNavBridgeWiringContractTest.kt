package com.byd.clusternav.launcher

import com.byd.clusternav.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ CẦU ClusterNav — bài canh DÂY NỐI (quét mã nguồn `:app`) ═══════════════════════════════════
 *
 * Spec `docs/specs/kachi-settings-ia-v2.html` T2 · N2 · N3.
 *
 * ## Vì sao phải canh bằng quét mã, không phải bằng chạy thử
 * [ClusterNavBridge] **lặp lại** chuỗi lời gọi của `MainActivity.kt` (không trích hàm ra dùng chung
 * được: `activity_main.xml`/`strings.xml` bị hash-seal T11 và `MainActivity.kt` bị ~10 wiring test
 * ghim source — spec §2/§9). Bản sao thì **trôi**: ai đó sửa một nhánh mà quên nhánh kia, mọi test
 * đơn vị vẫn xanh vì hai bên không gọi nhau. Bài này khoá đúng những mắt xích mà "quên một dòng" là
 * mất hẳn hành vi trên xe — mỗi ca nói rõ hỏng cái gì nếu mất.
 *
 * ## Vì sao bài nằm ở `:app` (không phải `:core`)
 * Nó quét **mã nguồn `:app`**. [ĐO] hai lần trong dự án: bài quét mã của module X mà đặt ở module Y
 * thì Gradle không coi tệp của X là đầu vào ⇒ task `UP-TO-DATE` ⇒ bài **không bao giờ chạy lại**
 * (dấu xanh giả). Xem KDoc `ProfileKeysWiringContractTest`.
 *
 * ## Bài KHÔNG còn ở đây
 * Mục **3. Cast** và **4. Phím vô-lăng** đã tách sang [ClusterNavBridgeCastKeysWiringContractTest]
 * (D2c — tệp này từng 511 dòng, quá trần 500 của CLAUDE.md §4.1). Cắt theo **tệp cầu được quét**: ở đây
 * còn `ClusterNavBridge.kt` (+ ràng buộc cứng của cả ba tệp), bên kia là `…Cast.kt` + `…Keys.kt`.
 * Không đổi một assert nào khi tách.
 */
class ClusterNavBridgeWiringContractTest {

    private val BRIDGE = "src/main/java/com/byd/clusternav/launcher/ClusterNavBridge.kt"
    private val KEYS = "src/main/java/com/byd/clusternav/launcher/ClusterNavBridgeKeys.kt"
    private val MSG = "src/main/java/com/byd/clusternav/launcher/ClusterNavBridgeMsg.kt"

    /** MÃ đã bỏ chú thích — mọi phép "phải/không được chứa chuỗi X" đều chạy trên bản này. */
    private fun bridge() = SourceRoots.codeOf(BRIDGE)
    private fun keys() = SourceRoots.codeOf(KEYS)

    private fun body(src: String, signature: String) = SourceRoots.body(src, signature)

    // ─────────────────────────────────────────────────────────────────────────────────────────────
    // 1. Công tắc Dẫn đường — mắt xích dài nhất, và là nơi "quên một dòng" tốn cả chuyến đi
    // ─────────────────────────────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────────────────────────────
    // 2. Mỗi setter ghi ĐÚNG khoá thật (bản đồ khoá spec §4.3) — không tạo khoá song song
    // ─────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * R2: "ghi đúng khoá thật đang được runtime đọc; không tạo khoá song song". Đây là danh sách
     * `hàm setter → lời gọi Prefs bắt buộc`. Sai một cái thì Settings mới đổi một khoá KHÁC với khoá
     * runtime đọc ⇒ nút bấm được, lưu được, mà xe không đổi gì (đúng kiểu "nút chết").
     */
    @Test
    fun `moi setter goi dung Prefs set cua khoa that`() {
        val b = bridge()
        val expected = listOf(
            // Android box B2 · W2d — `setMarquee` (chạy chữ tên đường trên cụm) gỡ cùng dẫn đường cụm.
            // Android box B2 · W2c — setter biển báo tốc độ + bong bóng VietMap gỡ cùng mã của chúng.
            // Android box B2 · W2e — setter ghế · lọc bụi · lấy gió gỡ cùng tiện nghi xe (bài `cau khong con tien nghi xe`).
            "fun setHeadlessAutostart(on: Boolean)" to "Prefs.setHeadlessAutostart(app, on)",
        )
        expected.forEach { (signature, call) ->
            assertTrue(call in body(b, signature), "`$signature` phải ghi qua `$call` (khoá thật, spec §4.3)")
        }
        // UX-OVERHAUL WP1 · R1.3 — công tắc glass thật/giả (theo XE, `clusternav_prefs`). Nó KHÔNG ở `ClusterNavBridge.kt`
        // mà ở `ClusterNavBridgeHome.kt`: tệp cầu chính đã **499 dòng** trước WP1 (trần 500 — CLAUDE.md §4.1) nên
        // không còn chỗ. Luật thì không đổi: setter vẫn phải ghi qua đúng khoá thật của `Prefs`.
        val home = SourceRoots.codeOf("src/main/java/com/byd/clusternav/launcher/ClusterNavBridgeHome.kt")
        assertTrue(
            "Prefs.setGlassReal(app, on)" in body(home, "fun ClusterNavBridge.setGlassReal(on: Boolean)"),
            "`setGlassReal` phải ghi qua `Prefs.setGlassReal(app, on)`",
        )
    }

    /** Android box B2 · W2c — biển báo tốc độ + bong bóng VietMap gỡ khỏi cầu (không mọc lại). */
    @Test
    fun `cau khong con badge va bong bong VietMap`() {
        val b = bridge()
        listOf("BadgeLayout", "VmOverlayPosition", "VietMapAutostartService", "speedSign", "setBadge", "setVmBubble", "SimpleCastRuntime")
            .forEach { assertTrue(it !in b, "cầu còn `$it`") }
    }

    /**
     * Android box B2 · W2e — ghế mát/sưởi, lọc bụi PM2.5 (công tắc · *Lọc ngay* · đọc mức trên thread nền) và lấy gió trong
     * gỡ khỏi cầu cùng mã HAL BYD. Ba bài cũ (applySeat · ba đường PM2.5 · đọc HAL trên thread nền) xoá cùng mã; bài này
     * chặn chúng mọc lại.
     */
    @Test
    fun `cau khong con tien nghi xe`() {
        val b = bridge()
        listOf(
            "SeatComfort", "Pm25Filter", "RecircApplier", "seatEnabled", "seatMode", "seatLevel", "seatCount",
            "pm25Enabled", "pm25CleanNow", "pm25Level", "recircOnStart", "CLEANING_AIR",
        ).forEach { assertTrue(it !in b, "cầu còn `$it`") }
        val system = SourceRoots.codeOf("src/main/java/com/byd/clusternav/launcher/ClusterNavBridgeSystem.kt")
        assertTrue("applyRecircNow" !in system && "RecircApplier" !in system, "cửa áp lấy gió ngay đã gỡ")
        val msg = SourceRoots.codeOf("src/main/java/com/byd/clusternav/launcher/ClusterNavBridgeMsg.kt")
        assertTrue("CLEANING_AIR" !in msg, "BridgeMsg.CLEANING_AIR đã gỡ")
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────────
    // 3. Ràng buộc cứng: cầu không giữ View, không đụng màn cũ  (mục 5 của bản gộp trước D2c)
    // ─────────────────────────────────────────────────────────────────────────────────────────────

    /**
     * N2/§4.2: cầu **không giữ View**. Cấm `import android.view.` / `import android.widget.` ở cả ba
     * tệp. (Tham chiếu ĐẦY ĐỦ `android.view.KeyEvent.keyCodeToString` được phép: đó là hàm tiện ích
     * tĩnh dịch mã phím sang tên, không phải một View bị giữ lại.)
     */
    @Test
    fun `cau khong giu View`() {
        listOf(BRIDGE to bridge(), KEYS to keys(), MSG to SourceRoots.codeOf(MSG))
            .forEach { (name, src) ->
            listOf("import android.view.", "import android.widget.").forEach { bad ->
                assertTrue(
                    bad !in src,
                    "$name có `$bad` — cầu sống lâu hơn Activity, giữ View là rò màn hình + crash sau recreate()",
                )
            }
        }
    }

    /**
     * N4 · `LauncherI18nContractTest`: cầu **không mang chữ**. Cấm `import com.byd.clusternav.Lang`
     * (cơ chế song ngữ lúc-chạy của màn cũ) ở cả ba tệp — mọi thông điệp đi ra bằng mã [BridgeMsg] /
     * [VoiceKeyStatus] và tầng Settings dịch bằng tài nguyên của launcher.
     *
     * Vì sao canh CHÍNH TỆP CẦU chứ không phó mặc bài i18n: bài i18n bắt theo **dấu tiếng Việt**, nên
     * một câu tiếng Anh viết cứng (`Lang.t` vế EN, hay chuỗi không dấu) vẫn lọt qua nó. Ở cầu thì luật
     * chặt hơn — *không câu nào cả*, kể cả tiếng Anh.
     */
    @Test
    fun `cau khong mang chu`() {
        listOf(BRIDGE to bridge(), KEYS to keys()).forEach { (name, src) ->
            assertTrue(
                "com.byd.clusternav.Lang" !in src,
                "$name dùng `Lang` — cầu phải trả MÃ (BridgeMsg/VoiceKeyStatus), để tầng Settings dịch bằng tài nguyên",
            )
            assertTrue(
                "toast: (BridgeMsg)" in bridge(),
                "chữ ký toast phải nhận BridgeMsg, không nhận String — nhận String là mở lại cửa cho câu chữ",
            )
        }
    }

    /** Cầu phải neo vào applicationContext, không giữ Activity (cùng lý do trên). */
    @Test
    fun `cau neo vao applicationContext`() {
        assertTrue(
            "internal val app: Context = app.applicationContext" in bridge(),
            "phải tự quy về applicationContext ngay tại cửa — chặn việc lỡ truyền Activity vào vật sống lâu",
        )
    }

    // Android box B2 · W2a: bài "không đụng vào hai tệp niêm phong T11" gỡ cùng module niêm phong `:offcar-planner`
    // (layout `activity_main.xml` đã xoá; `LegacyScreenAbsenceContractTest` canh nó không mọc lại).
}
