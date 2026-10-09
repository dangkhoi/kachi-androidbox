package com.byd.clusternav.launcher

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Catalog chỉ-launcher sau Android box B2 · W3 (2026-10-09): widget dựng tay (READ) + hành động launcher (LAUNCHER).
 * Thay `CapabilityCatalogTest` / `CapabilityPickerTest` / bản cũ của tệp này (ghim nút xe · datum · nhóm · gói lệnh ·
 * bảng lốp — gỡ cùng lõi HAL BYDAuto).
 */
class LauncherCatalogTest {

    @AfterEach fun resetLang() { Strings.current = Lang.VI }

    @Test fun `curated widget picks khop WidgetRegistry`() {
        assertEquals(WidgetRegistry.ALL.map { it.id }, WidgetCatalog.CURATED.map { it.id })
        WidgetRegistry.ALL.forEach { assertEquals(it.label, WidgetCatalog.pick(it.id)!!.label) }
        assertNull(WidgetCatalog.pick("w_board"))
    }

    @Test fun `kindOf - widget la READ, hanh dong launcher la LAUNCHER, ma la null`() {
        WidgetRegistry.ALL.forEach { assertEquals(CapabilityKind.READ, CapabilityCatalog.kindOf(it.id), it.id) }
        LauncherActions.placeable.forEach { assertEquals(CapabilityKind.LAUNCHER, CapabilityCatalog.kindOf(it.id), it.id) }
        listOf("", "khong_co", "ac_auto", "soc", "g_tyres", "mac_leave", "w_energy").forEach {
            assertNull(CapabilityCatalog.kindOf(it), "'$it' phải không còn trong catalog")
            assertNull(CapabilityCatalog.pick(it))
        }
    }

    @Test fun `all = widget truoc, hanh dong launcher sau, khong trung ma`() {
        val ids = CapabilityCatalog.all().map { it.id }
        assertEquals(WidgetRegistry.ALL.map { it.id } + LauncherActions.placeable.map { it.id }, ids)
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(emptyList<String>(), CapabilityCatalog.collisions())
    }

    @Test fun `pick giu nhan va icon cua bo dang ky goc`() {
        WidgetRegistry.ALL.forEach { w ->
            val p = CapabilityCatalog.pick(w.id)!!
            assertEquals(w.label, p.label); assertEquals(w.icon, p.icon); assertTrue(p.curated)
        }
        LauncherActions.placeable.forEach { a ->
            val p = CapabilityCatalog.pick(a.id)!!
            assertEquals(a.label, p.label); assertEquals(a.icon, p.icon); assertFalse(p.curated)
        }
    }

    @Test fun `moi muc co nhan EN va hien dung theo tieng`() {
        CapabilityCatalog.all().forEach { p ->
            assertFalse(p.labelEn.isNullOrBlank(), "${p.id} thiếu nhãn EN")
            Strings.current = Lang.EN
            assertEquals(p.labelEn, p.displayLabel)
            Strings.current = Lang.VI
            assertEquals(p.label, p.displayLabel)
        }
    }

    @Test fun `khong nhan nao trung nen khong co goi y loai`() {
        assertEquals(emptySet<String>(), CapabilityCatalog.collidingLabels())
        CapabilityCatalog.all().forEach { assertEquals("", it.typeHint, it.id) }
    }

    @Test fun `bo chon nut thanh chi bay hanh dong launcher va vua mot hang`() {
        val picks = CapabilityPicker.launcherPicks()
        assertEquals(LauncherActions.placeable.map { it.id }, picks.map { it.id })
        assertTrue(picks.all { it.kind == CapabilityKind.LAUNCHER })
        assertTrue(picks.size <= CapabilityPicker.COLS, "khối Launcher ${picks.size} ô > ${CapabilityPicker.COLS} cột")
    }
}
