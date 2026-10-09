package com.kachi.box.core

/**
 * Điểm neo của module :core.
 *
 * Sự tồn tại của module này là một bảo đảm cấu trúc, không phải một lời hứa: classpath ở đây không có
 * Android và không có dadb, nên code quyết định không thể gọi thiết bị.
 * Luật xếp chỗ: docs/layering-rules.md.
 */
object CoreBoundary {
    const val LAYER = "core"
}
