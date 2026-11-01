package com.example.pulseworkout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Удаляет сохранённое состояние приложения до и после UI-теста, чтобы тесты не зависели от порядка.
 * Repository пишет файл в фоне, поэтому после теста ждём, пока запись закончится.
 */
class CleanStateRule : TestWatcher() {
    private fun clean() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.filesDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    override fun starting(description: Description) = clean()

    override fun finished(description: Description) {
        Thread.sleep(500)
        clean()
    }
}
