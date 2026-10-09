package com.kachi.box.launcher

/**
 * BẢN ĐỒ KHOÁ của phía ClusterNav (IA v2 · §4.3) — *"khoá này nằm ở tệp prefs nào"* và *"khoá nào cố ý không lên
 * UI"*. Thuần Kotlin, không `android.*` ⇒ kiểm off-car.
 *
 * ## Bệnh nó chữa — và vì sao nó KHÔNG trùng với [SettingsCatalog.PREFS_FILES]
 * [SettingsCatalog.PREFS_FILES] trả lời câu *"phía launcher có mở tệp prefs thứ ba nào không"*, và bộ quét của nó
 * (`SettingsCoverageContractTest`) **cố ý không đọc** `Prefs.kt` của ClusterNav — kéo hàng chục khoá của tính năng
 * cũ vào sẽ biến bài R2 thành bài kiểm ClusterNav. Khi Settings gộp cả ClusterNav vào thì đúng những khoá bị bỏ ra
 * đó lại thành nội dung chính của bốn nhóm mới ⇒ phải có lưới canh riêng cho chúng, nếu không mỗi mục mới là một
 * chuỗi viết tay không ai đối chiếu với mã.
 *
 * Lưới đó là [CLUSTERNAV_KEYS]: mỗi khoá nói nó ở **tệp nào**, và `ClusterNavKeysContractTest` (`:app`) đòi chuỗi
 * đúng-nguyên-văn ấy có mặt trong **tệp nguồn khai nó**. Gõ sai một ký tự ⇒ đỏ, thay vì lặng lẽ ghi vào một khoá
 * không ai đọc (bẫy đã ăn một lần: spec ghi `recirc_on_start`, mã thật là `recirc_on_start_enabled`).
 */
internal object SettingsCatalogClusterNav {

    /**
     * Tiền tố khoá dựng động → lý do. Bài canh nguyên-văn tra bảng này trước khi kết luận "khoá không tồn tại".
     *
     * Android box B2 · W2e: RỖNG — tiền tố duy nhất (`seat_level_`, mức từng ghế) gỡ cùng mã ghế.
     */
    val DYNAMIC_KEY_PREFIXES: Map<String, String> = emptyMap()

    /**
     * Tệp SharedPreferences mà **phía ClusterNav** đang ghi → nơi khai tên tệp đó.
     *
     * Ba tệp, mỗi tệp một lý do lịch sử khác nhau — cố ý KHÔNG gộp lại: gộp nghĩa là viết lại đường đọc của runtime
     * đang chạy (`KachiKeyService`, `attachBaseContext`), tức đổi hành vi để cho gọn bảng. Android box B2 · W2c:
     * `simple_cast_prefs` (chiếu cụm) rời bảng cùng mã của nó.
     */
    val PREFS_FILES: Map<String, String> = mapOf(
        "clusternav_prefs" to "tệp CHÍNH của ClusterNav (`Prefs.FILE`)",
        "clusternav_theme" to "chỉ chứa `theme_choice`; `ThemeMode` cố ý để riêng vì nó phải đọc được ở attachBaseContext",
        "clusternav_lang" to "chỉ chứa `lang`; chỗ lưu ngôn ngữ DÙNG CHUNG cho cả APK (`Lang`)",
    )

