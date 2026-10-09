package com.kachi.box.modules.navaccess

import com.kachi.box.launcher.HomeActivityCmd
import com.kachi.box.system.StackParse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Khoá bài học hiện trường 29/09 (quyết định owner 2.83-B): sau lượt chữa force-stop + lắp lại dịch vụ Hỗ trợ,
 * **launcher phải lên lại, không bỏ người dùng trước một cửa sổ app khách mồ côi**.
 *
 * [ĐO owner 29/09] sau "Sửa ngay", YouTube của một Ô nổi thành cửa sổ freeform trên màn chính, launcher không lên;
 * bấm Home một lần là xong. Cơ chế + bằng chứng: KDoc [AccessibilityRebind.RETURN_HOME_IF_ORPHANED].
 *
 * Fixture là `am stack list` NGUYÊN VĂN từ xe 29/09 (`core/src/test/resources/diagnostics/am-stack-list-oncar-
 * 2026-09-29-*.txt`; chỉ có id stack/task, component, khung, cấu hình — không IP/serial/đích dẫn đường):
 *  • `after-fix-orphan-top` — 11:28:13, ngay sau "Sửa ngay": display 0 = YouTube freeform TRÊN home. Phải bắn Home.
 *  • `stuck-home-top`       — 11:09:44, lúc đang kẹt: display 0 = home trên cùng. Không được bắn.
 *  • `fullscreen-app-top`   — 12:11:56: display 0 = Google Maps toàn màn trên cùng. Không được bắn.
 *
 * Android box B2 · W2b (2026-10-09): rào camera lùi BYD (`com.byd.avc/`) gỡ — mọi lần Home của đuôi là Home TRẦN
 * ([AccessibilityRebind.GO_HOME]). Các ca `camera-*-derived` của bản BYD gỡ theo.
 *
 * Phần chạy-bằng-`sh`: dựng đúng chuỗi lệnh xe sẽ nhận, chạy bằng shell POSIX thật với `am`/`settings`/`sleep`
 * giả (ghi nhật ký, `am stack list` in fixture). Nó khoá cùng lúc: thứ tự, cổng đo, và việc cả chuỗi còn SỐNG
 * qua hai lớp trích dẫn (`sh -c '…'` bọc `"…"` và `$(…)`) — thứ mà so chuỗi thuần không chứng minh được, trong khi
 * trích dẫn hỏng đồng nghĩa với việc lắp lại dịch vụ không chạy = phím chết hẳn. Máy không có `/bin/sh` (Windows)
 * thì bỏ qua phần này, phần so chuỗi vẫn chạy.
 */
class ForceStopReturnHomeTest {

    private val pkg = "com.byd.launcher"
    private val comp = "com.byd.launcher/com.byd.clusternav.modules.navaccess.NavAccessibilityService"
    private val oem = "com.byd.vrassistant.xf/com.iflytek.autofly.access.service.AccessibilityServices"
    private val home = "am start -a android.intent.action.MAIN -c android.intent.category.HOME"

    private fun fixture(name: String): String =
        javaClass.getResourceAsStream("/diagnostics/am-stack-list-oncar-2026-09-29-$name.txt")
            ?.bufferedReader()?.readText()
            ?: error("thiếu fixture core/src/test/resources/diagnostics/am-stack-list-oncar-2026-09-29-$name.txt")

    // ── Chuỗi lệnh ────────────────────────────────────────────────────────────────────────────────────

    @Test
    fun `lenh day du KHOP TUNG BYTE - Home dung CUOI, sau khi lap lai dich vu`() {
        assertEquals(
            "nohup sh -c 'am force-stop com.byd.launcher ; sleep 4 ; " +
                "settings put secure enabled_accessibility_services \"$oem:$comp\" ; " +
                "settings put secure accessibility_enabled 1 ; sleep 2 ; " +
                "t=\$(am stack list | grep -A1 \"displayId=0 \" | head -n 2) ; " +
                "case \"\$t\" in *\" mWindowingMode=freeform mDisplayWindowingMode=fullscreen mActivityType=standard \"*) " +
                "$home ;; esac' >/dev/null 2>&1 </dev/null &",
            AccessibilityRebind.forceStopRebindCommand(oem, pkg, comp, pauseSec = 4),
        )
    }

