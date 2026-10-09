package com.byd.clusternav.modules.navaccess

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.byd.clusternav.Prefs
import com.byd.clusternav.modules.voicekey.AssistantLauncher
import com.byd.clusternav.modules.voicekey.VoiceKeyLearnBus
import com.byd.clusternav.voicekey.VoiceKeyAction
import com.byd.clusternav.voicekey.VoiceKeyConfig
import com.byd.clusternav.voicekey.KeyLearnTail
import com.byd.clusternav.voicekey.VoiceKeyMatcher

/**
 * Dịch vụ Hỗ trợ của Kachi — CHỈ còn NÚT VẬT LÝ → trợ lý / app / Kachi nghe ([onKeyEvent]).
 *
 * Android box B2 · W2d (2026-10-09): bộ đọc màn Google Maps (cự ly tới rẽ tinh chỉnh nội suy cho cụm/HUD BYD) gỡ cùng
 * dẫn đường cụm; `res/xml/nav_accessibility_config.xml` không còn xin đọc cửa sổ lẫn nhận sự kiện. Tên lớp GIỮ (đổi tên =
 * component mới ⇒ máy đã cài mất quyền Hỗ trợ — `docs/_handoff/androidbox-b2-brief.md`). W2f: bộ ghi/tra NGUỒN phím (L7 ·
 * 2.88 KEY-SOURCE-SPLIT, HAL BYD `AUDIO_VOLUME_CTRL_MODE` tách núm bệ giữa / vô-lăng) gỡ — mỗi mã phím một dòng gán.
 */
class NavAccessibilityService : AccessibilityService() {

    // T3: nút vật lý → trợ lý giọng nói. Matcher thuần ở :core; service chỉ map KeyEvent + phóng intent.
    private val voiceKeyMatcher = VoiceKeyMatcher()

    // QA 2.87 [P3] — phần còn lại (DOWN lặp + UP) của lần nhấn vừa HỌC: nuốt nốt, không để UP mồ côi tới app media.
    private val learnTail = KeyLearnTail()

    override fun onServiceConnected() {
        NavAccessibilitySource.connected = true
        voiceKeyMatcher.reset()
        learnTail.reset()
        Log.i(TAG, "accessibility key service connected")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        NavAccessibilitySource.connected = false
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {}

    /**
     * T3 — nút vật lý → trợ lý giọng nói. Chỉ chạy khi service được cấp quyền hỗ trợ + config
     * `canRequestFilterKeyEvents` + flag `flagRequestFilterKeyEvents` (xem nav_accessibility_config.xml).
     *
     * KHÔNG thay chức năng gốc: chỉ trả true (nuốt phím) cho mã phím CÓ TRONG danh sách gán của người dùng
     * — quyết định ở [VoiceKeyMatcher] (:core). Phím khác → super (pass-through).
     * "Học phím": nếu bật, ghi lại keycode nút vừa bấm (trên DOWN) rồi tự tắt cờ.
     */
    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: return super.onKeyEvent(event)
        val app = applicationContext

        // CHẨN ĐOÁN Bug 1 (owner 2026-09-01): log MỌI phím tới đây (chỉ DOWN, thưa). onKeyEvent được gọi ⟺ service
        // ĐANG bound + có cờ filter key. Dùng để chốt trên xe: (a) nút 305 (xoay màn) có TỚI accessibility không —
        // nếu bấm 305 mà KHÔNG có dòng này ⇒ hệ thống nuốt trước, KHÔNG map được (khác 328 mic tới được); (b) sau lái
        // xe bấm nút mà KHÔNG có dòng nào ⇒ service mất bound (rebind chưa phục hồi). Xem logcat tag "NavAccess".
        if (event.action == KeyEvent.ACTION_DOWN) {
            Log.i(TAG, "onKeyEvent DOWN keycode=${event.keyCode} (${KeyEvent.keyCodeToString(event.keyCode)})")
        }
        val learning = Prefs.voiceKeyLearn(app)

        val action = when (event.action) {
            KeyEvent.ACTION_DOWN -> VoiceKeyAction.DOWN
            KeyEvent.ACTION_UP -> VoiceKeyAction.UP
            else -> VoiceKeyAction.OTHER
        }
        // QA 2.87 [P3] — DOWN lặp / UP của CHÍNH lần nhấn vừa học (cờ học đã tắt trên DOWN) ⇒ nuốt nốt. Trước: UP đi đường
        // thường, phím chưa gán ⇒ tới hệ thống ⇒ [ĐO máy ảo `learn88-orphan-up.log` (bằng chứng phiên, ngoài repo)] UP mồ côi bật YT Music.
        val tail = learnTail.swallow(action, event.keyCode, event.downTime)
        if (tail) return true

        if (learning) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                Prefs.setVoiceKeyLearn(app, false)
                learnTail.learned(event.keyCode, event.downTime)
                Log.i(TAG, "learned voice keycode=${event.keyCode} (${KeyEvent.keyCodeToString(event.keyCode)})")
                VoiceKeyLearnBus.publish(event.keyCode)   // Activity (đang mở màn) hiện dialog đặt tên
            }
            return true   // nuốt trong lúc học để không kích hoạt gì khác
        }

        // KHÔNG I/O, KHÔNG HAL ở đây: onKeyEvent chạy trên main, framework chỉ chờ 500 ms.
        if (!Prefs.voiceKeyEnabled(app)) return super.onKeyEvent(event)

        // F3 (owner 2026-08-24): tra DANH SÁCH gán, không so với một mã nữa. Danh sách rỗng ⇒ mọi phím
        // pass-through (matcher trả IGNORE) ⇒ không nuốt nhầm phím nào của xe.
        // Review Pass 2 [P2]: dòng gán đích đã gỡ (nút xe `ctl:` / camera `cam:` của Kachi BYD) KHÔNG được nuốt phím.
        val cfg = VoiceKeyConfig(enabled = true, bindings = AssistantLauncher.liveBindings(Prefs.voiceKeyBindings(app)))
        val decision = voiceKeyMatcher.onKey(cfg, action, event.keyCode, event.downTime)
        // Đích lấy TỪ quyết định (bất biến: fire ⟺ targetSpec != null) — KHÔNG tra lại prefs, tra hai lần
        // có thể ra hai kết quả nếu owner vừa sửa danh sách giữa DOWN và lúc phóng intent.
        val spec = decision.targetSpec
        if (decision.fire && spec != null) {
            Log.i(TAG, "voice-key fire → target=$spec key=${event.keyCode}")
            runCatching { AssistantLauncher.launch(app, spec) }
                .onFailure { Log.e(TAG, "assistant launch failed", it) }
        }
        return if (decision.consume) true else super.onKeyEvent(event)
    }

    /** Không nhận sự kiện nào (cấu hình không khai `accessibilityEventTypes` ⇒ eventTypes = 0); lớp gốc bắt buộc override. */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    companion object {
        private const val TAG = "NavAccess"
    }
}
