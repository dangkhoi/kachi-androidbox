package com.kachi.box.launcher

import com.kachi.box.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

/**
 * ═══ V-CLUSTER (owner 2026-09-30: *"Phần cụm lưu hết thành profile nhé"*) — DÂY NỐI phía `:app` của lớp lưu trữ ═════
 *
 * Spec `docs/specs/kachi-profiles-are-everything.html` §11.3 VC-R3 · VC-R7 · VC-R8. Phép tính thuần đã có bài chạy thật ở
 * `:core` (`ProfileScopeMigrationTest`, `ProfileTransferTest`). Bài này hỏi câu mà `:core` không hỏi được: *mã Android có
 * THẬT SỰ đi qua các phép đó chưa*. Android box B2 · W2c: phần chiếu cụm (`SimpleCastRuntime`, `CastAppCatalog`, họ
 * `cast_geometry`, khoá hoãn `cast_enabled`, lượt rót V-CLUSTER, merge khung lúc nhập) gỡ — các bài của nó gỡ theo.
 *
 * Mọi phép cắt vùng dùng [SourceRoots.body] (đếm ngoặc, nổ nếu mốc vắng) — không `substringAfter/Before`.
 */
class ClusterProfileScopeCoverageTest {

    private fun code(file: String) = SourceRoots.codeOf("src/main/java/com/kachi/box/$file")

    private val snapshot by lazy { code("launcher/WorkspacePrefsSnapshot.kt") }
    private val migrations by lazy { code("launcher/WorkspacePrefsMigrations.kt") }
    private val profileIo by lazy { code("launcher/WorkspacePrefsProfile.kt") }
    private val repo by lazy { code("launcher/PrefsWorkspaceRepository.kt") }

    /** Android box B2 · W2c — mã chiếu cụm (hai tệp prefs của nó) gỡ hẳn khỏi `:app`. */
    @Test
    fun `ma chieu cum da go, tep cua no duoc don`() {
        listOf("modules/clustercast/simplified/SimpleCastRuntime.kt", "cast/platform/CastAppCatalog.kt").forEach {
            assertFalse(SourceRoots.exists("src/main/java/com/kachi/box/$it"), "$it còn")
        }
        // Android box B2 · W4 — tên khoá chiếu cụm rời mọi bảng phạm vi (dọn khỏi máy một lần — `BydDeadPrefs`).
        assertEquals(ProfileScope.Scope.UNKNOWN, ProfileScope.scopeOf("scale-dpi:a.b"))
        assertEquals(ProfileScope.Scope.UNKNOWN, ProfileScope.scopeOf("config_density_a.b"))
        assertTrue("simple_cast_prefs" in BydDeadPrefs.DEAD_FILES && "cast-v2-app-catalog" in BydDeadPrefs.DEAD_FILES)
    }

