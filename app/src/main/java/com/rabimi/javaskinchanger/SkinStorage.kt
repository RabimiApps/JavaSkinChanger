package com.rabimi.javaskinchanger

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object SkinStorage {

    fun getSkinDir(context: Context): File {
        val dir = File(context.filesDir, "skins")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun saveSkin(context: Context, uri: Uri, name: String): Boolean {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return false
            val file = File(getSkinDir(context), "$name.png")
            val output = FileOutputStream(file)
            input.copyTo(output)
            input.close()
            output.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun listSkins(context: Context): List<File> {
        return getSkinDir(context).listFiles()?.toList() ?: emptyList()
    }
}