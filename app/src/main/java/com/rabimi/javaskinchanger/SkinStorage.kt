package com.rabimi.javaskinchanger

import android.app.Activity
import android.net.Uri
import java.io.File

object SkinStorage {

    fun saveSkin(activity: Activity, uri: Uri, name: String): Boolean {
        return try {
            val input = activity.contentResolver.openInputStream(uri)
            val dir = File(activity.filesDir, "skins")
            if (!dir.exists()) dir.mkdirs()
            val out = File(dir, "$name.png").outputStream()
            input?.copyTo(out)
            input?.close()
            out.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun loadAllSkins(activity: Activity): List<Uri> {
        val dir = File(activity.filesDir, "skins")
        if (!dir.exists()) return emptyList()
        return dir.listFiles()?.map { Uri.fromFile(it) } ?: emptyList()
    }
}