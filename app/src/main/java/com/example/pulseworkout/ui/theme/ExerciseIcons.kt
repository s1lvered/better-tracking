package com.example.pulseworkout.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.example.pulseworkout.data.Catalog

/**
 * Иконки упражнений (Figma BETR, страница «Icons · Exercises»): контур 1,4 на сетке 24×24, скруглённые концы и стыки.
 * В отличие от [DsIcons] это обводка, а не заливка; цвет задаётся через `tint` у `Icon`.
 */
object ExerciseIcons {
    private fun icon(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach { d ->
            b.addPath(
                PathParser().parsePathString(d).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} ${cy}A$r $r 0 1 0 ${cx + r} ${cy}A$r $r 0 1 0 ${cx - r} ${cy}Z"

    private fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float) =
        "M${cx - rx} ${cy}A$rx $ry 0 1 0 ${cx + rx} ${cy}A$rx $ry 0 1 0 ${cx - rx} ${cy}Z"

    private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r} ${y}H${x + w - r}A$r $r 0 0 1 ${x + w} ${y + r}V${y + h - r}A$r $r 0 0 1 ${x + w - r} ${y + h}" +
            "H${x + r}A$r $r 0 0 1 $x ${y + h - r}V${y + r}A$r $r 0 0 1 ${x + r} ${y}Z"

    /** Блин: окружность с точкой-втулкой. */
    private fun plate(cx: Float, cy: Float, r: Float) = arrayOf(circle(cx, cy, r), circle(cx, cy, 0.5f))

    // ---------------- группы мышц ----------------

    val Chest by lazy { icon("chest", "M2.5 17.5H16.5M4.5 17.5V21M14.5 17.5V21M7 15.5H13.5L16.5 12.5L19.5 21M10 15.5V9.5", circle(4.8f, 15f, 1.7f), *plate(10f, 6.6f, 2.9f)) }
    val Back by lazy { icon("back", "M3 3H21M6.5 3L7.5 8L9.5 10H14.5L16.5 8L17.5 3M12 10V16M12 16L10.5 21M12 16L13.5 21", circle(12f, 7.3f, 1.8f)) }
    val LowerBack by lazy { icon("lower-back", "M9.5 21.5L11 16.5L9.8 12L16.2 8.2L14.6 15", circle(18.3f, 6.8f, 1.8f), *plate(14.2f, 18.2f, 2.9f)) }
    val Shoulders by lazy { icon("shoulders", "M3 9H21M3 7V11M21 7V11M12 9V15.5M12 15.5L10.5 21M12 15.5L13.5 21", circle(12f, 5.4f, 1.9f)) }
    val Traps by lazy {
        icon(
            "traps",
            "M7.5 11.5L9.5 10H14.5L16.5 11.5M7.5 11.5V17M16.5 11.5V17M6 17.5H9M15 17.5H18M12 10V16M12 16L10.5 21.5M12 16L13.5 21.5" +
                "M4.5 8V3.5M3 5L4.5 3.5L6 5M19.5 8V3.5M18 5L19.5 3.5L21 5",
            circle(12f, 6.8f, 1.8f),
        )
    }
    val Biceps by lazy { icon("biceps", "M12 6.6V14M12 14L11 21.5M12 14L13.5 21.5M12 7.6L12.6 12.2L16 9M15 7V10M18.5 7V10M15 8.5H18.5", circle(12f, 4.2f, 1.8f)) }
    val Triceps by lazy {
        icon("triceps", "M11 6.8L10.5 14M10.5 14L9.5 21.5M10.5 14L12 21.5M11 7.6L12.2 12L16 15.5M17.5 3.5L16 15.5M14.8 16.6L17.2 14.4", circle(11f, 4.5f, 1.8f), circle(17.6f, 2.6f, 0.9f))
    }
    val Forearms by lazy {
        icon("forearms", "M8.5 8H15.5M8.5 8L7.5 14M15.5 8L16.5 14M12 6.8V15M12 15L10 21.5M12 15L14 21.5", circle(12f, 4.5f, 1.8f), rect(6f, 15f, 3f, 3.6f, 1f), rect(15f, 15f, 3f, 3.6f, 1f))
    }
    val Quads by lazy { icon("quads", "M11.5 21.5H14M11.5 21.5L14.5 14.5L8 13.8L12.4 7.6", circle(15.2f, 3.5f, 1.8f), *plate(9.6f, 6.6f, 2.6f)) }
    val Hamstrings by lazy { icon("hamstrings", "M3 15H20M5 15V19.5M18 15V19.5M6.3 13H17.5L15.6 7.2", circle(4.5f, 11.8f, 1.8f), circle(15.2f, 5.6f, 1.3f)) }
    val Glutes by lazy { icon("glutes", "M7 12.5L15 12.5M7 12.5V20.5M15 12.5L13.6 20.5H17.5M15 12.5L19.4 9.4L21 4.5", circle(4.6f, 10.8f, 1.8f)) }
    val Adductors by lazy {
        icon(
            "adductors",
            "M12 5.6V11M12 7L8.5 10.5M12 7L15.5 10.5M12 11L6.5 21.5M12 11L17.5 21.5M9.6 17.5H14.4M11 16.1L9.6 17.5L11 18.9M13 16.1L14.4 17.5L13 18.9",
            circle(12f, 3.2f, 1.8f),
        )
    }
    val Calves by lazy { icon("calves", "M11 5.8V13M11 13V19L13.5 20.5M11 7L10.3 12.5M9 21.5H17M17.5 12V5.5M15.8 7.2L17.5 5.5L19.2 7.2", circle(11f, 3.5f, 1.8f)) }
    val Abs by lazy { icon("abs", "M2 21H22M10 19L14.5 13.5L18.5 20M10 19L6 15.2L10.5 14.2", circle(4.4f, 13f, 1.8f)) }
    val FullBody by lazy { icon("full-body", "M12 6V13M12 7.5L6.5 4M12 7.5L17.5 4M12 13L7.5 21M12 13L16.5 21", circle(12f, 3.4f, 1.8f)) }
    val Cardio by lazy {
        icon("cardio", "M13.5 6L11 12.5M13 7.5L16 10L18.5 8.5M13 7.5L9.5 9L7.5 7.5M11 12.5L15 15L14 20.5M11 12.5L9 16.5L4.5 17.5", circle(14.6f, 3.6f, 1.8f))
    }

