package com.byd.clusternav.launcher

import com.byd.clusternav.launcher.testbridge.TestBridgeCommands
import com.byd.clusternav.testsupport.SourceRoots
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ T-BRIDGE · BÀI CANH AN TOÀN CỦA CẦU KIỂM THỬ QUA adb ════════════════════════════════════════════════════
 *
 * Spec `docs/specs/kachi-test-bridge.html` §Verification. Cầu này là thứ **duy nhất** trong dự án cố tình
 * `exported="true"` cho uid shell, nên bốn tính chất dưới đây không được phép rữa:
 *
 *  1. receiver khai `exported` **và** action dựng từ `${'$'}{applicationId}` (một nguồn cho manifest + mã);
 *  2. **mọi** nhánh lệnh nằm sau cổng `TestBridgeStore.isOn` — không có đường vòng;
 *  3. `auto_confirm` chỉ có tác dụng bên trong cầu, và luôn để lại dấu `AUTO-CONFIRM` trong nhật ký;
 *  4. không nhánh nào chạm màn cụm / tầng chiếu cụm / shell thô, và mọi tệp dưới trần 500 dòng.
 *
 * ## Vì sao bài này quét VĂN BẢN chứ không gọi hàm
 * Vì thứ cần canh là **hình dạng của mã**, không phải một giá trị trả về: "có còn cổng không", "có ai mở một
 * đường thứ hai không". Một bài gọi hàm chỉ chứng minh được đường đang có chạy đúng — nó mù với đường mới mà ai
 * đó thêm ngày mai. Cùng lối với `LauncherWindowingGuardTest` (§SOURCE-SCAN) và `GridSeamGuardTest`.
 */
class TestBridgeSafetyContractTest {

    private val dir = "src/main/java/com/byd/clusternav/launcher/testbridge"

    private fun code(name: String): String = SourceRoots.codeOf("$dir/$name")

