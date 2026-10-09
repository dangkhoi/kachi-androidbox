package com.kachi.box.launcher

import android.util.Log

/**
 * ═══ S4 · R5 + V-CLUSTER — CHỤP / ÁP ảnh cấu hình ClusterNav theo hồ sơ — phần của [WorkspacePrefs] ════════════════
 *
 * Tách khỏi `WorkspacePrefsProfile.kt` ở V-CLUSTER (2026-09-30 — trần 500 dòng). Tầng này chỉ **đọc tệp sống → gọi
 * phép thuần → đổ kết quả vào `Editor`**: logic (kiểm kiểu, phạm vi) là [PrefSnapshotPlan] ở `:core`, có test chạy thật.
 *
 * Android box B2 · W2c: họ tiền tố `cast_geometry`, khoá hoãn `cast_enabled` và lượt merge khung chiếu lúc nhập GỠ cùng
 * chiếu cụm; tệp `simple_cast_prefs` không còn trong ảnh chụp (W4 dọn khỏi máy — [BydDeadPrefs]). Còn **kiểm kiểu** trước khi ghi:
 * ảnh trên đĩa sửa tay được và tệp nhập là dữ liệu người khác gửi.
 */

private const val TAG = "KachiProfile"

/**
 * Chụp giá trị ĐANG có của mọi khoá ClusterNav theo hồ sơ vào ảnh chụp của [profile].
 *
 * Một chuỗi cho **một tệp** prefs (khoá `<hồ sơ>__cn__<tên tệp>`, hậu tố sinh bởi [ProfileScope.snapshotSuffix] nên
 * nó nằm sẵn trong [ProfileScope.LAUNCHER_SUFFIXES] ⇒ xoá hồ sơ là xoá cả ảnh chụp, không phải nhớ thêm gì).
 *
 * ## ⚠⚠ Khoá VẮNG MẶT cũng phải được chụp — dưới dạng `null` tường minh
 * `sp.all` chỉ trả khoá **có mặt**. Nếu ảnh chụp bỏ qua khoá vắng thì lượt áp sau này cũng bỏ qua nó, tức nó **giữ
 * nguyên giá trị của hồ sơ vừa rời**: A chưa từng bật bong bóng, B bật ⇒ A → B → A và A tự nhiên có bong bóng, vĩnh
 * viễn, không có đường quay lại. [PrefSnapshot] có thẻ kiểu `n` đúng cho ca này, và [applyClusterNav] dịch nó thành
 * `remove(key)` — tức "trả khoá về đúng trạng thái chưa-ai-đặt".
 *
 */
internal fun WorkspacePrefs.snapshotClusterNav(profile: String) {
    val e = sp.edit()
    ProfileScope.CLUSTERNAV_KEYS.forEach { (file, keys) ->
        val all = clusterNavPrefs(file).all
        // `associateWith` giữ CẢ khoá vắng (giá trị `null`) — xem KDoc ở trên, đây là nửa dễ quên nhất của phép chụp.
        val values: Map<String, Any?> = keys.associateWith { all[it] }
        e.putString(keyOf(profile, ProfileScope.snapshotSuffix(file)), PrefSnapshot.encode(values))
    }
    e.apply()
}

/**
 * Ghi ảnh chụp của [profile] trở lại đúng tệp prefs mà dịch vụ ClusterNav đang đọc.
 *
 * **Hồ sơ chưa có ảnh chụp ⇒ KHÔNG làm gì với tệp đó** (giữ nguyên giá trị hiện tại), đúng R5: *"hồ sơ mới = bản sao
 * của hiện tại"*. Đây là phân biệt giữa *"chưa có ảnh"* (khoá vắng) và *"ảnh rỗng"* (khoá có, chuỗi rỗng) — ảnh rỗng
 * chỉ xảy ra khi tệp đó thật sự không có khoá nào, và lúc đó **không có gì để áp** nên hai ca ra cùng kết quả.
 *
 * ⚠ Ghi **đúng kiểu**: `getBoolean` trên giá trị ghi bằng `putString` **ném** `ClassCastException`, và chỗ đọc là dịch
 * vụ đang chạy trên xe (`FloatingBubbleService`, `KachiKeyService`) chứ không phải màn Cài đặt — mất kiểu thì
 * không hỏng lúc đổi hồ sơ mà hỏng **trên đường** (xem KDoc [PrefSnapshot]). V-CLUSTER: giá trị SAI kiểu (khai sẵn ở
 * [ProfileScopeTypes.CLUSTERNAV], hoặc khác kiểu của giá trị sống) bị bỏ và ghi log thay vì ghi.
 *
 * ⚠ Ghi bằng `apply()` chứ không `commit()`: `apply()` cập nhật bản đồ **trong RAM ngay lập tức** (lượt đọc kế tiếp
 * của dịch vụ thấy ngay) và đẩy xuống đĩa ở thread nền — còn `commit()` chặn luồng vẽ để chờ I/O đúng lúc người dùng
 * vừa chạm đổi hồ sơ.
 */
