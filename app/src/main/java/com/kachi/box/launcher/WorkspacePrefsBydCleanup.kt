package com.kachi.box.launcher

import android.util.Log

/**
 * ═══ Android box B2 · W4 — DỌN MỘT LẦN khoá chết của Kachi BYD — phần của [WorkspacePrefs] ═════════════════════════════
 *
 * Danh sách + phép thuần ở `:core` [BydDeadPrefs] (`BydDeadPrefsTest`); tệp này chỉ đọc/ghi prefs. Chạy ở
 * `init` của `PrefsWorkspaceRepository`, SAU ba lượt di trú có sẵn — cùng chỗ, cùng chi phí: một lượt `all` trên hai tệp
 * đã nạp sẵn + một lượt ghi, rồi dấu [BydDeadPrefs.MARK] chặn mọi lần sau (0 chi phí). Không thêm luồng, không lệnh shell.
 *
 * ## Thứ tự và vì sao dấu ghi SAU
 * Xoá khoá chết là VÔ HẠI nếu chạy lại, còn dấu ghi TRƯỚC mà chết máy giữa chừng thì khoá chết nằm lại mãi. Nên: xoá tệp
 * chết → xoá khoá chết của `clusternav_prefs` (`commit()`) → xoá hậu tố chết + làm sạch ảnh chụp + sổ đã-rót của
 * `kachi_workspace` và đặt dấu trong CÙNG một `Editor` (`commit()` — dấu chỉ lên đĩa cùng lượt dọn của chính tệp nó).
 * `commit()` chứ không `apply()`: chạy một lần trong đời cài đặt, và nó phải xong trước lượt `load()` đầu tiên đọc tệp.
 */
internal fun WorkspacePrefs.cleanBydDeadPrefsOnce() {
    // Đọc qua `all` + so kiểu, KHÔNG `getBoolean`: giá trị sai kiểu mà ném ở `init` là HOME sập mỗi lần mở.
    val stored = sp.all
    if (stored[BydDeadPrefs.MARK] == true) return
    var files = 0
    BydDeadPrefs.DEAD_FILES.forEach { name ->
        // `deleteSharedPreferences` (API 24) bỏ cả bản cache trong tiến trình; tệp vắng ⇒ `true`, không ném.
        if (runCatching { appCtx.deleteSharedPreferences(name) }.getOrDefault(false)) files++
    }
    val cn = clusterNavPrefs(BydDeadPrefs.CLUSTERNAV_FILE)
    val cnAll = cn.all
    val cnDead = cnAll.keys.filter(BydDeadPrefs::isDeadClusterNavKey)
    val ids = (cnAll[K_CONFIRM_IDS] as? Set<*>)?.filterIsInstance<String>()?.toSet()
    val cleanIds = ids?.let(BydDeadPrefs::cleanConfirmIds)
    if (cnDead.isNotEmpty() || cleanIds != null) {
        val e = cn.edit()
        cnDead.forEach { e.remove(it) }
        if (cleanIds != null) e.putStringSet(K_CONFIRM_IDS, cleanIds)
        // Ghi hỏng ⇒ vẫn đặt dấu bên dưới: khoá chết nằm lại là vô hại, còn thử lại mỗi lần mở thì thành chi phí lặp.
        if (!e.commit()) Log.w(TAG, "dọn clusternav_prefs: commit hỏng — khoá chết nằm lại (vô hại)")
    }
    val e = sp.edit()
    val wsDead = stored.keys.filter(BydDeadPrefs::isDeadLauncherKey)
    wsDead.forEach { e.remove(it) }
    var shots = 0
    val shotSuffix = "__" + ProfileScope.snapshotSuffix(BydDeadPrefs.CLUSTERNAV_FILE)
    stored.keys.filter { it.endsWith(shotSuffix) }.forEach { key ->
        val raw = stored[key] as? String ?: return@forEach
        BydDeadPrefs.cleanSnapshot(PrefSnapshot.decode(raw))?.let { e.putString(key, PrefSnapshot.encode(it)); shots++ }
    }
    val ledger = (stored[ProfileScopeMigration.FILLED_LEDGER_KEY] as? Set<*>)?.filterIsInstance<String>()?.toSet()
    if (ledger != null && ledger.any(BydDeadPrefs::isDeadLedgerEntry)) {
        e.putStringSet(ProfileScopeMigration.FILLED_LEDGER_KEY, ledger.filterNot(BydDeadPrefs::isDeadLedgerEntry).toSet())
    }
    if (!e.putBoolean(BydDeadPrefs.MARK, true).commit()) Log.w(TAG, "dọn kachi_workspace: commit hỏng — lần mở sau dọn lại")
    // Một dòng cho log phiên (CLAUDE.md §11) — không có adb vẫn biết lượt dọn đã chạy và dọn bao nhiêu.
    Log.i(
        TAG,
        "dọn khoá Kachi BYD: $files tệp · ${cnDead.size} khoá clusternav_prefs · ${wsDead.size} khoá hồ sơ · $shots ảnh · " +
            "hỏi-lại ${if (cleanIds != null) "đã lọc" else "giữ"}",
    )
}

/** Cùng khoá của `PrefsVoiceV3.voiceConfirmIds` (tệp `clusternav_prefs`) — tập hỏi lại có thể còn mã nút xe ≤ 2.98. */
private const val K_CONFIRM_IDS = "voice_confirm_ids"

private const val TAG = "KachiProfile"
