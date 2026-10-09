package com.kachi.box.launcher

import android.content.Context
import android.view.KeyEvent
import android.widget.LinearLayout
import com.kachi.box.R

/**
 * Nhóm **"Phím vật lý"** (IA v2 §4.1 nhóm 7) — công tắc nhận nút vật lý · trạng thái dịch vụ Hỗ trợ · danh sách
 * gán (xoá từng dòng) · thêm gán · học phím mới · danh sách nút tự học.
 *
 * ## Hai danh sách phải DỰNG LẠI được, nên chúng nằm trong hai khối con
 * Xoá một gán / học thêm một nút làm **đổi nội dung danh sách**, mà trang Cài đặt thì được nhớ lại
 * ([SettingsPanel.pages]) nên nó không tự dựng lại. Mỗi danh sách vì thế là một `LinearLayout` con riêng
 * ([bindingList] / [buttonList]) và chỉ khối đó được dựng lại — giữ nguyên chỗ đang cuộn, đúng lý do R1 bắt vỏ
 * bảng giữ chính thực thể `ScrollView`.
 *
 * ## Nhãn NÚT và nhãn ĐÍCH tra ở đâu
 * Cầu trả **mã** ([ButtonOption] `code` + `customName?`, [TargetOption] `spec` + `appLabel?`), không trả câu —
 * xem KDoc [ClusterNavBridge]. Quy ước ở đây:
 *  - nút **preset** (`customName == null`) ⇒ tra tài nguyên theo mã ([presetLabel]); nút **tự học** ⇒ hiện đúng
 *    tên người dùng đã đặt (chữ của họ, không dịch);
 *  - đích là **app** (`appLabel != null`) ⇒ tên do `PackageManager` dịch; đích **sentinel** ⇒ tra tài nguyên
 *    theo **thứ tự khai** của cầu ([sentinelLabel]) — xem KDoc hàm đó để biết vì sao không so chuỗi `__ASSIST__`.
 */
