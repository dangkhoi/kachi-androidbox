package com.kachi.box.launcher

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * ═══ DI TRÚ MỘT LẦN khi khoá ClusterNav đổi phạm vi XE → HỒ SƠ — phần của [WorkspacePrefs] ════════════════════════
 *
 * Tách khỏi `WorkspacePrefsProfile.kt` ở V-CLUSTER (2026-09-30, spec `kachi-profiles-are-everything.html` §11.4.9) vì
 * trần 500 dòng (CLAUDE.md §4.1). Cùng khuôn hàm mở rộng của CHÍNH [WorkspacePrefs] (dùng lại `sp`, không mở cửa thứ
 * hai vào `kachi_workspace`).
 *
 * Hai lượt, cùng một hợp đồng *"rót xuống, chỉ điền chỗ trống, dấu ghi CÙNG một Editor với dữ liệu"*:
 *  • [migrateNavScheduleOnce] — lịch tự dẫn đường (2026-09-28), dời NGUYÊN VĂN (bài canh
 *    `NavScheduleProfileScopeWiringTest` khoá thân hàm; CLAUDE.md §6: không viết lại đường đang chạy tốt) — trừ đúng MỘT
 *    lượt đọc ảnh đổi sang [storedSnapshot] (senior review 2.84: ảnh sai kiểu không được làm `init` ném);
 *  • [fillNewProfileKeysOnce] — 2.92 PROFILE-NEW-KEYS: khoá vào phạm vi hồ sơ ở bản SAU, theo sổ đã-rót.
 *
 * Lối mở tệp prefs ClusterNav ([clusterNavPrefs]) cũng nằm ở đây: tệp có lời gọi mở prefs là tệp mà
 * `SettingsCoverageContractTest` quét hằng khoá, nên hai dấu boolean bên dưới vẫn bị đòi lý do ở `NOT_SETTINGS`; sổ đã-rót
 * (StringSet — bộ quét chưa đọc `putStringSet`) có lý do ở đó và ở `ProfileScope.DEVICE_KEYS` bằng tay. Android box B2 · W2c:
 * lượt rót một lần V-CLUSTER (`migrateClusterProfileOnce`, cụm/chiếu/nút nổi) gỡ cùng chiếu cụm.
 */

/**
 * DI TRÚ MỘT LẦN: *tự dẫn đường theo lịch* từ phạm vi **theo XE** sang **theo HỒ SƠ** (owner 2026-09-28).
 *
 * ## Vì sao cần, và vì sao làm sai là mất dữ liệu của người dùng
 * Trước bản này, luật lịch và sổ đã-dẫn nằm chung cả máy. Đổi phạm vi mà không làm gì thì [applyClusterNav] gặp
 * ảnh chụp CŨ (chụp hồi hai khoá còn theo xe nên KHÔNG chứa chúng), `filterKeys` loại chúng ra, và giá trị đang
 * sống được giữ nguyên. Nghe thì có vẻ lành, nhưng hệ quả là **hồ sơ nào được chụp trước thì chiếm luật**, các hồ
 * sơ còn lại ăn theo cái đang sống rồi lệch dần — kiểu hỏng không ai thấy cho tới lúc một buổi sáng lịch không nổ.
 *
 * ## Cách làm: rót xuống, không bốc lên
 * Chép giá trị ĐANG SỐNG vào ảnh chụp của **mọi** hồ sơ hiện có. Sau lượt này ai cũng bắt đầu bằng đúng cái lịch
 * người dùng đang có, rồi mới tách ra khi họ sửa. Không ai mất gì, và không hồ sơ nào bỗng dưng trống.
 *
 * ⚠ Chỉ ĐIỀN VÀO CHỖ TRỐNG (`putIfAbsent`): một hồ sơ đã có sẵn khoá trong ảnh chụp — vì người dùng đã đổi hồ sơ
 * sau khi nâng cấp — thì giữ nguyên của nó. Ghi đè ở đây là xoá lựa chọn vừa mới đặt.
 *
 * ⚠ Dấu đã-di-trú ghi **CÙNG một lượt** với dữ liệu, đúng bài học của [migrateScenesOnce]: tách hai lượt thì một
 * lần chết máy giữa chừng cho lượt sau chạy lại trên dữ liệu đã chuyển.
 *
 * ⚠ Khoá đang VẮNG ở tệp sống vẫn được rót — dưới dạng `null` tường minh (thẻ `n` của [PrefSnapshot]), cùng hợp
 * đồng với [snapshotClusterNav]. Bỏ nó đi là để sổ ĐÃ-DẪN của hồ sơ vừa rời **tràn sang** hồ sơ mới; xem chú
 * thích tại chỗ.
 *
 * Chạy xong là **đặt dấu** rồi thôi, kể cả khi không có gì để rót — khỏi quét lại mỗi lần mở.
 */
