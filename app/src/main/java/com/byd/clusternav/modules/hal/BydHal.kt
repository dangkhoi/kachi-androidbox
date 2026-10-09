package com.byd.clusternav.modules.hal

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager

/**
 * Hạ tầng HAL DÙNG CHUNG cho các module chạm xe IN-PROCESS (không shell-out dadb).
 * Gồm: (1) BydPermissionBypassContext (port kim.apk) — bọc Context để check*Permission→0 cho quyền BYDAUTO,
 * cho HAL getInstance chạy từ app uid không cần OEM perm; (2) reflection getInstance/set/get/probe (SDK BYDAuto
 * KHÔNG có trên classpath → reflection thuần như NavOpen.java).
 *
 * Đây là INFRA (như ModuleHost), nhiều module HAL dùng chung. Xoá hết module HAL (inprochal/tpms/vehicle)
 * → có thể xoá luôn thư mục modules/hal/. Không module nào ngoài HAL import nó.
 */
object BydHal {
    const val EV = "android.hardware.bydauto.BYDAutoEventValue"
    const val IDS = "android.hardware.bydauto.BYDAutoFeatureIds"

    // FQN device (reflection) — đủ cho các module hiện có.
    const val INSTRUMENT = "android.hardware.bydauto.instrument.BYDAutoInstrumentDevice"
    const val SETTING = "android.hardware.bydauto.setting.BYDAutoSettingDevice"
    const val TYRE = "android.hardware.bydauto.tyre.BYDAutoTyreDevice"
    const val SPEED = "android.hardware.bydauto.speed.BYDAutoSpeedDevice"
    const val GEARBOX = "android.hardware.bydauto.gearbox.BYDAutoGearboxDevice"
    const val CHARGING = "android.hardware.bydauto.charging.BYDAutoChargingDevice"
    const val ENERGY = "android.hardware.bydauto.energy.BYDAutoEnergyDevice"
    const val STATISTIC = "android.hardware.bydauto.statistic.BYDAutoStatisticDevice"
    // Điều hoà / tiện-nghi ghế (device 1023 = ghế làm-mát/sưởi). Dùng CHUNG hạ tầng device()/setInt() —
    // KHÔNG chế reflection mới; feature-id ghế + value ở :core `com.byd.clusternav.comfort.SeatComfort`.
    const val AC = "android.hardware.bydauto.ac.BYDAutoAcDevice"
    // Cảm biến bụi mịn PM2.5 (device 1008) — ĐỌC mức bụi (getPM2p5Level()[0]). Dùng CHUNG hạ tầng device()
    // + reflection getter; các method GHI "tự lọc" (enablePurificationFunctionPrompt/setAutoCleanAirState/
    // setQuickCleanAirState) nằm trên AC device, CHỈ có trên ROM xe (KHÔNG trong SDK jar) ⇒ gọi qua reflection
    // ở com.byd.clusternav.comfort.Pm25FilterApplier — KHÔNG thêm lời gọi biên dịch cho method ROM-only ở đây.
    const val PM2P5 = "android.hardware.bydauto.pm2p5.BYDAutoPM2p5Device"
    // Nguồn HƯỚNG cho dead-reckoning (recon: getInstance được không trên ROM này?): góc lái + tốc độ 4 bánh.
    const val BODYWORK = "android.hardware.bydauto.bodywork.BYDAutoBodyworkDevice"   // getSteeringWheelValue (±780°)
    const val SPECIAL = "android.hardware.bydauto.special.BYDAutoSpecialDevice"      // getWheelSpeed(area) 4 bánh → yaw
    // (ENGINE / EngineVoiceSimulator đã theo tiếng pô sang app com.byd.posound — bỏ khỏi ClusterNav)

    // Quyền KHÔNG có substring "byd" nhưng kim.apk vẫn allowlist (để bypass đầy đủ như reference).
    private val EXTRA_PERMS = setOf(
        "android.permission.WRITE_SECURE_SETTINGS", "android.permission.INJECT_EVENTS",
        "android.permission.MEDIA_CONTENT_CONTROL", "android.permission.START_ACTIVITIES_FROM_BACKGROUND")

