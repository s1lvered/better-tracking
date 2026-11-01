package com.example.pulseworkout.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Токены дизайн-системы SilverGains (Figma → Variable collection).
 * Названия повторяют переменные: White/Primary…Opacity, Black/Primary…Opacity, Lime/Accent…Secondary, Negative/Primary.
 */
object Ds {
    val white = Color(0xFFFFFFFF)          // White/Primary
    val whiteSecondary = Color(0xCCFFFFFF) // White/Secondary 80%
    val whiteTertiary = Color(0x99FFFFFF)  // White/Tertiary 60%
    val whiteQuaternary = Color(0x66FFFFFF) // White/Quaternary 40%
    val whiteQuinternary = Color(0x33FFFFFF) // White/Quinternary 20%
    val whiteOpacity = Color(0x1AFFFFFF)   // White/Opacity 10%

    val black = Color(0xFF212121)          // Black/Primary
    val blackSecondary = Color(0xCC212121) // 80%
    val blackTertiary = Color(0x99212121)  // 60%
    val blackQuaternary = Color(0x66212121) // 40%
    val blackQuinternary = Color(0x33212121) // 20%
    val blackOpacity = Color(0x1A212121)   // 10%

    val lime = Color(0xFFA6DE20)           // Lime/Accent
    val limeGradient = Color(0xFFCBFF4D)   // Lime/Gradient
    val limeSecondary = Color(0x99A6DE20)  // Lime/Secondary
    val negative = Color(0xFFFF4347)       // Negative/Primary
    val sheet = Color(0xFF00090D)          // Surface/Sheet
    val record = Color(0xFFF4B536)         // корона рекорда (crown_fill)

    // Фон экрана: тёмный линейный градиент с лаймовым радиальным отсветом
    val screenTop = Color(0xFF040009)
    val screenBottom = Color(0xFF0C0B0C)
    val screenGlow = Color(0xFF212F02)

    /** Заливка Button/Primary и FAB: Lime/Accent → Lime/Gradient. */
    val primaryBrush = Brush.horizontalGradient(listOf(lime, limeGradient))
    /** Picker и TimerHero: градиентная обводка White 28% → 8%. */
    val borderBrush = Brush.verticalGradient(listOf(Color(0x47FFFFFF), Color(0x14FFFFFF)))
}

/** Совместимые имена для экранов; все значения берутся из токенов [Ds]. */
object Bt {
    val bg = Ds.screenTop
    val surface = Ds.whiteOpacity
    val surfaceHi = Ds.whiteOpacity
    val surfaceTop = Ds.whiteQuinternary
    val field = Ds.blackQuaternary
    val stroke = Ds.whiteOpacity
    val strokeStrong = Ds.whiteQuinternary

    val text = Ds.white
    val muted = Ds.whiteTertiary
    val faint = Ds.whiteQuaternary

    val lime = Ds.lime
    val limeBright = Ds.lime
    val onLime = Ds.black
    val danger = Ds.negative
    val dangerSoft = Ds.negative
    val good = Ds.lime
    val bad = Ds.negative

    val panel get() = surface
    val panelRaised get() = surfaceHi
    val panelSubtle get() = surfaceTop
    val line get() = stroke
    val muted2 get() = faint

    val cardBrush: Brush = SolidColor(Ds.whiteOpacity)
    val heroBrush: Brush = SolidColor(Ds.blackTertiary)

    private val named = mapOf(
        "lime" to Color(0xFFA6DE20),
        "purple" to Color(0xFF9C8CFF),
        "coral" to Color(0xFFFF776F),
        "cyan" to Color(0xFF61D9CE),
        "amber" to Color(0xFFF4B536),
        "blue" to Color(0xFF65A9FF),
        "pink" to Color(0xFFF27DB2),
        "orange" to Color(0xFFFF9657),
        "indigo" to Color(0xFF7F8CFF),
        "mint" to Color(0xFF65D6A8),
        "red" to Color(0xFFFF4347),
        "teal" to Color(0xFF2FC4B2),
        "yellow" to Color(0xFFF4C969),
        "gray" to Color(0xFF8B929D),
        "violet" to Color(0xFFC985FF),
        "sky" to Color(0xFF46B9FF),
    )

    fun color(name: String?): Color = named[name] ?: named.getValue("lime")
}

/**
 * Текстовые стили дизайн-системы (Roboto, Regular/Bold; размер/интерлиньяж в px из Figma).
 * Имена совпадают со стилями в Figma: Label1R…Display1B.
 */
object DsType {
    private fun s(size: Int, line: Int, bold: Boolean) = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = size.sp,
        lineHeight = line.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = Ds.white,
    )

    val Footnote = s(9, 14, false)
    val Label1R = s(11, 16, false); val Label1B = s(11, 16, true)
    val Label2R = s(12, 16, false); val Label2B = s(12, 16, true)
    val Label3R = s(14, 20, false); val Label3B = s(14, 20, true)
    val Body1R = s(12, 16, false); val Body1B = s(12, 16, true)
    val Body2R = s(14, 20, false); val Body2B = s(14, 20, true)
    val Body3R = s(16, 24, false); val Body3B = s(16, 24, true)
    val Title1R = s(14, 20, false); val Title1B = s(14, 20, true)
    val Title2R = s(16, 24, false); val Title2B = s(16, 24, true)
    val Title3R = s(22, 28, false); val Title3B = s(22, 28, true)
    val Headline1R = s(24, 32, false); val Headline1B = s(24, 32, true)
    val Headline2R = s(28, 36, false); val Headline2B = s(28, 36, true)
    val Headline3R = s(32, 40, false); val Headline3B = s(32, 40, true)
    val Display1R = s(40, 48, false); val Display1B = s(40, 48, true)
}

/** Роли текста в приложении, сопоставленные со стилями [DsType]. */
object BtType {
    val display = DsType.Headline1B
    val title = DsType.Title3B
    val cardTitle = DsType.Title2B
    val body = DsType.Body2R
    val caption = DsType.Label2R.copy(color = Ds.whiteTertiary)
    val label = DsType.Label2R.copy(color = Ds.whiteTertiary)
    val number = DsType.Title2B.copy(fontFeatureSettings = "tnum")
    val timer = DsType.Display1B.copy(fontFeatureSettings = "tnum")
}

@Composable
fun BetterTrackingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ds.lime,
            onPrimary = Ds.black,
            secondary = Ds.lime,
            background = Ds.screenTop,
            onBackground = Ds.white,
            surface = Ds.sheet,
            onSurface = Ds.white,
            surfaceVariant = Ds.sheet,
            onSurfaceVariant = Ds.whiteTertiary,
            surfaceContainerLow = Ds.sheet,
            surfaceContainer = Ds.sheet,
            surfaceContainerHigh = Ds.sheet,
            surfaceContainerHighest = Color(0xFF1A1D1F),
            outline = Ds.whiteQuaternary,
            outlineVariant = Ds.whiteOpacity,
            error = Ds.negative,
        ),
        typography = MaterialTheme.typography.let { t ->
            t.copy(
                bodyLarge = DsType.Body3R, bodyMedium = DsType.Body2R, bodySmall = DsType.Body1R,
                labelLarge = DsType.Label3B, labelMedium = DsType.Label2B, labelSmall = DsType.Label1B,
                titleLarge = DsType.Title3B, titleMedium = DsType.Title2B, titleSmall = DsType.Title1B,
            )
        },
        content = content,
    )
}
