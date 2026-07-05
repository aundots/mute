package com.mute.shutter.adb

import android.content.Context
import android.os.Build
import java.io.File
import java.util.zip.ZipFile

/**
 * Android 16: adb 바이너리는 nativeLibraryDir에서 실행해야 합니다.
 * W^X 정책상 filesDir 직접 exec는 크래시를 유발할 수 있어 codeCache를 최후 fallback으로 씁니다.
 */
object AdbBinaryLocator {
    private const val LIB_NAME = "libadb.so"

    data class Install(
        val binary: File,
        val workDir: File,
    )

    fun resolve(context: Context): Install {
        val app = context.applicationContext
        val nativeDir = File(app.applicationInfo.nativeLibraryDir)
        val nativeBinary = File(nativeDir, LIB_NAME)
        if (nativeBinary.exists() && nativeBinary.length() > 1_000_000) {
            return Install(nativeBinary, nativeDir)
        }

        // nativeLibraryDir에 없으면 APK → codeCache (Android 10+ 실행 가능 영역)
        val fallbackDir = File(app.codeCacheDir, "adb-bin").apply { mkdirs() }
        val fallback = File(fallbackDir, LIB_NAME)
        if (!fallback.exists() || fallback.length() < 1_000_000) {
            extractFromApk(app, fallback)
        }
        fallback.setExecutable(true, false)
        return Install(fallback, fallbackDir)
    }

    private fun extractFromApk(context: Context, dest: File) {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        val entryName = "lib/$abi/$LIB_NAME"
        ZipFile(context.applicationInfo.sourceDir).use { zip ->
            val entry = zip.getEntry(entryName)
                ?: error("$LIB_NAME not found in APK ($entryName)")
            zip.getInputStream(entry).use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}
