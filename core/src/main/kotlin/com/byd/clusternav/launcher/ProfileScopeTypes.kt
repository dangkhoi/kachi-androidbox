package com.byd.clusternav.launcher

/**
 * Kiểu KHAI SẴN của khoá ClusterNav theo hồ sơ mà tầng này biết chắc (đọc từ chỗ `get*`/`put*` thật). Lượt áp/nhập bỏ
 * giá trị sai kiểu — kể cả khi tệp sống đang VẮNG khoá (ca mà phép so với kiểu sống không bắt được).
 *
 * Android box B2 · W2c: tách từ `ProfileScopeCluster.DECLARED_TYPES` (tệp đó gỡ cùng chiếu cụm). W4: chỉ còn khoá của
 * phần giữ (phím · khởi động nền · app nhạc mặc định · lịch tự dẫn · chủ đề).
 *
 * Ca đắt nhất (senior review V-CLUSTER Pass 2): `voicekey_bindings` đọc bằng `getString` mỗi lần bấm phím vô-lăng — tệp
 * nhập đặt nó thành Boolean trên máy chưa từng gán phím (tệp sống VẮNG khoá) ⇒ dịch vụ phím nổ. Bài canh `:app`
 * `ClusterProfileScopeCoverageTest` đòi MỌI khoá của ảnh chụp có kiểu ở bảng này.
 */
object ProfileScopeTypes {

    val CLUSTERNAV: Map<String, PrefType> = buildMap {
        // Android box B2 · W4 — kiểu của khoá camera / tiện nghi / dẫn đường cụm / biển báo / bong bóng BYD gỡ cùng các khoá ấy.
        listOf("voicekey_enabled", "headless_autostart")
            .forEach { put(it, PrefType.BOOLEAN) }
        listOf(
            "voicekey_bindings", "voicekey_custom_buttons", "voice_music_default_app", "nav_automation_rules",
            "nav_automation_fired", "theme_choice",
        ).forEach { put(it, PrefType.STRING) }
    }
}
