package com.kachi.box.launcher

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * ═══ LÀN NỀN TUẦN TỰ theo tên ([submitSerial]) — việc nhỏ ghi đĩa không được chặn luồng chính ═════════════════════════
 *
 * Android box B2 · W3 (2026-10-09): tách nguyên văn phần làn tuần tự của `MacroExec` (bộ chạy gói lệnh xe — gỡ cùng
 * `ActionMacros`). Người dùng còn lại: nhật ký phiên `:wake` (`WakeSessionLog`). Cùng một hồ chứa 2 luồng daemon tự tắt khi rỗi
 * 30 s như bản cũ — không thêm luồng nào.
 */
object SerialLanes {

    /** Hai luồng như `MacroExec` cũ (hàng đợi không trần — việc nhỏ, ghi đĩa). */
    private const val MAX_THREADS = 2

    /** Luồng rỗi sống thêm ngần này rồi tự tắt — không giữ luồng nào khi không có việc. */
    private const val IDLE_SECONDS = 30L

    private val pool: ThreadPoolExecutor = ThreadPoolExecutor(
        MAX_THREADS,
        MAX_THREADS,
        IDLE_SECONDS,
        TimeUnit.SECONDS,
        LinkedBlockingQueue<Runnable>(),
        ThreadFactory { r -> Thread(r, "kachi-lane").apply { isDaemon = true } },
    ).apply { allowCoreThreadTimeOut(true) }

    /**
     * Chạy [body] trên luồng nền dùng chung, **tuần tự theo [lane]**: mọi lượt cùng làn chạy đúng thứ tự nộp,
     * không bao giờ hai lượt cùng làn chạy song song, và một lượt ném không nuốt lượt kế.
     */
    fun submitSerial(lane: String, body: () -> Unit) {
        val next = lanes.compute(lane) { _, prev ->
            (prev ?: CompletableFuture.completedFuture<Void?>(null))
                .handleAsync<Void?>({ _, _ -> named(lane, body); null }, pool)
        } ?: return
        // Đuôi làn đã chạy xong thì bỏ khỏi bảng (chỉ khi nó VẪN là đuôi — `remove(k, v)` nguyên tử).
        //
        // ⚠ [SOÁT Pass 1 · 2026-09-25] Dòng này phải nằm **NGOÀI** `compute`: [body] có thể xong TRƯỚC khi luồng gọi
        // kịp đăng ký (việc rất ngắn), lúc ấy `whenComplete` chạy **ngay trên luồng gọi** ⇒
        // `lanes.remove` khi còn trong `compute` là *sửa map lúc đang compute* — điều `ConcurrentHashMap` cấm:
        // [ĐO JDK 17, khoá chưa có trong bảng] `IllegalStateException: Recursive update` tại
        // `ConcurrentHashMap.replaceNode:1167`, bị `CompletableFuture` nuốt vào future dẫn xuất (không ai đọc) ⇒
        // đuôi làn không bao giờ được dọn và một ngoại lệ chìm hẳn. Đăng ký sau khi `compute` trả về thì cùng lượt
        // dọn ấy chạy trên map đã ổn định.
        next.whenComplete { _, _ -> lanes.remove(lane, next) }
    }

    /** Đuôi hiện tại của mỗi làn; lượt mới xâu vào sau đuôi. */
    private val lanes = ConcurrentHashMap<String, CompletableFuture<Void?>>()

    private fun named(name: String, body: () -> Unit) {
        val t = Thread.currentThread()
        val old = t.name
        runCatching { t.name = name }
        try {
            body()
        } finally {
            runCatching { t.name = old }
        }
    }
}