    /** Android box B2 · W2b — cửa Home duy nhất của đuôi là Home TRẦN: không rào camera, không đọc lại trước khi Home. */
    @Test
    fun `moi lan Home cua duoi la Home tran - khong con rao camera`() {
        assertEquals(home, AccessibilityRebind.GO_HOME)
        for (tail in AccessibilityRebind.HomeTail.entries) {
            val cmd = AccessibilityRebind.forceStopRebindCommand(oem, pkg, comp, homeTail = tail)
            assertFalse(cmd.contains("com.byd.avc"), "$tail: không còn dấu camera BYD: $cmd")
            assertFalse(cmd.contains("visible=true"), "$tail: không còn lượt đọc rào camera: $cmd")
        }
    }

    @Test
    fun `Home chi duoc dung SAU accessibility_enabled 1`() {
        val cmd = AccessibilityRebind.forceStopRebindCommand(oem, pkg, comp)
        val kill = cmd.indexOf("am force-stop $pkg")
        val readd = cmd.indexOf("settings put secure enabled_accessibility_services")
        val enable = cmd.indexOf("settings put secure accessibility_enabled 1")
        val probe = cmd.indexOf("am stack list")
        val goHome = cmd.indexOf(home)
        assertTrue(kill in 0 until readd, "giết trước, lắp lại sau")
        assertTrue(readd < enable, "danh sách trước, công tắc sau")
        assertTrue(
            enable in 0 until probe && probe < goHome,
            "đường lắp lại dịch vụ (giữ phím sống) KHÔNG được chờ hay phụ thuộc bước làm đẹp màn: đo + Home phải " +
                "đứng CUỐI (CLAUDE.md §6)",
        )
        assertEquals(1, Regex(Regex.escape(home)).findAll(cmd).count(), "đúng MỘT lần mở Home")
    }

    @Test
    fun `chot cung goi van con - lech goi thi KHONG co lenh nao, ke ca Home`() {
        for (bad in listOf("com.google.android.youtube", "", "  ")) {
            assertEquals("", AccessibilityRebind.forceStopRebindCommand(oem, bad, comp), "gói='$bad' ⇒ không dựng lệnh")
        }
        assertEquals(
            "",
            AccessibilityRebind.forceStopRebindCommand("oem/svc';reboot;'", pkg, comp),
            "mục mang ký tự shell ⇒ từ chối cả lệnh (đuôi Home không được lọt ra một mình)",
        )
    }

    @Test
    fun `lenh Home dung hinh dang de thanh loai HOME tren display 0`() {
        assertEquals(home, HomeActivityCmd.GO_HOME, "byte đã chạy ngoài hiện trường (đường trả nền của VietMap)")
        assertEquals(1, Regex(" -c ").findAll(HomeActivityCmd.GO_HOME).count(),
            "isHomeIntent (ActivityRecord.java:1241) đòi ĐÚNG MỘT category")
        assertFalse(HomeActivityCmd.GO_HOME.contains(" -n "),
            "chỉ định component từ uid shell ⇒ loại standard, không vào stack home (ActivityRecord.java:1283; " +
                "[ĐO xe 29/09] một lượt mở chỉ định `…KachiHomeActivity` nằm ở stack 41 riêng, mActivityType=standard)")
        assertFalse(HomeActivityCmd.GO_HOME.contains("--display"), "không --display ⇒ DEFAULT_DISPLAY (ActivityStarter.java:1484-1486)")
    }

    @Test
    fun `duoi khong co dau nhay don - ca chuoi nam trong sh -c nhay don`() {
        assertFalse(AccessibilityRebind.RETURN_HOME_IF_ORPHANED.contains('\''), AccessibilityRebind.RETURN_HOME_IF_ORPHANED)
        assertFalse(AccessibilityRebind.GO_HOME.contains('\''), AccessibilityRebind.GO_HOME)
    }

    // ── Dấu vân tay đo trên dump thật ─────────────────────────────────────────────────────────────────

    @Test
    fun `dau van tay chi khop DUNG stack mo coi tren ca ba dump that`() {
        val sig = AccessibilityRebind.ORPHAN_SIGNATURE
        assertEquals(1, Regex(Regex.escape(sig)).findAll(fixture("after-fix-orphan-top")).count(),
            "dump sau khi chữa: đúng MỘT stack mồ côi (YouTube, stack 32)")
        assertEquals(0, Regex(Regex.escape(sig)).findAll(fixture("stuck-home-top")).count(),
            "stack freeform của màn ảo Ô (`mDisplayWindowingMode=freeform`) KHÔNG được khớp")
        assertEquals(0, Regex(Regex.escape(sig)).findAll(fixture("fullscreen-app-top")).count())
    }

