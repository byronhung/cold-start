package com.byronhung.firstlight.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyThemesTest {

    @Test fun `a Plus theme without Plus falls back to Sunrise`() {
        for (t in SkyTheme.entries.filter { it.plus }) assertEquals(SkyTheme.SUNRISE, SkyTheme.effective(t.code, isPlus = false))
    }

    @Test fun `with Plus every theme is itself`() {
        for (t in SkyTheme.entries) assertEquals(t, SkyTheme.effective(t.code, isPlus = true))
    }

    @Test fun `unknown theme codes are Sunrise`() {
        assertEquals(SkyTheme.SUNRISE, SkyTheme.of(42))
    }

    @Test fun `codes are unique, so a saved theme always means one sky`() {
        assertEquals(SkyTheme.entries.size, SkyTheme.entries.map { it.code }.toSet().size)
    }

    @Test fun `only Sunrise is free`() {
        assertEquals(listOf(SkyTheme.SUNRISE), SkyTheme.entries.filter { !it.plus })
    }

    @Test fun `every hour of the day has a sky, carrying its theme's scene`() {
        for (t in SkyTheme.entries) for (h in 0..23) assertEquals(t.scene, t.forHour(h).scene)
    }

    @Test fun `every theme is dark at night, light at midday, and rings on a dark dawn`() {
        for (t in SkyTheme.entries) {
            assertFalse(t.forHour(2).isLight)
            assertTrue(t.forHour(12).isLight)
            assertFalse(t.night.isLight)
            assertFalse(t.dawn.isLight)
            assertTrue(t.morning.isLight)
        }
    }

    @Test fun `dawn hours read as dawn so scenes fade their night effects`() {
        for (t in SkyTheme.entries) {
            assertEquals(Phase.DAWN, t.forHour(6).phase)
            assertEquals(Phase.DAWN, t.dawn.phase)
            assertEquals(Phase.NIGHT, t.forHour(23).phase)
        }
    }

    // ---------- appearance ----------

    @Test fun `dark never brightens, at any hour, in any theme`() {
        for (t in SkyTheme.entries) for (h in 0..23) assertFalse("${t.label} $h", t.forHour(h, Appearance.DARK).isLight)
    }

    @Test fun `light never goes dark, at any hour, in any theme`() {
        for (t in SkyTheme.entries) for (h in 0..23) assertTrue("${t.label} $h", t.forHour(h, Appearance.LIGHT).isLight)
    }

    @Test fun `dark and light still change through the day`() {
        for (t in SkyTheme.entries) {
            assertTrue((0..23).map { t.forHour(it, Appearance.DARK) }.toSet().size >= 4)
            assertTrue((0..23).map { t.forHour(it, Appearance.LIGHT) }.toSet().size >= 3)
        }
    }

    @Test fun `by the hour is the plain day cycle`() {
        for (t in SkyTheme.entries) for (h in 0..23) assertEquals(t.forHour(h), t.forHour(h, Appearance.BY_HOUR))
    }

    @Test fun `unknown appearance codes are by the hour`() = assertEquals(Appearance.BY_HOUR, Appearance.of(9))
}
