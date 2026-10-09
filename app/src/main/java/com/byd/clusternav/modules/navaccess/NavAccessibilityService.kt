package com.byd.clusternav.modules.navaccess

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.byd.clusternav.AppContainer
import com.byd.clusternav.Prefs
import com.byd.clusternav.modules.voicekey.AssistantLauncher
import com.byd.clusternav.modules.voicekey.KeySourceRecorder
import com.byd.clusternav.modules.voicekey.VoiceKeyLearnBus
import com.byd.clusternav.voicekey.KeySample
import com.byd.clusternav.voicekey.KeySourceLookup
import com.byd.clusternav.voicekey.KeySourceReading
import com.byd.clusternav.voicekey.VoiceKeyAction
import com.byd.clusternav.voicekey.VoiceKeyBindings
import com.byd.clusternav.voicekey.VoiceKeyConfig
import com.byd.clusternav.voicekey.KeyLearnTail
import com.byd.clusternav.voicekey.VoiceKeyMatcher

/**
 * Dịch vụ Hỗ trợ của Kachi — CHỈ còn NÚT VẬT LÝ → trợ lý / app / Kachi nghe ([onKeyEvent]).
 *
 * Android box B2 · W2d (2026-10-09): bộ đọc màn Google Maps (cự ly tới rẽ tinh chỉnh nội suy cho cụm/HUD BYD) gỡ cùng
 * dẫn đường cụm; `res/xml/nav_accessibility_config.xml` không còn xin đọc cửa sổ lẫn nhận sự kiện. Tên lớp GIỮ (đổi tên =
 * component mới ⇒ máy đã cài mất quyền Hỗ trợ — `docs/_handoff/androidbox-b2-brief.md`).
 */
class NavAccessibilityService : AccessibilityService() {

    // T3: nút vật lý → trợ lý giọng nói. Matcher thuần ở :core; service chỉ map KeyEvent + phóng intent.
    private val voiceKeyMatcher = VoiceKeyMatcher()

    // QA 2.87 [P3] — phần còn lại (DOWN lặp + UP) của lần nhấn vừa HỌC: nuốt nốt, không để UP mồ côi tới app media.
    private val learnTail = KeyLearnTail()

    // L7 · KEY-SOURCE-SPLIT tầng 1 — CHỈ ĐO nguồn phím (chữ ký + feature HAL đánh dấu nguồn), không đổi khớp/gán.
    // Sống theo một lần bind: dựng ở onServiceConnected, dừng ở onUnbind/onDestroy.
    private var keySource: KeySourceRecorder? = null