    // ---------------- кардио-тренажёры ----------------

    val Treadmill by lazy { icon("treadmill", rect(2.5f, 17.5f, 16f, 3f, 1.5f), "M17 17.5L19 6.5M16.5 6.5H21.5M5 11H9M3.5 14H8", circle(5f, 19f, 0.5f), circle(16f, 19f, 0.5f)) }
    val UprightBike by lazy { icon("upright-bike", "M3 21H21M8 21L9.5 9M7 9H12M16.5 21L17.5 6.5M15.5 6.5H20M9 13.5L17 12.5", circle(12.5f, 17f, 2.2f)) }
    val RecumbentBike by lazy { icon("recumbent-bike", "M3 21H21M4.5 10.5L6 16.5H11.5M8.5 16.5V21M18 21L17 9M15 9H19.5M11.5 17.5L14 17", circle(16.4f, 16.6f, 2.2f)) }
    val SpinBike by lazy { icon("spin-bike", "M4 21H11M13 21H20.5M5 7.5H9.5M7.5 7.5L9.5 16.5H16.5M16.5 12V5.5M14 5.5H18.5", *plate(16.5f, 16.5f, 4f)) }
    val Elliptical by lazy { icon("elliptical", "M3 21H21M18 21L17 8.5M17 8.5L19 3M17 8.5L8.5 16M6 16H11", ellipse(9.5f, 18.3f, 5.5f, 1.6f)) }
    val StairClimber by lazy { icon("stair-climber", "M3 20.5H7V16.5H11V12.5H15V8.5H19.5V20.5H3M5 12L17 3.5") }
    val Stepper by lazy {
        icon("stepper", "M3.5 21H20.5M5 13H10.5M13.5 17H19M7.75 13V21M16.25 17V21M7.75 9V3.5M6.25 5L7.75 3.5L9.25 5M16.25 6V11.5M14.75 10L16.25 11.5L17.75 10")
    }
    val Rower by lazy { icon("rower", "M2.5 18H15M4 18V21M13 18V21M5.5 16H10M14 18L15.5 15M15 14.5L10.5 12.5M10.2 11V14", *plate(18.5f, 14.5f, 3.5f)) }
    val SkiErg by lazy { icon("ski-erg", "M8.5 21.5H19.5M15 21.5V9M13.4 6.6L7.5 13M14.2 7.9L10.5 15.5M6.4 12L8.6 14M9.2 14.6L11.8 16.4", circle(15f, 5.5f, 2.5f)) }
    val AirBike by lazy {
        icon("air-bike", "M3.5 21H11M5 7H9.5M7.5 7L9.5 15.5H16M13 3.5L16 11", circle(16f, 15.5f, 4.5f), "M16 11.8V19.2M12.8 13.7L19.2 17.3M12.8 17.3L19.2 13.7")
    }
    val ArcTrainer by lazy { icon("arc-trainer", "M3 21H21M18.5 21L17.5 8.5M17.5 8.5L19.5 3M17.5 8.5L9 15M6.5 15H11.5M3.5 19C7 14.5 12 14 16 17.5") }
    val JumpRope by lazy { icon("jump-rope", rect(4.8f, 2.5f, 2.4f, 6.5f, 1.2f), rect(16.8f, 2.5f, 2.4f, 6.5f, 1.2f), "M6 9C6 23 18 23 18 9") }