    /**
     * Khoá của ClusterNav **có mặt trên UI Kachi** → tệp prefs chứa nó.
     *
     * Gồm cả khoá đi-kèm ([COMPANION_KEYS]) vì câu hỏi ở đây là *"khoá này ở tệp nào"*, không phải *"mục nào sở
     * hữu"* — câu sau đã có [SettingsCatalog.groupOf].
     */
    val KEYS: Map<String, String> = buildMap {
        // ── clusternav_prefs (Prefs.kt + các tệp hàm mở rộng `Prefs*.kt`) ──
        // Android box B2 · W4 — khoá dẫn đường cụm/HUD (`enabled` · `nav_cluster_screen_mode` · `marquee`), biển báo tốc độ
        // (`badge_*`) và bong bóng VietMap (`vm_bubble_*`) rời bảng cùng mã; lượt dọn một lần xoá chúng khỏi máy
        // ([BydDeadPrefs]).
        listOf(
            "voicekey_enabled", "voicekey_bindings", "voicekey_custom_buttons", "voicekey_learn",
            "headless_autostart",
            // V1 pha NÓI · R4 (spec `kachi-voice-feedback.html` T9) — hai công tắc của đường ra TIẾNG. Khoá nằm
            // cùng tệp với `voice_mic_pill` (cũng của `Prefs`), nên "cấu hình giọng nói ở đâu" có một câu trả lời.
            "voice_speak_replies", "voice_prefer_offline",
            // V3 (spec `kachi-voice-fast-natural.html`) — ba khoá của đợt "nhanh + tự nhiên". `voice_ask_aloud`
            // RỜI [HIDDEN_KEYS] sang đây ở 1.66: nó nay có hàng thật trong mục *"Hỏi xác nhận trước khi chạy"*,
            // đúng như dòng lý do cũ đã hẹn (*"đi cùng batch chọn nút nào phải hỏi"*).
            "voice_mic_source", "voice_confirm_ids", "voice_ask_aloud",
            // ⚠ `voice_keep_log` (H2 · 1.69) đã RỜI bảng này sang [HIDDEN_KEYS] ở bản release 2026-09-21: ô tích của
            // nó gỡ khỏi Cài đặt cùng mọi bề mặt dev/log, mà bảng này chỉ nhận khoá **có mặt trên UI**. Khoá vẫn
            // sống (mặc định BẬT, `VoiceUtteranceLog` vẫn ghi) — chỉ không còn hàng nào để bấm.
            // App dẫn đường mặc định (owner 2026-09-18) — cùng tệp `clusternav_prefs` với mọi khoá giọng nói.
            "voice_nav_default_app",
            // App nhạc mặc định (owner 2026-09-21) — cùng tệp, cùng lẽ với app dẫn đường.
            "voice_music_default_app",
            // AUTOMATION (1.85, spec kachi-automation) — khoá CẤU HÌNH của lịch tự dẫn. Khai ở `PrefsAutomation.kt` (hàm mở
            // rộng của `Prefs`, cùng tệp `clusternav_prefs` — tách vì trần 500 dòng, xem KDoc tệp đó).
            // ⚠ `nav_automation_fired` KHÔNG ở đây: nó là TRẠNG THÁI CHẠY, khai ở `SettingsCatalog.NOT_SETTINGS`.
            "nav_automation_rules",
            // V8 (owner 2026-09-25) — công tắc *"Tự động cập nhật"*. Chủ là mục `system_update` (hàng *Kiểm tra
            // cập nhật* của nhóm Hệ thống nay có một công tắc **và** một nút) — xem [COMPANION_KEYS].
            "auto_update_enabled",
        ).forEach { put(it, "clusternav_prefs") }
        // ── hai tệp một-khoá ──
        put("theme_choice", "clusternav_theme")
        put("lang", "clusternav_lang")
    }

    /**
     * Khoá ĐI KÈM → mã mục đặt nó. Đây là ca *"một điều khiển ghi hai khoá"*, không phải ca *"khoá không ai nhận"*.
     *
     * Bất biến "một khoá đúng một chủ" của [SettingsCatalog] vẫn nguyên: chủ là **mục**, và mục đó ghi cả cặp trong
     * MỘT lượt (cùng lập luận đã dùng cho cặp cờ boot ở [SettingsCatalog.NOT_SETTINGS]).
     */
    val COMPANION_KEYS: Map<String, String> = mapOf(
        // IA v2 · R3 — chip sáng/tối của Kachi ghi CẢ `theme_mode` (nguồn sự thật của launcher) lẫn `theme_choice`
        // (màn nâng cao đọc ở attachBaseContext). Một khái niệm, một công tắc, hai chỗ lưu vì hai màn đọc khác nhau.
        "theme_choice" to "display_theme",
        // ── V8 (owner 2026-09-25) — công tắc *"Tự động cập nhật"* ──
        // Chủ là `system_update`, mục vốn KHÔNG có khoá (nó là một VIỆC LÀM: nút *Kiểm tra cập nhật*). Nay hàng đó
        // có hai nửa của cùng một việc — *tự* dò và *tự tay* dò — nên mục ấy nhận khoá của nửa thứ nhất. Không mở
        // mục mới: một hàng, hai nửa của cùng một việc.
        "auto_update_enabled" to "system_update",
    )