internal fun WorkspacePrefs.applyClusterNav(profile: String) {
    val stored = sp.all
    ProfileScope.CLUSTERNAV_KEYS.forEach { (file, keys) ->
        val raw = storedSnapshot(stored, keyOf(profile, ProfileScope.snapshotSuffix(file))) ?: return@forEach
        // ⚠⚠ Lọc theo [ProfileScope] NGAY LÚC ÁP, không chỉ lúc chụp: ảnh chụp nằm trên đĩa của xe **lâu hơn** bản
        // phân loại đã sinh ra nó. Một khoá bị xếp lại phạm vi ở bản sau vẫn còn nguyên trong ảnh chụp cũ, và không có
        // phép lọc này thì lượt đổi hồ sơ **vẫn** ghi đè nó. [ProfileScope] phải là nguồn duy nhất ở CẢ hai đầu.
        val values = PrefSnapshot.decode(raw).filterKeys { PrefSnapshotPlan.inScope(it, keys) }
        if (values.isEmpty()) return@forEach
        val target = clusterNavPrefs(file)
        val plan = PrefSnapshotPlan.apply(target.all, values, keys, ProfileScopeTypes.CLUSTERNAV)
        logDropped("apply $file/$profile", plan.dropped)
        if (plan.writes.isEmpty()) return@forEach
        val e = target.edit()
        plan.writes.forEach { (k, v) ->
            when (v) {
                // Khoá vắng lúc chụp ⇒ trả nó về "chưa ai đặt" thay vì để nguyên giá trị của hồ sơ vừa rời.
                null -> e.remove(k)
                is Boolean -> e.putBoolean(k, v)
                is Int -> e.putInt(k, v)
                is Long -> e.putLong(k, v)
                is Float -> e.putFloat(k, v)
                is String -> e.putString(k, v)
                // `PrefSnapshot` chỉ sinh `Set<String>`; lọc lại vì kiểu tĩnh là `Set<*>` (mất kiểu qua `Any?`).
                is Set<*> -> e.putStringSet(k, v.filterIsInstance<String>().toSet())
                else -> Unit
            }
        }
        e.apply()
    }
}

/**
 * V-CLUSTER · VC-R8 lớp 1 — làm sạch giá trị của MỘT hậu tố hồ sơ trong **tệp nhập** trước khi nó chạm đĩa.
 *
 * Hậu tố không phải ảnh chụp ClusterNav ⇒ trả nguyên [value] (bố cục/chip… có bộ giải mã tự chữa riêng). Hậu tố ảnh
 * chụp ⇒ chỉ giữ khoá trong phạm vi, đúng kiểu khai sẵn (khoá ngoài phạm vi — vd khoá chiếu cụm BYD — bỏ im lặng);
 * giá trị không phải chuỗi ⇒ `null` (chỗ gọi xoá khoá đích). Số khoá bị bỏ được ghi log — không ném, không im lặng.
 *
 * PROFILE-IO-0930: `null` = hậu tố VẮNG trong tệp ([ProfileTransfer.planImport] trả mọi hậu tố, vắng ⇒ `null` ⇒ chỗ gọi
 * `remove`) — ca bình thường, trả `null` không ghi log (trước đó mỗi lượt nhập phun một cảnh báo giả cho mỗi ảnh vắng).
 */
internal fun cleanImportedSnapshot(suffix: String, value: Any?): Any? {
    val file = ProfileScope.CLUSTERNAV_KEYS.keys.firstOrNull { ProfileScope.snapshotSuffix(it) == suffix } ?: return value
    if (value == null) return null
    val raw = value as? String ?: run {
        Log.w(TAG, "nhập hồ sơ: bỏ ảnh $file — kiểu ${value?.let { it::class.simpleName }}, cần chuỗi")
        return null
    }
    val clean = PrefSnapshotPlan.sanitize(
        PrefSnapshot.decode(raw), ProfileScope.CLUSTERNAV_KEYS.getValue(file), ProfileScopeTypes.CLUSTERNAV,
    )
    logDropped("import $file", clean.dropped)
    return PrefSnapshot.encode(clean.values)
}

