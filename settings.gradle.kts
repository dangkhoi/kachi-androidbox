pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // V2 pha NGHE · sherpa-onnx AAR (offline Zipformer-vi ASR) chỉ phát hành qua JitPack:
        // `com.github.k2-fsa.sherpa-onnx:sherpa-onnx` (engine Apache-2.0, kiểm Context7 2026-09-14 = v1.13.8).
        // Giới hạn `group` để JitPack KHÔNG trở thành nguồn giải phụ thuộc chung — mọi thứ khác vẫn đi
        // google()/mavenCentral(), đúng tinh thần FAIL_ON_PROJECT_REPOS ở trên.
        maven {
            url = uri("https://jitpack.io")
            content { includeGroup("com.github.k2-fsa.sherpa-onnx") }
        }
    }
}
rootProject.name = "ClusterNav"
include(":app")
include(":core")
include(":car-integration")

