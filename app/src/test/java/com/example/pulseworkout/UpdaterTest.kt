package com.example.pulseworkout

import com.example.pulseworkout.update.GithubRelease
import com.example.pulseworkout.update.Updater
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {
    @Test
    fun comparesVersionsNumerically() {
        assertTrue(Updater.isNewer("2.1.0", "2.0.0"))
        assertTrue(Updater.isNewer("2.10.0", "2.9.3"))
        assertTrue(Updater.isNewer("3", "2.9.9"))
        assertFalse(Updater.isNewer("2.1", "2.1.0"))
        assertFalse(Updater.isNewer("2.0.9", "2.1.0"))
        assertFalse(Updater.isNewer("2.1.0-beta", "2.1.0"))
    }

    @Test
    fun parsesGithubRelease() {
        val body = """{"tag_name":"v2.2.0","name":"2.2","body":"Исправления","html_url":"https://github.com/o/r/releases/tag/v2.2.0",
            "draft":false,"prerelease":false,"extra":1,
            "assets":[{"name":"notes.txt","browser_download_url":"https://x/notes.txt","size":10},
                      {"name":"Better-Tracking-2.2.0.apk","browser_download_url":"https://x/app.apk","size":3146421}]}"""
        val release = Json { ignoreUnknownKeys = true }.decodeFromString<GithubRelease>(body)
        assertEquals("2.2.0", release.version)
        assertEquals("https://x/app.apk", release.apk?.url)
        assertTrue(Updater.isNewer(release.version, "2.1.0"))
    }
}