    @Test
    fun `cong shell va StackParse dong y voi nhau tren cung dump`() {
        // Định nghĩa "cửa sổ nổi lạc trên màn chính" đã có ở Kotlin (StackParse.floatingOnMain, lỗi hiện trường
        // 22/07). Cổng trong shell phải cùng kết luận về stack TRÊN CÙNG của display 0 — lệch là một trong hai sai.
        for ((name, orphanOnTop) in listOf("after-fix-orphan-top" to true, "stuck-home-top" to false, "fullscreen-app-top" to false)) {
            val entries = StackParse.parse(fixture(name))
            val top = entries.first { it.displayId == 0 }
            assertEquals(orphanOnTop, top in StackParse.floatingOnMain(entries), "$name: đỉnh display 0 = ${top.brief()}")
        }
    }

    // ── Chạy thật bằng sh ─────────────────────────────────────────────────────────────────────────────

    private fun shells(): List<String> {
        val found = listOf("/bin/sh", "/bin/dash").filter { File(it).canExecute() }
        assumeTrue(found.isNotEmpty() && File("/usr/bin/nohup").canExecute(), "không có shell POSIX (Windows) — bỏ qua")
        return found
    }

    /** Chạy NGUYÊN chuỗi [cmd] (kèm `wait` để đợi phần tách rời) và trả nhật ký các lệnh giả đã nhận. */
    private fun runOnShell(shell: String, cmd: String, stackList: String, dir: Path): List<String> {
        val bin = Files.createDirectories(dir.resolve("bin"))
        val log = dir.resolve("log").toFile().apply { writeText("") }
        val stacks = dir.resolve("stacks.txt").toFile().apply { writeText(stackList) }
        fun stub(name: String, body: String) = bin.resolve(name).toFile().apply {
            writeText("#!/bin/sh\necho \"$name \$*\" >> \"\$LOG\"\n$body\nexit 0\n"); setExecutable(true)
        }
        stub("am", "if [ \"\$1 \$2\" = \"stack list\" ]; then cat \"\$FIXTURE\"; fi")
        stub("settings", "")
        stub("sleep", "")   // không ngủ thật: thứ tự mới là thứ cần khoá
        Files.deleteIfExists(bin.resolve("sh"))
        Files.createSymbolicLink(bin.resolve("sh"), Path.of(shell))   // `nohup sh -c` bên trong dùng CÙNG shell
        val pb = ProcessBuilder(shell, "-c", "$cmd wait").redirectErrorStream(true)
        pb.environment().apply {
            put("PATH", "$bin:/usr/bin:/bin"); put("LOG", log.path); put("FIXTURE", stacks.path)
        }
        val p = pb.start()
        val out = p.inputStream.bufferedReader().readText()
        assertTrue(p.waitFor(20, TimeUnit.SECONDS), "shell treo: $out")
        return log.readLines().filter { it.isNotBlank() }
    }

    /** Đuôi TỰ ĐỘNG: [orphan] = đỉnh display 0 là cửa sổ mồ côi ⇒ Home (trần, không đọc lại). */
    private fun expectedLog(orphan: Boolean) = listOfNotNull(
        "am force-stop $pkg",
        "sleep 4",
        "settings put secure enabled_accessibility_services $oem:$comp",
        "settings put secure accessibility_enabled 1",
        "sleep 2",
        "am stack list",
        home.takeIf { orphan },
    )

    @Test
    fun `sh that - cua so mo coi tren dinh display 0 thi DUNG MOT lan Home, sau khi lap lai`(@TempDir dir: Path) {
        val cmd = AccessibilityRebind.forceStopRebindCommand(oem, pkg, comp, pauseSec = 4)
        for (shell in shells()) {
            assertEquals(expectedLog(orphan = true), runOnShell(shell, cmd, fixture("after-fix-orphan-top"), dir.resolve("o${shell.hashCode()}")), shell)
        }
    }

