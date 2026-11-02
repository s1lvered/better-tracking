package com.example.pulseworkout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File

/**
 * Удаляет сохранённое состояние приложения до и после UI-теста, чтобы тесты не зависели от порядка.
 * Repository пишет файл в фоне, поэтому после теста ждём, пока запись закончится.
 *
 * Robolectric даёт каждому тесту свой filesDir, но в одном прогоне приложение может продолжать писать
 * в папку первого теста. Поэтому чистим все папки, которые встречались в этом прогоне.
 */
class CleanStateRule : TestWatcher() {
    private fun clean() {
        seenDirs += ApplicationProvider.getApplicationContext<Context>().filesDir
        seenDirs.forEach { dir -> dir.listFiles()?.forEach { it.deleteRecursively() } }
    }

    override fun starting(description: Description) = clean()

    override fun finished(description: Description) {
        Thread.sleep(500)
        clean()
    }

    private companion object {
        val seenDirs = mutableSetOf<File>()
    }
}
