package com.example.pulseworkout.data

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.Executors

/** Хранит состояние приложения в JSON-файле во внутреннем хранилище (аналог localStorage). */
class Repository(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = true
    }
    private val file get() = File(context.filesDir, "pulse-workout-state-v1.json")

    val catalog: List<Exercise> by lazy {
        val text = context.assets.open("exercise-catalog.json").bufferedReader().use { it.readText() }
        json.decodeFromString(ListSerializer(Exercise.serializer()), text)
    }

    fun load(): AppState {
        val saved = runCatching { json.decodeFromString(AppState.serializer(), file.readText()) }.getOrNull()
        return normalize(saved ?: AppState())
    }

    // Сериализуем на вызывающем потоке (модель меняется только там), пишем на диск в фоне по порядку.
    private val writer = Executors.newSingleThreadExecutor()

    fun save(state: AppState) {
        val text = json.encodeToString(AppState.serializer(), state)
        writer.execute {
            runCatching {
                val tmp = File(context.filesDir, "state.tmp")
                tmp.writeText(text)
                if (!tmp.renameTo(file)) {
                    file.delete()
                    tmp.renameTo(file)
                }
            }
        }
    }

    /** Порт normalizeState() из app.js: подмешивает встроенный каталог, чинит категории и цвета сплитов. */
    private fun normalize(saved: AppState): AppState {
        val savedById = saved.exercises.associateBy { it.id }
        val catalogIds = catalog.map { it.id }.toSet()
        val custom = saved.exercises.filter { it.id !in catalogIds }.map { exercise ->
            val category = Catalog.normalizeCategory(exercise.category.ifBlank { exercise.muscle })
            exercise.copy(category = category, muscle = if (category == Catalog.UNCATEGORIZED) "" else category)
        }
        saved.exercises = (catalog.map { base ->
            val stored = savedById[base.id]
            base.copy(
                splitIds = stored?.splitIds?.toMutableList() ?: mutableListOf(),
                universal = stored?.universal ?: false,
                archived = stored?.archived ?: false,
            )
        } + custom).toMutableList()
        saved.catalogVersion = CATALOG_VERSION
        if (saved.otherName.isBlank()) saved.otherName = "Прочее"
        saved.cardioHiddenIds = saved.cardioHiddenIds.distinct().toMutableList()

        val used = mutableSetOf<String>()
        saved.splits.forEachIndexed { index, split ->
            var color = split.color
            if (color !in Catalog.colors || color in used) {
                color = Catalog.colors.firstOrNull { it !in used } ?: Catalog.colors[index % Catalog.colors.size]
            }
            split.color = color
            used += color
        }
        return saved
    }
}
