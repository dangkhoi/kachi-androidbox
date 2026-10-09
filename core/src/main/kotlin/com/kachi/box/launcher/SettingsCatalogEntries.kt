package com.kachi.box.launcher

/**
 * DỮ LIỆU của [SettingsCatalog.ENTRIES] — tách khỏi [SettingsCatalog] vì trần 500 dòng (CLAUDE.md §4.1), không vì
 * lý do kiến trúc: đây vẫn là **một** danh mục, chỉ là thân dữ liệu của nó nằm ở tệp riêng với các phép kiểm.
 *
 * ## Vì sao mỗi mục của ClusterNav có một dòng chú thích chỉ tới điều khiển ở màn cũ
 * IA v2 gộp màn ClusterNav vào Kachi bằng cách **dựng lại điều khiển, ghi cùng khoá** — không trích hàm khỏi
 * `MainActivity.kt` (tệp đó bị ~10 bài wiring test ghim source, còn `activity_main.xml` thì bị hash-seal T11). Nên
 * trong một thời gian **hai màn cùng sửa một khoá**. Dòng `// <R.id ở màn cũ> · <API ghi>` cạnh mỗi mục là sợi dây
 * duy nhất nối hai bề mặt đó: người dựng section mới (T4) biết phải bắt chước điều khiển nào, và người gỡ màn cũ
 * (OQ1) biết mỗi id XML sắp xoá đã có ai thay chưa. Không có nó thì "đã gộp đủ chưa" lại thành câu hỏi đếm bằng mắt.
 */
internal object SettingsCatalogEntries {

    /** MỌI mục cài đặt, theo thứ tự nhóm rồi thứ tự hiện ra trong nhóm. */
    val ALL: List<SettingsEntry> = LAUNCHER() + CLUSTER_NAV()

    // ── Phía LAUNCHER: khoá nằm trong `kachi_workspace` (trừ `lang`) ─────────────────────────────

