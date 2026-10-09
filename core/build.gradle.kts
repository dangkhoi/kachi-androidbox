// :core — pure Kotlin JVM module (no Android, no dadb).
plugins {
    id("java-library")
    id("java-test-fixtures")
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // W1b: StateFlow cho CarStatusRepository (poll 2 nhịp). kotlinx-coroutines-core = JVM THUẦN (không android) →
    // hợp luật Q1 :core. Version 1.11.0 khớp :app (kotlinx-coroutines-android) — CLOSE-8 2026-09-26, Context7: 1.11.0 = bản mới nhất, runTest/StateFlow/delay không đổi.
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    testImplementation(platform("org.junit:junit-bom:6.1.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // W1b: test StateFlow/poll của CarStatusRepository (runTest/advanceTimeBy/runCurrent).
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}

/**
 * ⚠⚠ `:core:test` ĐỌC CÂY NGUỒN CỦA MODULE KHÁC — phải khai, không thì bài canh cho dấu xanh GIẢ.
 *
 * Các bài ở đây quét mã của module khác (`SourceRoots` / `moduleSourceRoots()` giải sang `app/`, `car-integration/`):
 * `LauncherWindowingGuardTest` (cấm `:app` chạm display ≥ 1), `PersistentWindowStateWriterGuardTest`
 * (một-nơi-ghi-duy-nhất). `:core` KHÔNG phụ thuộc `:car-integration`/`:app` (chiều ngược lại) nên classpath của nó
 * không có gì động khi văn bản ở đó đổi.
 *
 * [ĐO] 2026-09-12: đổi một chuỗi trong `car-integration/src/main/kotlin` mà bài `:core` canh ⇒ `:core:test`
 * **UP-TO-DATE / BUILD SUCCESSFUL**; ép chạy ⇒ **BUILD FAILED** — lý do các `inputs` dưới đây tồn tại.
 * (Android box B2 · W2a: gỡ hai đầu vào của bộ đo xe — sổ `verdicts.tsv` và kế hoạch phiên T10 — cùng các bài đọc chúng.)
 *
 * `src/main/kotlin` của chính `:core` cũng khai tường minh: đường gián tiếp qua classpath chỉ bắt thay đổi
 * làm ĐỔI BYTECODE, còn các bài này quét văn bản gốc (kể cả chú thích/KDoc).
 */
tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    // Golden voice-coverage test đọc corpus câu nói ở scripts/voice/data/*.tsv qua clusternav.root.
    systemProperty("clusternav.root", rootProject.projectDir.absolutePath)
    inputs.dir(rootProject.layout.projectDirectory.dir("scripts/voice/data"))
        .withPropertyName("voiceGoldenCorpus")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    inputs.dir(rootProject.layout.projectDirectory.dir("app/src/main/java"))
        .withPropertyName("appSourceTextForCoreGuardTests")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(rootProject.layout.projectDirectory.dir("car-integration/src/main/kotlin"))
        .withPropertyName("carIntegrationSourceTextForGuardTests")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(layout.projectDirectory.dir("src/main/kotlin"))
        .withPropertyName("coreSourceTextForCoreGuardTests")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    // 2.89 · B2 VM-PREREQ-TRUTH — đầu ra nguyên văn máy ảo (`AppPrereqReadTest` · `VietMapBubbleWaitTest` đọc qua clusternav.root).
    inputs.dir(rootProject.layout.projectDirectory.dir("docs/diagnostics/vm-prereq-emulator-2026-10-05"))
        .withPropertyName("vmPrereqEmulatorFixtures")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