    /**
     * Android box B2 · W2b (2026-10-09): mã camera BYD (đọc/ghi `camera_*`) gỡ khỏi `:app`; W4 gỡ tên khoá khỏi mọi bảng phạm
     * vi (lượt dọn một lần xoá họ `camera_` khỏi máy). KHÔNG một dòng mã `:app` nào còn đọc/ghi chúng.
     */
    @Test
    fun `khong con ma app nao doc ghi khoa camera`() {
        val root = SourceRoots.moduleSourceRoots().first { it.endsWith(Paths.get("app", "src", "main", "java")) }
        val hits = Files.walk(root).use { s -> s.filter { it.toString().endsWith(".kt") }.toList() }.filter { p ->
            Regex(""""camera_[a-z_]+"""").containsMatchIn(SourceRoots.codeOf("src/main/java/" + root.relativize(p).joinToString("/")))
        }.map { it.fileName.toString() }
        assertEquals(emptyList<String>(), hits, "mã :app còn literal khoá camera")
        assertTrue(ProfileScope.CLUSTERNAV_PROFILE_KEYS.none { it.startsWith("camera_") }, "khoá camera rời ảnh chụp")
        assertTrue(BydDeadPrefs.isDeadClusterNavKey("camera_zoom"), "lượt dọn xoá họ camera_")
    }

    /** Khoá theo hồ sơ KHÔNG có mục Cài đặt phải có mặt NGUYÊN VĂN ở tệp khai nó (chặt ngang `ClusterNavKeysContractTest`). */
    @Test
    fun `khoa them theo ho so co nguyen van o tep khai`() {
        assertTrue(code("PrefsAutomation.kt").contains("getSharedPreferences(\"clusternav_prefs\""))
    }

    // ── 2.92 · PROFILE-NEW-KEYS ────────────────────────────────────────────────────────────────

    @Test
    fun `luot ro V-CLUSTER (cum, chieu, nut noi) da go khoi init`() {
        assertFalse(SourceRoots.body(repo, "    init {").contains("migrateClusterProfileOnce"))
        assertFalse(migrations.contains("migrateClusterProfileOnce"))
    }

    /**
     * 2.92 · PROFILE-NEW-KEYS — khoá vào phạm vi hồ sơ ở bản SAU (QA máy ảo 2.92 [ĐO]: kiểu hình/thu phóng camera của hồ
     * sơ vừa rời rò sang hồ sơ lưu bằng 2.91). Lượt rót theo sổ phải chạy ở `init`, SAU lượt lịch dẫn đường, TRƯỚC `load()`.
     */
    @Test
    fun `khoa moi vao pham vi ho so duoc rot theo so, o init, mot Editor`() {
        val init = SourceRoots.body(repo, "    init {")
        val nav = init.indexOf("prefs.migrateNavScheduleOnce()")
        val fill = init.indexOf("prefs.fillNewProfileKeysOnce()")
        assertTrue(nav in 0 until fill, "thứ tự init: lịch → khoá mới ($nav/$fill)")
        assertTrue(repo.indexOf("init {") < repo.indexOf("override fun load()"))
        val fn = SourceRoots.body(migrations, "internal fun WorkspacePrefs.fillNewProfileKeysOnce()")
        assertEquals(1, Regex("""\bsp\.edit\(\)""").findAll(fn).count(), "một Editor duy nhất")
        assertTrue(
            fn.contains("val scope = ProfileScope.CLUSTERNAV_KEYS") && fn.contains("ProfileScopeMigration.pendingKeys(scope, done)"),
            "sổ duyệt ĐÚNG bảng ảnh chụp",
        )
        // "Đã chụp" xét trên ảnh của MỌI tệp (không riêng tệp đang rót) — khoá ở tệp MỚI phải được rót. Thử ĐỎ: bỏ `captured`.
        assertTrue(fn.contains("scope.keys.associateWith") && fn.contains("ProfileScopeMigration.captured(shots)"))
        assertTrue(
            fn.contains(".fillNewKeys(shots.getValue(file), clusterNavPrefs(file).all, newKeys, captured)"),
            "fillNewKeys phải nhận tập đã-chụp — phép rót ở :core (có test chạy thật)",
        )
        assertTrue(fn.contains("storedSnapshot(stored,"), "ảnh sai kiểu không được làm init ném")
        assertFalse(fn.contains("getStringSet("), "đọc sổ bằng ép kiểu an toàn, không getStringSet (ném ClassCastException ở init)")
        assertTrue(
            fn.contains("e.putStringSet(K_PROFILE_KEYS_FILLED, ProfileScopeMigration.ledgerOf(scope)).apply()"),
            "sổ ghi trên CHÍNH Editor đã ghi dữ liệu",
        )
        assertTrue(
            migrations.contains("K_PROFILE_KEYS_FILLED = \"${ProfileScopeMigration.FILLED_LEDGER_KEY}\""),
            "literal phải BẰNG hằng :core",
        )
    }

    /**
     * 2.92 · PROFILE-IMPORT-GAP-KEYS — NHẬP tệp xuất từ bản cũ (thiếu khoá theo hồ sơ mới): mọi ảnh ClusterNav của hồ sơ nhập đi qua
     * CÙNG phép điền chỗ trống của lượt nâng cấp, SAU lớp làm sạch. Android box B2 · W2c: lượt merge khung chiếu cụm gỡ.
     */
    @Test
    fun `nhap tep cu - anh ClusterNav dien cho trong bang gia tri song, sau lam sach`() {
        val importFn = SourceRoots.body(profileIo, "internal fun WorkspacePrefs.importProfile(data: String, name: String? = null)")
        val captured = importFn.indexOf("val captured = importedCaptured(plan.writes)")
        val clean = importFn.indexOf("cleanImportedSnapshot(suffix, v)")
        val fill = importFn.indexOf("val value = fillImportedSnapshot(suffix, clean, captured)")
        assertTrue(captured in 0 until clean, "2.93 PROFILE-NEW-FILE-FILL: 'đã chụp' tính TRƯỚC vòng ghi, trên MỌI ảnh của tệp")
        assertTrue(clean in 0 until fill, "thứ tự: làm sạch → điền ($clean/$fill)")
        assertFalse(importFn.contains("mergeImportedCast"), "lượt merge khung chiếu cụm đã gỡ")
        val fn = SourceRoots.body(snapshot, "internal fun WorkspacePrefs.fillImportedSnapshot(suffix: String, value: Any?, captured: Boolean)")
        assertTrue(fn.contains("ProfileScopeMigration.fillNewKeys(") && fn.contains("ProfileScope.CLUSTERNAV_KEYS"), "cùng phép :core")
        // Ảnh VẮNG chỉ được dựng khi hồ sơ nhập đã chụp ở tệp khác (S4 · R5 giữ cho tệp không có ảnh ClusterNav nào).
        assertTrue(fn.contains("if (value == null && captured) \"\" else return value"), fn)
        val cap = SourceRoots.body(snapshot, "internal fun importedCaptured(writes: Map<String, Any?>): Boolean")
        assertTrue(cap.contains("PrefSnapshotPlan.sanitize(") && cap.contains("ProfileScopeTypes.CLUSTERNAV"),
            "'đã chụp' đo trên ảnh ĐÃ làm sạch bằng cùng phép của cleanImportedSnapshot")
    }

    // ── VC-R2/R7/R8 · chụp–áp–nhập đi qua phép thuần ──────────────────────────────────────────

    @Test
    fun `chup va ap di qua PrefSnapshotPlan voi kieu khai san`() {
        val snap = SourceRoots.body(snapshot, "internal fun WorkspacePrefs.snapshotClusterNav(profile: String)")
        assertTrue(snap.contains("keys.associateWith { all[it] }"), "khoá VẮNG cũng phải vào ảnh (null tường minh)")
        val apply = SourceRoots.body(snapshot, "internal fun WorkspacePrefs.applyClusterNav(profile: String)")
        listOf("PrefSnapshotPlan.inScope(", "PrefSnapshotPlan.apply(", "ProfileScopeTypes.CLUSTERNAV", "target.all")
            .forEach { assertTrue(apply.contains(it), "lượt áp thiếu `$it`") }
    }

    @Test
    fun `nhap ho so lam sach anh chup truoc khi ghi`() {
        val fn = SourceRoots.body(profileIo, "internal fun WorkspacePrefs.importProfile(data: String, name: String? = null): ProfileImportReport?")
        assertTrue(fn.contains("cleanImportedSnapshot(suffix, v)"), "mọi hậu tố nhập phải qua lớp làm sạch")
        val clean = SourceRoots.body(snapshot, "internal fun cleanImportedSnapshot(suffix: String, value: Any?): Any?")
        assertTrue(clean.contains("PrefSnapshotPlan.sanitize(") && clean.contains("ProfileScopeTypes.CLUSTERNAV"))
    }

    /**
     * Senior review Pass 3 — ảnh chụp ĐÃ LƯU ở `kachi_workspace` có thể sai kiểu từ TRƯỚC bản này: [ĐO code b5c0e87]
     * lượt nhập tới 2.83 chép thẳng giá trị đã giải mã (`copyValue`, chưa làm sạch) ⇒ `<hồ sơ>__cn__<tệp>` có thể là
     * Boolean/Int. `sp.getString` trên nó NÉM: ở lượt di trú (trong `init`, trước màn nhà) là launcher sập MỌI lần mở và
     * dấu chạy-một-lần không bao giờ được ghi; ở lượt áp là sập đúng lúc chạm chip hồ sơ. Hai chỗ đọc phải đi qua
     * `storedSnapshot` (vắng HOẶC sai kiểu ⇒ `null` + log), và lượt di trú rót ảnh sạch đè lên ⇒ tự chữa.
     */
    @Test
    fun `doc anh chup da luu khong nem khi sai kieu`() {
        listOf(
            "fillNewProfileKeysOnce" to SourceRoots.body(migrations, "internal fun WorkspacePrefs.fillNewProfileKeysOnce()"),
            "applyClusterNav" to SourceRoots.body(snapshot, "internal fun WorkspacePrefs.applyClusterNav(profile: String)"),
            // Senior review bản gộp 2.84: lượt di trú THỨ HAI trong `init` cùng đọc ảnh — máy nâng thẳng từ ≤ 2.81 (lượt
            // này chưa từng chạy) với ảnh sai kiểu từ tệp nhập là HOME sập mỗi lần mở, y hệt ca Pass 3 đã vá.
            "migrateNavScheduleOnce" to SourceRoots.body(migrations, "internal fun WorkspacePrefs.migrateNavScheduleOnce()"),
        ).forEach { (name, body) ->
            assertFalse(body.contains("sp.getString("), "$name: getString NÉM ClassCastException khi ảnh đã lưu sai kiểu")
            assertTrue(body.contains("storedSnapshot(stored, keyOf("), "$name phải đọc ảnh qua storedSnapshot")
            assertTrue(body.contains("val stored = sp.all"), "$name: một lượt chụp `sp.all`, không đọc có kiểu")
        }
        val helper = SourceRoots.body(snapshot, "internal fun storedSnapshot(stored: Map<String, *>, key: String): String?")
        assertTrue(helper.contains("if (v is String) return v"), "chỉ chuỗi mới là ảnh chụp")
        assertTrue(helper.contains("Log.w("), "ảnh sai kiểu phải để lại vết, không im lặng")
        assertFalse(Regex("""\bget(String|Boolean|Int|Long|Float|StringSet)\(""").containsMatchIn(helper))
    }

    // ── VC-R8 · kiểu KHAI SẴN phải là kiểu mã THẬT ghi (senior review Pass 2) ────────────────────

    /**
     * Khoá mà mã ghi qua một hàm trung gian (tên khoá là BIẾN ở lời `put*`) — bộ quét không nối được tên, nên mỗi khoá
     * mang kiểu + tệp + đoạn mã ghi thật làm bằng chứng. Bài dưới đòi bảng này khớp ĐÚNG tập khoá quét hụt (không mục).
     */
    private val indirect: Map<String, Pair<PrefType, List<Pair<String, String>>>> = mapOf(
        "voicekey_bindings" to (
            PrefType.STRING to listOf(
                "Prefs.kt" to "VoiceKeyBindingStore.write(p, K_VK_BINDINGS",
                "modules/voicekey/VoiceKeyBindingStore.kt" to "sp.edit().putString(key, encode(bindings))",
            )
        ),
    )

    /**
     * Khai SAI kiểu thì lượt đổi hồ sơ bỏ đúng giá trị HỢP LỆ của người lái ở mọi lượt — mất cấu hình im lặng, đắt
     * ngang lỗi bảng kiểu sinh ra để chữa (`ClassCastException` trong dịch vụ). Quét mọi lời `put*(<khoá>` của `:app`
     * (khoá literal, hằng `const val` cùng tệp, hoặc mẫu `"seat_level_$…"`) và đòi tập kiểu mã ghi == kiểu khai.
     */
    @Test
    fun `kieu khai san khop luot ghi that trong ma`() {
        val declared = ProfileScopeTypes.CLUSTERNAV
        val root = SourceRoots.moduleSourceRoots().first { it.endsWith(Paths.get("app", "src", "main", "java")) }
        val typeOf = mapOf(
            "Boolean" to PrefType.BOOLEAN, "Int" to PrefType.INT, "Long" to PrefType.LONG, "Float" to PrefType.FLOAT,
            "String" to PrefType.STRING, "StringSet" to PrefType.STRING_SET,
        )
        val put = Regex("""\bput(Boolean|Int|Long|Float|StringSet|String)\(\s*(\w+|"[^"]*")""")
        val constant = Regex("""const val (\w+)\s*=\s*"([^"$]+)"""")
        val seen = HashMap<String, MutableSet<PrefType>>()
        val files = Files.walk(root).use { s -> s.filter { it.toString().endsWith(".kt") }.toList() }
        assertTrue(files.size > 100, "bộ quét phải thấy cả cây :app (đang thấy ${files.size})")
        files.forEach { p ->
            val src = SourceRoots.codeOf("src/main/java/" + root.relativize(p).joinToString("/"))
            val consts = constant.findAll(src).associate { it.groupValues[1] to it.groupValues[2] }
            put.findAll(src).forEach { m ->
                val arg = m.groupValues[2]
                val keys = when {
                    arg.startsWith("\"") && '$' in arg ->
                        arg.trim('"').split('$').first().let { pre -> declared.keys.filter { pre.isNotEmpty() && it.startsWith(pre) } }
                    arg.startsWith("\"") -> listOf(arg.trim('"'))
                    else -> listOfNotNull(consts[arg])
                }
                keys.filter { it in declared }.forEach { seen.getOrPut(it) { mutableSetOf() } += typeOf.getValue(m.groupValues[1]) }
            }
        }
        assertEquals(
            emptyMap<String, Set<PrefType>>(), seen.filter { (k, types) -> types != setOf(declared.getValue(k)) },
            "kiểu khai ≠ kiểu mã ghi ⇒ đổi hồ sơ bỏ giá trị hợp lệ",
        )
        // Android box B2 · W4 — bảng kiểu không còn khoá đã gỡ mã ghi (camera · biển báo · bong bóng · tiện nghi): mọi khoá khai
        // kiểu phải có lời ghi thật hoặc nằm ở `indirect`.
        assertEquals(indirect.keys, declared.keys - seen.keys, "khoá quét hụt phải nằm ở `indirect` (và chỉ chúng)")
        indirect.forEach { (key, ev) ->
            val (type, evidence) = ev
            assertEquals(type, declared.getValue(key), key)
            evidence.forEach { (file, snippet) ->
                assertTrue(code(file).contains(snippet), "$key: không còn thấy lượt ghi `$snippet` ở $file")
            }
        }
    }
}