    @Test
    fun `sh that - home hoac app toan man dang tren dinh thi KHONG bam Home`(@TempDir dir: Path) {
        val cmd = AccessibilityRebind.forceStopRebindCommand(oem, pkg, comp, pauseSec = 4)
        for (shell in shells()) {
            for (name in listOf("stuck-home-top", "fullscreen-app-top")) {
                assertEquals(expectedLog(orphan = false), runOnShell(shell, cmd, fixture(name), dir.resolve("$name${shell.hashCode()}")), "$shell · $name")
            }
        }
    }

    // ── Người dùng TỰ BẤM "Sửa ngay" ⇒ LUÔN về màn nhà (owner chốt 29/09) ──────────────────────────────────
    // [ĐO máy ảo E2E 2.83 ca 5c, 13:58:27] "Sửa ngay" giết Kachi ⇒ `am_remove_task 1809` (task home) ⇒
    // `am_resume_activity … MapsActivity`: Maps TOÀN MÀN lên đỉnh display 0, dấu vân tay mồ côi không khớp ⇒ đuôi
    // đo-rồi-Home không làm gì ⇒ người bấm nút ở màn nhà kết thúc ở Maps. Owner: "nút tự chữa đó phải trả về home".

    // Senior review 2 [P2]: ca 5c gỡ task home ⇒ chính lần Home vô điều kiện DỰNG TƯƠI KachiHome, và [ĐO xe 3/3] mỗi
    // KachiHome dựng tươi khi kênh shell chưa lên đều tự mở lại app ô thành cửa sổ nổi trên display 0 ⇒ cửa sổ đó có
    // thể rơi TRÊN màn nhà vừa mở (dạng KEY-7). Nên sau Home vô điều kiện còn MỘT lượt đo-rồi-Home (KDoc HomeTail.ALWAYS).

    @Test
    fun `bam tay - lenh day du KHOP TUNG BYTE, Home roi MOT luot do-roi-Home`() {
        assertEquals(
            "nohup sh -c 'am force-stop com.byd.launcher ; sleep 4 ; " +
                "settings put secure enabled_accessibility_services \"$oem:$comp\" ; " +
                "settings put secure accessibility_enabled 1 ; sleep 2 ; " +
                "$home ; sleep 3 ; " +
                "t=\$(am stack list | grep -A1 \"displayId=0 \" | head -n 2) ; " +
                "case \"\$t\" in *\" mWindowingMode=freeform mDisplayWindowingMode=fullscreen mActivityType=standard \"*) " +
                "$home ;; esac' >/dev/null 2>&1 </dev/null &",
            AccessibilityRebind.forceStopRebindCommand(oem, pkg, comp, pauseSec = 4, homeTail = AccessibilityRebind.HomeTail.ALWAYS),
        )
    }

    @Test
    fun `ai yeu cau quyet dinh duoi - bam tay LUON, tu dong giu nguyen lenh co dieu kien cu`() {
        assertEquals(AccessibilityRebind.HomeTail.ALWAYS, AccessibilityRebind.homeTailFor(userAsked = true))
        assertEquals(AccessibilityRebind.HomeTail.IF_ORPHANED, AccessibilityRebind.homeTailFor(userAsked = false))
        assertEquals(
            AccessibilityRebind.forceStopRebindCommand(oem, pkg, comp, pauseSec = 4),
            AccessibilityRebind.forceStopRebindCommand(oem, pkg, comp, pauseSec = 4, homeTail = AccessibilityRebind.homeTailFor(false)),
            "lớp 1/2 (userAsked=false) phải ra ĐÚNG byte của đuôi mặc định đo-rồi-Home — không đá người lái khỏi app họ vừa mở",
        )
        assertEquals(AccessibilityRebind.HomeTail.IF_ORPHANED, AccessibilityRebind.HomeTail.entries.first(),
            "mặc định của tham số phải là bản bảo thủ (xem chữ ký hàm)")
        val manual = AccessibilityRebind.forceStopRebindCommand(oem, pkg, comp, homeTail = AccessibilityRebind.HomeTail.ALWAYS)
        assertTrue(manual.contains("sleep 2 ; " + AccessibilityRebind.GO_HOME + " ; sleep 3 ; "),
            "bấm tay: lần về nhà đầu là ĐÚNG cửa Home của đuôi (một chỗ dựng — DRY), ngay sau nhịp chờ")
        assertEquals(2, Regex(Regex.escape(home)).findAll(manual).count(),
            "đúng HAI chỗ Home: một Home trần, một trong nhánh đo-rồi-Home")
        assertTrue(manual.endsWith(AccessibilityRebind.RETURN_HOME_IF_ORPHANED + "' >/dev/null 2>&1 </dev/null &"),
            "lượt hai là ĐÚNG đuôi đo-rồi-Home của lớp 1/2 (một chỗ dựng — DRY), không phải Home mù thứ hai")
        assertTrue(manual.indexOf("settings put secure accessibility_enabled 1") < manual.indexOf(home),
            "Home đứng SAU khi lắp lại dịch vụ (CLAUDE.md §6)")
    }