    /**
     * Context bọc: check*Permission→0 + enforce*→no-op cho mọi quyền BYDAUTO/BYDACQUISITION/byd + EXTRA_PERMS
     * (port BydPermissionBypassContext kim.apk) VÀ getPackageName="com.byd.dashcast" + getApplicationContext=this
     * (khớp NavOpen.wrap đã CHỨNG MINH ghi được cụm — getInstance vài ROM cần spoof package này).
     */
    fun bypass(base: Context): Context = object : ContextWrapper(base) {
        private fun byd(p: String?): Boolean = !p.isNullOrBlank() &&
            (p.contains("BYDAUTO", true) || p.contains("BYDACQUISITION", true) || p.contains("byd", true) || p in EXTRA_PERMS)
        override fun checkPermission(p: String, pid: Int, uid: Int) = if (byd(p)) PackageManager.PERMISSION_GRANTED else super.checkPermission(p, pid, uid)
        override fun checkCallingPermission(p: String) = if (byd(p)) PackageManager.PERMISSION_GRANTED else super.checkCallingPermission(p)
        override fun checkCallingOrSelfPermission(p: String) = if (byd(p)) PackageManager.PERMISSION_GRANTED else super.checkCallingOrSelfPermission(p)
        override fun checkSelfPermission(p: String) = if (byd(p)) PackageManager.PERMISSION_GRANTED else super.checkSelfPermission(p)
        override fun enforcePermission(p: String, pid: Int, uid: Int, m: String?) { if (!byd(p)) super.enforcePermission(p, pid, uid, m) }
        override fun enforceCallingPermission(p: String, m: String?) { if (!byd(p)) super.enforceCallingPermission(p, m) }
        override fun enforceCallingOrSelfPermission(p: String, m: String?) { if (!byd(p)) super.enforceCallingOrSelfPermission(p, m) }
        override fun getPackageName() = "com.byd.dashcast"
        override fun getApplicationContext(): Context = this
    }

    /** System context (ActivityThread) đã bọc bypass — kiểu MapMode dùng. null nếu fail. */
    fun systemBypassContext(): Context? = runCatching {
        exemptHiddenApis()
        val at = Class.forName("android.app.ActivityThread")
        val thread = at.getMethod("currentActivityThread").invoke(null)
        (at.getMethod("getSystemContext").invoke(thread) as? Context)?.let { bypass(it) }
    }.getOrNull()

    /** getInstance(Context) của device qua reflection (thử nhiều ctx: system rồi app, đều bọc bypass). null nếu fail. */
    fun device(fqn: String, vararg ctxs: Context?): Any? {
        for (c in ctxs) {
            val d = runCatching { Class.forName(fqn).getMethod("getInstance", Context::class.java).invoke(null, c) }.getOrNull()
            if (d != null) return d
        }
        return null
    }

    // D2 (closeout 1.28): cache name→id so writeNavFrame's ~20 featureId() lookups/frame stop hitting reflection
    // on the ~4/sec hot path. Mirrors the getterCache pattern (nay ở BydHalRead). Behaviour IDENTICAL: same Int, or null when
    // the field is absent (ConcurrentHashMap can't hold null → a separate absent-set records the misses so the
    // reflect+catch runs once per name, not once per frame). Field values are static-final ints → stable.
    private val featureIdCache = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private val featureIdAbsent = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    fun featureId(name: String): Int? {
        featureIdCache[name]?.let { return it }
        if (name in featureIdAbsent) return null
        val v = runCatching { Class.forName(IDS).getField(name).getInt(null) }.getOrNull()
        if (v != null) featureIdCache[name] = v else featureIdAbsent.add(name)
        return v
    }
    /** Test-only: clear the featureId cache between tests (cache is process-global in the object). */
    internal fun resetFeatureIdCacheForTest() { featureIdCache.clear(); featureIdAbsent.clear() }

    /** Mọi field trong BYDAutoFeatureIds khớp 1 trong [subs] (substring, không phân biệt hoa thường) → (tên, id). */
    fun featureIdsMatching(vararg subs: String): List<Pair<String, Int>> = runCatching {
        Class.forName(IDS).fields
            .filter { f -> subs.any { f.name.contains(it, true) } }
            .mapNotNull { f -> runCatching { f.name to f.getInt(null) }.getOrNull() }
            .sortedBy { it.first }
    }.getOrElse { emptyList() }