    override fun onServiceConnected() {
        NavAccessibilitySource.connected = true
        voiceKeyMatcher.reset()
        learnTail.reset()
        keySource?.stop()
        val app = applicationContext
        keySource = KeySourceRecorder(app) { AppContainer.get(app).halGateway }.also { it.start() }
        // 2.88 · KEY-SOURCE-SPLIT tầng 2 — có dòng gán theo nguồn ⇒ đọc mồi MỘT lượt trên luồng `kachi-keysrc-sync` (chỉ
        // gửi việc, main không chờ) để lần nhấn đầu không trả giá nạp bảng feature-id + getInstance.
        if (VoiceKeyBindings.anySource(Prefs.voiceKeyBindings(app))) keySource?.primeSource()
        Log.i(TAG, "accessibility key service connected")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        NavAccessibilitySource.connected = false
        keySource?.stop()
        keySource = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        keySource?.stop()
        keySource = null
        super.onDestroy()
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
        // L7 tầng 1: chép chữ ký NGAY (field nguyên thuỷ — event bị recycle sau khi hàm này trả về); ghi nhật ký ở dưới.
        val sample = if (event.action == KeyEvent.ACTION_DOWN) {
            Log.i(TAG, "onKeyEvent DOWN keycode=${event.keyCode} (${KeyEvent.keyCodeToString(event.keyCode)})")
            KeySourceRecorder.sampleOf(event)
        } else null
        val learning = Prefs.voiceKeyLearn(app)

        val action = when (event.action) {
            KeyEvent.ACTION_DOWN -> VoiceKeyAction.DOWN
            KeyEvent.ACTION_UP -> VoiceKeyAction.UP
            else -> VoiceKeyAction.OTHER
        }
        // QA 2.87 [P3] — DOWN lặp / UP của CHÍNH lần nhấn vừa học (cờ học đã tắt trên DOWN) ⇒ nuốt nốt. Trước: UP đi đường
        // thường, phím chưa gán ⇒ tới hệ thống ⇒ [ĐO máy ảo `learn88-orphan-up.log` (bằng chứng phiên, ngoài repo)] UP mồ côi bật YT Music.
        val tail = learnTail.swallow(action, event.keyCode, event.downTime)
        // 2.88 · KEY-SOURCE-SPLIT tầng 2 — chỉ đường KHỚP (không đuôi học, không đang học, công tắc bật) mới có thể đọc
        // nguồn đồng bộ. Mọi đường khác ghi nhật ký tầng 1 NGAY, y như 2.87 — TRƯỚC khi bus học phím báo mã (hộp đặt tên
        // phải thấy dòng của lần học). KHÔNG I/O, KHÔNG HAL ở đây: onKeyEvent chạy trên main, framework chỉ chờ 500 ms.
        val matching = !tail && !learning && Prefs.voiceKeyEnabled(app)
        if (sample != null && !matching) keySource?.onDown(sample, learned = learning)
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

        if (!matching) return super.onKeyEvent(event)

        // F3 (owner 2026-08-24): tra DANH SÁCH gán, không so với một mã nữa. Danh sách rỗng ⇒ mọi phím
        // pass-through (matcher trả IGNORE) ⇒ không nuốt nhầm phím nào của xe.
        val cfg = VoiceKeyConfig(enabled = true, bindings = Prefs.voiceKeyBindings(app))
        // 2.88 — matcher gọi hàm tra nguồn CHỈ trên DOWN đầu của một mã có dòng gán theo nguồn (R3); mọi mã khác đi đường
        // 2.87 y nguyên, không HAL. Số đọc (nếu có) chuyển cho nhật ký tầng 1 để không đọc HAL lần hai (R-nf2).
        var preRead: KeySourceReading? = null
        val decision = voiceKeyMatcher.onKey(cfg, action, event.keyCode, event.downTime) {
            lookupSource(sample).also { preRead = it.reading }
        }
        if (sample != null) keySource?.onDown(sample, learned = false, preRead = preRead)
        // Đích lấy TỪ quyết định (bất biến: fire ⟺ targetSpec != null) — KHÔNG tra lại prefs, tra hai lần
        // có thể ra hai kết quả nếu owner vừa sửa danh sách giữa DOWN và lúc phóng intent.
        val spec = decision.targetSpec
        if (decision.fire && spec != null) {
            Log.i(TAG, "voice-key fire → target=$spec key=${event.keyCode} src=${decision.reason ?: NO_SOURCE_READ}")
            runCatching { AssistantLauncher.launch(app, spec) }
                .onFailure { Log.e(TAG, "assistant launch failed", it) }
        } else if (!decision.consume && decision.reason != null && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            // R6 — mã có dòng gán theo nguồn mà lần nhấn này KHÔNG khớp dòng nào (vd vô-lăng khi chỉ núm được gán) ⇒ phím đi
            // tiếp; một dòng để anh em gửi log là chốt được ca "núm/vô-lăng làm sai việc".
            Log.i(TAG, "voice-key pass key=${event.keyCode} src=${decision.reason}")
        }
        return if (decision.consume) true else super.onKeyEvent(event)
    }

    /**
     * 2.88 — hàm tra nguồn mà [VoiceKeyMatcher] gọi (chỉ trên DOWN đầu, chỉ khi cần): đọc ĐỒNG BỘ qua bộ ghi, trần 100 ms
     * ([com.byd.clusternav.voicekey.KeySourceResolver]). Bộ ghi chưa chạy / không có chữ ký / lỗi bất ngờ ⇒ KHÔNG BIẾT
     * nguồn (matcher chỉ dùng dòng không nguồn) — tuyệt đối không ném ra khỏi onKeyEvent của dịch vụ giữ phím.
     */
    private fun lookupSource(sample: KeySample?): KeySourceLookup {
        val rec = keySource ?: return KeySourceLookup.NO_LOOKUP
        sample ?: return KeySourceLookup.NO_LOOKUP
        return try {
            rec.lookupSource(sample)
        } catch (e: RuntimeException) {
            Log.w(TAG, "tra nguồn phím hỏng — coi như không biết nguồn", e)
            KeySourceLookup.NO_LOOKUP
        }
    }

    /** Không nhận sự kiện nào (cấu hình không khai `accessibilityEventTypes` ⇒ eventTypes = 0); lớp gốc bắt buộc override. */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    companion object {
        private const val TAG = "NavAccess"
        /** `src=` của dòng `voice-key fire` khi lần nhấn KHÔNG tra nguồn (mã không có dòng gán theo nguồn). */
        private const val NO_SOURCE_READ = "-"
    }
}
