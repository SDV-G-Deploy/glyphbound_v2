package com.sdvgdeploy.glyphbound

import android.util.Log
import com.sdvgdeploy.glyphbound.core.model.SaveCodec
import com.sdvgdeploy.glyphbound.core.model.SaveGame
import java.io.File

/** Single-slot save file. Writes are atomic (temp file + rename); unreadable saves are ignored. */
class SaveStore(private val file: File) {
    fun load(): SaveGame? {
        if (!file.exists()) return null
        val raw = runCatching { file.readText() }.getOrNull() ?: return null
        return when (val result = SaveCodec.decode(raw)) {
            is SaveCodec.DecodeResult.Ok -> result.save
            is SaveCodec.DecodeResult.UnsupportedVersion -> {
                Log.w(TAG, "Ignoring save with unsupported schema ${result.found}")
                null
            }
            is SaveCodec.DecodeResult.Corrupt -> {
                Log.w(TAG, "Ignoring corrupt save: ${result.reason}")
                null
            }
        }
    }

    fun write(save: SaveGame) {
        runCatching {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(SaveCodec.encode(save))
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }.onFailure { Log.w(TAG, "Save failed", it) }
    }

    private companion object {
        const val TAG = "GlyphboundSave"
    }
}