class SettingsKeysSection(
    private val context: Context,
    private val rows: SettingsRows,
    private val deps: SettingsDeps,
) {

    private val bridge get() = deps.bridge

    private lateinit var statusRow: SettingsRows.StatusRow
    private val bindingList = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val buttonList = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    /**
     * Phiên "học phím" của **section này** đang mở hay không — xem [learn]/[dispose].
     *
     * Có cờ chứ không gọi [ClusterNavBridge.stopLearn] vô điều kiện lúc đóng bảng: `stopLearn` gỡ listener của
     * bus DÙNG CHUNG và hạ cờ `voicekey_learn`. Gọi khi mình chưa từng học là cướp phiên học của chỗ khác (màn
     * nâng cao cũng dùng đúng bus đó) — đúng họ lỗi "lệnh không có phạm vi tường minh" của CLAUDE.md §4.
     */
    private var learning = false

    fun build(body: LinearLayout) {
        body.addView(rows.sectionLabel(context.getString(R.string.kachi_sec_keys)))
        body.addView(rows.checkRow(
            on = bridge.voiceKeyEnabled(),
            title = context.getString(R.string.kachi_keys_enabled_title),
            sub = context.getString(R.string.kachi_keys_enabled_sub),
        ) { on -> bridge.setVoiceKeyEnabled(on, grant = !(on && byHand())) { refreshStatus() } })
        statusRow = rows.statusRow(KachiTheme.MUT2, "")
        body.addView(statusRow.view)
        refreshStatus()
        body.addView(rows.button(context.getString(R.string.kachi_keys_check)) {
            if (!byHand()) bridge.checkFix { refreshStatus() }
        })

        body.addView(rows.subHeader(context.getString(R.string.kachi_sub_keys_bindings)))
        body.addView(bindingList)
        rebuildBindings()
        body.addView(rows.button(context.getString(R.string.kachi_keys_add)) { addBinding() })

        body.addView(rows.subHeader(context.getString(R.string.kachi_sub_keys_custom)))
        body.addView(buttonList)
        rebuildButtons()
        body.addView(rows.button(context.getString(R.string.kachi_keys_learn)) { learn() })
    }

    // ── Trạng thái dịch vụ ───────────────────────────────────────────────────────────────────────

    /**
     * `BOX-KEYS-NOSHELL-TOAST` — kênh shell đã đo là không có ⇒ đường dadb chỉ ra toast "bấm Cho phép gỡ lỗi USB" (sai
     * trên box không adb). Thay bằng ĐƯỜNG TAY: câu chỉ việc + mở màn Trợ năng của hệ thống (đường B3 sẵn có). `true` =
     * đã đi đường tay, bên gọi KHÔNG chạy dadb. Có kênh / đang dò / adbd đang hỏi ⇒ `false`, y như cũ.
     */
    private fun byHand(): Boolean {
        if (!NoShellFallback.accessibilityByHand(ShellAccessUi.usableNow(), com.kachi.box.ShellReadiness.phase())) return false
        refreshStatus()   // vừa bật tay ở màn hệ thống rồi quay về ⇒ dòng trạng thái đọc lại, không mở lại màn vô cớ
        if (bridge.accessibilityBound()) return true
        android.widget.Toast.makeText(context, R.string.kachi_keys_manual, android.widget.Toast.LENGTH_LONG).show()
        val activity = context as? android.app.Activity
        if (activity == null || !SystemSettingsOpener.open(activity, ManualFix.ACCESSIBILITY_SETTINGS)) {
            android.widget.Toast.makeText(context, R.string.kachi_perm_open_failed, android.widget.Toast.LENGTH_LONG).show()
        }
        return true
    }

    /**
     * Ba trạng thái, ba màu — luật ba ca nằm ở `:core` ([ClusterNavSettingsModel.voiceKeyStatus] / cầu trả
     * [VoiceKeyStatus]); chỗ này chỉ dịch mã sang chữ + màu.
     *
     * "MẤT KẾT NỐI" là ca hay gặp nhất sau khi khởi động lại xe (service ENABLED trong setting nhưng chưa
     * bound), nên câu của nó **nói luôn cách chữa**: bấm nút Kiểm tra / Sửa ngay ngay phía dưới.
     */
    private fun refreshStatus() {
        val (colour, text) = when (bridge.voiceKeyStatus()) {
            VoiceKeyStatus.OFF -> KachiTheme.MUT2 to R.string.kachi_keys_status_off
            VoiceKeyStatus.ACTIVE -> KachiTheme.GREEN to R.string.kachi_keys_status_active
            VoiceKeyStatus.DISCONNECTED -> KachiTheme.RED to R.string.kachi_keys_status_broken
        }
        statusRow.update(colour, context.getString(text))
    }

    // ── Danh sách GÁN ────────────────────────────────────────────────────────────────────────────

    /**
     * ⚠ [SOÁT SENIOR 2026-09-13] Hai bảng tra đọc **một lần** cho cả danh sách, không một lần mỗi dòng.
     *
     * [ĐO] `ClusterCast.listInstalledApps` (nguồn của `bridge.targetOptions()`) chạy `queryIntentActivities` rồi
     * `loadLabel` + `loadIcon` cho **mọi** app có launcher. Bản trước gọi `buttonLabel`/`targetLabel` trong vòng
     * lặp, mà mỗi hàm đó lại gọi lại `bridge.buttonOptions()`/`targetOptions()` ⇒ N dòng gán = **2N lượt duyệt
     * toàn bộ app đã cài, trên luồng vẽ**, mỗi lần xoá một dòng lại làm lại. Trên đầu xe đó là đơ thấy được.
     */
    private fun rebuildBindings() {
        bindingList.removeAllViews()
        val bindings = bridge.bindings()
        if (bindings.isEmpty()) {
            bindingList.addView(rows.note(context.getString(R.string.kachi_keys_no_bindings)))
            return
        }
        val buttons = bridge.buttonOptions()
        val targets = bridge.targetOptions()
        bindings.forEach { binding ->
            bindingList.addView(rows.listRow(
                title = buttonLabel(binding.keyCode, buttons),
                sub = targetLabel(binding.targetSpec, targets),
                actionLabel = context.getString(R.string.kachi_delete),
            ) {
                bridge.removeBinding(binding.keyCode)
                rebuildBindings()
            })
        }
    }

    /**
     * "Thêm gán" = hai bước: **chọn nút** rồi **chọn đích**. Hộp thoại thứ hai chỉ mở sau khi bước một xong, nên
     * huỷ giữa chừng không ghi gì.
     *
     * Hai bước chứ không một màn hai cột: trên xe, một danh sách dài một cột bấm dễ hơn hẳn; và [ĐO] màn cũ dùng
     * hai `Spinner` cạnh nhau — cùng số bước, nhưng `Spinner` không nói ra là mình bấm được.
     */
    private fun addBinding() {
        val buttons = bridge.buttonOptions()
        SettingsDialogs.pick(
            context,
            context.getString(R.string.kachi_keys_pick_button),
            buttons.map { optionLabel(it) },
            context.getString(R.string.kachi_keys_no_buttons),
        ) { buttonIndex ->
            val option = buttons[buttonIndex]
            pickTarget { spec ->
                bridge.addBinding(option.code, spec)
                rebuildBindings()
            }
        }
    }

    /**
     * Bước 2 chọn ĐÍCH: app hoặc trợ lý (danh sách cầu `targetOptions`). Android box B2 · W1 (2026-10-09): bỏ hai loại đích
     * BYD của bước này — NHÓM nút xe (`ctl:<nút>:<việc>`, FIX286 · R-KC) và *Camera theo yêu cầu* (`cam:…`, 2.93). Dòng gán
     * cũ mang hai mã ấy vẫn hiện tên ([targetLabel]) để người dùng nhận ra và xoá.
     */
    private fun pickTarget(onSpec: (String) -> Unit) = pickApp(onSpec)

    private fun pickApp(onSpec: (String) -> Unit) {
        val targets = bridge.targetOptions()
        SettingsDialogs.pick(
            context,
            context.getString(R.string.kachi_keys_pick_target),
            targets.map { targetOptionLabel(it, targets) },
            context.getString(R.string.kachi_keys_no_targets),
        ) { targetIndex -> onSpec(targets[targetIndex].spec) }
    }

    // ── Nút TỰ HỌC ───────────────────────────────────────────────────────────────────────────────

    private fun rebuildButtons() {
        buttonList.removeAllViews()
        val custom = bridge.customButtons()
        if (custom.isEmpty()) {
            buttonList.addView(rows.note(context.getString(R.string.kachi_keys_no_custom)))
            return
        }
        custom.forEach { button ->
            buttonList.addView(rows.listRow(
                title = button.name,
                sub = context.getString(R.string.kachi_key_code, button.keyCode),
                actionLabel = context.getString(R.string.kachi_delete),
            ) {
                bridge.removeCustomButton(button.keyCode)
                rebuildButtons()
            })
        }
    }

    /**
     * "Học phím mới": mở bus học → người dùng bấm nút vật lý → hỏi tên → lưu.
     *
     * ⚠ **Tên lưu phải giữ khuôn `"<tên> (mã <code>)"` của màn cũ** — [ĐO] KDoc
     * `ClusterNavBridge.addCustomButton`: màn cũ tự ghép chuỗi đó rồi lưu nguyên, còn cầu không được mang chữ
     * nên khuôn ấy nay là tài nguyên `kachi_key_custom_name` của launcher. Lệch khuôn thì cùng một nút hiện hai
     * tên khác nhau ở hai màn.
     *
     * Android box B2 · W1/W2f — không còn số đo nguồn phím (2.88 · R1, HAL BYD): nút học chỉ có tên + mã.
     *
     * [ClusterNavBridge.stopLearn] gọi ngay sau khi nhận được mã: bus chỉ giữ **một** listener, để treo là
     * phiên Settings sau (hoặc màn cũ) không học được nữa.
     *
     * ⚠⚠ [SOÁT SENIOR 2026-09-13] Và phải gỡ cả khi người dùng **KHÔNG** bấm nút nào: mở "Học phím mới" rồi đóng
     * Cài đặt là ca thường gặp nhất (bấm nhầm, hoặc không tìm ra nút trên vô-lăng). Không gỡ thì
     * `VoiceKeyLearnBus` giữ mãi một listener trỏ về `ui` của Activity ĐÃ HUỶ (rò cả màn hình), và cờ
     * `voicekey_learn` nằm lại `true` — tức xe vẫn ở chế độ học sau khi người dùng đã bỏ đi. Đường gỡ là
     * [dispose], được [SettingsSections.dispose] gọi khi bảng rời khỏi cây view.
     */
    private fun learn() {
        learning = true
        bridge.startLearn { code ->
            learning = false
            bridge.stopLearn()
            SettingsDialogs.askName(
                context,
                context.getString(R.string.kachi_keys_learn_name),
                bridge.defaultLearnName(code),
            ) { name ->
                bridge.addCustomButton(context.getString(R.string.kachi_key_custom_name, name, code), code)
                rebuildButtons()
            }
        }
    }

    /**
     * Bảng Cài đặt đóng ⇒ đóng nốt phiên học còn treo.
     *
     * Chỉ làm gì khi CHÍNH section này đang học ([learning]) — xem KDoc cờ đó. Gọi nhiều lần an toàn.
     */
    fun dispose() {
        if (!learning) return
        learning = false
        bridge.stopLearn()
    }

    // ── Nhãn ─────────────────────────────────────────────────────────────────────────────────────

    /** Nhãn một dòng trong danh sách chọn NÚT. */
    private fun optionLabel(option: ButtonOption): String =
        option.customName ?: presetLabel(option.code)

    /**
     * Nhãn của một mã phím ĐÃ GÁN — tra trong cùng bảng mà hộp chọn dùng, để hai chỗ không hiện khác nhau.
     *
     * [buttons] truyền VÀO (không tự gọi `bridge.buttonOptions()`): xem KDoc [rebuildBindings].
     */
    private fun buttonLabel(code: Int, buttons: List<ButtonOption>): String =
        // 2.93 · KEY-LABEL-PRESET-SHADOW — nút tự học trùng mã thắng preset (tra cũ lấy mục khớp ĐẦU TIÊN = preset).
        buttons.labelOwner(code)?.let { optionLabel(it) } ?: presetLabel(code)

    /**
     * Tên của một mã phím preset. Mã lạ (nút tự học đã bị xoá, hoặc dữ liệu cũ) ⇒ ghép từ tên hằng của framework
     * + mã số: nó KHÔNG đẹp, nhưng nó **nhận ra được** — người dùng còn biết dòng nào để xoá.
     */
    private fun presetLabel(code: Int): String = when (code) {
        328 -> context.getString(R.string.kachi_key_mic_hold)
        231 -> context.getString(R.string.kachi_key_voice_assist)
        219 -> context.getString(R.string.kachi_key_assist)
        85 -> context.getString(R.string.kachi_key_play_pause)
        88 -> context.getString(R.string.kachi_key_prev)
        87 -> context.getString(R.string.kachi_key_next)
        79 -> context.getString(R.string.kachi_key_headset)
        5 -> context.getString(R.string.kachi_key_call)
        84 -> context.getString(R.string.kachi_key_search)
        else -> context.getString(
            R.string.kachi_key_unknown, KeyEvent.keyCodeToString(code).removePrefix(KEYCODE_PREFIX), code,
        )
    }

    /**
     * Nhãn một ĐÍCH: app thì dùng tên hệ thống dịch; sentinel thì tra tài nguyên.
     *
     * [targets] truyền VÀO để [sentinelLabel] khỏi gọi lại `bridge.targetOptions()` — xem KDoc [rebuildBindings].
     */
    private fun targetOptionLabel(option: TargetOption, targets: List<TargetOption>): String =
        option.appLabel ?: sentinelLabel(option.spec, targets)

    /**
     * [targets] truyền VÀO (không tự gọi `bridge.targetOptions()`): xem KDoc [rebuildBindings].
     * Android box B2 · W2b/W3: đích camera cũ (`cam:…`, 2.93) và nút xe cũ (`ctl:…`, FIX286) không còn bộ đọc nhãn ⇒ nguyên
     * chuỗi (vẫn nhận ra dòng để xoá).
     */
    private fun targetLabel(spec: String, targets: List<TargetOption>): String =
        targets.firstOrNull { it.spec == spec }?.let { targetOptionLabel(it, targets) } ?: spec

    /**
     * Tên các mục đặc biệt (ba mục cũ + *"Kachi nghe"* của V1 pha NGHE), tra theo **THỨ TỰ KHAI** của `ClusterNavBridge.targetOptions()` chứ không so chuỗi
     * `"__ASSIST__"`.
     *
     * ## Vì sao không so chuỗi
     * Ba sentinel là hằng của `Prefs` (phía ClusterNav). Viết lại chúng ở đây là **bản sao thứ hai của một khoá
     * lưu bền** — đổi tên hằng bên kia thì nhãn ở đây im lặng lùi về hiện nguyên chuỗi `__ASSIST__` cho người
     * dùng, và không bài canh nào thấy. Còn `import Prefs` thì section chạm thẳng lớp lưu của ClusterNav, đúng
     * thứ N2 cấm (`ClusterNavSettingsWiringContractTest`). Thứ tự khai thì cầu **đã ghim bằng KDoc** và
     * `ClusterNavKeysContractTest` canh — mượn lại phép ghim đó là rẻ nhất và không tạo bản sao nào.
     */
    private fun sentinelLabel(spec: String, targets: List<TargetOption>): String {
        val sentinels = targets.filter { it.appLabel == null }.map { it.spec }
        return when (sentinels.indexOf(spec)) {
            0 -> context.getString(R.string.kachi_key_target_assist)
            1 -> context.getString(R.string.kachi_key_target_gemini)
            2 -> context.getString(R.string.kachi_key_target_recognizer)
            3 -> context.getString(R.string.kachi_key_target_kachi_voice)
            else -> spec
        }
    }

    private companion object {
        /** Tiền tố hằng mã phím của framework (`KEYCODE_VOICE_ASSIST`) — cắt đi cho dòng chữ đỡ ồn. */
        const val KEYCODE_PREFIX = "KEYCODE_"
    }
}