    /** Ghi 1 feature int qua set(int[], EventValue). Trả rc; ném nếu không có method set. */
    fun setInt(dev: Any, id: Int, value: Int): Any? = setEv(dev, id) { ev ->
        evField("intValue").setInt(ev, value)
    }

    /** Ghi buffer (vd tên đường UTF-16LE) qua set(int[], EventValue).bufferDataValue. */
    fun setBytes(dev: Any, id: Int, bytes: ByteArray): Any? = setEv(dev, id) { ev ->
        evField("bufferDataValue").set(ev, bytes)
    }

    // D2 (closeout 1.28) reflection-handle cache: the EventValue class/ctor/fields and the device
    // set(int[],EventValue) method are STABLE per class/process. writeNavFrame formerly resolved all of them
    // ~15×/frame (getDeclaredConstructor + getField + a full dev.javaClass.methods scan). Cache once. Behaviour +
    // thrown-exception semantics are identical: a device missing set(int[],EventValue) still throws
    // NoSuchMethodException, and off-car the EV Class.forName still throws (callers already runCatching it).
    private val evClass by lazy { Class.forName(EV) }
    private val evCtor by lazy { evClass.getDeclaredConstructor() }
    private val evFieldCache = java.util.concurrent.ConcurrentHashMap<String, java.lang.reflect.Field>()
    private fun evField(name: String): java.lang.reflect.Field =
        evFieldCache[name] ?: evClass.getField(name).also { evFieldCache[name] = it }
    private val setMethodCache = java.util.concurrent.ConcurrentHashMap<String, java.lang.reflect.Method>()
    private fun setMethod(dev: Any): java.lang.reflect.Method {
        setMethodCache[dev.javaClass.name]?.let { return it }
        val set = dev.javaClass.methods.firstOrNull {
            it.name == "set" && it.parameterTypes.size == 2 && it.parameterTypes[0] == IntArray::class.java
        } ?: throw NoSuchMethodException("set(int[], EventValue)")
        setMethodCache[dev.javaClass.name] = set
        return set
    }

    private inline fun setEv(dev: Any, id: Int, fill: (Any) -> Unit): Any? {
        val ev = evCtor.newInstance()
        fill(ev)
        return setMethod(dev).invoke(dev, intArrayOf(id), ev)
    }

    /** Gọi method [name] (0/1 arg int, có thể String) → (ok, "rc=..." / lỗi). Cho probe HAL set/get/hasFeature bất kỳ. */
    fun invokeM(dev: Any, name: String, arg: Any? = null): Pair<Boolean, String> {
        val m = dev.javaClass.methods.firstOrNull {
            it.name == name && it.parameterTypes.size == (if (arg == null) 0 else 1) &&
                (arg == null ||
                    (arg is Int && it.parameterTypes[0] == Int::class.javaPrimitiveType) ||
                    (arg is String && it.parameterTypes[0] == String::class.java))
        } ?: return false to "no method $name(${arg?.javaClass?.simpleName ?: ""})"
        return runCatching { true to "${if (arg == null) m.invoke(dev) else m.invoke(dev, arg)}" }.getOrElse { false to root(it) }
    }

    /**
     * Gọi method [method] với **N tham số int** qua reflection (`getMethod(method, int×N).invoke(dev, args)`).
     * Trả `"rc=<giá trị trả về>"` khi gọi được (kể cả rc lỗi của HAL, hoặc method void → `"rc=null"`), hoặc
     * chuỗi lỗi rút gọn [root] khi ROM thiếu method / HAL ném. **Degrade-safe: KHÔNG BAO GIỜ ném.**
     *
     * Đây là ĐƯỜNG GHI method-TÊN dùng chung — cho probe HAL on-car (vehicleTest-only),
     * [com.byd.clusternav.body.BodyworkControl] (cửa sổ/cốp) và (tương lai) seat-fix. Các method
     * GHI trên device BYDAuto (`setBodyWindowCtrlState` / `setHetchDoorStatus` / `setSeatVentilatingState`…)
     * KHÔNG có trong SDK jar ⇒ chỉ gọi được qua reflection tên-method, giống cách
     * [com.byd.clusternav.comfort.Pm25FilterApplier] gọi `setAutoCleanAirState` trên AC device.
     */
    fun callNamedInt(dev: Any, method: String, vararg args: Int): String = runCatching {
        val types = Array<Class<*>?>(args.size) { Int::class.javaPrimitiveType }
        val boxed = Array<Any?>(args.size) { args[it] }
        val m = dev.javaClass.getMethod(method, *types)
        "rc=${m.invoke(dev, *boxed)}"
    }.getOrElse { root(it) }

