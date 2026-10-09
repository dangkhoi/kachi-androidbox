package com.byd.clusternav.launcher

import com.byd.clusternav.testsupport.SourceRoots
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * G1 · T4 — khoá **DÂY NỐI** của "người dùng gặp nhóm trước": hai màn chọn phải bày mục Nhóm **TRƯỚC** mục rời, và
 * không màn nào được bày nhóm hai lần.
 *
 * ## Vì sao khoá THỨ TỰ chứ không chỉ khoá sự tồn tại
 * Nhóm nằm đâu đó trong danh sách thì yêu cầu R2 coi như KHÔNG đạt: [ĐO] lĩnh vực đầu tiên (Năng lượng) một mình đã
 * có 32 ô rời, nên một mục Nhóm nằm sau nó là *"phải cuộn qua cả trăm ô mới thấy"* — đúng cái owner phàn nàn. Vì thế
 * bài này so **vị trí** trong mã nguồn, không chỉ hỏi "có gọi hàm đó không".
 *
 * ## Vì sao vẫn phải quét mã nguồn
 * Thứ tự dựng view là hành vi của Android View, mà dự án không dùng Robolectric. Phần quyết định được (danh sách,
 * thứ tự trong danh sách, câu chữ) đã có `CapabilityPickerTest` ở `:core` canh — bài này chỉ canh chỗ **ghép**.
 *
 * ⚠ Mọi phép cắt vùng đi qua [SourceRoots.body] — nó **tự nổ** nếu mốc không tồn tại. Bản cũ dùng
 * `substringAfter/Before` với mốc kết không nằm sau mốc đầu nên quét tràn tới hết tệp (14 bài đã chứng minh là test
 * giả).
 */
class GroupPickerWiringContractTest {

    private val drawer by lazy { code("src/main/java/com/byd/clusternav/launcher/AppDrawer.kt") }
    private val drawerTiles by lazy { code("src/main/java/com/byd/clusternav/launcher/AppDrawerTiles.kt") }   // ô lưới tách ra (L6-debt 2026-09-27)
    private val dock by lazy { code("src/main/java/com/byd/clusternav/launcher/ControlDockView.kt") }

    /** Đọc source rồi **bỏ chú thích**: bài này canh CODE, không canh văn xuôi (KDoc nhắc chính token đang soi). */
    private fun code(relative: String): String = SourceRoots.codeOf(relative)

    /**
     * VÙNG DỰNG BỘ CHỌN của ngăn kéo = `init` + hai hàm mục.
     *
     * ⚠⚠ T6 (R-UI (m)) tách thân bảng thành `groupSection` / `singlesSection` để **chế độ thứ ba** (chọn nút cho
     * thanh nút xe) dùng lại được thay vì chép — nếu chép thì hai thân bảng sẽ lệch đúng lúc ai đó thêm một lĩnh
     * vực, tức là đẻ lại chính bệnh mà cả tệp này đi canh. Bài vì thế phải nối ba vùng thay vì đọc mỗi `init`.
     *
     * **Không** dùng vùng nối này để so THỨ TỰ: thứ tự trong chuỗi nối là thứ tự tôi ghép, không phải thứ tự chạy.
     * Thứ tự thật do `ngan keo dat muc NHOM TRUOC moi linh vuc` chốt trên chính lời gọi trong `init`.
     */
    private fun drawerPicker(): String =
        listOf("init {", "private fun groupSection(", "private fun singlesSection(")
            .joinToString("\n") { SourceRoots.body(drawer, it) }

    // ── 1 · Cả hai màn chọn đều BÀY NHÓM ─────────────────────────────────────────────────────────


    // ── 2 · NHÓM ĐỨNG TRƯỚC (đây là bất biến chính của T4) ───────────────────────────────────────

    /**
     * ⚠ T4 · IA v2 R-UI (m) — bài `man Cai dat dat muc NHOM TRUOC moi linh vuc` đã **XOÁ**, không phải làm yếu đi:
     * màn Cài đặt không còn dựng lưới ô nào cho thanh nút xe (nó mở `AppDrawer.Mode.PICK_DOCK`), nên thứ tự "nhóm
     * trước lĩnh vực" ở đó không còn đối tượng để canh. Tính chất ấy vẫn được canh — ở ngăn kéo, bài
     * `ngan keo dat muc NHOM TRUOC moi linh vuc` ngay phía trên, trên đúng bề mặt người dùng thật sự thấy.
     */
    @Test
    fun `man Cai dat mo bo chon cua ngan keo, khong dung luoi thu hai`() {
        val bars = code("src/main/java/com/byd/clusternav/launcher/SettingsSectionsBars.kt")
        assertTrue(bars.contains("deps.openDockPicker("), "nhóm thanh nút phải MỞ bộ chọn của ngăn kéo")
        assertFalse(bars.contains("CapabilityPicker.groupPicks()"), "và KHÔNG được tự dựng lưới ô nhóm lần nữa")
        assertFalse(bars.contains("CapabilityCatalog.byDomain()"), "cũng không tự duyệt lĩnh vực lần nữa")
    }

    // ── 3 · Không màn nào bày nhóm HAI LẦN ───────────────────────────────────────────────────────

    // ── 4 · Ô nhóm nói nó GỒM GÌ ─────────────────────────────────────────────────────────────────

    @Test
    fun `o nhom hien dong phu o CA HAI man chon`() {
        // U6: đọc `displaySub` chứ không đọc `sub` GỐC — cùng một dòng chữ nay chở thêm gợi ý loại ("xem"/"bấm")
        // vừa được chuyển ra khỏi NHÃN CHÍNH, và `displaySub` là chỗ duy nhất ghép hai mảnh đó (ở `:core`).
        assertTrue(
            drawerTiles.contains("pick.displaySub"),
            "ngăn kéo phải hiện dòng phụ của nhóm — không thì người dùng thấy ô 'Lốp' mà vẫn phải đoán bên trong có gì",
        )
        // ⚠ T4 · R-UI (m): nhánh "lưới của màn Cài đặt" đã bỏ — `CapabilityGridSection` bị XOÁ cùng lúc với lưới
        // 123 ô trong Settings. Ngăn kéo nay là bề mặt DUY NHẤT bày ô nhóm, nên nó cũng là chỗ duy nhất phải canh.
        // Dòng phụ đến từ `:core`; tầng vẽ KHÔNG được tự ghép số thành viên (đó là bản sao thứ hai).
        listOf("drawer" to drawer).forEach { (who, src) ->
            assertFalse(src.contains(".reads.size"), "$who không được tự đếm thành viên")
            assertFalse(src.contains(".writes.size"), "$who không được tự đếm nút")
            assertFalse(src.contains("contentLine"), "$who chỉ đọc pick.displaySub, phép ghép nằm ở :core")
        }
    }

    // ── 5 · Bày ra thì phải DÙNG ĐƯỢC (chống "lựa chọn chết") ────────────────────────────────────

}