    /**
     * Trong nhóm [SettingsGroup.HOME] thứ tự đi từ **cả bộ** ra **từng phần**, rồi từ **khung** ra **nội dung**:
     * chọn bố cục trước thì các lựa chọn sau mới có nghĩa.
     */
    private fun LAUNCHER(): List<SettingsEntry> = listOf(
        // ── Màn hình chính ──
        // ⚠⚠ S4 · R1 — BA MỤC "CẢNH" ĐÃ XOÁ khỏi đây: `home_scenes` (khoá `scenes`), `home_scene_boot` (khoá
        // `boot_scene`) và `home_scene_save` (nút lưu, không khoá). Owner 2026-09-14: *"có cảnh rồi có hồ sơ nữa hơi
        // khó hiểu"* — và backlog P7 đã ghi hai dòng đó tả **cùng một** khái niệm. Chức năng lên cấp chứ không mất:
        // mỗi cảnh cũ thành một hồ sơ cùng tên (R2, `ScenesMigration` ở `:core`), còn "cảnh lúc nổ máy" thành
        // "profiles_boot" dưới nhóm Hồ sơ tài xế (R6). Hai khoá `scenes`/`boot_scene` bị gỡ khỏi `WorkspacePrefs`
        // cùng lượt, nên `SettingsCoverageContractTest` không còn khoá nào mồ côi.
        SettingsEntry("home_preset", SettingsGroup.HOME, "Bố cục sẵn", "preset", "Preset layout"),
        SettingsEntry("home_grid", SettingsGroup.HOME, "Bố cục tự vẽ", "grid_layout", "Custom layout"),
        // Không lưu gì: đây là NÚT mở bảng vẽ. Bố cục vẽ ra thì lưu ở "home_grid" phía trên — một khoá, một chủ.
        SettingsEntry("home_grid_editor", SettingsGroup.HOME, "Vẽ bố cục riêng…", labelEn = "Draw your own layout…"),
        // 2.87 · R-AH3 (owner 03/10) — nút ⇄ của KHUNG tự ẩn: nói về chính các khung vừa chọn ở trên, trước nội dung (hình nền).
        SettingsEntry("home_swap_autohide", SettingsGroup.HOME, "Tự ẩn nút ⇄", "swap_button_autohide", "Auto-hide the ⇄ button"),
        SettingsEntry(
            "home_wallpaper", SettingsGroup.HOME, "Hình nền & trình chiếu",
            "wallpaper_prefs", "Wallpaper & slideshow",
        ),

        // ── Thanh trạng thái & thanh nút (IA v2 · R-UI a — tách khỏi Màn hình chính) ──
        // ⚠ Mã mục đổi `home_*` → `bars_*` cùng lúc với nhóm. Kiểm trước khi đổi: [ĐO] grep `home_top_strip` /
        // `home_dock_edge` / `home_dock_items` trên cả `:app` và `:core` — **0 chỗ tra theo mã mục** (chỉ
        // `home_scene_save` bị `SceneWiringContractTest` tra), nên đổi mã ở đây không phá dây nối nào. KHOÁ lưu bền
        // thì giữ nguyên (`top_strip`/`dock_edge`/`dock_enabled`) — đổi khoá là mất cấu hình của người đang dùng.
        // Android box B2 · W1 — `bars_top_strip` (khoá `top_strip`) + `bars_top_strip_labels` (khoá `top_strip_labels`) gỡ:
        // mọi chip thanh trạng thái là chip dữ liệu xe BYD. W4: khoá chết dọn một lần — [BydDeadPrefs].
        // UX-OVERHAUL · WP4 (owner 2026-09-20) — *"cho user CHỌN VỊ TRÍ item BÊN TRONG header + taskbar"*. Đứng SAU
        // hai mục *chọn chip* vì nó sắp lại thứ vừa chọn: sắp chỗ cho một vật chưa có mặt là một câu hỏi vô nghĩa.
        SettingsEntry(
            "bars_header_order", SettingsGroup.BARS, "Vị trí trên thanh trên",
            "header_order", "Status-bar item order",
        ),
        // Viền TRƯỚC danh sách nút: thứ tự khai ở đây LÀ thứ tự hiện ra, và mục "nút trên thanh" là lưới 123 ô. Khai
        // ngược lại thì muốn đổi viền phải cuộn qua hết lưới — thứ tự danh mục phải là thứ tự dùng được, không chỉ
        // là thứ tự nghe hợp lý khi đọc danh sách.
        // S1b — ẩn/hiện thanh nút. TRƯỚC danh sách nút + viền vì "có hiện không" là câu hỏi đầu tiên; ẩn rồi thì
        // viền/nút bên dưới không còn tác dụng ngay, nhưng vẫn để lộ ra để đặt sẵn cho lần hiện lại.
        SettingsEntry("bars_dock_visible", SettingsGroup.BARS, "Hiện thanh nút", "dock_visible", "Show the button bar"),
        SettingsEntry("bars_dock_edge", SettingsGroup.BARS, "Viền đặt thanh nút", "dock_edge", "Button bar edge"),
        SettingsEntry("bars_dock_items", SettingsGroup.BARS, "Nút trên thanh nút", "dock_enabled", "Buttons on the button bar"),
        // UX-OVERHAUL · WP4 — sắp lại chỗ đứng của những nút vừa chọn.
        //
        // ⚠⚠ `prefKey = null` là **BẮT BUỘC**, không phải bỏ sót: thứ tự nút LÀ thứ tự của `dock_enabled`
        // ([DockConfig.moveEnabled]) và [SettingsCatalog] ép bất biến *"một khoá chỉ thuộc ĐÚNG một mục"*
        // ([ĐO] khai `dock_enabled` lần thứ hai ở đây ⇒ `ExceptionInInitializerError` *"khoá hai chủ"*, 45 bài
        // đỏ). Đúng tiền lệ `home_grid_editor`: một mục **sửa** giá trị mà mục khác **sở hữu**.
        SettingsEntry(
            "bars_dock_order", SettingsGroup.BARS, "Vị trí trên thanh nút",
            null, "Button-bar item order",
        ),
        // F1 (owner 01/10, spec shortcuts-autostart R1.4) — app nào hiện trên khối lối tắt (thanh nút) + widget
        // `w_apps`, mỗi app một kiểu mở. Ở nhóm THANH vì chỗ người dùng gặp khối này đầu tiên là chính thanh nút.
        SettingsEntry("bars_app_shortcuts", SettingsGroup.BARS, "Lối tắt ứng dụng", "app_shortcuts", "App shortcuts"),

        // ── Hiển thị & đơn vị ──
        // Android box B2 · W1 — `display_units` (khoá `unit_prefs`) gỡ: đơn vị chỉ dùng cho dữ liệu xe. W4: khoá dọn một lần — [BydDeadPrefs].
        // [ĐO] kiểm kê S1 §2: khoá này lưu bền, có enum + có đường ghi, nhưng TRƯỚC S1 không có nút nào chạm tới.
        // IA v2 · R3 — cùng một chip nay ghi THÊM `theme_choice` (tệp `clusternav_theme`) để màn nâng cao theo cùng
        // lựa chọn; khoá thứ hai đó khai ở [SettingsCatalog.CLUSTERNAV_COMPANION_KEYS], không mở mục riêng.
        SettingsEntry("display_theme", SettingsGroup.DISPLAY, "Giao diện sáng/tối", "theme_mode", "Light / dark theme"),
        // VISUAL-REFRESH P1b · R8 — màu nhấn (8 ô + theo ảnh nền) và tông thẻ, theo hồ sơ; mã hoá ở `ColorChoice`.
        SettingsEntry("display_color", SettingsGroup.DISPLAY, "Màu sắc", "color_choice", "Colours"),
        // 2.89 · B3 DOCK-SCALE (owner 05/10 *"50-150% đi"*) — cỡ thanh nút theo %, theo hồ sơ (`dock_scale`, mã hoá `BarScale`).
        SettingsEntry("display_bar_scale", SettingsGroup.DISPLAY, "Cỡ thanh nút", "dock_scale", "Button bar size"),
        // U5·T3 — NGÔN NGỮ. ⚠ Khoá `lang` KHÔNG nằm trong tệp `kachi_workspace` mà trong tệp lưu ngôn ngữ đã có của
        // ClusterNav (`clusternav_lang`, `com.kachi.box.Lang`) — cố ý, để một APK chỉ có MỘT công tắc ngôn ngữ
        // thay vì hai cái lệch nhau; lập luận đầy đủ ở KDoc `WorkspacePrefs.langMode`.
        SettingsEntry("display_lang", SettingsGroup.DISPLAY, "Ngôn ngữ", "lang", "Language"),
        // UX-OVERHAUL WP1 · R1.3 — công tắc "Kính thật (làm mờ nền)". Khoá `ui_glass_real` nằm ở `clusternav_prefs`
        // (qua `Prefs`, THEO XE — xem `ProfileScope.DEVICE_KEYS`), không ở `kachi_workspace`, nên nó KHÔNG có bản
        // theo-hồ-sơ; control đi qua `deps.bridge.setGlassReal` như `system_headless_autostart`. Ở nhóm Hiển thị vì
        // nó là **cách trình bày** (chất liệu bề mặt), đúng định nghĩa nhóm DISPLAY.
        SettingsEntry(
            "display_glass_real", SettingsGroup.DISPLAY, "Kính thật (làm mờ nền)",
            "ui_glass_real", "Real glass (blur the backdrop)",
        ),

        // ── Hồ sơ tài xế ──
        // S4 · R3: từ đây một hồ sơ giữ **tất cả** lựa chọn của người dùng (xem `ProfileScope`), nên nhóm này không
        // còn là "một danh sách tên" mà là chỗ đổi cả bộ cấu hình. Thứ tự khai = thứ tự hiện ra: danh sách (việc
        // hằng ngày) → hồ sơ lúc nổ máy (đặt một lần) → thêm hồ sơ (hiếm hơn nữa).
        SettingsEntry("profiles_list", SettingsGroup.PROFILES, "Danh sách hồ sơ", "profiles", "Profile list"),
        SettingsEntry("profiles_active", SettingsGroup.PROFILES, "Hồ sơ đang dùng", "active_profile", "Active profile"),
        // S4 · R6 — thay cho "home_scene_boot". ⚠ Khoá `boot_profile` là khoá **theo XE**, không mang tiền tố hồ sơ
        // (R4): nó trả lời *"máy lên bằng hồ sơ nào"*, nên cất nó bên trong một hồ sơ là vòng tròn. Rỗng = "hồ sơ
        // dùng gần nhất" (mặc định), tức giữ nguyên hành vi trước S4.
        SettingsEntry(
            "profiles_boot", SettingsGroup.PROFILES, "Hồ sơ lúc nổ máy", "boot_profile", "Profile on engine start",
        ),
        // Không lưu gì: đây là NÚT tạo. Danh sách hồ sơ thì nằm ở "profiles_list" phía trên — một khoá, một chủ
        // (cùng lối với "home_grid_editor" và "home_grid"). ⚠ Nhãn nói rõ **bản sao**: từ R3 một hồ sơ trắng nghĩa
        // là mất sạch mọi thứ người dùng đã chỉnh, nên "thêm" ở đây luôn là nhân bản hồ sơ đang dùng (R8).
        SettingsEntry(
            "profiles_add", SettingsGroup.PROFILES, "Thêm hồ sơ (bản sao)", labelEn = "Add profile (a copy)",
        ),

        // ── Sổ địa chỉ (spec `kachi-voice-addresses.html`) — nằm trong nhóm DẪN ĐƯỜNG ──
        // ⚠ Khoá ở phía LAUNCHER (`kachi_workspace`, theo hồ sơ) dù mục hiện trong nhóm [SettingsGroup.NAV] — đó là
        // lý do hai dòng này khai ở đây chứ không ở khối CLUSTER_NAV bên dưới (khối đó toàn khoá của `Prefs`).
        // Nhóm chọn theo **thứ người dùng đang nghĩ tới** (KDoc [SettingsGroup]), không theo tệp lưu: người ta vào
        // *Dẫn đường* để sửa địa chỉ nhà, không vào *Hồ sơ tài xế* — dù sổ đi theo hồ sơ.
        SettingsEntry("places_list", SettingsGroup.NAV, "Sổ địa chỉ", "saved_places", "Address book"),
        // Không lưu gì: đây là NÚT thêm/sửa (một khoá, một chủ — cùng lối "home_grid_editor" / "profiles_add").
        SettingsEntry("places_add", SettingsGroup.NAV, "Thêm địa chỉ…", labelEn = "Add an address…"),
    )

