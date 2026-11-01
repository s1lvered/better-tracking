package com.example.pulseworkout.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

@Serializable
data class GithubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val url: String = "",
    val size: Long = 0,
)

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tag: String = "",
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val pageUrl: String = "",
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GithubAsset> = emptyList(),
) {
    /** Номер версии из тега: «v2.1.0» → «2.1.0». */
    val version: String get() = tag.trim().removePrefix("v").removePrefix("V")
    val apk: GithubAsset? get() = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
}

/** Состояние обновления для попапа. */
sealed interface UpdateState {
    data class Available(val release: GithubRelease) : UpdateState
    data class Downloading(val release: GithubRelease, val progress: Float?) : UpdateState
    data class Ready(val release: GithubRelease, val file: File) : UpdateState
    data class Failed(val release: GithubRelease, val message: String) : UpdateState
}

/**
 * Проверка обновлений через последний релиз GitHub (`/repos/{owner}/{repo}/releases/latest`).
 * Новая версия — если номер из тега больше [currentVersion]; APK берётся из файлов релиза.
 */
class Updater(private val context: Context, private val repo: String, private val currentVersion: String) {
    private val json = Json { ignoreUnknownKeys = true }

    val enabled: Boolean get() = repo.matches(Regex("[\\w.-]+/[\\w.-]+"))

    /** Возвращает релиз, если он новее установленной версии; null — обновления нет или сеть недоступна. */
    suspend fun check(): GithubRelease? = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext null
        runCatching {
            val conn = (URL("https://api.github.com/repos/$repo/releases/latest").openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "BetterTracking/$currentVersion")
            }
            try {
                if (conn.responseCode != 200) return@runCatching null
                val release = json.decodeFromString<GithubRelease>(conn.inputStream.bufferedReader().use { it.readText() })
                release.takeIf { !it.draft && !it.prerelease && isNewer(it.version, currentVersion) }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    /** Скачивает APK в кэш приложения, сообщая прогресс 0..1 (null — размер неизвестен). */
    suspend fun download(asset: GithubAsset, onProgress: (Float?) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // старые загрузки больше не нужны
        val target = File(dir, asset.name.ifBlank { "update.apk" })
        val conn = (URL(asset.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 30000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "BetterTracking/$currentVersion")
        }
        try {
            if (conn.responseCode !in 200..299) error("Сервер ответил ${conn.responseCode}")
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: asset.size.takeIf { it > 0 }
            var read = 0L
            var lastReported = -1
            conn.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        val percent = total?.let { (read * 100 / it).toInt() } ?: -1
                        if (percent != lastReported) {
                            lastReported = percent
                            withContext(Dispatchers.Main) { onProgress(total?.let { read.toFloat() / it }) }
                        }
                    }
                }
            }
            if (total != null && read != total) error("Файл загружен не полностью")
            target
        } catch (e: Exception) {
            target.delete()
            throw e
        } finally {
            conn.disconnect()
        }
    }

    /** Нужно ли сначала разрешить установку из этого приложения (Android 8+). */
    fun needsInstallPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Открывает системный установщик для скачанного APK. */
    fun install(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openPage(release: GithubRelease) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.pageUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    companion object {
        /** Сравнивает версии по числовым частям: «2.10.0» > «2.9.3», «2.1» == «2.1.0». */
        fun isNewer(candidate: String, current: String): Boolean {
            fun parts(v: String) = v.substringBefore('-').split('.', '_').map { p -> p.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
            val a = parts(candidate)
            val b = parts(current)
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}
