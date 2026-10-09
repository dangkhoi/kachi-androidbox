package com.byd.clusternav.launcher.voice

import com.byd.clusternav.launcher.CapabilityCatalog
import java.io.File
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ GOLDEN DATASET — đo coverage THẬT của [VoiceIntentParser] trên corpus câu nói ═══════════════════════════
 *
 * Owner 2026-09-22: *"dùng model xịn generate 50-100 cách nói/chức năng + nhiều ca mix, làm golden dataset test
 * voice"* + *"phải có cả người nói ĐÚNG và người nói SAI — Nam nói đức=đứt, s=x; Hải Phòng l=n «máy nạnh»; mở
 * máy nạnh 25 độ chứ không chịu nói máy lạnh"*.
 *
 * ## Vì sao bài này khác mọi bài voice đang có
 * `mishear-table.py` chạy trên corpus nhưng bằng một **bộ phân tích XẤP XỈ** (KDoc của chính nó ghi vậy), không
 * phải [VoiceIntentParser]. `variants.tsv`/`misspell.tsv` (do LLM soạn khuôn + máy nở) **chưa từng** được chạy
 * qua parser THẬT trong một bài canh. Bài này đóng đúng lỗ đó: mỗi câu → [VoiceIntentParser.parse] → so id.
 *
 * ## Corpus (đọc từ `scripts/voice/data/` qua `clusternav.root`)
 *  • `variants.tsv` — câu ĐÚNG, nở theo vùng/kiểu (bắc/trung/nam · ngắn/dài/lịch-sự/thân-mật/anh-việt/số-liệu).
 *  • `misspell.tsv` — câu SAI/phương ngữ (real = người thật nói · tts · rule = sinh từ bảng lẫn âm).
 *
 * Id đã GỠ khỏi registry (lock/door/window…) bị bỏ qua — corpus giữ lịch sử, registry là sự thật hiện tại.
 *
 * ## Android box B2 · W3 (2026-10-09) — corpus nay đo HAI việc
 * Lõi HAL BYDAuto gỡ ⇒ mọi id nút/datum/gói lệnh trong corpus là id đã gỡ. Cơ chế "bỏ câu trỏ id đã gỡ" giữ
 * nguyên, nhưng nếu chỉ có nó thì mẫu số về 0 và cả bài im lặng xanh — tức bài TỰ TẮT. Nên:
 *  1. **Id còn sống** = nhãn corpus của launcher (`open_app` · `open_app_slot` · `launcher_*`) ⇒ đo coverage như cũ, sàn mới
 *     ghim theo số đo (xem từng bài). Nhãn nhạc/dẫn đường/bố cục/hồ sơ (`NOT_CAR`) chưa đo ở đây — như trước W3.
 *  2. **Id đã gỡ** (câu về xe) ⇒ bài riêng: KHÔNG câu nào được ra một lệnh khác (mở app · việc launcher · nhạc ·
 *     dẫn đường · hồ sơ · bố cục) — đúng yêu cầu "câu xe ⇒ đã gỡ, không bao giờ thành lệnh khác". Sàn 100 %.
 *
 * ## Ngưỡng
 * KHÔNG ghim 100%: một số câu cố ý mơ hồ / ngoài từ vựng. Ghim **sàn coverage** để không bao giờ TỤT (một thay
 * đổi làm rơi coverage = đỏ). Câu FAIL in ra để lượt sau nở corpus/vá parser bám vào.
 */
class VoiceGoldenCoverageTest {

    private val root = System.getProperty("clusternav.root")
        ?: error("clusternav.root chưa set — xem core/build.gradle.kts")

    /** App giả định đã cài — gồm hai nhãn gần âm với câu xe ("kính" · "pin") để bài 2 soi đúng ca nguy hiểm. */
    private val apps = listOf("Google Maps", "YouTube", "Spotify", "Zalo", "Kinh Thánh", "Pin Tester")

    /** Nhãn corpus còn sống: câu mở app (có/không chỉ định ô) + hành động launcher. */
    private val liveIds: Set<String> =
        setOf(OPEN_APP, OPEN_APP_SLOT) + com.byd.clusternav.launcher.LauncherActions.ALL.map { it.id }

    /** Id mà một [VoiceIntent] trỏ tới — để so với id mong đợi của corpus. */
    private fun resolvedIds(intents: List<VoiceIntent>): Set<String> = intents.flatMapTo(HashSet()) {
        when (it) {
            is VoiceIntent.OpenApp -> if (it.slot != null) listOf(OPEN_APP, OPEN_APP_SLOT) else listOf(OPEN_APP)
            is VoiceIntent.Launcher -> listOf(it.id)
            else -> emptyList()
        }
    }

    private data class Case(val id: String, val text: String, val source: String)

