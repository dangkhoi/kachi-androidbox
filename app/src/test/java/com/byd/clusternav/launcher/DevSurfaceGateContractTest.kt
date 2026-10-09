package com.byd.clusternav.launcher

import com.byd.clusternav.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Khoá **DÂY NỐI** của việc dọn dev/debug UI khỏi màn Cài đặt.
 *
 * ## ⚠ Bài này đã ĐẢO CHIỀU ở bản release production (owner 2026-09-21)
 * Bản đầu (UX-OVERHAUL · WP7, owner 2026-09-20: *"ẩn hết đồ dev/debug/log/lấy-info-xe khỏi UI, giữ chức năng chạy
 * qua adb"*) canh rằng mọi bề mặt dev **đứng SAU cổng** `DevMode.unlocked`. Owner chốt lại gọn hơn cho bản giao xe:
 * *"DỌN HẾT dev/debug UI khỏi màn Cài đặt, CHỈ GIỮ đúng công tắc Chế độ kiểm thử qua adb"* ⇒ các bề mặt ấy không
 * còn **ở đâu cả** trên màn, gác hay không gác đều hết nghĩa.
 *
 * Nên bài không bị xoá mà **đổi câu hỏi** — cùng lý do nó tồn tại từ đầu: đây là loại bất biến không nhìn thấy
 * được off-car. Mọi thứ vẫn biên dịch và test hành vi vẫn xanh kể cả khi một bề mặt dev **mọc lại**; nó chỉ lộ khi
 * có người mở màn Cài đặt trên xe. Ba nhóm:
 *  1. **Công tắc test-mode là bề mặt đồ đo DUY NHẤT còn lại** trong khối *Nâng cao*.
 *  2. **Không bề mặt dev nào mọc lại** — Diagnostics · VietMap-data · Gõ-lệnh-chữ · CAPTEST · nhật-ký-voice.
 *  3. **Đường thoát THẬT của người dùng ở lại** — hai nút cứu-hộ cast KHÔNG bị gỡ và cũng KHÔNG bị gác (cụm đang
 *     tối là việc của người lái, không phải của người viết code).
 *
 * ⚠ KHẢ NĂNG thì không mất, chỉ BỀ MẶT mất: `KachiTestBridge` vẫn nhận `say`/`captest`/`prefs_set`/`voice_dump`. Bài
 * [duong adb khong bi cat theo] ở dưới ghim điều đó — dọn UI mà dọn luôn đường adb thì lượt lên xe sau mất sạch công cụ.
 *
 * ⚠ Đính chính (2.93 wave 2B · DIAG-SCREENS-UNREACHABLE): câu cũ *"hai màn chẩn đoán vẫn mở được bằng `am start -n`"* SAI —
 * `exported=false` ⇒ uid shell bị từ chối (AOSP 10 r47 `checkStartAnyActivityPermission`; [ĐO xe 29/09]). Nay hai màn mở
 * được CHỈ bằng lệnh cầu `diag_screen` (sau công tắc test-mode) — một LỆNH, không phải bề mặt UI; luật *"không dựng lại lối
 * vào trong Cài đặt"* giữ nguyên ([hai man chan doan chi mo qua lenh cau sau cong test-mode]).
 */
class DevSurfaceGateContractTest {

    private fun code(rel: String) = SourceRoots.codeOf(rel)
    private val sections by lazy { code("src/main/java/com/byd/clusternav/launcher/SettingsSections.kt") }
    private val voiceModel by lazy { code("src/main/java/com/byd/clusternav/launcher/voice/VoiceModelSettings.kt") }
    private val devMode by lazy { code("src/main/java/com/byd/clusternav/launcher/DevMode.kt") }

    /**
     * [DevMode] gác bằng CHÍNH cửa sổ test-mode, KHÔNG bằng một khoá `dev_mode` riêng (bẫy hai-bản-sao).
     *
     * ⚠ [ĐO 2026-09-21] Sau lượt dọn, `DevMode.unlocked` có **0 chỗ gọi trong mã sản phẩm** — owner giữ tệp lại
     * (một cổng fail-safe đã đo, dùng được ngay nếu cần bày lại đồ đo nào). Bài này vì thế canh **hình dạng** của
     * cổng chứ không canh chỗ dùng: hôm nào có người nối lại thì nó phải nối vào cái cổng đúng, không phải dựng
     * cổng thứ hai.
     */
    @Test
    fun `cong dev gac bang cua so test-mode, khong sinh khoa thu hai`() {
        assertTrue(devMode.contains("TestBridgeStore.remainingMinutes"), "cổng đọc chính cửa sổ test-mode")
        assertTrue(devMode.contains(".getOrDefault(false)"), "đọc hỏng ⇒ ĐÓNG (mặc định fail-safe cho đồ dev)")
        assertEquals(
            0, Regex(""""dev_mode"""").findAll(devMode).count(),
            "không được sinh khoá dev_mode thứ hai — mở cửa thứ hai cho cùng một quyền là bẫy hai-bản-sao",
        )
    }

    /**
     * Khối *Nâng cao* của nhóm Hệ thống: **đúng một** bề mặt — công tắc test-mode.
     *
     * Ghim bằng cách ĐẾM chứ không chỉ bằng `contains`: `contains` một mình vẫn xanh khi có ai đó thêm bề mặt thứ
     * hai vào cùng khối, mà đó chính là hình dạng của lần trôi kế tiếp. Ô tick của công tắc do `testBridge(body)`
     * dựng bên trong nó, nên khối này **không được** còn lời gọi `body.addView(` nào.
     */
    @Test
    fun `khoi Nang cao chi con cong tac test-mode`() {
        val fn = SourceRoots.body(sections, "private fun system(")
        val advanced = fn.substringAfter("R.string.kachi_sub_advanced")
        assertTrue(advanced.contains("testBridge(body)"), "công tắc *Chế độ kiểm thử qua adb* phải còn")
        assertEquals(
            0, Regex("""body\.addView\(|\w+Console\(""").findAll(advanced).count(),
            "khối *Nâng cao* chỉ được gọi `testBridge(body)`; mọi hàng dựng thêm ở đây là đồ dev mọc lại",
        )
    }

    /** Năm bề mặt dev đã GỠ: không tệp section nào dựng lại chúng. */
    @Test
    fun `khong be mat dev nao con duoc dung trong Cai dat`() {
        val surfaces = listOf(
            "openDiagnostics()" to "mở DiagActivity",
            "openVietMapData()" to "mở màn dữ liệu/widget VietMap",
            "CapTestConsole(" to "bảng kiểm từng nút (CAPTEST)",
            "VoiceTextConsole(" to "ô gõ lệnh chữ",
            "VoiceUtteranceLog." to "ô tích + nút xuất nhật ký lượt nói",
        )
        val built = surfaces.filter { (needle, _) ->
            sections.contains(needle) || voiceModel.contains(needle)
        }
        assertEquals(
            emptyList<String>(), built.map { "${it.first} (${it.second})" },
            "bề mặt dev được dựng lại trong Cài đặt — owner 2026-09-21 chốt gỡ HẾT, chỉ giữ công tắc test-mode",
        )
        // `logRows` là hàm đã xoá: một lời gọi rỗng còn lại thì không ai đọc ra là nó rỗng.
        assertFalse(voiceModel.contains("logRows("), "khối nhật ký voice phải gỡ cả hàm lẫn lời gọi")
    }

    // Android box B2 · W2c — bài "cứu hộ cast ở lại" gỡ cùng nhóm Chiếu cụm (không còn cụm để trả về đồng hồ).

    /**
     * Dọn BỀ MẶT không được dọn theo KHẢ NĂNG: đường adb phải còn nguyên.
     *
     * Đây là nửa còn lại của lời giao ("các đường adb VẪN GIỮ"), và là nửa dễ mất im lặng nhất — gỡ một nút thì
     * thấy ngay, gỡ một nhánh `when` của receiver thì chỉ lượt lên xe sau mới biết.
     */
    @Test
    fun `duong adb khong bi cat theo`() {
        val bridge = code("src/main/java/com/byd/clusternav/launcher/testbridge/KachiTestBridge.kt")
        // Ba lệnh dev đi qua [TestBridgeNoHome] (chạy được cả khi màn chính chưa lên — đúng ca đang chẩn đoán).
        val noHome = code("src/main/java/com/byd/clusternav/launcher/testbridge/TestBridgeNoHome.kt")
        assertTrue(bridge.contains("TestBridgeCommands.SAY ->"), "`say` phải còn: ô Gõ lệnh chữ nay CHỈ còn đường này")
        // Android box B2 · W1 — `CAPTEST` (kiểm từng nút xe BYD) rời cầu; hai lệnh dev còn lại phải giữ nhánh.
        assertTrue(!noHome.contains("TestBridgeCommands.CAPTEST ->"), "captest đã gỡ khỏi Android box")
        listOf("PREFS_SET", "VOICE_DUMP").forEach {
            assertTrue(
                noHome.contains("TestBridgeCommands.$it ->"),
                "mất nhánh `$it` ⇒ lượt lên xe sau không còn công cụ, mà không bài nào khác thấy",
            )
        }
    }

    /**
     * Android box B2 · W1 (2026-10-09) — ĐỔI GHIM có lý do: hai màn chẩn đoán BYD (`DiagActivity` cụm · `VietMapWidgetDiagActivity`)
     * RỜI manifest, và lệnh cầu `diag_screen` (lối duy nhất của chúng từ 2.93 wave 2B) rời bảng lệnh ⇒ không còn đường nào mở
     * được chúng: manifest không khai, `TestBridgeNoHome` không có nhánh, và không mã sản phẩm nào ngoài tệp ánh xạ mồ côi
     * (`TestBridgeScreens.kt`, W2 xoá) nhắc hai lớp. Hai cửa cũ của cầu Cài đặt vẫn phải vắng.
     */
    @Test
    fun `hai man chan doan BYD khong con loi vao nao`() {
        val noHome = code("src/main/java/com/byd/clusternav/launcher/testbridge/TestBridgeNoHome.kt")
        assertTrue(!noHome.contains("DIAG_SCREEN"), "lệnh diag_screen đã gỡ khỏi cầu")
        val manifest = SourceRoots.text("src/main/AndroidManifest.xml")
        listOf(".modules.clustercast.DiagActivity", ".vietmapwidget.VietMapWidgetDiagActivity").forEach { name ->
            assertTrue(!manifest.contains("android:name=\"$name\""), "$name không được khai trong manifest Android box")
        }
        val main = SourceRoots.path("src/main/java")
        val touching = java.nio.file.Files.walk(main).use { s ->
            s.filter { it.toString().endsWith(".kt") }.filter { f ->
                val src = SourceRoots.codeOf("src/main/java/" + main.relativize(f).toString().replace('\\', '/'))
                src.contains("DiagActivity::class") || src.contains("VietMapWidgetDiagActivity::class")
            }.map { it.fileName.toString() }.toList()
        }.toSet()
        // Android box B2 · W2c — tệp ánh xạ mồ côi `TestBridgeScreens.kt` + hai lớp màn đã xoá.
        assertEquals(emptySet<String>(), touching, "không mã nào còn nhắc hai lớp màn chẩn đoán BYD")
        val mentions = java.nio.file.Files.walk(main).use { s ->
            s.filter { it.toString().endsWith(".kt") }.map { f ->
                Regex("""\b(openDiagnostics|openVietMapData)\(""")
                    .findAll(SourceRoots.codeOf("src/main/java/" + main.relativize(f).toString().replace('\\', '/'))).count()
            }.toList().sum()
        }
        assertEquals(0, mentions, "hai cửa cũ của cầu Cài đặt đã gỡ — mọc lại định nghĩa hay chỗ gọi là mọc lại bề mặt")
    }
}