    // ---------------- инвентарь ----------------

    val Barbell by lazy {
        icon("barbell", "M2 12H22", rect(4f, 6f, 2.6f, 12f, 1.3f), rect(17.4f, 6f, 2.6f, 12f, 1.3f), rect(7.6f, 8.5f, 1.8f, 7f, 0.9f), rect(14.6f, 8.5f, 1.8f, 7f, 0.9f))
    }
    val EZBar by lazy { icon("ez-bar", "M2 12H7.6M16.4 12H22M9.4 12L10.4 10.5L12 13.5L13.6 10.5L14.6 12", rect(4f, 6f, 2.6f, 12f, 1.3f), rect(17.4f, 6f, 2.6f, 12f, 1.3f)) }
    val Plate by lazy { icon("plate", circle(12f, 12f, 9.5f), circle(12f, 12f, 6f), circle(12f, 12f, 1.6f)) }
    val Cable by lazy { icon("cable", "M9 2.5H15M12 2.5V3M12 7.4V16M8.5 16.5H15.5M9.5 16.5L10.5 20.5H13.5L14.5 16.5", circle(12f, 5.2f, 2.2f)) }
    val Rope by lazy { icon("rope", "M12 2.5V7.5M12 7.5L8 18M12 7.5L16 18", circle(7.5f, 19.6f, 1.6f), circle(16.5f, 19.6f, 1.6f)) }
    val Smith by lazy { icon("smith", "M5 2.5V21.5M19 2.5V21.5M2.5 21.5H8M16 21.5H21.5M2.5 10H21.5", rect(6.8f, 7f, 1.8f, 6f, 0.9f), rect(15.4f, 7f, 1.8f, 6f, 0.9f)) }
    val Machine by lazy { icon("machine", rect(6f, 2.5f, 12f, 19f, 2f), "M8.5 13.5H15.5M8.5 16H15.5M8.5 18.5H15.5M12 5V11") }
    val Bench by lazy { icon("bench", rect(3f, 9f, 18f, 3f, 1.5f), "M6.5 12V20M17.5 12V20M4.5 20H8.5M15.5 20H19.5") }
    val Band by lazy { icon("band", rect(2.5f, 7.5f, 19f, 9f, 4.5f), rect(5.5f, 10f, 13f, 4f, 2f)) }

    /** Иконка категории каталога; для «Прочее» и неизвестных — общая иконка упражнения. */
    fun forCategory(category: String): ImageVector = when (category) {
        "Грудь" -> Chest
        "Спина" -> Back
        "Поясница" -> LowerBack
        "Плечи" -> Shoulders
        "Трапеции и шея" -> Traps
        "Бицепс" -> Biceps
        "Трицепс" -> Triceps
        "Предплечья и хват" -> Forearms
        "Квадрицепс" -> Quads
        "Задняя поверхность бедра" -> Hamstrings
        "Ягодицы" -> Glutes
        "Приводящие и отводящие мышцы" -> Adductors
        "Икры" -> Calves
        "Пресс и кор" -> Abs
        "Всё тело" -> FullBody
        "Кардио" -> Cardio
        else -> DsIcons.Exercise
    }

    /** Иконка варианта кардио по id из [Catalog.cardioOptions]; своё кардио — бегущий человек. */
    fun forCardio(id: String?): ImageVector = when (id) {
        "treadmill" -> Treadmill
        "upright-bike" -> UprightBike
        "recumbent-bike" -> RecumbentBike
        "spin-bike" -> SpinBike
        "elliptical" -> Elliptical
        "stair-climber" -> StairClimber
        "stepper" -> Stepper
        "rower" -> Rower
        "ski-erg" -> SkiErg
        "air-bike" -> AirBike
        "arc-trainer" -> ArcTrainer
        "jump-rope" -> JumpRope
        else -> Cardio
    }
}