    /**
     * Khoá của ClusterNav **cố ý KHÔNG lên UI**, kèm lý do tại chỗ.
     *
     * Cùng khuôn [SettingsCatalog.NOT_SETTINGS]: không có danh sách này thì không phân biệt được *"quên gom"* với
     * *"cố ý không gom"*, và câu trả lời cho *"sao Settings mới thiếu cái này"* sẽ phải đi tìm lại trong mã.
     *
     * Phạm vi: đúng những khoá mà kiểm kê §4.3 xét qua. Khoá trạng-thái-máy (`disclaimer_shown`) chưa bao giờ là một dòng
     * cài đặt. Android box B2 · W4: sáu khoá ép-giá-trị của dẫn đường cụm (`interpolate` · `acc_booster` · `lane` ·
     * `source_mode` · `anim_opt` · `hud`) rời bảng cùng mã — lượt dọn một lần xoá chúng ([BydDeadPrefs]).
     */
    val HIDDEN_KEYS: Map<String, String> = mapOf(
        // owner 2026-09-21 (bản release production) — RỜI [KEYS] sang đây: ô tích *Giữ nhật ký lượt nói* và nút
        // *Xuất nhật ký voice* gỡ khỏi Cài đặt cùng mọi bề mặt dev/debug/log, nên khoá này nay là ca kinh điển của
        // bảng này — **còn sống, cố ý không có UI**. (Chiều ngược lại của `voice_ask_aloud`, khoá đã rời bảng này
        // sang [KEYS] ở 1.66 khi nó có hàng thật.)
        "voice_keep_log" to
            "H2 (1.69) — giữ nhật ký lượt nói, **mặc định BẬT**; `VoiceUtteranceLog` vẫn ghi + tự dọn (30 mục / " +
                "30 MB) như trước, chỉ hàng bấm là mất. Tắt ghi luôn thì buổi RE sau cắm máy vào sẽ không còn " +
                "nhật ký của những lượt nói TRƯỚC đó — mất đúng thứ nhật ký sinh ra để giữ. Đọc/ghi qua cầu kiểm " +
                "thử (`prefs_set --es key voice_keep_log --es text true|false`), lấy zip bằng `voice_dump`",
        "voice_confirm_default_v286" to
            "FIX286 · SR5 (2.86) — mốc ĐI KÈM `voice_confirm_ids`, không phải một lựa chọn: \"tập đã lưu là lựa " +
                "chọn thật của người dùng kể từ khi mặc định có mở-cửa-sổ-trời\". Hàm ghi tập đặt nó trong cùng " +
                "lượt `edit()`; không có ô nào để bấm vì người dùng chỉ thấy (và chỉ cần thấy) các ô tích của tập",
        "voice_follow_up_ms" to
            "V3 · R9 — quãng GIỮ MICRO sau khi trả lời xong, cho câu tiếp (owner D1: 5 giây). Không lên UI vì " +
                "công tắc người dùng thật sự cần là *bật/tắt* hội thoại, còn con số thì là một hằng ĐO trên " +
                "cabin này (đủ để nói tiếp, không đủ để nghe nhầm một câu của người ngồi cạnh). Bày một ô nhập " +
                "mili-giây ra là mời đặt 30 000 và để micro mở suốt chuyến. Tắt hội thoại = đặt 0 qua cầu kiểm " +
                "thử; nếu owner muốn một công tắc thật thì nó là một mục MỚI, không phải ô số này",
        "inputd_disabled" to
            "1.69 — ép CHẠM trong ô đi đường lùi theo cử chỉ (tắt daemon bơm chạm). Là một công tắc ĐO, không " +
                "phải một lựa chọn: nó chỉ tồn tại để máy ảo diễn được đúng nhánh mà xe đang mắc kẹt ([ĐO xe " +
                "2026-09-16] daemon không lên lần nào). Bày ra UI là mời người dùng tự làm chạm của mình chậm " +
                "đi mà không hiểu vì sao; chỗ đặt đúng của nó là `run-as` trên bản vehicleTest",
        // ── H5 + [P0-2] (1.69) — NĂM núm chỉnh bộ nghe, cố ý KHÔNG lên UI ──
        // Cùng lý do [voice_follow_up_ms]: chúng là hằng ĐO trên cabin, không phải sở thích. Chỗ chỉnh đúng của
        // chúng là cầu kiểm thử (`prefs_set`, danh sách trắng) trong một lượt lên xe — đổi số, nói lại một câu,
        // đọc `KachiVoiceTiming`. Bày năm ô nhập số ra màn Cài đặt của một chiếc xe đang chạy là mời người ta
        // chỉnh mù rồi kết luận nhầm rằng mô hình kém.
        "voice_endpoint_silence_ms" to
            "V3 · R2 — im bao lâu thì chốt câu (600–1500 ms, mặc định 800 = hằng cũ). Đặt ngắn quá thì cắt giữa " +
                "hai vế một câu ghép; dài quá thì người lái ngồi chờ. Chỉ đo được trên cabin thật đang chạy",
        "voice_endpoint_min_speech_ms" to
            "V3 · R2 — phải có ngần này tiếng cộng dồn mới được phép chốt (200–1000 ms, mặc định 400 = hằng cũ). " +
                "Đây là cổng chặn 'chốt vì một tiếng cạch'; ngưỡng của nó phụ thuộc mức ồn cabin, không phụ " +
                "thuộc người lái",
        "voice_endpoint_floor_cap" to
            "[P0-2] TRẦN của mức nền đo được (40–400, mặc định 90). Núm QUAN TRỌNG NHẤT của lượt xe kế tiếp: " +
                "[ĐO xe 2026-09-16] một cửa sổ đo nền bị nhiễm bởi chính tiếng bíp của Kachi cho ra nền 531/602/" +
                "888 (cá biệt 4317) trong khi giọng thật chỉ rms 206–627 ⇒ ngưỡng nằm TRÊN giọng và 189/300 lượt " +
                "nghe không bao giờ nhận ra tiếng nói. Trần này giới hạn hậu quả; nới nó lên là mở lại đúng lỗi ấy",
        "voice_beam" to
            "V2 — bề rộng chùm giải mã sherpa (4 hoặc 8, mặc định 4 = hằng cũ). Beam là chi phí NHÂN LÊN ở mỗi " +
                "khung: [ĐO xe] một câu 8 s đã mất 2,35 s để giải mã với beam 4. Cho một ô nhập tự do là mời gõ " +
                "16 trên một chiếc xe đang chạy rồi kết luận nhầm rằng mô hình chậm",
        "voice_hotword_score" to
            "V2 — điểm biasing hotwords (2.0–4.0, mặc định 3.0 = hằng cũ). Hai đầu dải là hai kiểu hỏng khác " +
                "nhau: thấp quá thì biasing không kéo được `pin`/`tắt`/`âm lượng` về, cao quá thì nó CHÈN lệnh " +
                "vào câu tự do. Đúng thứ phải đo bằng tai trên cabin, không phải chọn bằng cảm giác trong Cài đặt",
    )
}