    private fun rows(file: String, idCol: Int, textCol: Int, srcCol: Int?): List<Case> {
        val f = File(root, "scripts/voice/data/$file")
        if (!f.exists()) return emptyList()
        return f.readLines().asSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val c = line.split("\t")
                if (c.size <= maxOf(idCol, textCol)) return@mapNotNull null
                val id = c[idCol].trim()
                if (id == "-" || id.isEmpty()) return@mapNotNull null   // câu cố ý KHÔNG phải lệnh
                Case(id, c[textCol].trim(), srcCol?.let { c.getOrNull(it)?.trim() } ?: file)
            }
            .toList()
    }

    private fun load(file: String, idCol: Int, textCol: Int, srcCol: Int?): List<Case> =
        rows(file, idCol, textCol, srcCol).filter { it.id in liveIds }   // bỏ câu trỏ id đã gỡ

    private fun measure(name: String, cases: List<Case>, floorPct: Int) {
        assertTrue(cases.isNotEmpty(), "$name: 0 câu còn sống ⇒ bài tự tắt — corpus hoặc liveIds hỏng")
        val fails = cases.filter { c -> c.id !in resolvedIds(VoiceIntentParser.parse(c.text, apps = apps)) }
        val pass = cases.size - fails.size
        val pct = pass * 100 / cases.size
        val sample = fails.take(40).joinToString("\n") { "  [${it.id}·${it.source}] \"${it.text}\"" }
        println("GOLDEN $name: $pct% ($pass/${cases.size}) — ${fails.size} FAIL")
        if (fails.isNotEmpty()) println("GOLDEN $name FAIL (≤40):\n$sample")
        assertTrue(pct >= floorPct, "$name coverage $pct% ($pass/${cases.size}) < sàn $floorPct%. FAIL mẫu:\n$sample")
    }

    /**
     * Câu ĐÚNG — câu mở app + việc launcher của `variants.tsv`. Sàn [ĐO 2026-10-09 W3]: 55 % (611/1103) (trước W3 sàn 85 % tính trên câu
     * nút/datum xe; câu mở app khi ấy KHÔNG nằm trong mẫu số vì `open_app` không phải id của bộ đăng ký nào).
     */
    @Test fun `corpus cau DUNG - coverage tren parser that`() {
        measure("variants(ĐÚNG)", load("variants.tsv", idCol = 0, textCol = 4, srcCol = 2), floorPct = 55)
    }

    /**
     * ═══ Câu về xe (id đã gỡ) — KHÔNG BAO GIỜ thành một lệnh khác ════════════════════════════════════════════
     *
     * Mọi câu trỏ id đã gỡ ở `variants.tsv` · `misspell.tsv` · `mix.tsv`: kết quả phân tích chỉ được gồm
     * [VoiceIntent.Unknown] / [VoiceIntent.EndSession] — không mở app (nhất là app tên gần âm "Kinh Thánh" ·
     * "Pin Tester"), không việc launcher, nhạc, dẫn đường, hồ sơ, bố cục. Sàn 100 %. Phần câu ra đúng
     * `FEATURE_GONE` in ra để theo dõi (không ghim: câu hỏi lại `NO_OBJECT` cũng an toàn).
     */
    @Test fun `corpus cau ve xe da go KHONG thanh lenh khac`() {
        fun isCar(id: String) = id !in liveIds && id !in NOT_CAR && !id.startsWith("unknown_")
        val car = (rows("variants.tsv", 0, 4, 2) + rows("misspell.tsv", 0, 1, 2)).filter { isCar(it.id) } +
            mixRows().filter { (want, _) -> want.all { isCar(it) } }.map { (w, t) -> Case(w.joinToString(","), t, "mix") }
        assertTrue(car.size > 1000, "tiền đề: corpus còn đủ câu xe để soi (${car.size})")
        car.forEach { assertTrue(CapabilityCatalog.kindOf(it.id.substringBefore(',')) == null, "id '${it.id}' phải đã gỡ") }
        val parsed = car.map { it to VoiceIntentParser.parse(it.text, apps = apps) }
        val leaks = parsed.filter { (c, got) ->
            c.text !in KNOWN_TYPO_LEAKS && got.any { it !is VoiceIntent.Unknown && it != VoiceIntent.EndSession }
        }
        val gone = parsed.count { (_, got) ->
            got.isNotEmpty() && got.all { it is VoiceIntent.Unknown && it.reason == VoiceUnknownReason.FEATURE_GONE }
        }
        println("GOLDEN car-gone: ${gone * 100 / car.size}% FEATURE_GONE ($gone/${car.size}); ${leaks.size} câu thành lệnh khác")
        val sample = leaks.take(40).joinToString("\n") { (c, got) -> "  [${c.id}·${c.source}] \"${c.text}\" → $got" }
        assertTrue(leaks.isEmpty(), "${leaks.size}/${car.size} câu về xe thành lệnh KHÁC:\n$sample")
    }

    private fun mixRows(): List<Pair<List<String>, String>> {
        val f = File(root, "scripts/voice/data/mix.tsv")
        if (!f.exists()) return emptyList()
        return f.readLines().asSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val c = line.split("\t"); if (c.size < 2) return@mapNotNull null
                c[0].split(",").map { it.trim() }.filter { it.isNotEmpty() } to c[1].trim()
            }.toList()
    }

    private companion object {
        const val OPEN_APP = "open_app"
        const val OPEN_APP_SLOT = "open_app_slot"

        /**
         * Nhãn corpus KHÔNG phải xe và không đo ở bài coverage (nhạc · dẫn đường · bố cục · hồ sơ · cách gọi app theo
         * tên đích) + mọi `unknown_*` (câu cố ý không hiểu). Còn lại = id nút/datum/gói lệnh xe đã gỡ.
         */
        /**
         * [ĐO 2026-10-09] Hai câu SINH bằng luật lẫn âm (`misspell.tsv` nguồn `rule`) mà chữ *"cụm"* đã thành một chữ
         * khác (*"pum"* · *"tum"*) — không còn dấu hiệu xe nào để nhận ra; ra `Media(QUERY "trên pum")` (tìm bài, không
         * chạm gì ngoài app nhạc). Ghi đích danh để danh sách KHÔNG nở thành chỗ giấu lỗi: câu mới nào lọt là đỏ.
         */
        val KNOWN_TYPO_LEAKS = setOf("bật nhạc trên pum", "bật nhạc trên tum")

        val NOT_CAR = setOf(
            "media_play", "media_pause", "media_next", "media_prev", "media_query", "nav", "nav_saved", "layout",
            "profile", "gmaps", "waze", "youtube",
        )
    }
}
