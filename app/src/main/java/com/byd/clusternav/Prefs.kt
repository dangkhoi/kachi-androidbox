package com.byd.clusternav

import android.content.Context
import android.content.SharedPreferences
import com.byd.clusternav.launcher.voice.VoiceWakePrefsMain
import com.byd.clusternav.modules.voicekey.VoiceKeyBindingStore
import com.byd.clusternav.modules.voicekey.VoiceKeyCustomButtonStore
import com.byd.clusternav.voicekey.VoiceKeyBinding
import com.byd.clusternav.voicekey.VoiceKeyBindings
import com.byd.clusternav.voicekey.VoiceKeyCustomButton
import com.byd.clusternav.voicekey.VoiceKeyCustomButtons

/** Lựa chọn người dùng phía ClusterNav (tệp `clusternav_prefs`): phím vật lý · giọng nói · khởi động nền · kính thật. */
object Prefs {
    // Android box B2 · W4 — khoá dẫn đường cụm/HUD (`enabled` · `source_mode` · `marquee` · `nav_cluster_screen_mode` ·
    // `lane` · `interpolate` · `hud` · `acc_booster`), `bubble_auto` · `anim_opt` · `nav_verbose_log` · `mod_*`, biển báo tốc
    // độ và tiện nghi xe gỡ cùng mã; lượt dọn một lần xoá chúng khỏi máy (`:core BydDeadPrefs`).

    private const val FILE = "clusternav_prefs"

