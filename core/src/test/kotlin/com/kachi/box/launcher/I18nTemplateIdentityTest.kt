package com.kachi.box.launcher

import com.kachi.box.launcher.voice.VoiceAppEvidence
import com.kachi.box.launcher.voice.VoiceAppTargets
import com.kachi.box.launcher.voice.VoiceClarify
import com.kachi.box.launcher.voice.VoiceFeatureGone
import com.kachi.box.launcher.voice.VoiceFeedbackPhrase
import com.kachi.box.launcher.voice.VoiceIntent
import com.kachi.box.launcher.voice.VoiceIntentParser
import com.kachi.box.launcher.voice.VoiceReply
import com.kachi.box.launcher.voice.VoiceUnknownReason
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ═══ R-nf1 — câu CÓ BIẾN chuyển `Strings.t("…$x…")` → `Strings.f("…{0}…")` ra Y TỪNG BYTE ở VI và EN ═══════════
 *
 * Mỗi bài dựng câu mong đợi **đúng theo mã CŨ** (chuỗi mẫu `$` của Kotlin + `old()` = luật `t` cũ hai nhánh), rồi so
 * với đầu ra của mã mới qua đường công khai. Phần lớn các câu tiếng Anh này chưa từng bị test nào khoá (kiểm kê
 * 2026-10-03) — đây là chỗ một lần chuyển tay lệch một dấu cách/số ít-nhiều sẽ lộ ra.
 */
class I18nTemplateIdentityTest {

    @AfterEach
    fun `dọn`() {
        Strings.current = Lang.VI
    }

    private val both = listOf(Lang.VI, Lang.EN)

    /** Luật `Strings.t` CŨ (trước zh/th/ms). */
    private fun old(l: Lang, vi: String, en: String) = if (l == Lang.EN) en else vi

    private fun <T> at(l: Lang, block: () -> T): T = I18nPairs.inLang(l, block)

    @Test
    fun `chu ky trinh chieu`() = both.forEach { l ->
        // Android box B2 · W3: "mã N" (bảng mã datum) và kết luận lốp gỡ cùng lõi HAL BYDAuto.
        for (sec in listOf(15, 59, 60, 300, 3599, 3600, 7200)) {
            val exp = when {
                sec < 60 -> old(l, "$sec giây", "$sec s")
                sec < 3600 -> old(l, "${sec / 60} phút", "${sec / 60} min")
                else -> old(l, "${sec / 3600} giờ", "${sec / 3600} h")
            }
            assertEquals(exp, at(l) { Slideshow.intervalLabel(sec) })
        }
    }

    @Test
    fun `ho so`() = both.forEach { l ->
        // Android box B2 · W3: gói lệnh, mức ghế, dòng nội dung nhóm gỡ cùng lõi HAL BYDAuto.
        for ((preset, slots) in listOf(LayoutPreset.TWO_COL to 1, LayoutPreset.ONE to 2, null to 3)) {
            val layout = at(l) { preset?.label } ?: old(l, "Tự vẽ", "Custom")
            val en = if (slots == 1) "1 slot filled" else "$slots slots filled"
            assertEquals(old(l, "$layout · $slots ô có nội dung", "$layout · $en"), at(l) { ProfileNames.summary(preset, slots) })
        }
    }

    @Test
    fun `loi bo cuc tu ve`() = both.forEach { l ->
        val c = WorkspaceGrid.COLS
        val r = WorkspaceGrid.ROWS
        val mc = WorkspaceGrid.MIN_COLS
        val mr = WorkspaceGrid.MIN_ROWS
        val small = GridFrame(0, 0, 1, 1)
        assertEquals(
            listOf(old(l, "khung 1 nhỏ quá (1×1), tối thiểu " + "$mc×$mr", "frame 1 is too small (1×1), minimum " + "$mc×$mr")),
            at(l) { GridLayout(listOf(small)).problems() },
        )
        assertEquals(
            listOf(old(l, "khung 1 ra ngoài lưới $c×$r", "frame 1 falls outside the $c×$r grid")),
            at(l) { GridLayout(listOf(GridFrame(c - 1, 0, 2, 1))).problems() },
        )
        val a = GridFrame(0, 0, 2, 1)
        assertEquals(
            listOf(old(l, "khung 1 và khung 2 đè lên nhau", "frame 1 and frame 2 overlap")),
            at(l) { GridLayout(listOf(a, a)).problems() },
        )
        val two = GridLayout(listOf(a, GridFrame(2, 0, 2, 1)))
        assertEquals(
            old(l, "bố cục tự vẽ có 2 khung, bản này đỡ tối đa 1", "the custom layout has 2 frames; this build supports at most 1"),
            at(l) { EffectiveLayout.ignoredReason(two, cap = 1) },
        )
    }

