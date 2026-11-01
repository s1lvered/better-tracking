package com.example.pulseworkout.data

data class CardioOption(val id: String, val name: String, val met: Double)

data class CategoryGroup(val id: String, val name: String, val categories: List<String>)

object Catalog {
    const val UNCATEGORIZED = "Без категории"

    val colors = listOf(
        "lime", "purple", "coral", "cyan", "amber", "blue", "pink", "orange",
        "indigo", "mint", "red", "teal", "yellow", "gray", "violet", "sky",
    )

    val cardioOptions = listOf(
        CardioOption("treadmill", "Беговая дорожка", 8.5),
        CardioOption("upright-bike", "Вертикальный велотренажёр", 7.0),
        CardioOption("recumbent-bike", "Горизонтальный велотренажёр", 6.0),
        CardioOption("spin-bike", "Сайкл-тренажёр", 8.0),
        CardioOption("elliptical", "Эллипс", 6.5),
        CardioOption("stair-climber", "Лестница-степпер", 8.0),
        CardioOption("stepper", "Степпер", 6.0),
        CardioOption("rower", "Гребной тренажёр", 7.0),
        CardioOption("ski-erg", "Лыжный эргометр", 8.0),
        CardioOption("air-bike", "Аэробайк", 8.0),
        CardioOption("arc-trainer", "Арк-тренажёр", 7.0),
        CardioOption("jump-rope", "Скакалка", 11.8),
    )

    val categoryGroups = listOf(
        CategoryGroup("upper", "Верх тела", listOf("Грудь", "Спина", "Поясница", "Плечи", "Трапеции и шея", "Бицепс", "Трицепс", "Предплечья и хват")),
        CategoryGroup("lower", "Низ тела", listOf("Квадрицепс", "Задняя поверхность бедра", "Ягодицы", "Приводящие и отводящие мышцы", "Икры")),
        CategoryGroup("core", "Кор", listOf("Пресс и кор", "Всё тело")),
        CategoryGroup("cardio", "Кардио", listOf("Кардио")),
    )

    val categories: List<String> = categoryGroups.flatMap { it.categories }
    val groupByCategory: Map<String, String> = categoryGroups.flatMap { g -> g.categories.map { it to g.id } }.toMap()

    private val fixedCategoryColors = mapOf(
        "Грудь" to "coral",
        "Спина" to "purple",
        "Поясница" to "indigo",
        "Плечи" to "blue",
        "Трапеции и шея" to "cyan",
        "Бицепс" to "amber",
        "Трицепс" to "orange",
        "Предплечья и хват" to "yellow",
        "Квадрицепс" to "lime",
        "Задняя поверхность бедра" to "mint",
        "Ягодицы" to "pink",
        "Приводящие и отводящие мышцы" to "teal",
        "Икры" to "red",
        "Пресс и кор" to "violet",
        "Всё тело" to "gray",
        "Кардио" to "sky",
    )

    private val categoryAliases = mapOf(
        "ноги" to "Квадрицепс",
        "пресс" to "Пресс и кор",
        "пресс и кор" to "Пресс и кор",
        "трапеции" to "Трапеции и шея",
        "предплечья" to "Предплечья и хват",
        "бицепс/трицепс" to "Бицепс",
        "спина/плечи" to "Спина",
    )

    fun normalizeCategory(value: String?): String {
        val category = value.orEmpty().trim()
        if (category.isEmpty() || category.equals(UNCATEGORIZED, ignoreCase = true)) return UNCATEGORIZED
        categoryAliases[category.lowercase()]?.let { return it }
        return if (category in categories) category else UNCATEGORIZED
    }

    fun categoryColor(category: String): String =
        if (category == UNCATEGORIZED) "coral" else fixedCategoryColors[category] ?: "gray"
}