    // ── ĐỌC qua reflection (getter theo tên · feature-id đồng bộ) — thân ở [BydHalRead], tách theo VAI (DEBT-500) ──
    // Mặt tiền giữ nguyên chữ ký cho mọi call site cũ; mỗi dòng chỉ uỷ quyền.
    fun callGetter(dev: Any, name: String, arg: Int? = null, onError: ((Throwable) -> Unit)? = null): String? =
        BydHalRead.callGetter(dev, name, arg, onError)
    fun arrayToStr(arr: Any): String = BydHalRead.arrayToStr(arr)
    fun readGetters(dev: Any, specs: List<Pair<String, Int?>>): List<String> = BydHalRead.readGetters(dev, specs)
    fun hasSyncGet(dev: Any): Boolean = BydHalRead.hasSyncGet(dev)
    fun tryGet(dev: Any, id: Int, type: Class<*> = Integer.TYPE, onError: ((Throwable) -> Unit)? = null): Any? =
        BydHalRead.tryGet(dev, id, type, onError)
    /** Sentinel "không có giá trị" của `BYDAutoEventValue` [ĐO `BYDAutoEventValue.java:5,7,12-13`]: HAL trả object
     *  với field mặc định khi feature không provision — KHÔNG phải số đo ⇒ phải coi là unavailable, không đưa lên UI. */
    const val EV_INVALID_INT = -999999999
    const val EV_INVALID_FLOAT = -1.0E9f

    fun readFeature(dev: Any, id: Int, type: Class<*> = Integer.TYPE, onError: ((Throwable) -> Unit)? = null): String? =
        BydHalRead.readFeature(dev, id, type, onError)
    /** KIỂM CHỨNG GHI (cho self-test): set 1 feature int → (ok, chi tiết). ok=true nếu set() KHÔNG ném
     *  (bắt được SecurityException/HAL chặn). LƯU Ý: "không ném" mạnh hơn getInstance-non-null nhưng vẫn
     *  chưa chắc cụm render (set có thể trả rc lỗi / no-op âm thầm) → module vẫn bảo "nhìn cụm để chắc". */
    fun writeProbe(dev: Any, featureName: String, value: Int): Pair<Boolean, String> {
        val id = featureId(featureName) ?: return false to "không có feature-id $featureName"
        return runCatching { true to "rc=${setInt(dev, id, value)}" }.getOrElse { false to root(it) }
    }

    /** SET_NAVI_SCREEN_STATUS_SET (0x4C10E015 · BYDAutoSettingDevice) chọn CHẾ ĐỘ hiển thị nav trên cụm =
     *  đúng cái menu OEM "Đơn giản / Màn hình nhỏ / Toàn màn hình / OFF" (mở khoá 2026-08-13). Giá trị:
     *  ⚠️ CHƯA map chắc trên xe — navopen dùng 3 (rc=0, ứng viên "Toàn màn hình"), AmapService reset = 3.
     *  Cần sweep 0/1/2/3 on-car để chốt value = "Đơn giản" (Giữa+ETA). Default 3 = value đã-proven rc=0. */
    const val NAV_SCREEN_MODE_ON = 3

    /** RE 2026-08-15 (insight owner "mũi tên/cự ly/tên đường có tách domestic↔oversea"): CẢ HỌ guidance có bản
     *  OVERSEA (export) song song domestic — suffix id TRÙNG, chỉ khác prefix 0x1F7 (oversea) vs 0x43F (domestic).
     *  Xe export (Seal) đọc họ 0x1F7 → HUD KHÔNG lên gì khi app chỉ ghi 0x43F. Raw-id FALLBACK vì lớp reflect
     *  `BYDAutoFeatureIds` (bản tmap) thiếu các tên oversea. Nguồn: DiCarServer Instrument.java / InstrumentMapper. */
    const val EASY_NAVI_GUIDE_OVERSEA_ID = 0x1F701010   // mũi tên — INSTRUMENT_EASY_NAVI_GUIDE_INFOR_SET (~ GUIDE_INFO_SIMPLE)
    const val CROSSING_DIST_OVERSEA_ID = 0x1F701018     // cự ly  — INSTRUMENT_DISTANCE_TARGET_HEAD_SET (~ FRONT_CROSSING_DISTANCE)
    const val PATHNAME_OVERSEA_ID = 0x1F7A1008          // tên đường — INSTRUMENT_TARGET_NEXT_PATHNAME_INFO_OVERASEA_SET (~ NEXT_PATHNAME)

