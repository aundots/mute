package com.mute.shutter.adb

import android.content.Context
import java.io.File

/**
 * Android 10+ W^X: adb는 [nativeLibraryDir]에 추출된 libadb.so만 직접 실행 가능.
 * build.gradle에서 useLegacyPackaging=true + manifest extractNativeLibs=true 필수.
 */
object AdbBinaryLocator {
    private const val LIB_NAME = "libadb.so"
    private const val MIN_BINARY_BYTES = 1_000_000L

    data class Install(
        val binary: File,
        val workDir: File,
    )

    fun resolve(context: Context): Install {
        val app = context.applicationContext
        val nativeDir = File(app.applicationInfo.nativeLibraryDir)
        val nativeBinary = File(nativeDir, LIB_NAME)
        if (nativeBinary.exists() && nativeBinary.length() >= MIN_BINARY_BYTES) {
            return Install(nativeBinary, nativeDir)
        }

        error(
            buildString {
                append("ADB 바이너리를 실행할 수 없습니다. ")
                append("앱을 완전히 삭제한 뒤 다시 설치해주세요. ")
                append("(경로: ${nativeBinary.absolutePath}, ")
                append("존재=${nativeBinary.exists()}, ")
                append("크기=${nativeBinary.length()})")
            },
        )
    }
}