    private fun sources(): List<Path> = Files.walk(SourceRoots.path(dir)).use { s ->
        s.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }.sorted().toList()
    }

    private val manifest by lazy { SourceRoots.text("src/main/AndroidManifest.xml") }

    /**
     * Đọc một tệp ở GỐC repo (vd `scripts/…`) — [SourceRoots] chỉ biết cây source của module, còn `:app:test` chạy
     * với cwd = thư mục module nên script nằm ở `../`. Thử vài mức cha cho chắc (module / gốc repo).
     */
    private fun repoText(rel: String): String = repoPath(rel).toFile().readText()

    /** Đường dẫn thật của một mục ở gốc repo — dùng chung cho cả tệp lẫn thư mục. */
    private fun repoPath(rel: String): Path {
        val tries = listOf(rel, "../$rel", "../../$rel").map(java.nio.file.Paths::get)
        return tries.firstOrNull { java.nio.file.Files.exists(it) }
            ?: error("không tìm thấy $rel; đã thử: ${tries.joinToString()}")
    }

    // ── (1) Khai báo ở manifest ─────────────────────────────────────────────────────────────────

    @Test
    fun `receiver khai exported va action dung applicationId`() {
        assertTrue(
            manifest.contains("android:name=\".launcher.testbridge.KachiTestBridge\""),
            "receiver của cầu kiểm thử phải có mặt trong manifest",
        )
        val at = manifest.indexOf(".launcher.testbridge.KachiTestBridge")
        val block = manifest.substring(at, (at + 600).coerceAtMost(manifest.length))
        assertTrue(block.contains("android:exported=\"true\""), "uid shell không gửi được vào receiver non-exported")
        assertTrue(
            block.contains("\${applicationId}.TEST"),
            "action phải dựng từ \${applicationId} — đổi tên gói mà cầu câm lặng là lỗi không ai thấy",
        )
    }

    /** Hằng hậu tố action ở mã phải khớp manifest — hai nơi lệch nhau thì broadcast không bao giờ tới. */
    @Test
    fun `hau to action o ma khop manifest`() {
        assertTrue(
            code("KachiTestBridge.kt").contains("ACTION_SUFFIX = \".TEST\""),
            "hậu tố action ở mã phải là `.TEST`, đúng chuỗi manifest khai",
        )
    }

    // ── (2) Cổng công tắc ───────────────────────────────────────────────────────────────────────

    /**
     * ⚠⚠ Đây là bài quan trọng nhất của tệp này.
     *
     * `onReceive` phải hỏi [com.byd.clusternav.launcher.testbridge.TestBridgeStore].isOn **trước** khi phân tích
     * lệnh — phân tích trước rồi mới hỏi là mở đường cho một nhánh tương lai "chỉ đọc thôi, cho qua".
     */
    @Test
    fun `moi lenh deu nam sau cong cong tac`() {
        val src = code("KachiTestBridge.kt")
        val gate = src.indexOf("TestBridgeStore.isOn(")
        val dispatch = src.indexOf("TestBridgeCommands.parse(")
        assertTrue(gate > 0, "không còn cổng `TestBridgeStore.isOn` trong receiver")
        assertTrue(dispatch > gate, "cổng phải đứng TRƯỚC lượt phân tích lệnh")
        assertEquals(
            1, Regex("""fun\s+dispatch\(""").findAll(src).count(),
            "chỉ được có MỘT cửa điều phối — cửa thứ hai là cửa không ai canh",
        )
    }

    /**
     * Hai đường GHI thô của lệnh `hal` (`set` và `setev` — generic `AbsBYDAutoDevice.set`) đều phải đứng sau cổng
     * `auto_confirm` và để lại dấu `AUTO-CONFIRM` trong nhật ký — cùng cổng với `ctl`/`say`. Bài canh cũ chỉ quét
     * `KachiTestBridge.kt`; cổng của `hal` nằm ở `TestBridgeHal.kt` nên một lượt tách tệp có thể làm rơi nó im
     * lặng (CLAUDE.md §8). Đề nghị của lượt scan bảo mật 2026-09-16.
     */
    @Test
    fun `hal set va setev deu qua cong auto_confirm va ghi dau AUTO-CONFIRM`() {
        val src = code("TestBridgeHal.kt")
        val setEvStart = src.indexOf("private fun runSetEv(")
        assertTrue(setEvStart > 0, "không còn `runSetEv` — đổi tên thì sửa bài canh CÓ CHỦ Ý")
        val setPart = src.substring(0, setEvStart)
        val setEvPart = src.substring(setEvStart)
        for ((label, part) in listOf("hal set" to setPart, "hal setev" to setEvPart)) {
            val gate = part.indexOf("!cmd.autoConfirm")
            val log = part.indexOf("AUTO-CONFIRM: $label")
            assertTrue(gate > 0, "$label: mất cổng `!cmd.autoConfirm` ⇒ ghi thân xe không cần xác nhận")
            assertTrue(log > gate, "$label: dấu `AUTO-CONFIRM` phải đứng SAU cổng (ghi rồi mới log là log cho lệnh đã chạy)")
            assertTrue(part.substring(gate, log).contains("ERR_NEEDS_CONFIRM"), "$label: thiếu lối từ chối `ERR_NEEDS_CONFIRM` giữa cổng và dấu")
        }
    }

    /** Không có đường BẬT nào ngoài màn Cài đặt: không lệnh `enable`, và receiver không gọi `TestBridgeStore.enable`. */
    @Test
    fun `khong co duong bat che do kiem thu tu xa`() {
        val switchLike = setOf("enable", "on", "unlock", "grant", "allow")
        assertTrue(
            TestBridgeCommands.NAMES.none { it in switchLike },
            "bảng lệnh không được có lệnh bật chế độ kiểm thử: ${TestBridgeCommands.NAMES}",
        )
        sources().filter { it.fileName.toString() != "TestBridgeStore.kt" }.forEach { f ->
            val src = SourceRoots.codeOf("$dir/${f.fileName}")
            assertTrue(
                !src.contains("TestBridgeStore.enable("),
                "${f.fileName}: chỉ màn Cài đặt được bật chế độ kiểm thử (người trong xe chủ động)",
            )
        }
        // Và bề mặt bật thật sự phải TỒN TẠI — nếu không thì cầu này là mã chết (CLAUDE.md §8).
        val settings = SourceRoots.codeOf("src/main/java/com/byd/clusternav/launcher/SettingsSections.kt")
        assertTrue(settings.contains("TestBridgeStore.enable("), "màn Cài đặt phải có công tắc bật")
        assertTrue(settings.contains("TestBridgeStore.disable("), "và phải tắt được ngay")
    }

    // ── (3) Cổng xác nhận ───────────────────────────────────────────────────────────────────────

    @Test
    fun `auto confirm luon de lai dau trong nhat ky`() {
        val src = code("KachiTestBridge.kt")
        val at = src.indexOf("cmd.autoConfirm")
        assertTrue(at > 0, "nhánh auto-confirm đã biến mất")
        val branch = src.substring(at, (at + 400).coerceAtMost(src.length))
        assertTrue(
            branch.contains("AUTO-CONFIRM"),
            "một việc mức CONFIRM không được xảy ra mà không có dấu vết grep được trong logcat",
        )
    }

    // ── (4) Vùng cấm ────────────────────────────────────────────────────────────────────────────

    /**
     * Không nhánh nào chạm màn cụm hay tầng chiếu cụm.
     *
 * Android box B2 · W2c: `modules.clustercast` gỡ hẳn (kể cả `ClusterDiag`, ngoại lệ chỉ-đọc cũ của lệnh `diag`).
     */
    @Test
    fun `khong nhanh nao cham man cum hay tang chieu cum`() {
        val forbidden = listOf("CastShell", "SimpleCastRuntime", "SimpleCastCoordinator", "CastAutomation", "evictVd")
        val displayGe1 = Regex("""--display\s+[1-9]""")
        sources().forEach { f ->
            val src = SourceRoots.codeOf("$dir/${f.fileName}")
            forbidden.forEach { token ->
                assertTrue(!src.contains(token), "${f.fileName}: cầu kiểm thử KHÔNG được chạm `$token`")
            }
            assertTrue(!displayGe1.containsMatchIn(src), "${f.fileName}: display ≥ 1 là màn cụm — vùng cấm")
        }
    }

    /** Android box B2 · W2c — cầu không còn nhắc `modules.clustercast` ở đâu (lệnh `diag` + tệp `TestBridgeScreens` gỡ). */
    @Test
    fun `cau khong con cham modules clustercast`() {
        sources().forEach { f ->
            assertTrue(!SourceRoots.text("$dir/${f.fileName}").contains("com.byd.clusternav.modules.clustercast"), "${f.fileName}")
        }
        assertTrue(!SourceRoots.exists("$dir/TestBridgeScreens.kt"), "tệp ánh xạ màn chẩn đoán BYD đã xoá")
    }

    /** Cầu không tự chạy lệnh shell: mọi việc phải đi qua đường mà một cú chạm đi (CLAUDE.md §4). */
    @Test
    fun `khong nhanh nao tu chay lenh am hay wm`() {
        val raw = Regex("""["'](am |wm |settings |pm |dumpsys |service call )""")
        sources().forEach { f ->
            val src = SourceRoots.codeOf("$dir/${f.fileName}")
            val hits = raw.findAll(src).map { it.value }.toList()
            assertTrue(hits.isEmpty(), "${f.fileName}: lệnh shell thô $hits — phải đi qua đường đã có")
        }
    }

    /**
     * Mọi lệnh mang `--es pkg` phải đi qua cổng [installed] TRƯỚC khi chạm launcher.
     *
     * ⚠ Khoá lại phát hiện review 09-14 (P0): `pkg` là chuỗi từ ngoài tiến trình, và đường đi tiếp của nó có
     * một đoạn **shell** (`AppOpener.openByShell` → `FreeformLaunch.resolveCmd` nhét nguyên chuỗi vào
     * `cmd package resolve-activity … $pkg`). Gói không cài ⇒ `openByIntent` trả false ⇒ rơi đúng xuống nhánh
     * shell. Bỏ cổng này là mở lại đường chèn lệnh.
     */
    @Test
    fun `lenh mang pkg deu qua cong app da cai`() {
        val src = code("KachiTestBridge.kt")
        listOf("runSlot(", "runOpen(").forEach { fn ->
            val at = src.indexOf("private fun $fn")
            assertTrue(at > 0, "hàm $fn đã biến mất")
            val body = src.substring(at, (at + 320).coerceAtMost(src.length))
            assertTrue(
                body.contains("installed(app, cmd.pkg)"),
                "$fn phải hỏi `installed(app, cmd.pkg)` trước khi chạm launcher — `pkg` đi tới một đoạn shell",
            )
        }
    }

    /** Gói extra đến từ tiến trình khác: một `Parcelable` lạ không được phép giết launcher. */
    @Test
    fun `doc extra khong lam chet tien trinh`() {
        val src = code("KachiTestBridge.kt")
        assertTrue(
            src.contains("runCatching { readExtrasOrThrow(intent) }"),
            "lượt bung gói extra phải bọc `runCatching` — ném trong `onReceive` là launcher chết",
        )
    }

    /** Tệp kết quả phải có trần: receiver `exported` ⇒ mọi app bắn được, kể cả khi chế độ kiểm thử TẮT. */
    @Test
    fun `tep ket qua co tran so luong`() {
        val src = code("TestBridgeReply.kt")
        assertTrue(src.contains("prune(dir)"), "lượt ghi phải gọi `prune` — không có trần là một đường làm đầy bộ nhớ")
        assertTrue(src.contains("KEEP = 50"), "trần số tệp kết quả phải còn đó")
    }

    @Test
    fun `moi tep duoi tran 500 dong`() {
        sources().forEach { f ->
            val n = f.toFile().readLines().size
            assertTrue(n <= 500, "${f.fileName}: $n dòng — trần 500 (CLAUDE.md §4.1)")
        }
        assertTrue(sources().size >= 5, "bộ quét chỉ thấy ${sources().size} tệp — nghi sai gốc quét")
    }

    /**
     * Tên màn ảo của ô: bản sao ở `TestBridgeState` phải còn khớp `VdAppHost`.
     *
     * Đây là bản sao **có chủ ý** (hai tệp đang do hai người sửa), nên nó cần một lưới an toàn: đổi cách đặt tên
     * ở `VdAppHost` mà quên ở đây thì lệnh `state` báo *"0 màn ảo"* trong khi có 4 — một con số sai trông y như
     * một con số đúng.
     */
    @Test
    fun `tien to ten man ao con khop VdAppHost`() {
        // 2.98 · R4: tên màn ảo ô nay do `SlotVdName` (:core) đặt — VdAppHost phải gọi nó, và tiền tố sống ở đó.
        val host = SourceRoots.codeOf("src/main/java/com/byd/clusternav/launcher/VdAppHost.kt")
        assertTrue(
            host.contains("SlotVdName.pick(slot,"),
            "VdAppHost không còn đặt tên màn ảo qua `SlotVdName.pick` ⇒ sửa `TestBridgeState.VD_NAME_PREFIX`",
        )
        assertEquals("kachi-slot-", com.byd.clusternav.launcher.SlotVdName.PREFIX, "tiền tố `SlotVdName` đổi ⇒ sửa gương")
        assertTrue(
            code("TestBridgeState.kt").contains("VD_NAME_PREFIX = \"kachi-slot-\""),
            "tiền tố ở `TestBridgeState` phải khớp `VdAppHost`",
        )
    }

    // ── (5) Lệnh `ctl` — bắn control qua ĐÚNG applier + cổng CONFIRM ─────────────────────────────

    /** `ctl` đi qua cổng port [hooks.control] (applier), KHÔNG dựng adapter thứ hai / không tự gọi HAL. */
    @Test
    fun `ctl ban control qua dung applier port`() {
        val src = code("TestBridgeCtl.kt")
        assertTrue(src.contains("hooks.control("), "ctl phải bắn qua `hooks.control` = cổng CarControlAdapter.actByKind")
        // Không được import/gọi thẳng gateway/BydHal (đường thứ hai xuống xe).
        listOf("BydHalGateway", "BydHal.", "CarControlAdapter(").forEach { token ->
            assertTrue(!src.contains(token), "TestBridgeCtl KHÔNG được chạm `$token` — phải đi qua port đã tiêm")
        }
    }

    /** Câu chữ HAL đọc TRONG tiến trình (HalWriteProbe), KHÔNG spawn `logcat`/tiến trình con. */
    @Test
    fun `ctl doc ket qua HAL trong tien trinh khong spawn logcat`() {
        val src = code("TestBridgeCtl.kt")
        assertTrue(src.contains("HalWriteProbe.clear()"), "phải xoá sổ TRƯỚC khi bắn")
        assertTrue(src.contains("HalWriteProbe.last"), "phải đọc kết quả ghi từ sổ trong tiến trình")
        listOf("logcat", "ProcessBuilder", "Runtime.getRuntime").forEach { token ->
            assertTrue(!src.contains(token), "TestBridgeCtl KHÔNG được spawn tiến trình (`$token`) — đọc trong tiến trình")
        }
    }

    /** Control mở/khoá thân xe bị cổng CONFIRM chặn (auto_confirm), có dấu AUTO-CONFIRM. */
    @Test
    fun `ctl co cong CONFIRM cho control mo khoa than xe`() {
        val src = code("TestBridgeCtl.kt")
        assertTrue(src.contains("CtlSafetyPolicy.needsConfirm("), "ctl phải hỏi CtlSafetyPolicy trước khi bắn")
        val at = src.indexOf("CtlSafetyPolicy.needsConfirm(")
        val branch = src.substring(at, (at + 500).coerceAtMost(src.length))
        assertTrue(branch.contains("autoConfirm"), "phải TỪ CHỐI khi thiếu auto_confirm")
        assertTrue(branch.contains("AUTO-CONFIRM"), "việc mức CONFIRM phải để dấu grep được trong logcat")
    }

    /** Sổ ghi HAL chỉ được điền ở tầng gateway (một chỗ), không rải rác. */
    @Test
    fun `HalWriteProbe chi ghi tu BydHalGateway`() {
        val gw = SourceRoots.codeOf("src/main/java/com/byd/clusternav/launcher/BydHalGateway.kt")
        assertTrue(gw.contains("HalWriteProbe.record("), "gateway phải ghi kết quả mỗi lượt write control")
    }

    // ── (6) Script sweep HAL — Android box B2 · W2a ─────────────────────────────────────────────────
    // `scripts/vehicle/kachi/71-hal-sweep.sh` (quét HAL trên xe, DENYLIST khớp `CtlSafetyPolicy`) gỡ cùng bộ script xe BYD;
    // hai bài canh nó gỡ theo. Cầu kiểm thử không còn lệnh `sweep` (W1 — ra `unknown_cmd`).

    /** Mọi lượt chạy đều để lại dấu: một dòng nhật ký lúc NHẬN và một dòng lúc TRẢ LỜI. */
    @Test
    fun `moi luot chay deu ghi nhat ky`() {
        assertTrue(code("KachiTestBridge.kt").contains("Log.i(TAG, \"cmd \""), "phải ghi nguyên văn lệnh nhận được")
        assertTrue(code("TestBridgeReply.kt").contains("Log.i(TAG, \"reply"), "phải ghi lời đáp")
        assertTrue(code("TestBridgeReply.kt").contains("TAG = \"KachiTest\""), "tag nhật ký phải là `KachiTest`")
    }

    // ── (7) Lệnh gọi cầu phải TƯỜNG MINH ────────────────────────────────────────────────────────

    /**
     * ⚠⚠ [ĐO xe 2026-09-20 §0] Lệnh gọi cầu PHẢI TƯỜNG MINH — mang `-n <thành phần>` hoặc `-p <gói>`, không chỉ `-a`.
     *
     * Android 10 chặn broadcast ngầm tới receiver khai trong manifest, và cái chặn đó **im lặng**: `am broadcast
     * -a <gói>.TEST …` trả `result=0` **không có `data=`** — đọc giống hệt ca *"cầu chưa bật"* hoặc *"adb hỏng"*. Mấy buổi
     * test đã nghi oan cho kết nối (ghi vào tài liệu là "BUG2") trong khi lỗi nằm ở đúng một dòng dựng lệnh.
     *
     * Android box B2 · W2a: bộ script xe `scripts/vehicle/kachi/` (nơi bài này từng canh `_common.sh`) đã gỡ; bộ script còn
     * gọi cầu là QA máy ảo `scripts/emulator/` ⇒ bài chuyển sang đó. Vế thứ hai là phép so **ranh giới script ↔ mã**: chuỗi
     * `-n` nào có thì tên lớp trong đó phải là tên lớp THẬT (lấy từ `KachiTestBridge::class.java.name`, không chép tay).
     */
    @Test
    fun `script may ao goi cau bang dich tuong minh khop ten lop that`() {
        val fqn = com.byd.clusternav.launcher.testbridge.KachiTestBridge::class.java.name
        val calls = bridgeCalls()
        assertTrue(calls.isNotEmpty(), "bộ quét phải thấy lệnh gọi cầu trong scripts/emulator — bài đang tự tắt")
        val implicit = calls.filter { (_, l) -> !l.contains("-n ") && !l.contains("-p ") }
        assertTrue(implicit.isEmpty(), "lệnh gọi cầu thiếu -n/-p (sẽ im lặng rơi trên Android 10): $implicit")
        val wrongClass = calls.filter { (_, l) -> l.contains("-n ") && !l.contains("/$fqn") }
        assertTrue(wrongClass.isEmpty(), "chuỗi -n phải trỏ đúng lớp thật $fqn: $wrongClass")
    }

    /** Mọi dòng MÃ (không chú thích) của các tệp `.sh` dưới `scripts/emulator` bắn broadcast vào cầu `<gói>.TEST`. */
    private fun bridgeCalls(): List<Pair<String, String>> = Files.walk(repoPath("scripts/emulator")).use { s ->
        s.filter { it.toString().endsWith(".sh") }
            .toList()
            .flatMap { p -> Files.readAllLines(p).map { p.fileName.toString() to it.trim() } }
            .filter { (_, l) -> !l.startsWith("#") && l.contains("am broadcast") && l.contains(".TEST") }
    }

    // ── (8) Camera BYD — Android box B2 · W2b ───────────────────────────────────────────────────
    // Ba lệnh camera (`camera` · `camera_frame` · `camera_synth`) cùng mã thi hành (`TestBridgeCamera*` · `TestBridgeSynth` ·
    // `TestBridgePerCam`) và ba móc camera của `TestBridgeHooks` gỡ hẳn. Bốn bài canh chuỗi dây `camera_frame` gỡ theo.

    @Test
    fun `cau kiem thu khong con lenh, tep hay moc camera`() {
        listOf("camera", "camera_frame", "camera_synth").forEach {
            assertTrue(it !in TestBridgeCommands.NAMES, "`$it` không được quay lại bảng lệnh")
        }
        listOf("TestBridgeCamera.kt", "TestBridgeCameraFrame.kt", "TestBridgeSynth.kt", "TestBridgePerCam.kt").forEach {
            assertFalse(SourceRoots.exists("src/main/java/com/byd/clusternav/launcher/testbridge/$it"), "$it đã xoá ở W2b")
        }
        val hooks = code("TestBridgeHooks.kt")
        listOf("cameraTick", "cameraFrame", "cameraSynth", "cameraSignal").forEach {
            assertFalse(hooks.contains(it), "móc camera `$it` đã gỡ khỏi TestBridgeHooks")
        }
    }
}