    // ── TASK 5 (closeout 1.28): per-feature-id in-process rejection cache ─────────────────────────────────
    /**
     * rc mà BYDAuto HAL trả khi feature-id KHÔNG được provision trên trim này = `0x800003E8`
     * = `Int.MIN_VALUE + 1000` = **-2147482648**. Quan sát LẶP LẠI trên xe owner (Seal), KHÔNG suy đoán:
     *   `docs/diagnostics/oncar-runbook-4mode-track-a-probes-2026-08-14.md` — "rc=-2147482648 (REJECTED)";
     *   `docs/diagnostics/app-code-updates-2026-08-16.md` — "rc=-2147482648 + no permission device 1007";
     *   `scripts/vehicle/hud-provisioning-compare.sh` — "-2147482648 = NOT provisioned/no-permission".
     * ⚠ ĐÂY KHÔNG PHẢI `Int.MIN_VALUE` (-2147483648) dù vài tài liệu prose gọi nhầm là "Int.MIN_VALUE" —
     * lệch đúng 1000 (0x3E8). Dùng đúng số đo được, nếu không cache sẽ KHÔNG BAO GIỜ khớp → vô dụng.
     */
    const val NOT_PROVISIONED_RC = -2147482648

    // Feature-id (INT/bytes) đã bị HAL từ chối (rc == NOT_PROVISIONED_RC) → SKIP ở frame sau (hết spam
    // 'no permission device 1007' mỗi frame). In-memory, reset khi process restart. Xe provision oversea
    // (Sealion 6) KHÔNG bao giờ nhận sentinel → không cache → VẪN ghi (KHÔNG hard-remove code oversea).
    private val rejectedFeatures = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()
    // SDK method (sendSimpleGuidanceInfo/sendNextPathName/sendRestRouteInfo) NÉM lỗi (no-permission hoặc method
    // absent — cả hai đều ỔN ĐỊNH theo process) → skip lần sau. Key = tên method (stable). Sealion 6 chạy OK →
    // không ném → không cache → vẫn gọi.
    private val rejectedSdk = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    internal fun isFeatureRejected(id: Int): Boolean = id in rejectedFeatures
    /** Ghi nhận rc của 1 INT/bytes write; cache [id] nếu rc == [NOT_PROVISIONED_RC]. Trả true nếu vừa cache. */
    internal fun recordFeatureRc(id: Int, rc: Any?): Boolean {
        val rejected = (rc as? Int) == NOT_PROVISIONED_RC
        if (rejected) rejectedFeatures.add(id)
        return rejected
    }
    internal fun isSdkRejected(key: String): Boolean = key in rejectedSdk
    internal fun recordSdkFailure(key: String) { rejectedSdk.add(key) }
    /** Test-only: xả cache giữa các test (cache là process-global trong object). */
    internal fun resetRejectionCacheForTest() { rejectedFeatures.clear(); rejectedSdk.clear() }

    /** setInt qua cache rejection: skip nếu id đã bị từ chối; nếu không → setInt rồi cache khi rc == sentinel. */
    internal fun cachedSetInt(dev: Any, id: Int, value: Int): String {
        if (isFeatureRejected(id)) return "skip"
        val r = runCatching { setInt(dev, id, value) }.getOrElse { return root(it) }
        recordFeatureRc(id, r)
        return "$r"
    }
    /** setBytes qua cache rejection (chung set rejectedFeatures theo id — kill spam tên-đường oversea trên owner). */
    internal fun cachedSetBytes(dev: Any, id: Int, bytes: ByteArray): String {
        if (isFeatureRejected(id)) return "skip"
        val r = runCatching { setBytes(dev, id, bytes) }.getOrElse { return root(it) }
        recordFeatureRc(id, r)
        return "$r"
    }
    /** Gọi 1 SDK method qua cache rejection: skip nếu đã ném trước đó; nếu không → gọi và cache khi ném. */
    private fun cachedSdk(key: String, rc: StringBuilder, tag: String, block: () -> Any?) {
        if (isSdkRejected(key)) { rc.append(" $tag=skip"); return }
        runCatching { block() }
            .onSuccess { rc.append(" $tag=$it") }
            .onFailure { e -> rc.append(" $tag!=").append(root(e)); recordSdkFailure(key) }
    }