internal fun WorkspacePrefs.migrateNavScheduleOnce() {
    if (sp.getBoolean(K_MIGRATED_NAV_SCHEDULE, false)) return
    val e = sp.edit()
    // Senior review 2.84: đọc ảnh qua [storedSnapshot], KHÔNG `sp.getString` — cùng lỗ [migrateClusterProfileOnce] đã vá
    // (Pass 3): máy nâng thẳng từ ≤ 2.81 (lượt này chưa từng chạy) mà có ảnh sai kiểu từ tệp nhập thì `getString` NÉM
    // ngay trong `init` của `PrefsWorkspaceRepository` ⇒ HOME sập mỗi lần mở, không bao giờ tới được dấu chạy-một-lần.
    val stored = sp.all
    ProfileScope.CLUSTERNAV_PROFILE_STATE_KEYS.entries
        .groupBy({ it.value }, { it.key })
        .forEach { (file, stateKeys) ->
            // Hai khoá của cùng một tính năng phải đi cùng nhau: luật (đã có trong danh mục) + sổ đã-dẫn (không).
            val keys = (ProfileScope.CLUSTERNAV_KEYS[file].orEmpty().toSet() + stateKeys)
                .filter { it.startsWith("nav_automation") }
            if (keys.isEmpty()) return@forEach
            val live = clusterNavPrefs(file).all
            // ⚠⚠ Khoá VẮNG ở tệp sống cũng phải vào ảnh, dưới dạng `null` TƯỜNG MINH — đúng hợp đồng đã ghi ở
            // KDoc [snapshotClusterNav] và thẻ `n` của [PrefSnapshot]. Lọc `null` ra (bản đầu của hàm này) là bỏ
            // khoá ấy khỏi ảnh của mọi hồ sơ, và lượt [applyClusterNav] khi đó **giữ nguyên giá trị của hồ sơ vừa
            // rời**. Ca thật: lúc di trú, `nav_automation_fired` thường CHƯA có (chưa lịch nào bắn) ⇒ vắng ở mọi
            // ảnh ⇒ hồ sơ A bắn xong, đổi sang B thì sổ đã-dẫn của A vẫn còn sống, mà luật của B là BẢN SAO cùng
            // id (chính lượt di trú này rót xuống) ⇒ B bị coi là **đã bắn** và bỏ đúng một lượt, im lặng. Đó là
            // R5 của spec `kachi-profile-scope-nav-schedule`.
            val carry: Map<String, Any?> = keys.associateWith { live[it] }
            val suffix = ProfileScope.snapshotSuffix(file)
            profiles().forEach { p ->
                val shot = PrefSnapshot.decode(storedSnapshot(stored, keyOf(p, suffix))).toMutableMap()
                var touched = false
                carry.forEach { (k, v) -> if (k !in shot) { shot[k] = v; touched = true } }
                if (touched) e.putString(keyOf(p, suffix), PrefSnapshot.encode(shot))
            }
        }
    e.putBoolean(K_MIGRATED_NAV_SCHEDULE, true).apply()
}

/**
 * Dấu đã chuyển *tự dẫn đường theo lịch* từ theo-XE sang theo-HỒ-SƠ — xem [migrateNavScheduleOnce].
 *
 * Đặt ở ĐÂY chứ không trong `WorkspacePrefs` vì đó là nơi DUY NHẤT đọc nó, và vì tệp kia đã sát trần 500 dòng
 * (CLAUDE.md §4.1) — thêm vào đấy là đẩy nó qua trần, bài canh kích thước đỏ ngay. Hằng ở cạnh chỗ dùng cũng
 * đúng hơn: dấu di trú thuộc về phép di trú.
 */
private const val K_MIGRATED_NAV_SCHEDULE = "migrated_nav_schedule_v1"