    // ── Phía CLUSTERNAV: khoá nằm trong `clusternav_prefs` / `simple_cast_prefs` ─────────────────

    /**
     * Mục dựng lại từ màn ClusterNav (IA v2 · §4.3). Mỗi dòng chú thích nói **điều khiển nào ở màn cũ** làm đúng
     * việc đó, để T4 dựng lại mà không phải đọc lại cả `MainActivity.kt` 1386 dòng.
     */
    private fun CLUSTER_NAV(): List<SettingsEntry> = listOf(
        // ── Dẫn đường ──
        // Android box B2 · W1 — gỡ `nav_enabled` · `nav_cluster_mode` · `nav_marquee` (dẫn đường lên cụm/HUD BYD).
        // App dẫn đường MẶC ĐỊNH (owner 2026-09-18): nói "dẫn đường" không nêu app ⇒ dùng cái này.
        SettingsEntry(
            "nav_default_app", SettingsGroup.NAV, "App dẫn đường mặc định",
            "voice_nav_default_app", "Default navigation app",
        ),
        // AUTOMATION #2 (1.85, spec kachi-automation R2.1) — sổ luật "tự dẫn đường theo lịch". Cùng nhóm NAV với
        // sổ địa chỉ vì nó CHỌN điểm đến từ sổ đó; control ở tệp section riêng (`SettingsSectionsAutomation`).
        SettingsEntry(
            "nav_automation", SettingsGroup.NAV, "Tự dẫn đường theo lịch",
            "nav_automation_rules", "Scheduled navigation",
        ),
        // Android box B2 · W1 — gỡ `nav_reconnect`, biển báo tốc độ (`badge_*`), bong bóng VietMap (`vm_bubble_*`) và CẢ
        // nhóm Chiếu màn lên cụm (`cast_*`): phần chỉ-BYD. W4: khoá chết dọn một lần — [BydDeadPrefs].

        // ── Phím vô-lăng ──
        // switch_voicekey_enabled · Prefs.setVoiceKeyEnabled + KeyServiceConnect.grantAccessibility(reset=true)
        SettingsEntry("keys_enabled", SettingsGroup.KEYS, "Nhận nút vật lý", "voicekey_enabled", "Listen to physical buttons"),
        // list_voicekey_bindings + btn_binding_remove · Prefs.add/removeVoiceKeyBinding (JSON [{k,t,s?}] — s 2.88)
        SettingsEntry("keys_bindings", SettingsGroup.KEYS, "Danh sách gán nút", "voicekey_bindings", "Button bindings"),
        // spinner_voicekey_button + btn_voicekey_add · Prefs.add/removeVoiceKeyCustomButton (MainActivity.kt:819)
        SettingsEntry(
            "keys_custom_buttons", SettingsGroup.KEYS, "Nút tự học thêm",
            "voicekey_custom_buttons", "Self-learned buttons",
        ),
        // btn_voicekey_learn · Prefs.setVoiceKeyLearn(true) rồi chờ VoiceKeyLearnBus (MainActivity.kt:919)
        SettingsEntry("keys_learn", SettingsGroup.KEYS, "Học phím mới", "voicekey_learn", "Learn a new key"),
        // btn_voicekey_recheck + txt_voicekey_status · refreshVoiceKeyStatus (MainActivity.kt:998–1016) — VIỆC LÀM
        SettingsEntry("keys_check", SettingsGroup.KEYS, "Kiểm tra và sửa ngay", labelEn = "Check and fix now"),

        // Android box B2 · W1 — gỡ CẢ nhóm Tiện nghi xe (`car_*`: lấy gió trong, ghế, lọc bụi, tự sấy kính): HAL BYD.

        // ── Giọng nói: ĐỌC phản hồi (spec `kachi-voice-feedback.html` R4 · T9) ──
        // Ba mục đứng cạnh hàng *Nhận dạng giọng nói* trong nhóm Hệ thống (§Nâng cao) vì chúng là hai nửa của
        // MỘT việc: cái tai (mô hình nghe) và cái miệng (gói đọc + hai công tắc). DEBT-CAT-2: bề mặt đã vẽ thì
        // phải có mục danh mục, nếu không rail nói một đằng mà trang có một nẻo.
        // "Hey Kachi" — công tắc nghe câu gọi rảnh tay. Khoá thật `voice_wake_enabled` nằm ở `clusternav_prefs`
        // (ghi qua ClusterNavBridge.setWakeEnabled), KHÔNG khai ở đây (prefKey=null) — như các công tắc bridge khác.
        SettingsEntry("voice_wake", SettingsGroup.VOICE, "\"Hey Kachi\" gọi bằng giọng", labelEn = "\"Hey Kachi\" wake word"),
        SettingsEntry(
            "voice_speak_replies", SettingsGroup.VOICE, "Đọc phản hồi bằng giọng",
            "voice_speak_replies", "Speak replies out loud",
        ),
        SettingsEntry(
            "voice_prefer_offline", SettingsGroup.VOICE, "Ưu tiên giọng offline",
            "voice_prefer_offline", "Prefer the offline voice",
        ),
        // App nhạc mặc định (owner 2026-09-21) — cùng lẽ `nav_default_app` ở nhóm NAV; khoá `clusternav_prefs`.
        SettingsEntry(
            "voice_music_default_app", SettingsGroup.VOICE, "App nhạc mặc định",
            "voice_music_default_app", "Default music app",
        ),
        // F3 "Tự mở nhạc khi lên xe" — 2.89 · A5(a): đã sang nhóm SYSTEM (`system_ignition_music`, ngay dưới `system_ignition_apps`).
        // Không lưu khoá: đây là NÚT tải/gỡ gói giọng (cùng lối `profiles_add` / `system_default_home`). Gói nằm
        // trên đĩa của chính xe này, trạng thái đọc từ đĩa (`VoiceModelStore.isReady`) — không có pref nào để nhớ.
        SettingsEntry("voice_tts_pack", SettingsGroup.VOICE, "Giọng đọc offline", labelEn = "Offline voice pack"),
        // 2.74 · R3 — danh sách câu nói được ở CUỐI nhóm. Mục THÔNG TIN: `prefKey = null` vì không có gì để lưu
        // (trạng thái gập/mở cố ý không bền — xem KDoc `SettingsRowsDisclosure`), nhưng nó vẫn phải có mặt ở danh
        // mục IA v2: rail đếm số mục của nhóm, và `SettingsCatalogControlContractTest` canh hai chiều *"mục ⇔ có
        // control thật trên màn"*. Một bề mặt đã vẽ mà không có mục là một hàng lậu.
        SettingsEntry("voice_commands", SettingsGroup.VOICE, "Câu lệnh nói được", labelEn = "Spoken commands"),
        // 2.91 VOICE-APP-NAMES (spec §4.5) — khoá hồ sơ `voice_app_names`; trang danh sách mọi app + hộp dạy bằng giọng.
        SettingsEntry("voice_app_names_list", SettingsGroup.VOICE, "Dạy tên app", "voice_app_names", "Teach app names"),
        // ── V3 · "nhanh + tự nhiên" (spec `kachi-voice-fast-natural.html`) ──
        // R7 — mục liệt kê MỌI việc có thể hỏi lại, mỗi việc một ô tích; mặc định KHÔNG tích cái nào (owner
        // 2026-09-16: *"cái nào nguy hiểm lái xe mới hỏi, chứ mở cửa hỏi làm gì"*).
        SettingsEntry(
            "voice_confirm_ids", SettingsGroup.VOICE, "Hỏi xác nhận trước khi chạy",
            "voice_confirm_ids", "Ask before running",
        ),
        // OQ4 — đứng NGAY dưới mục trên, vì nó chỉ có nghĩa khi có ít nhất một việc được tích: nó quyết định câu
        // hỏi ấy có được ĐỌC LÊN hay chỉ hiện chữ.
        SettingsEntry(
            "voice_ask_aloud", SettingsGroup.VOICE, "Đọc to câu hỏi xác nhận",
            "voice_ask_aloud", "Read confirmation questions aloud",
        ),
        // R1 — nguồn micro. Ở nhóm Hệ thống cạnh hàng *Nhận dạng giọng nói*: nó là một tính chất của PHẦN CỨNG
        // xe này, không phải một sở thích; và nó tồn tại để đo được từng nguồn trên đường mà không build lại.
        SettingsEntry(
            "voice_mic_source", SettingsGroup.VOICE, "Nguồn micro",
            "voice_mic_source", "Microphone source",
        ),
        // ── H2/H6 (1.69) — nhật ký lượt nói + đổi mô hình nghe ──
        //
        // ⚠ `voice_keep_log` (ô tích *Giữ nhật ký lượt nói*) + `voice_log_export` (nút *Xuất nhật ký voice*) đã XOÁ
        // 2026-09-21 (owner, bản release production): `VoiceModelSettings.logRows` — nơi DỰNG cả hai — đã gỡ cùng
        // lượt dọn mọi bề mặt dev/debug/log khỏi màn Cài đặt. Danh mục này là bản đồ **của màn hình**, và
        // `SettingsCatalogControlContractTest` canh hai chiều *"mục ⇔ điều khiển có thật"*, nên để mục ở lại là
        // dựng một lời hứa rỗng ở rail — đúng bệnh mà cả danh mục sinh ra để chữa.
        //
        // KHẢ NĂNG thì KHÔNG mất, chỉ bề mặt mất — và khoá vẫn còn nguyên chủ ở chỗ khác:
        //   • `voice_keep_log` vẫn **mặc định BẬT**, `VoiceUtteranceLog` vẫn ghi; khoá chuyển sang
        //     [SettingsCatalogClusterNav.HIDDEN_KEYS] (*"cố ý không có UI"*, kèm lý do) và đọc/ghi qua
        //     `prefs_set --es key voice_keep_log`;
        //   • xuất zip vẫn là lệnh cầu `voice_dump` (`TestBridgeVoiceDump` → cùng `VoiceUtteranceLog.exportZip`).
        //
        // `voice_model_light` (hai nút *chuyển sang mô hình nhẹ* / *gỡ bản nặng*) cũng rời danh mục CÙNG LƯỢT, vì
        // lý do khác hai mục trên: nó không phải đồ đo, nó là một bề mặt **chọn mô hình** — và danh mục mô hình
        // nghe nay chỉ còn ĐÚNG MỘT gói (`SherpaModelCatalog.ALL`, owner chốt dừng thử nghiệm), nên không còn gì
        // để chọn giữa. `VoiceModelSettings.lightModelRows` — nơi dựng hai nút — đã gỡ. Hàng *trạng thái + Tải/Gỡ*
        // của gói duy nhất thì Ở LẠI (mục `voice_model` không tồn tại từ trước: nó thuộc khối dựng tay cùng
        // `voice_tts_pack`).
        // ── Hệ thống & quyền ──
        // Không lưu gì: hàng quyền chỉ ĐỌC trạng thái thật rồi tự xin lại (xem [LauncherRequirements]).
        SettingsEntry("system_permissions", SettingsGroup.SYSTEM, "Quyền còn thiếu", labelEn = "Missing permissions"),
        // [ĐO] kiểm kê S1 §2: khoá thứ hai không có đường tới trước S1 — chỉ được đọc/ghi trong mã.
        SettingsEntry(
            "system_autostart", SettingsGroup.SYSTEM, "Tự mở Kachi khi nổ máy",
            "launcher_autostart", "Auto-start Kachi on engine start",
        ),
        // cb_headless_autostart · Prefs.setHeadlessAutostart. ⚠ KHÁC `launcher_autostart` ngay trên: cái kia mở
        // **màn hình** Kachi, cái này chạy **dịch vụ** dẫn đường/cụm nền. Hai nghĩa khác nhau ⇒ hai công tắc, nhưng
        // phải đứng cạnh nhau với câu chữ nói đúng việc (IA v2 · R3), không thì trông như một cái bị lặp.
        SettingsEntry(
            "system_headless_autostart", SettingsGroup.SYSTEM, "Chạy dịch vụ nền khi nổ máy",
            "headless_autostart", "Run background service on engine start",
        ),
        // F2 (owner 01/10, spec shortcuts-autostart R2.1) — app mở khi nổ máy, đứng cạnh hai công tắc khởi động: cả ba trả
        // lời "nổ máy thì Kachi làm gì". Khoá `ignition_apps` theo hồ sơ (S4).
        SettingsEntry("system_ignition_apps", SettingsGroup.SYSTEM, "Mở app khi nổ máy", "ignition_apps", "Open apps at ignition"),
        // F3 (owner 01/10, spec shortcuts-autostart R3.1) — nhạc khi lên xe. 2.89 · A5(a) (spec `kachi-289-field-fixes.html`): rời
        // nhóm Giọng nói (id cũ `voice_ignition_music`, chỉ là id danh mục — không lưu ở đâu), đứng NGAY dưới "Mở app khi nổ máy":
        // cùng câu hỏi "nổ máy thì Kachi làm gì". KHÔNG dùng chung khoá với "App nhạc mặc định" (miền giá trị khác — §4.6).
        SettingsEntry("system_ignition_music", SettingsGroup.SYSTEM, "Tự mở nhạc khi lên xe", "ignition_music", "Play music when you get in"),
        // ── Màn hình chính (S5) ──
        // btn_set_home · ClusterNavBridge.setDefaultHome — VIỆC LÀM (không lưu khoá): ROM BYD KHÔNG hiện hộp chọn
        // HOME khi bấm nút Home, nên đây là đường đặt được duy nhất. Nút gọi `cmd package set-home-activity` qua
        // dadb uid-shell ([ĐO] DiLink3.0 2026-09-14 ⇒ Success). Trạng thái ("đang là"/"chưa — hệ thống dùng <gói>")
        // ĐỌC qua PackageQueries, không shell.
        SettingsEntry("system_default_home", SettingsGroup.SYSTEM, "Màn hình chính", labelEn = "Home screen"),
        // cb_keep_home_on_boot · ClusterNavBridge.setKeepHomeOnBoot. Nổ máy thì đặt lại HOME một lần nếu ROM reset
        // (mặc định TẮT — [SUY] chưa đo ROM có reset không, chờ P7 trên xe). Khoá `keep_home_on_boot` là **theo XE**
        // ([ProfileScope.DEVICE_KEYS]): màn hình chính là thuộc tính của cả xe, không phải của một tài xế.
        SettingsEntry(
            "system_keep_home_on_boot", SettingsGroup.SYSTEM, "Giữ Kachi làm màn hình chính khi nổ máy",
            "keep_home_on_boot", "Keep Kachi as home screen on engine start",
        ),
        // btn_check_update · VIỆC LÀM
        SettingsEntry("system_update", SettingsGroup.SYSTEM, "Kiểm tra cập nhật", labelEn = "Check for updates"),
        // Android box B2 · W1 — `system_nav_stop` (dừng mọi đầu ra dẫn đường lên cụm/HUD) gỡ.
        // ⚠ `system_advanced_screen` (mở màn ClusterNav cũ) đã XOÁ 2026-09-13 — màn đó bị gỡ hẳn
        // (docs/specs/kachi-remove-legacy-screen.html R1, đóng OQ1 của IA v2). Không có mục thay thế: mọi cấu
        // hình của nó đã nằm ở các nhóm nav/cast/keys/car từ IA v2 và ghi đúng cùng khoá.
        //
        // ⚠ `system_vietmap_data` (btn_vietmap_widget_diag) + `system_diagnostics` (cast_diagnostics/DiagActivity)
        // đã XOÁ 2026-09-21 (owner, bản release production): mọi bề mặt dev/debug/log gỡ khỏi màn Cài đặt, còn lại
        // ĐÚNG `system_test_bridge` ngay dưới. Danh mục này canh *"mục khai ở đây phải có một điều khiển trên
        // màn"* (`SettingsCatalogControlContractTest`) ⇒ giữ mục cho một hàng không còn tồn tại là để rail hứa một
        // trang trống. Hai màn ấy vẫn mở được qua adb (`am start -n <gói>/<lớp Activity>`), tức KHẢ NĂNG không mất,
        // chỉ bề mặt mất — cùng lẽ với `voice_keep_log`/`voice_log_export` ở nhóm Giọng nói trên.
        // T-BRIDGE · `KachiTestBridge` — công tắc mở **cầu kiểm thử qua adb** (docs/specs/kachi-test-bridge.html).
        // ⚠ Khoá `test_bridge_until` là TRANSIENT, không phải một sở thích: nó tự hết hạn sau 60 phút và chết theo
        // lần nổ máy (xem `TestBridgeWindow`). Đứng ở "Nâng cao" cạnh hai màn chẩn đoán vì cùng loại — một chỗ ĐO,
        // không phải một bề mặt cấu hình.
        SettingsEntry(
            "system_test_bridge", SettingsGroup.SYSTEM, "Chế độ kiểm thử qua adb",
            "test_bridge_until", "ADB test mode",
        ),

        // ── Giới thiệu ──
        SettingsEntry("about_version", SettingsGroup.ABOUT, "Phiên bản và giấy phép", labelEn = "Version and licence"),
        // maybeShowDisclaimer (MainActivity.kt:620) — hộp thoại một-lần, gác bằng `disclaimer_shown`. Ở đây là bản
        // ĐỌC LẠI bất cứ lúc nào; khoá `disclaimer_shown` vẫn thuộc màn cũ (trạng thái "đã hiện chưa", không phải
        // một lựa chọn) nên mục này KHÔNG nhận khoá.
        SettingsEntry("about_disclaimer", SettingsGroup.ABOUT, "Miễn trừ trách nhiệm", labelEn = "Disclaimer"),
    )
}