    @Test
    fun `bam tay - chot cung goi van con, lech goi thi KHONG co lenh nao ke ca Home`() {
        for (bad in listOf("com.google.android.youtube", "", "  ")) {
            assertEquals("", AccessibilityRebind.forceStopRebindCommand(oem, bad, comp, homeTail = AccessibilityRebind.HomeTail.ALWAYS),
                "gói='$bad' ⇒ không dựng lệnh")
        }
        assertEquals("", AccessibilityRebind.forceStopRebindCommand("oem/svc';reboot;'", pkg, comp, homeTail = AccessibilityRebind.HomeTail.ALWAYS),
            "mục mang ký tự shell ⇒ từ chối cả lệnh (Home không được lọt ra một mình)")
    }

    /**
     * `am stack list` giả trả fixture ở lượt đo SAU Home vô điều kiện — tức trạng thái display 0 ~3 s sau lần Home đầu:
     *  • `after-fix-orphan-top` — cửa sổ nổi của app ô rơi trên màn nhà vừa dựng (dạng KEY-7) ⇒ Home lần hai;
     *  • `stuck-home-top` / `fullscreen-app-top` / rỗng — không mồ côi, hoặc đọc hỏng ⇒ chỉ Home vô điều kiện.
     */
    @Test
    fun `sh that - bam tay thi luon ve man nha, mo coi thi Home lan hai`(@TempDir dir: Path) {
        val cmd = AccessibilityRebind.forceStopRebindCommand(oem, pkg, comp, pauseSec = 4, homeTail = AccessibilityRebind.HomeTail.ALWAYS)
        fun expected(orphanAfterHome: Boolean) = listOfNotNull(
            "am force-stop $pkg",
            "sleep 4",
            "settings put secure enabled_accessibility_services $oem:$comp",
            "settings put secure accessibility_enabled 1",
            "sleep 2",
            home,
            "sleep 3",
            "am stack list",
            home.takeIf { orphanAfterHome },
        )
        for (shell in shells()) {
            assertEquals(expected(orphanAfterHome = true),
                runOnShell(shell, cmd, fixture("after-fix-orphan-top"), dir.resolve("mo${shell.hashCode()}")),
                "$shell: cửa sổ nổi trên màn nhà vừa mở ⇒ Home lần hai (đúng việc owner làm tay)")
            for (name in listOf("stuck-home-top", "fullscreen-app-top")) {
                assertEquals(expected(orphanAfterHome = false), runOnShell(shell, cmd, fixture(name), dir.resolve("m$name${shell.hashCode()}")),
                    "$shell · $name: home / app toàn màn trên đỉnh ⇒ chỉ Home vô điều kiện, không Home mù lần hai")
            }
            assertEquals(expected(orphanAfterHome = false), runOnShell(shell, cmd, "", dir.resolve("me${shell.hashCode()}")),
                "$shell: `am stack list` rỗng/không hỗ trợ ⇒ chỉ Home vô điều kiện (Android box: không rào camera)")
        }
    }

    @Test
    fun `sh that - am stack list rong hoac khong ho tro thi lui ve hanh vi 2_82`(@TempDir dir: Path) {
        val cmd = AccessibilityRebind.forceStopRebindCommand(oem, pkg, comp, pauseSec = 4)
        for (shell in shells()) {
            assertEquals(
                expectedLog(orphan = false), runOnShell(shell, cmd, "", dir.resolve("e${shell.hashCode()}")),
                "$shell: đọc hỏng ⇒ không mở gì; dịch vụ Hỗ trợ vẫn đã được lắp lại TRƯỚC đó",
            )
        }
    }
}
