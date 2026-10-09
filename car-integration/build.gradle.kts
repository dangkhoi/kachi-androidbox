// :car-integration — JVM module talking to the device's own adbd via dadb (no Android).
plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core"))
    implementation("dev.mobile:dadb:2.0.0")
    testImplementation(platform("org.junit:junit-bom:6.1.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

/**
 * ⚠⚠ Test của module này quét VĂN BẢN gốc của chính `src/main/kotlin` — phải khai, không thì xanh GIẢ
 * (đường gián tiếp qua classpath chỉ bắt thay đổi làm đổi bytecode; sửa chú thích thì không).
 *
 * Android box B2 · W2a (2026-10-09): gỡ CLI đo xe (`CarExecCli` + vỏ `scripts/vehicle/carexec.sh` + `run-on-car.md`) và
 * bộ chạy phiên T10 (gói `vehicleprobe`, task `runHudSignT10`, plugin `application`) cùng các đầu vào test của chúng.
 */
tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    inputs.dir(layout.projectDirectory.dir("src/main/kotlin"))
        .withPropertyName("carIntegrationSourceTextForContractTests")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