    // D2 (closeout 1.28): cache the OEM SDK method handles (sendSimpleGuidanceInfo / sendNextPathName /
    // sendRestRouteInfo) resolved via getMethod on the real-push path. Keyed by device-class#method (param types
    // are fixed per name here). getMethod still THROWS NoSuchMethodException when the method is absent → cachedSdk's
    // runCatching records the SDK-rejection (TASK 5) exactly as before → skipped next frame. On Sealion 6 (methods
    // present) the handle is resolved once then reused instead of scanned every real push.
    private val sdkMethodCache = java.util.concurrent.ConcurrentHashMap<String, java.lang.reflect.Method>()
    private fun sdkMethod(dev: Any, name: String, vararg params: Class<*>?): java.lang.reflect.Method {
        val key = "${dev.javaClass.name}#$name"
        return sdkMethodCache[key] ?: dev.javaClass.getMethod(name, *params).also { sdkMethodCache[key] = it }
    }

    // Android box B2 · W2d — đường ghi dẫn đường lên cụm/HUD (`writeNavFrame` · `clearNavFrame`) + payload nguồn ẢNH
    // (`pushNavigation` · `blankNavDistance` · `pushLane` · `pushCamera`, thân `BydHalContentPush`) + hằng register làn/camera
    // gỡ cùng dẫn đường cụm. Phần còn lại (đọc/ghi feature chung) gỡ ở W3.

    // firstReadable / readValue / methods — thân ở [BydHalRead] (tách theo VAI, DEBT-500).
    fun firstReadable(dev: Any, ids: List<Pair<String, Int>>): Pair<String, String>? = BydHalRead.firstReadable(dev, ids)
    fun readValue(result: Any?): String = BydHalRead.readValue(result)
    fun methods(dev: Any, vararg prefixes: String): List<String> = BydHalRead.methods(dev, *prefixes)

    fun exemptHiddenApis() {
        runCatching {
            val vm = Class.forName("dalvik.system.VMRuntime")
            val rt = vm.getMethod("getRuntime").invoke(null)
            vm.getMethod("setHiddenApiExemptions", Array<String>::class.java).invoke(rt, arrayOf("L") as Any)
        }
    }

    /** Trần độ sâu khi lần theo `cause` — cùng con số `carexec.LocalShellFailures.MAX_CAUSE_DEPTH`. */
    private const val MAX_CAUSE_DEPTH = 8

    /**
     * Gốc của chuỗi `cause` → `"Class: message"`.
     *
     * ⚠ PHẢI chặn VÒNG, không chỉ chặn tự-trỏ. Bản cũ dừng ở `c.cause !== c`, tức chỉ bắt được vòng MỘT mắt
     * xích; một vòng hai mắt xích (`a.cause = b; b.cause = a` — sinh ra khi một tầng bọc lại đúng cái lỗi nó
     * vừa nhận, kiểu `AdbConnectException("handshake failed", e)` gặp lại chính `e`) làm vòng lặp chạy VĨNH
     * VIỄN. Hàm này nằm trên đường ghi nav (`cachedSetInt`/`cachedSdk`, ~4 lần/giây) nên đơ ở đây = đơ luồng
     * ghi HAL trên xe đang chạy. `carexec.LocalShellFailures.causeChain` đã chặn đúng cách từ trước — đây là
     * cùng một lá chắn, đặt vào chỗ còn thiếu.
     */
    fun root(t: Throwable): String {
        var c: Throwable = t
        val seen = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Throwable, Boolean>())
        seen.add(c)
        var depth = 0
        while (depth++ < MAX_CAUSE_DEPTH) {
            val next = c.cause ?: break
            if (!seen.add(next)) break        // đã đi qua mắt xích này ⇒ chuỗi có vòng ⇒ dừng
            c = next
        }
        return "${c.javaClass.simpleName}: ${c.message}"
    }

}