/**
 * 2.92 · PROFILE-NEW-KEYS — rót mọi khoá ClusterNav theo hồ sơ CHƯA có trong sổ đã-rót xuống ảnh đã có của mọi hồ sơ.
 *
 * Lượt di trú một lần chỉ phủ một bảng khoá cố định, nên khoá vào phạm vi hồ sơ ở bản sau (2.90
 * `vm_bubble_hidden`, 2.92 `camera_projection`/`camera_zoom`) chưa từng được rót: [ĐO máy ảo 2.92] chọn *Thẳng rộng*
 * 120 % ở hồ sơ A rồi đổi sang hồ sơ lưu bằng 2.91 ⇒ hồ sơ đó cũng *Thẳng rộng* 120 %, và lượt rời nó chụp luôn giá trị
 * lạc. Sổ `tệp/khoá` thay cho cờ boolean ⇒ bản sau thêm khoá là tự rót, không phải nhớ viết thêm một lượt di trú.
 *
 * Phép tính ở [ProfileScopeMigration.fillNewKeys] (thuần, `ProfileScopeMigrationTest`): chỉ điền chỗ trống, chỉ vào hồ sơ
 * ĐÃ CHỤP, giá trị = đang sống (vắng ⇒ `null` tường minh ⇒ lượt áp trả về mặc định). Lần đầu (sổ rỗng) duyệt mọi khoá —
 * ảnh đủ khoá không đổi byte.
 *
 * 2.93 · PROFILE-NEW-FILE-FILL — *"đã chụp"* xét trên MỌI tệp ảnh chụp ([ProfileScopeMigration.captured]) ⇒ khoá ở một
 * tệp prefs MỚI cũng được rót, không còn ca *"sổ ghi xong mà không hồ sơ nào được rót"* (senior review 2.92 F1).
 *
 * ⚠ Sổ ghi CÙNG một `Editor` với mọi ảnh (bài học [migrateScenesOnce]); chạy SAU [migrateNavScheduleOnce] ở `init`
 * của `PrefsWorkspaceRepository`, TRƯỚC lượt `load()` đầu tiên — tức trước khi người lái kịp chọn giá trị cho khoá mới.
 */
internal fun WorkspacePrefs.fillNewProfileKeysOnce() {
    // Đọc qua `sp.all` + ép kiểu an toàn, KHÔNG `getStringSet`: giá trị sai kiểu mà ném ở `init` là HOME sập mỗi lần mở.
    val stored = sp.all
    val done = (stored[K_PROFILE_KEYS_FILLED] as? Set<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
    val scope = ProfileScope.CLUSTERNAV_KEYS
    val pending = ProfileScopeMigration.pendingKeys(scope, done)
    if (pending.isEmpty()) return
    val e = sp.edit()
    val names = profiles()
    // Ảnh của MỌI tệp (không riêng tệp đang rót) — để biết hồ sơ nào đã chụp ở bất kỳ đâu.
    val shots = scope.keys.associateWith { file ->
        names.associateWith { PrefSnapshot.decode(storedSnapshot(stored, keyOf(it, ProfileScope.snapshotSuffix(file)))) }
    }
    val captured = ProfileScopeMigration.captured(shots)
    var filled = 0
    pending.forEach { (file, newKeys) ->
        val suffix = ProfileScope.snapshotSuffix(file)
        ProfileScopeMigration
            .fillNewKeys(shots.getValue(file), clusterNavPrefs(file).all, newKeys, captured)
            .forEach { (p, shot) -> e.putString(keyOf(p, suffix), PrefSnapshot.encode(shot)); filled++ }
    }
    e.putStringSet(K_PROFILE_KEYS_FILLED, ProfileScopeMigration.ledgerOf(scope)).apply()
    // Một dòng cho log phiên / `ClusterDiag` (CLAUDE.md §11): xe ngoài đường không có adb — đọc log là biết lượt rót đã chạy.
    Log.i("KachiProfile", "rót khoá mới theo hồ sơ: ${pending.values.sumOf { it.size }} mục sổ · ${captured.size} hồ sơ đã chụp · $filled ảnh")
}

/**
 * Sổ đã-rót của [fillNewProfileKeysOnce]. Literal (không trỏ hằng `:core`), cùng khuôn dấu lịch dẫn đường; `ClusterProfileScopeCoverageTest`
 * khoá nó BẰNG [ProfileScopeMigration.FILLED_LEDGER_KEY]. Xếp loại theo XE: `ProfileScope.DEVICE_KEYS` + `SettingsCatalog.NOT_SETTINGS`.
 */
private const val K_PROFILE_KEYS_FILLED = "profile_keys_filled_v1"

/**
 * Tệp prefs của phía ClusterNav theo tên. Mở qua `Context` (mỗi tên là một `SharedPreferences` riêng) — Android
 * cache theo tên trong cùng tiến trình, nên đây **vẫn là** đúng đối tượng mà dịch vụ đang giữ, và mọi listener đã
 * đăng ký (`registerOnSharedPreferenceChangeListener`) đều nhận được lượt ghi này.
 */
internal fun WorkspacePrefs.clusterNavPrefs(file: String): SharedPreferences =
    appCtx.getSharedPreferences(file, Context.MODE_PRIVATE)

