package com.byd.clusternav.launcher

/**
 * Báo cáo của MỘT lượt nhập hồ sơ (FIX286 · PI3): tên hồ sơ vừa tạo (tên trùng ⇒ hậu tố số) + loại tệp.
 *
 * Android box B2 · W2c: phần "chiếu cụm của tệp" (`ClusterImportSummary`) gỡ cùng chiếu cụm — tệp `.kachi` của Kachi BYD
 * mang ảnh `simple_cast_prefs` thì ảnh đó bị lượt nhập bỏ im lặng ([RetiredClusterKeys]).
 */
data class ProfileImportReport(
    val name: String,
    val kind: ProfileTransfer.Kind,
)

/** [WorkspaceRepository.importProfileData]: state mới (đã có hồ sơ nhập) + báo cáo của lượt nhập. */
data class ProfileImported(val state: HomeUiState, val report: ProfileImportReport)