    /** `internal` (không `private`): các tệp hàm mở rộng `Prefs*.kt` dùng lại đúng hàm này — một hàm, một literal tên tệp. */
    internal fun sp(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // ★ 1.21 Item 1 (owner): "Tự khởi động nền" — khởi động máy → app tự làm việc nền (cấp lại trợ năng cho phím ·
    // giữ phím sống · lịch tự dẫn) mà KHÔNG bung màn hình nào. MẶC ĐỊNH BẬT. RebindReceiver đọc cờ này lúc boot/OTA:
    // BẬT → BootSetupService (chạy nền).
    fun headlessAutostart(ctx: Context): Boolean = sp(ctx).getBoolean("headless_autostart", true)
    fun setHeadlessAutostart(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("headless_autostart", v).apply()

    // ─── UX-OVERHAUL WP1 · R1.3 — "Kính thật (làm mờ nền)" ─────────────────────────────────────────
    // Glass GIẢ (mặc định) = gradient dọc + fill trong mờ, 0 mép/viền/blur (KachiTheme.surface). Glass THẬT (option) =
    // RenderEffect.createBlurEffect làm mờ nền phía sau (chỉ API 31+; API thấp lùi về giả — KachiGlassMode). MẶC
    // ĐỊNH TẮT: nền GPU đầu máy DiLink yếu, và [ĐO] xe là API 29 nên glass-thật ở đó luôn lùi về giả. Theo XE.
    fun glassReal(ctx: Context): Boolean = sp(ctx).getBoolean("ui_glass_real", false)
    fun setGlassReal(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("ui_glass_real", v).apply()

    // ─── T3 (1.13): Nút vật lý → Trợ lý giọng nói ───────────────────────────────────────────────
    // 1.19: KHÔNG thay chức năng gốc — onKeyEvent chỉ "nuốt" đúng keycode đã cấu hình, còn lại pass-through.
    // Bỏ cử chỉ (Nhấn/Nhấn-giữ) vì nút short/long ra keycode khác nhau. Đích lưu STRING (package/sentinel);
    // nút tự học lưu JSON. MẶC ĐỊNH TẮT. Xem VoiceKeyMatcher (:core).
    private const val K_VK_ENABLED = "voicekey_enabled"
    private const val K_VK_KEYCODE = "voicekey_keycode"
    private const val K_VK_TARGET = "voicekey_target"          // 1.19: STRING (package hoặc sentinel __ASSIST__/__RECOGNIZER__)
    private const val K_VK_LEARN = "voicekey_learn"
    private const val K_VK_CUSTOM = "voicekey_custom_buttons"  // 1.19: JSON [{"n":name,"k":keycode}] nút tự học (+"s" 2.88)
    private const val K_VK_BINDINGS = "voicekey_bindings"      // F3: JSON [{"k":keycode,"t":target}] danh sách gán (+"s" 2.88)
    const val VK_KEYCODE_DEFAULT = 328   // nút mic vô-lăng giữ trên xe này (đo on-car 2026-08-13). "Học phím mới" nếu xe khác.
    const val VK_TARGET_ASSIST = "__ASSIST__"
    const val VK_TARGET_RECOGNIZER = "__RECOGNIZER__"
    // 1.20: phát KEYCODE_VOICE_ASSIST (231) qua dadb shell (như app 8hare) → route tới trợ lý hệ thống.
    // Sạch hơn ACTION_ASSIST (không chooser, không nhầm intent). Chọn target này cũng đặt trợ lý hệ thống = Google/Gemini.
    const val VK_TARGET_GEMINI_KEY = "__VOICEKEY231__"

    /**
     * V1 pha NGHE — đích *"Kachi nghe"*: phím vô-lăng mở **phiên nghe của chính Kachi**, không mở app nào.
     *
     * ## Vì sao là một sentinel THỨ TƯ, không phải tên gói của chính mình
     * Đặt `com.byd.launcher` làm đích thì [com.byd.clusternav.modules.voicekey.AssistantLauncher] sẽ đi nhánh
     * "mở app theo launch-intent" — tức về màn chính và **không** nghe gì. Ý nghĩa *"mở phiên nghe"* không phải
     * là *"mở app Kachi"*, nên nó phải có mã riêng, đúng như ba sentinel kia.
     *
     * ⚠ **KHÔNG** đổi [VK_TARGET_DEFAULT]: nút 328 trên vô-lăng owner vẫn thuộc Kiki và đang chạy tốt
     * (CLAUDE.md §6 — không đảo đường đã chạy tốt ngoài hiện trường). Đây chỉ là một dòng **thêm vào danh sách
     * chọn**; ai muốn đổi thì tự gán.
     */
    const val VK_TARGET_KACHI_VOICE = "__KACHI_VOICE__"

    // ─── V1 pha NGHE: nút mic trên thanh trạng thái ─────────────────────────────────────────────
    // Khoá THEO XE (không theo hồ sơ), cùng họ với `headless_autostart` ở trên: nó phụ thuộc thứ thuộc về
    // MÁY — mô hình nhận dạng đã tải hay chưa — chứ không phụ thuộc người đang lái. Mặc định BẬT: thanh trên
    // chỉ vẽ nút này khi mô hình đã có (xem `KachiTopStrip.voicePill`), nên trên máy chưa tải nó vô hình,
    // còn trên máy đã tải thì người ta vừa chủ động tải xong — giấu đi mới là bất ngờ.
    private const val K_VOICE_PILL = "voice_mic_pill"

    fun voiceMicPill(ctx: Context): Boolean = sp(ctx).getBoolean(K_VOICE_PILL, true)
    fun setVoiceMicPill(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(K_VOICE_PILL, v).apply()

    // ─── V1 pha NÓI · R4 (spec `docs/specs/kachi-voice-feedback.html` T9) ──────────────────────
    // Hai công tắc THEO XE, cùng họ `voice_mic_pill` ngay trên: cái quyết định chúng trả lời được hay không là
    // **máy này có giọng gì** (engine hệ thống · gói offline đã lắp chưa), không phải người đang lái. Để chúng
    // theo hồ sơ nghĩa là đổi hồ sơ xong loa im, mà không ai hiểu vì sao (`ProfileScope.DEVICE_KEYS` ghi lý do).
    private const val K_VOICE_SPEAK = "voice_speak_replies"
    private const val K_VOICE_OFFLINE = "voice_prefer_offline"

    /**
     * Có ĐỌC câu trả lời thành tiếng không. **Mặc định BẬT** — R1 của spec: câu trả lời chỉ hiện chữ thì người
     * lái phải rời mắt khỏi đường để đọc nó, tức đúng thứ mà một trợ lý giọng nói sinh ra để khỏi phải làm.
     * Tắt vẫn còn nguyên tấm chữ + âm báo, không mất chức năng nào.
     */
    fun voiceSpeakReplies(ctx: Context): Boolean = sp(ctx).getBoolean(K_VOICE_SPEAK, true)
    fun setVoiceSpeakReplies(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(K_VOICE_SPEAK, v).apply()

    /**
     * Ưu tiên **giọng offline tại máy** (gói Piper) hơn máy đọc của hệ thống. Mặc định TẮT: đường hệ thống rẻ
     * hơn hẳn (0 byte đĩa, 0 byte RAM) và trên đầu xe có sẵn `vi-VN` thì nó đọc ngay. Công tắc chỉ có tác dụng
     * khi gói đã lắp — `VoiceSpeakerSelector` tự lùi về đường còn dùng được, không bao giờ im lặng vì một cờ.
     */
    fun voicePreferOffline(ctx: Context): Boolean = sp(ctx).getBoolean(K_VOICE_OFFLINE, false)
    fun setVoicePreferOffline(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(K_VOICE_OFFLINE, v).apply()

    // ─── OQ4 · ĐỌC câu hỏi xác nhận rồi mới mở micro — **MẶC ĐỊNH TẮT** (owner chốt 2026-09-16) ───
    private const val K_VOICE_ASK_ALOUD = "voice_ask_aloud"

    /**
     * Có đọc to **câu hỏi xác nhận** (rồi mới mở micro) không. Mặc định **false**.
     *
     * ## Vì sao một công tắc riêng, không đi chung [voiceSpeakReplies]
     * Hai việc khác hẳn nhau về cái giá của lỗi. Đọc **phản hồi** (R4) xảy ra khi việc đã xong — đọc thừa chỉ tốn
     * hai giây. Đọc **câu hỏi** thì nằm ngay trước một lượt mở micro, nên nó kéo theo cả chuỗi *đọc → chờ mốc
     * xong → mở micro*: dài hơn, và có thêm một đường để hỏng. Owner 2026-09-16 chốt **KHÔNG** đọc câu hỏi (chỉ
     * đọc phản hồi sau lệnh) ⇒ mặc định tắt.
     *
     * Đường mã của OQ4 (`VoiceSession.askAloudThenListen` + hợp đồng `VoiceSpeaker.speak(text, onDone)`) **giữ
     * nguyên**, không gỡ: nó đã có bài canh, và quyết định này là một **lựa chọn hành vi** chứ không phải một
     * kết luận *"cơ chế ấy sai"*. Chưa có hàng trong Cài đặt (owner xếp vào batch sau, cùng mục *"chọn nút nào
     * phải hỏi"*); tới lúc đó khoá này lên UI và ra khỏi [SettingsCatalog.CLUSTERNAV_HIDDEN_KEYS].
     */
    fun voiceAskAloud(ctx: Context): Boolean = sp(ctx).getBoolean(K_VOICE_ASK_ALOUD, false)
    fun setVoiceAskAloud(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(K_VOICE_ASK_ALOUD, v).apply()

    // ─── APP DẪN ĐƯỜNG MẶC ĐỊNH (owner 2026-09-18) — nói "dẫn đường" không nêu app thì dùng cái này ───
    private const val K_VOICE_NAV_APP = "voice_nav_default_app"

    /**
     * Mã app dẫn đường MẶC ĐỊNH (`gmaps`/`vietmap`/`waze`) khi câu KHÔNG nêu tên app. Mặc định **`gmaps`** —
     * đường tin cậy nhất (nhận CHỮ thẳng, Google tự geocode, không kẹt mạng xe). Owner đổi được trong Cài đặt ›
     * Dẫn đường. Giá trị = `VoiceAppTargets` key; `VoiceTargetDispatch` tự lùi về [VoiceAppTargets] nếu app chưa cài.
     */
    fun voiceNavDefaultApp(ctx: Context): String = sp(ctx).getString(K_VOICE_NAV_APP, "gmaps") ?: "gmaps"
    fun setVoiceNavDefaultApp(ctx: Context, key: String) = sp(ctx).edit().putString(K_VOICE_NAV_APP, key).apply().also { VoiceWakePrefsMain.publish(ctx) }

    // ─── APP NHẠC MẶC ĐỊNH (owner 2026-09-21) — nói "phát nhạc" không nêu app + không có nhạc đang phát thì dùng ───
    private const val K_VOICE_MUSIC_APP = "voice_music_default_app"

    /**
     * Mã app nhạc MẶC ĐỊNH khi câu KHÔNG nêu app. Mặc định **rỗng** = "tự chọn" (giữ hành vi cũ: app đang phát,
     * rồi app nhạc đầu tiên đã cài). Owner đổi trong Cài đặt › Giọng nói. Giá trị = `VoiceAppTargets` key
     * (`ytmusic`/`spotify`/`zing`/…); `VoiceTargetDispatch` tự lùi nếu app chưa cài.
     */
    fun voiceMusicDefaultApp(ctx: Context): String = sp(ctx).getString(K_VOICE_MUSIC_APP, "") ?: ""
    fun setVoiceMusicDefaultApp(ctx: Context, key: String) = sp(ctx).edit().putString(K_VOICE_MUSIC_APP, key).apply().also { VoiceWakePrefsMain.publish(ctx) }

    // "Hey Kachi" wake-word (W-WAKE) — theo XE (ProfileScope.DEVICE_KEYS), mặc định **TẮT** (nghe nền = rủi ro CPU
    // → opt-in). Câu gọi = preset id ([VoiceWakePhrase]). VoiceWakeService.sync() đọc cờ này để bật/tắt FGS.
    //
    // ⚠ [P2 2026-09-19] Cờ owner `voice_wake_enabled` nằm trong `clusternav_prefs` (tệp CHUNG, 38 key = mọi
    // setting launcher). MODE_PRIVATE **không** an toàn đa-tiến-trình: nếu tiến trình `:wake` ghi tệp này bằng
    // map-RAM cũ của nó, `apply()` ghi đè cả tệp ⇒ **xoá mọi setting launcher đã đổi từ lúc :wake khởi động**.
    // ⇒ `:wake` CHỈ được ghi marker RIÊNG [wakeDisabledMarker]; `clusternav_prefs` chỉ tiến trình launcher ghi.
    //
    // ⚠ [SOÁT 2026-09-19] Marker là TỆP RỖNG, KHÔNG phải một tệp prefs thứ hai: `SharedPreferences` cache map
    // theo tiến trình và KHÔNG thấy tiến trình khác ghi ⇒ nếu cờ tự-tắt nằm trong prefs, launcher (đã cache)
    // đọc lại vẫn thấy cũ ⇒ công tắc hiện ON dù bộ nghe đã tắt. `File.exists()` đọc thẳng hệ tệp (không cache)
    // ⇒ launcher thấy NGAY. Một tệp create/delete = op FS đơn, an toàn hai tiến trình.
    private fun wakeDisabledMarker(ctx: Context) =
        java.io.File(ctx.applicationContext.filesDir, "kachi_wake_disabled")

    /** Cầu chì false-accept: `:wake` tự-tắt bằng cách tạo marker RIÊNG (KHÔNG đụng `clusternav_prefs`). */
    fun wakeServiceDisabled(ctx: Context): Boolean =
        runCatching { wakeDisabledMarker(ctx).exists() }.getOrDefault(false)
    fun setWakeServiceDisabled(ctx: Context, disabled: Boolean) {
        runCatching { wakeDisabledMarker(ctx).let { if (disabled) it.createNewFile() else it.delete() } }
    }

    /** BẬT-HIỆU-LỰC = owner bật (`clusternav_prefs`) **VÀ** service chưa tự-tắt (marker `kachi_wake_disabled`). */
    fun wakeEnabled(ctx: Context): Boolean =
        sp(ctx).getBoolean("voice_wake_enabled", false) && !wakeServiceDisabled(ctx)
    /** Công tắc THÔ của owner (chưa gồm cầu chì) — FIX286 · VK4: ảnh chụp cho `:wake` mang bản thô, cầu chì `:wake` tự đọc. */
    fun wakeSwitchOn(ctx: Context): Boolean = sp(ctx).getBoolean("voice_wake_enabled", false)

    /** CHỈ tiến trình LAUNCHER gọi (công tắc Cài đặt). Bật lại ⇒ xoá marker tự-tắt của `:wake` để service chạy lại. */
    fun setWakeEnabled(ctx: Context, on: Boolean) {
        sp(ctx).edit().putBoolean("voice_wake_enabled", on).apply()
        if (on) setWakeServiceDisabled(ctx, false)
        VoiceWakePrefsMain.publish(ctx)   // FIX286 · VK4 — `:wake` quyết chế độ từ ảnh chụp, không từ cache cũ của nó
    }
    fun wakePhraseId(ctx: Context): String = sp(ctx).getString("voice_wake_phrase", "hey_kachi") ?: "hey_kachi"
    fun setWakePhraseId(ctx: Context, id: String) = sp(ctx).edit().putString("voice_wake_phrase", id).apply()

    /** Engine wake: true = ASR no-train (mặc định, owner 2026-09-22), false = KWS gigaspeech. */
    fun wakeEngineAsr(ctx: Context): Boolean = sp(ctx).getBoolean("voice_wake_engine_asr", true)
    fun setWakeEngineAsr(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("voice_wake_engine_asr", v).apply()

    const val VK_TARGET_DEFAULT = "ai.zalo.kiki.car"           // mặc định Kiki (khớp default cũ 0=Kiki)

    fun voiceKeyEnabled(ctx: Context): Boolean = sp(ctx).getBoolean(K_VK_ENABLED, false)
    fun setVoiceKeyEnabled(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(K_VK_ENABLED, v).apply().also { VoiceWakePrefsMain.publish(ctx) }

    /** CŨ (trước F3) — mã phím DUY NHẤT. Từ F3 chỉ còn dùng để **migrate** sang [voiceKeyBindings]. */
    fun voiceKeyCode(ctx: Context): Int = voiceKeyCode(sp(ctx))

    /** Bản nhận thẳng ô nhớ — xem ghi chú "vì sao có nạp chồng" ở [voiceKeyBindings]. */
    fun voiceKeyCode(p: SharedPreferences): Int = p.getInt(K_VK_KEYCODE, VK_KEYCODE_DEFAULT)

    /**
     * CŨ (trước F3) — đích DUY NHẤT: package name hoặc sentinel. Migrate cấu hình đời đầu (int ordinal →
     * string) rồi ghi lại. Từ F3 chỉ còn dùng để **migrate** sang [voiceKeyBindings].
     */
    fun voiceKeyTargetSpec(ctx: Context): String = voiceKeyTargetSpec(sp(ctx))

    /** Bản nhận thẳng ô nhớ — xem ghi chú "vì sao có nạp chồng" ở [voiceKeyBindings]. */
    fun voiceKeyTargetSpec(p: SharedPreferences): String =
        when (val raw = p.all[K_VK_TARGET]) {
            is String -> raw
            is Int -> (when (raw) { 1 -> "com.byd.autovoice"; 2 -> VK_TARGET_RECOGNIZER; 3 -> VK_TARGET_ASSIST; else -> VK_TARGET_DEFAULT })
                .also { p.edit().putString(K_VK_TARGET, it).apply() }
            else -> VK_TARGET_DEFAULT
        }

    // ─── F3 (owner 2026-08-24): DANH SÁCH gán (nhiều phím → nhiều app) ──────────────────────────
    // Owner: "chọn nút + chọn app xong → add, thì ra 1 dòng đã binding nút và app, xong có thể chọn thêm
    // add thêm, mình listen thì listen theo cái danh sách đã save đó thôi".
    // Đây là NGUỒN CHÂN LÝ DUY NHẤT cho khớp phím (NavAccessibilityService.onKeyEvent). Danh sách RỖNG ⇒
    // không phím nào bị nuốt, không app nào được mở — đúng ý "rỗng thì không có gì chạy".

    /**
     * Đọc danh sách gán. Lần đọc ĐẦU TIÊN trên một máy chưa có khoá danh sách sẽ **migrate cấu hình
     * một-cặp** của 1.19 rồi ghi xuống ngay — nâng cấp KHÔNG được làm mất cấu hình owner đang chạy.
     *
     * Điều kiện migrate nằm ở [VoiceKeyBindings.migrateLegacy] (thuần, test off-device được); ở đây chỉ
     * cấp cho nó **dấu vết thật** trong file prefs (`contains`), vì cả hai khoá cũ đều có giá trị mặc định
     * nên "đọc ra được" không chứng minh owner từng cấu hình.
     *
     * Sau khi ghi khoá danh sách (kể cả khi migrate ra RỖNG → ghi `"[]"`), migrate KHÔNG chạy lại: nếu
     * chạy lại thì owner xoá hết dòng gán rồi mở lại app sẽ thấy dòng cũ sống lại.
     *
     * ── VÌ SAO CÓ NẠP CHỒNG NHẬN THẲNG `SharedPreferences` (2026-08-24) ─────────────────────────
     * Bản chỉ-nhận-`Context` **không chạy được trong test off-device** (`getSharedPreferences` là API
     * Android; repo không dùng Robolectric), nên đường migrate — đúng chỗ nguy hiểm nhất, sai một lần là
     * **mất vĩnh viễn cấu hình owner** — trước đó chỉ được khoá bằng cách *quét chuỗi source*, tức vẫn
     * xanh nếu ai đó đổi thân hàm thành `writeVoiceKeyBindings(ctx, emptyList())`. Tách ô nhớ ra thành
     * tham số cho phép `VoiceKeyBindingMigrationTest` **chạy thật** cả 4 ca (chưa có khoá + có dấu vết /
     * chưa có khoá + không dấu vết / đã có `"[]"` / đã có danh sách) với một ô nhớ giả.
     * Bản `Context` chỉ còn là lớp vỏ một dòng — không còn logic nào nằm ngoài tầm test.
     */
    fun voiceKeyBindings(ctx: Context): List<VoiceKeyBinding> = voiceKeyBindings(sp(ctx))

    fun voiceKeyBindings(p: SharedPreferences): List<VoiceKeyBinding> {
        VoiceKeyBindingStore.rawOrNull(p, K_VK_BINDINGS)?.let { return VoiceKeyBindingStore.decode(it) }
        val migrated = VoiceKeyBindings.migrateLegacy(
            hasLegacyKeyCode = p.contains(K_VK_KEYCODE),
            hasLegacyTarget = p.contains(K_VK_TARGET),
            enabled = p.getBoolean(K_VK_ENABLED, false),
            keyCode = voiceKeyCode(p),
            targetSpec = voiceKeyTargetSpec(p),
        )
        writeVoiceKeyBindings(p, migrated)
        return migrated
    }

    /**
     * Thêm một dòng gán. Mã phím đã được gán ⇒ **GHI ĐÈ** (giữ nguyên vị trí dòng) và trả về đích CŨ để UI báo cho owner
     * biết đã thay cái gì — cấm im lặng. Dòng mới ⇒ trả `null`.
     */
    fun addVoiceKeyBinding(ctx: Context, keyCode: Int, targetSpec: String): String? =
        addVoiceKeyBinding(sp(ctx), keyCode, targetSpec).also { VoiceWakePrefsMain.publish(ctx) }

    fun addVoiceKeyBinding(p: SharedPreferences, keyCode: Int, targetSpec: String): String? {
        val result = VoiceKeyBindings.put(voiceKeyBindings(p), keyCode, targetSpec)
        writeVoiceKeyBindings(p, result.bindings)
        return result.replaced
    }

    /** Xoá dòng gán của mã phím (nút xoá trên từng dòng). */
    fun removeVoiceKeyBinding(ctx: Context, keyCode: Int) =
        removeVoiceKeyBinding(sp(ctx), keyCode).also { VoiceWakePrefsMain.publish(ctx) }

    fun removeVoiceKeyBinding(p: SharedPreferences, keyCode: Int) =
        writeVoiceKeyBindings(p, VoiceKeyBindings.remove(voiceKeyBindings(p), keyCode))

    private fun writeVoiceKeyBindings(p: SharedPreferences, list: List<VoiceKeyBinding>) =
        VoiceKeyBindingStore.write(p, K_VK_BINDINGS, list)

    /** "Học phím mới": khi BẬT, onKeyEvent kế tiếp bắt keycode nút vừa bấm rồi tự tắt cờ + báo Activity đặt tên. */
    fun voiceKeyLearn(ctx: Context): Boolean = sp(ctx).getBoolean(K_VK_LEARN, false)
    fun setVoiceKeyLearn(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(K_VK_LEARN, v).apply()

    /** Nút tự học (tên, mã) — lưu JSON để dropdown dựng lại + xoá được. Khoá một nút = mã phím. */
    fun voiceKeyCustomButtons(ctx: Context): List<VoiceKeyCustomButton> = VoiceKeyCustomButtonStore.read(sp(ctx), K_VK_CUSTOM)
    fun addVoiceKeyCustomButton(ctx: Context, name: String, code: Int) =
        writeCustomButtons(ctx, VoiceKeyCustomButtons.put(voiceKeyCustomButtons(ctx), VoiceKeyCustomButton(name, code)))
    fun removeVoiceKeyCustomButton(ctx: Context, code: Int) =
        writeCustomButtons(ctx, VoiceKeyCustomButtons.remove(voiceKeyCustomButtons(ctx), code))
    private fun writeCustomButtons(ctx: Context, items: List<VoiceKeyCustomButton>) =
        sp(ctx).edit().putString(K_VK_CUSTOM, VoiceKeyCustomButtonStore.encode(items)).apply()

    // Miễn trừ lần đầu (no-warranty / không liên kết hãng nào / tự chịu rủi ro) — hiện MỘT lần rồi ghim cờ.
    private const val K_DISCLAIMER_SHOWN = "disclaimer_shown"
    fun disclaimerShown(ctx: Context): Boolean = sp(ctx).getBoolean(K_DISCLAIMER_SHOWN, false)
    fun setDisclaimerShown(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(K_DISCLAIMER_SHOWN, v).apply()

    // ─── Công tắc ẨN: ép đường lùi của CHẠM trong ô (1.69, spec kachi-open-app-correctly §4.6) ────
    // MẶC ĐỊNH TẮT. Bật ⇒ `InputDaemonClient` KHÔNG khởi daemon bơm chạm và mọi cú chạm trong ô đi đường lùi
    // theo cử chỉ (`GestureFallback` + `input -d`). Tồn tại vì một lý do DUY NHẤT: trên máy ảo daemon lên bình
    // thường, nên nếu không ép được thì đúng cái nhánh mà XE đang mắc kẹt ([ĐO xe 2026-09-16] §9.1: daemon không
    // lên lần nào) sẽ chỉ kiểm được bằng... một chiếc xe. Không có bề mặt UI (xem SettingsCatalog HIDDEN_KEYS) —
    // đặt bằng `run-as` trên bản vehicleTest, đọc lại bằng cầu kiểm thử (`state.inputd` / `prefs`).
    // Đọc MỘT lần mỗi tiến trình ở AppContainer ⇒ đổi xong phải khởi động lại app.
    // inputd (công tắc ẩn `inputd_disabled` + token TCP loopback) tách sang `PrefsInputd.kt` (trần 500 dòng).

    // ─── A11Y-BIND-STUCK (2026-09-28) — mốc lần leo nấc force-stop gần nhất ───

    private const val K_A11Y_ESCALATED_AT = "a11y_forcestop_elapsed"

    /**
     * Mốc `SystemClock.elapsedRealtime()` của lần TỰ force-stop gần nhất để gỡ dịch vụ Hỗ trợ bị kẹt;
     * `-1` = chưa từng. Đồng hồ này về 0 khi khởi động lại máy, nên mốc lưu LỚN HƠN mốc hiện tại nghĩa là đã
     * reboot — xem [com.byd.clusternav.modules.navaccess.AccessibilityHealGates.escalatedThisBoot].
     */
    fun a11yEscalatedAt(ctx: Context): Long = sp(ctx).getLong(K_A11Y_ESCALATED_AT, -1L)

    /**
     * ⚠ `commit()` chứ KHÔNG `apply()`: mốc này được ghi NGAY TRƯỚC khi bắn lệnh tự giết tiến trình mình
     * (CLAUDE.md §5 — ghi marker TRƯỚC khi đổi state ngoài). `apply()` ghi nền, tiến trình chết trước khi
     * flush xong thì mốc mất ⇒ lần sau lại leo ⇒ vòng lặp giết launcher. Chặn bằng ghi đồng bộ.
     */
    fun setA11yEscalatedAt(ctx: Context, v: Long) =
        sp(ctx).edit().putLong(K_A11Y_ESCALATED_AT, v).commit()

    private const val K_DEEP_SLEEP_AT = "a11y_deep_sleep_ms"

    /**
     * Tổng thời gian máy đã NGỦ SÂU đọc được ở lượt watchdog TRƯỚC; `-1` = chưa có mốc.
     * Bước nhảy của số này giữa hai lượt = xe vừa đứng bao lâu — xem
     * [com.byd.clusternav.modules.navaccess.A11yBindJournal.wokeFromLongSleep].
     */
    fun lastDeepSleepMs(ctx: Context): Long = sp(ctx).getLong(K_DEEP_SLEEP_AT, -1L)

    fun setLastDeepSleepMs(ctx: Context, v: Long) =
        sp(ctx).edit().putLong(K_DEEP_SLEEP_AT, v).apply()
}