/**
 * 2.92 · PROFILE-IMPORT-GAP-KEYS — ảnh ClusterNav của tệp NHẬP thiếu khoá (tệp xuất từ bản cũ hơn) ⇒ điền chỗ trống bằng giá
 * trị ĐANG SỐNG của xe nhận (vắng ⇒ `null` tường minh = mặc định lúc áp) — CÙNG phép [ProfileScopeMigration.fillNewKeys] của lượt
 * nâng cấp (`fillNewProfileKeysOnce`). Không thì lượt đổi đầu tiên sang hồ sơ nhập giữ giá trị của hồ sơ vừa rời rồi chụp luôn
 * vào ảnh của nó (cùng bệnh PROFILE-NEW-KEYS, kích hoạt khác). Khoá có mặt (kể cả `null` tường minh của bản chia sẻ) ⇒ của tệp;
 * Hậu tố khác / giá trị sai kiểu ⇒ trả nguyên [value].
 *
 * 2.93 · PROFILE-NEW-FILE-FILL (review 2.92 Pass 2 G3) — [captured] = tệp nhập có ảnh ClusterNav KHÁC RỖNG ở ít nhất một tệp
 * ([importedCaptured]) ⇒ ảnh VẮNG/rỗng của tệp này cũng được điền (lối xuất luôn chụp đủ ảnh của hồ sơ đang dùng, nên ảnh vắng
 * = bản xuất cũ hơn). Tệp không có ảnh
 * ClusterNav nào ⇒ hồ sơ nhập *"chưa chụp"* ⇒ ảnh vắng giữ nguyên (*"bản sao của hiện tại"*, S4 · R5).
 */
internal fun WorkspacePrefs.fillImportedSnapshot(suffix: String, value: Any?, captured: Boolean): Any? {
    val file = ProfileScope.CLUSTERNAV_KEYS.keys.firstOrNull { ProfileScope.snapshotSuffix(it) == suffix } ?: return value
    val raw = value as? String ?: (if (value == null && captured) "" else return value)
    val filled = ProfileScopeMigration.fillNewKeys(
        mapOf(file to PrefSnapshot.decode(raw)), clusterNavPrefs(file).all, ProfileScope.CLUSTERNAV_KEYS.getValue(file),
        captured = if (captured) setOf(file) else emptySet(),
    )[file] ?: return value
    return PrefSnapshot.encode(filled)
}

/**
 * 2.93 · PROFILE-NEW-FILE-FILL — tệp nhập ([writes] = kế hoạch nhập, hậu tố → giá trị CHƯA làm sạch) có ảnh ClusterNav KHÁC RỖNG
 * sau làm sạch ở ít nhất MỘT tệp không — tức hồ sơ nhập *"đã chụp"* ([ProfileScopeMigration.captured]). Làm sạch bằng CÙNG phép
 * thuần [PrefSnapshotPlan.sanitize] của [cleanImportedSnapshot] nhưng không ghi log (lượt ghi thật sẽ ghi, một lần).
 */
internal fun importedCaptured(writes: Map<String, Any?>): Boolean =
    ProfileScope.CLUSTERNAV_KEYS.any { (file, keys) ->
        val raw = writes[ProfileScope.snapshotSuffix(file)] as? String ?: return@any false
        PrefSnapshotPlan.sanitize(PrefSnapshot.decode(raw), keys, ProfileScopeTypes.CLUSTERNAV).values.isNotEmpty()
    }

/** FIX286 · PI5 — một dòng log lúc nhập: tên hồ sơ vừa tạo + loại tệp. */
internal fun logImported(report: ProfileImportReport) {
    Log.i(TAG, "nhập hồ sơ «${report.name}» (${report.kind.code})")
}

/**
 * Chuỗi ảnh chụp ĐÃ LƯU ở khoá [key] của `kachi_workspace` ([stored] = `sp.all` chụp một lần) — `null` khi vắng HOẶC
 * sai kiểu (ghi log, coi như hồ sơ chưa có ảnh cho tệp đó).
 *
 * Senior review V-CLUSTER Pass 3 — không đọc bằng `sp.getString`: [ĐO code b5c0e87] lượt nhập hồ sơ tới 2.83 chép thẳng
 * giá trị đã giải mã (`copyValue`, chưa có [cleanImportedSnapshot]), nên một tệp nhập đặt `<hồ sơ>__cn__<tệp>` thành
 * Boolean/Int là nằm yên trên đĩa. `getString` trên nó NÉM `ClassCastException` — ở [fillNewProfileKeysOnce] (chạy
 * trong `init` của `PrefsWorkspaceRepository`, trước màn nhà) đó là launcher sập ở MỌI lần mở, không bao giờ tới được
 * dấu chạy-một-lần; ở [applyClusterNav] là sập đúng lúc chạm chip hồ sơ.
 */
internal fun storedSnapshot(stored: Map<String, *>, key: String): String? {
    val v = stored[key] ?: return null
    if (v is String) return v
    Log.w(TAG, "ảnh chụp đã lưu ${key.takeLast(40)} có kiểu ${v::class.simpleName}, cần chuỗi — coi như chưa có ảnh")
    return null
}

/** Một dòng log cho các khoá bị bỏ (giá trị hỏng / sai kiểu) — cắt ngắn, để một tệp độc không phun rác vào logcat. */
internal fun logDropped(what: String, dropped: List<String>) {
    if (dropped.isEmpty()) return
    val shown = dropped.take(5).joinToString { k -> k.take(60).map { c -> if (c < ' ') '?' else c }.joinToString("") }
    Log.w(TAG, "$what: bỏ ${dropped.size} khoá hỏng/sai kiểu: $shown")
}
