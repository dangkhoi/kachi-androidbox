package com.byd.clusternav

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.byd.clusternav.launcher.HomeViewModelFactory
import com.byd.clusternav.launcher.KachiLog
import com.byd.clusternav.launcher.PrefsWorkspaceRepository
import com.byd.clusternav.launcher.WorkspaceRepository
import com.byd.clusternav.system.ShellTransport
import com.byd.clusternav.system.WindowCommandDispatcher
import com.byd.clusternav.system.inputd.InputDaemonClient

/**
 * ĐỒ THỊ DI thủ công DUY NHẤT cho tiến trình launcher (Kachi) — B5 (part 2). Sở hữu/giữ (lazy) các process-singleton
 * PHÍA LAUNCHER thành MỘT đồ thị mạch lạc do [KachiApplication] khởi tạo:
 *  • [shellTransport] — chủ DUY NHẤT của kết nối dadb localhost:5555 (mọi lệnh cửa sổ/cast).
 *  • [windowDispatcher] — cổng ownership display (kèm `DisplayOwnershipRegistry` + `AppLocationRegistry` bên trong)
 *    chạy TRÊN [shellTransport].
 *  • [workspaceRepository] — tầng-dữ-liệu HOME (bọc `WorkspacePrefs`).
 *  • [inputDaemonClient] — client input-daemon dùng chung mọi ô; seam khởi động = [windowDispatcher] launcher-seam.
 *
 * `ShellTransport.get(ctx)` / `WindowCommandDispatcher.get(ctx)` NAY UỶ QUYỀN về đây (một chủ thật sự) — mọi caller cũ
 * (cast, `DadbShell`, `FreeformSeed`…) chạy y nguyên, chỉ khác là instance đến TỪ đồ thị này (thay các singleton
 * `X.get(ctx)` rải rác).
 *
 * Android box B2 · W2c/W3 (2026-10-09): `castRuntime` (chiếu cụm) và lớp dữ liệu + điều khiển xe (`halGateway` ·
 * `carData` · `carControl` · `carStatusRepository` · `carDemand` · `refreshForRead` · `readFresh` · `telemetryText` ·
 * `forgetCarDemand`) gỡ cùng lõi HAL BYDAuto.
 *
 * ── Vì sao init-lambda? ──────────────────────────────────────────────────────────────────────────────────────
 * Container dựng qua các init-lambda `by lazy` → nhánh KHÔNG cần Android (repository giả / [homeViewModelFactory]) test
 * JVM được off-device mà không phải chạm [shellTransport]/[windowDispatcher] (chỉ dựng khi được TRUY CẬP). Đồ thị Android-đầy-đủ verify bằng `:app:assembleDebug` (compile + wire).
 */
class AppContainer internal constructor(
    private val shellTransportInit: () -> ShellTransport,
    private val windowDispatcherInit: (ShellTransport) -> WindowCommandDispatcher,
    private val workspaceRepositoryInit: () -> WorkspaceRepository,
    private val inputDaemonClientInit: (WindowCommandDispatcher) -> InputDaemonClient?,
) {
    /** Chủ DUY NHẤT của kết nối dadb window/cast — [ShellTransport.get] uỷ quyền về đây. */
    val shellTransport: ShellTransport by lazy { shellTransportInit() }

    /** Cổng ownership display chạy trên [shellTransport] — [WindowCommandDispatcher.get] uỷ quyền về đây. */
    val windowDispatcher: WindowCommandDispatcher by lazy { windowDispatcherInit(shellTransport) }

    /** Tầng-dữ-liệu HOME (bọc `WorkspacePrefs`), nguồn cho [HomeViewModel]. */
    val workspaceRepository: WorkspaceRepository by lazy { workspaceRepositoryInit() }

    /** Client input-daemon dùng chung mọi ô (null nếu không đọc được đường APK). Seam = [windowDispatcher]. */
    val inputDaemonClient: InputDaemonClient? by lazy { inputDaemonClientInit(windowDispatcher) }

    /** Factory chuẩn AndroidX cấp [HomeViewModel] nối [workspaceRepository] + cờ [embedded] runtime. */
    fun homeViewModelFactory(embedded: Boolean): ViewModelProvider.Factory =
        HomeViewModelFactory(workspaceRepository, embedded)

    companion object {
        @Volatile private var instance: AppContainer? = null

        /** Container tiến-trình DUY NHẤT (tạo lười, thread-safe). [KachiApplication] gọi sớm để đồ thị sẵn sàng. */
        fun get(context: Context): AppContainer {
            val app = context.applicationContext
            return instance ?: synchronized(this) {
                instance ?: build(app).also { instance = it }
            }
        }

        private fun build(app: Context): AppContainer = AppContainer(
            shellTransportInit = { ShellTransport.createOwned(app) },
            windowDispatcherInit = { transport -> WindowCommandDispatcher.createOwned(transport) },
            workspaceRepositoryInit = { PrefsWorkspaceRepository(app) },
            inputDaemonClientInit = { dispatcher -> buildInputDaemonClient(app, dispatcher) },
        )

        /**
         * ⚠ 1.69 — hai dây MỚI, cả hai đều là đầu dò cho bệnh *"chạm trong ô hỏng"*
         * ([ĐO xe 2026-09-16] `docs/diagnostics/oncar-trace-2026-09-16b/README.md` §9.1):
         *  • `logDir` = thư mục log **ngoài thẻ** của [KachiLog] — chỗ mà tiến trình shell uid-2000 GHI ĐƯỢC
         *    (thư mục riêng `/data/data/<pkg>/files` là 0700 của app-uid; đổ redirect vào đó là chính lệnh khởi
         *    động chết vì *Permission denied*). Nhờ nó stderr của daemon rơi vào `kachi-logs/inputd-<stamp>.log`,
         *    cùng một lệnh `adb pull` với `usage-*.log`.
         *  • `disabled` = công tắc ẩn `inputd_disabled`, đọc **MỘT lần** mỗi tiến trình: nó được hỏi trên mỗi cú
         *    chạm, và một lượt đọc `SharedPreferences` mỗi sự kiện chạm là đúng kiểu chi phí mà 1.67 vừa dọn.
         *    Đổi công tắc ⇒ khởi động lại app (ghi bằng `run-as` thì vốn đã phải force-stop trước).
         */
        private fun buildInputDaemonClient(app: Context, dispatcher: WindowCommandDispatcher): InputDaemonClient? {
            val apk = runCatching { app.applicationInfo.sourceDir }.getOrNull()
            if (apk.isNullOrEmpty()) return null
            val forced = runCatching { Prefs.inputdDisabled(app) }.getOrDefault(false)
            // 1.70 — kênh TCP loopback: cổng theo uid + token theo cài đặt (xem KDoc `InputDaemonClient.port`).
            val uid = runCatching { app.applicationInfo.uid }.getOrDefault(0)
            val token = runCatching { Prefs.inputdToken(app) }.getOrDefault("")
            return InputDaemonClient(
                apkPath = apk,
                launchShell = dispatcher.launcherSeam(),
                port = com.byd.clusternav.system.inputd.InputDaemonLaunch.portFor(uid),
                token = token,
                logDir = { runCatching { KachiLog.dir(app)?.absolutePath }.getOrNull() },
                disabled = { forced },
            )
        }
    }
}
