package com.rabimi.javaskinchanger

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

object MinecraftSkinManager {

    // 現在のスキン画像 URL を取得
    fun getCurrentSkinUrl(mcToken: String): String? {
        return try {
            val url = URL("https://api.minecraftservices.com/minecraft/profile")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $mcToken")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val resp = try {
                conn.inputStream.bufferedReader().readText()
            } catch (e: Exception) {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            }

            if (conn.responseCode in 200..299 && resp.isNotEmpty()) {
                val json = JSONObject(resp)
                val textures = json.getJSONObject("skins")
                // 通常 Minecraft の API はスキン配列なので最初のスキンを返す
                if (textures.has("value")) {
                    textures.getString("value")
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 選択したスキンをアップロード
    fun uploadSkin(context: Context, skinUri: Uri, mcToken: String): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(skinUri) ?: return false
            val bytes = inputStream.readBytes()
            inputStream.close()

            val url = URL("https://api.minecraftservices.com/minecraft/profile/skins")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $mcToken")
            conn.setRequestProperty("Content-Type", "image/png")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val os: OutputStream = conn.outputStream
            os.write(bytes)
            os.flush()
            os.close()

            conn.responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}