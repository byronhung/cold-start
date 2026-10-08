package com.byronhung.firstlight.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** What moves on top of the gradient. Each theme has one; [SkyBackground] draws it. */
enum class Scene { SUN, AURORA, MONSOON, NEON, COAST }

/** Where a sky sits in the day. Scenes read it: aurora fades, neon windows switch off, the moon sets. */
enum class Phase { NIGHT, DAWN, DAY }

/**
 * A theme is a whole day of skies plus a scene, from the Plus mockups
 * (claude.ai/artifact/RJ5hJG9ZUmeCjKF3fH7S6f, "Five skies"). Every theme has the same eight hour stops
 * as Sunrise, so the list screen turns from night to day the same way whichever one you pick.
 * Saved by [code] in Settings; [plus] themes need First Light Plus.
 */
enum class SkyTheme(
    val code: Int,
    val label: String,
    val plus: Boolean,
    val scene: Scene,
    /** Hours 0, 5, 6, 8, 11, 17, 19, 21, as top / mid / bottom (and Sunrise's sun strength). */
    private val hours: List<Stop>,
    /** The ringing screen, always. */
    private val dawnStop: Stop,
    /** Wake checks: light, awake. */
    private val morningStop: Stop,
) {
    SUNRISE(
        0, "Sunrise", plus = false, Scene.SUN,
        hours = listOf(
            Stop(0xFF0A0E2A, 0xFF17183F, 0xFF2A1D52, sun = 0.35f),
            Stop(0xFF151638, 0xFF3B2564, 0xFF8A4170, sun = 0.6f),
            Stop(0xFF2C205E, 0xFFA8476F, 0xFFF49D5F, sun = 1f),
            Stop(0xFFF3AE78, 0xFFF8CDA2, 0xFFFCE7CB, sun = 0.8f),
            Stop(0xFFF4DABB, 0xFFF9E8D2, 0xFFFEF5E9, sun = 0.5f),
            Stop(0xFF4A2C6B, 0xFFC4566C, 0xFFF4A35E, sun = 0.9f),
            Stop(0xFF1D1B4B, 0xFF3D2664, 0xFF713668, sun = 0.5f),
            Stop(0xFF0B1030, 0xFF1A1B46, 0xFF2D2058, sun = 0.35f),
        ),
        dawnStop = Stop(0xFF2C205E, 0xFF7B3A72, 0xFFF7A35F, sun = 1f),
        morningStop = Stop(0xFFF09A63, 0xFFF7C08F, 0xFFFCE3C4, sun = 0.6f),
    ),
    AURORA(
        1, "Aurora", plus = true, Scene.AURORA,
        hours = listOf(
            Stop(0xFF050B1F, 0xFF0B1E3A, 0xFF123049),
            Stop(0xFF0E1636, 0xFF2A2F63, 0xFF5B4A86),
            Stop(0xFF1B2448, 0xFF6B5B9A, 0xFFF2A7C3),
            Stop(0xFFB9CCE8, 0xFFDCE6F4, 0xFFF7F0F6),
            Stop(0xFFDDE9F5, 0xFFEEF3FA, 0xFFFFFFFF),
            Stop(0xFF22285A, 0xFF7A5F9E, 0xFFE8A0BE),
            Stop(0xFF0B1534, 0xFF15284A, 0xFF1E3C58),
            Stop(0xFF060D24, 0xFF0D2140, 0xFF14334D),
        ),
        dawnStop = Stop(0xFF1B2448, 0xFF5E4F8E, 0xFFF2A7C3),
        morningStop = Stop(0xFFB9CCE8, 0xFFDCE6F4, 0xFFF7F0F6),
    ),
    MONSOON(
        2, "Monsoon", plus = true, Scene.MONSOON,
        hours = listOf(
            Stop(0xFF0E1620, 0xFF1B2633, 0xFF26364A),
            Stop(0xFF1A2430, 0xFF2E3C4D, 0xFF4A5B6E),
            Stop(0xFF2E3A4A, 0xFF5E7083, 0xFFA9B7C2),
            Stop(0xFFA9B7C2, 0xFFC9D3DB, 0xFFE2E8EC),
            Stop(0xFFC9D3DB, 0xFFE2E8EC, 0xFFF3F5F6),
            Stop(0xFF2A3646, 0xFF566778, 0xFF9AA9B6),
            Stop(0xFF141E2A, 0xFF223040, 0xFF33465A),
            Stop(0xFF0F1822, 0xFF1C2836, 0xFF283A4E),
        ),
        dawnStop = Stop(0xFF2E3A4A, 0xFF5E7083, 0xFFA9B7C2),
        morningStop = Stop(0xFFA9B7C2, 0xFFC9D3DB, 0xFFE2E8EC),
    ),
    NEON(
        3, "Neon city", plus = true, Scene.NEON,
        hours = listOf(
            Stop(0xFF12052A, 0xFF2A0B4F, 0xFF4B1466),
            Stop(0xFF1C0838, 0xFF3E0F5E, 0xFF7A1F6E),
            Stop(0xFF2A0B4F, 0xFFB2367A, 0xFFFF9A6B),
            Stop(0xFFFFB89A, 0xFFFFD6C2, 0xFFFFE9DD),
            Stop(0xFFFFD6C2, 0xFFFFE9DD, 0xFFFFF6F0),
            Stop(0xFF3A0D5C, 0xFFC23A78, 0xFFFF8A5C),
            Stop(0xFF1E0740, 0xFF3A0D5E, 0xFF621870),
            Stop(0xFF14052E, 0xFF2C0B52, 0xFF4D1468),
        ),
        dawnStop = Stop(0xFF2A0B4F, 0xFFB2367A, 0xFFFF9A6B),
        morningStop = Stop(0xFFFFB89A, 0xFFFFD6C2, 0xFFFFE9DD),
    ),
    COAST(
        4, "Coast", plus = true, Scene.COAST,
        hours = listOf(
            Stop(0xFF06142A, 0xFF0C2A4A, 0xFF123E66),
            Stop(0xFF0F1D3E, 0xFF22356A, 0xFF4A4F80),
            Stop(0xFF1E2F5C, 0xFFE08A6A, 0xFFFFC27A),
            Stop(0xFF9FD3EA, 0xFFC8E8F4, 0xFFEAF6FA),
            Stop(0xFFBFE3F2, 0xFFDDF1F8, 0xFFF2FAFD),
            Stop(0xFF24305E, 0xFFD27A66, 0xFFFFB070),
            Stop(0xFF0B1D3A, 0xFF163658, 0xFF1D4870),
            Stop(0xFF07162E, 0xFF0E2C4E, 0xFF14406A),
        ),
        dawnStop = Stop(0xFF1E2F5C, 0xFFE08A6A, 0xFFFFC27A),
        morningStop = Stop(0xFF9FD3EA, 0xFFC8E8F4, 0xFFEAF6FA),
    );

    fun forHour(hour: Int): Sky {
        val h = hour.coerceIn(0, 23)
        val i = HOURS.indexOfLast { it <= h }
        return hours[i].sky(PHASES[i], scene)
    }

    val dawn: Sky get() = dawnStop.sky(Phase.DAWN, scene)
    val morning: Sky get() = morningStop.sky(Phase.DAY, scene)

    /** Settings, history, setup: the late-evening sky you see when setting alarms. */
    val night: Sky get() = forHour(22)

    /** The Settings swatch: night into dawn, so each one shows what makes it different. */
    val swatch: List<Color> get() = listOf(hours[0].top, dawnStop.mid, dawnStop.bottom)

    companion object {
        private val HOURS = listOf(0, 5, 6, 8, 11, 17, 19, 21)
        private val PHASES = listOf(
            Phase.NIGHT, Phase.NIGHT, Phase.DAWN, Phase.DAY, Phase.DAY, Phase.DAWN, Phase.NIGHT, Phase.NIGHT,
        )

        fun of(code: Int): SkyTheme = entries.firstOrNull { it.code == code } ?: SUNRISE

        /** A Plus theme without Plus (unlock not bought, or refunded) shows as Sunrise. */
        fun effective(code: Int, isPlus: Boolean): SkyTheme = of(code).takeIf { !it.plus || isPlus } ?: SUNRISE
    }
}

/** One sky's colours. Light or dark ink follows the phase: day skies are pale in every theme. */
class Stop(top: Long, mid: Long, bottom: Long, private val sun: Float = 0f) {
    val top = Color(top)
    val mid = Color(mid)
    val bottom = Color(bottom)

    fun sky(phase: Phase, scene: Scene) = Sky(top, mid, bottom, isLight = phase == Phase.DAY, sunAlpha = sun, phase = phase, scene = scene)
}

/** The theme every screen draws its sky from. Set once at the top of each activity. */
val LocalSkyTheme = staticCompositionLocalOf { SkyTheme.SUNRISE }