    @Test
    fun `cau tra loi giong noi`() = both.forEach { l ->
        val target = VoiceAppTargets.ALL.first()
        val label = target.label
        val i = VoiceIntent.OpenApp(label)
        at(l) {
            // Android box B2 · W3: câu xem trước / "đã gửi" của nút xe gỡ cùng `VoiceIntent.Control`.
            assertTrue(VoiceReply.preview(VoiceIntent.OpenApp(label, slot = 3)).contains(old(l, " vào ô 3", " in slot 3")))
            assertTrue(VoiceReply.slotOutOfRange(i, 4).contains(old(l, "bố cục hiện chỉ có 4 ô", "the current layout only has 4 slot(s)")))
            assertTrue(VoiceReply.appNotInstalled(i, target.key).contains(old(l, "chưa cài $label trên máy", "$label is not installed")))
            assertTrue(
                VoiceReply.musicAppOpened(i, target).endsWith(
                    " — " + old(
                        l,
                        "đã mở $label; chưa có phiên nhạc nào để điều khiển — bấm Play trong app",
                        "opened $label; no music session to control yet — press Play in the app",
                    ),
                ),
            )
            val unknown = target.copy(evidence = VoiceAppEvidence.UNKNOWN)
            assertTrue(
                VoiceReply.handedOver(i, unknown).contains(old(l, "chưa kiểm cửa nhận chữ của $label", "$label's hand-over door is not verified yet")),
            )
            assertTrue(
                VoiceReply.confirmPlace(i, target, "Chợ Bến Thành")
                    .startsWith(old(l, "Dẫn đường tới «Chợ Bến Thành» trên $label?", "Navigate to «Chợ Bến Thành» on $label?") + "\n"),
            )
            assertTrue(
                VoiceReply.navOpenedNoHandover(i, target).endsWith(
                    old(l, "$label chưa nhận điểm đến bằng giọng; gõ tay trong app", "$label takes no destination from outside; type it in the app"),
                ),
            )
            assertTrue(
                VoiceReply.navNoPlace(i, target).endsWith(
                    old(l, "chưa tra được điểm đến (mạng?), mới chỉ mở $label", "could not look the place up (network?) — only opened $label"),
                ),
            )
            assertTrue(
                VoiceReply.placeNotSaved(i, "Nhà ngoại").contains(
                    old(
                        l,
                        "chưa lưu địa chỉ «Nhà ngoại» — thêm ở Cài đặt › Dẫn đường › Sổ địa chỉ",
                        "no address saved for «Nhà ngoại» — add it in Settings › Navigation › Address book",
                    ),
                ),
            )
            assertTrue(
                VoiceReply.placeNeedsCoords(i, target).endsWith(
                    old(
                        l,
                        "$label chỉ nhận toạ độ; thêm lat/lng cho mục này trong Sổ địa chỉ",
                        "$label only takes coordinates — add lat/lng to this entry in the address book",
                    ),
                ),
            )
            assertTrue(VoiceReply.cancelled(i, 2).endsWith(old(l, ", 2 việc sau không chạy", ", 2 later step(s) not run")))
        }
    }

    @Test
    fun `tinh nang da bo, gop cau doc, hoi lai`() = both.forEach { l ->
        assertTrue(VoiceFeatureGone.ALL.any { it.removed } && VoiceFeatureGone.ALL.any { !it.removed }, "cần đủ hai loại dòng")
        VoiceFeatureGone.ALL.forEach { g ->
            val exp = if (g.removed) {
                old(
                    l,
                    "Kachi không có tính năng ${g.label}",
                    "${g.labelEn.replaceFirstChar { it.uppercase() }} is not part of Kachi",
                )
            } else {
                old(l, "Kachi không điều khiển được ${g.label}", "Kachi cannot control ${g.labelEn}")
            }
            assertEquals(exp, at(l) { VoiceFeatureGone.reply(g) })
        }
        at(l) {
            val ok = VoiceReply.done(VoiceIntent.Launcher(LauncherActions.SETTINGS))
            // Vế hỏng NGẮN: câu gộp dài quá 12 từ thì bị cắt đuôi (clampWords) — đúng phần đuôi đang cần soi.
            assertEquals(old(l, "Chưa x, 2 việc khác đã xong", "Could not x, 2 other(s) done"), VoiceFeedbackPhrase.merge(listOf("✗ X", ok, ok)))
            assertEquals(old(l, "Đã xong 6 việc", "6 things done"), VoiceFeedbackPhrase.merge(List(6) { ok }))

            assertEquals(old(l, "Bật gì?", "Bật what?"), VoiceClarify.ask(VoiceIntent.Unknown(VoiceUnknownReason.NO_OBJECT, "bật"), 0)!!.question)
            // Android box B2 · W3: câu hỏi "Lọc nào / Kính nào — …" (danh sách nút xe) gỡ cùng `ControlRegistry`.
        }
    }
}
