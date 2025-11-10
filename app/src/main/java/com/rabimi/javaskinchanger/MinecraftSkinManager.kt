package com.rabimi.javaskinchanger

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

object MinecraftSkinManager {

    // 現在のスキン画像URLを取得
    fun getCurrentSkinUrl(mcToken: String): String? {
        return try {
            val url = URL("https://api.minecraftservices.com/minecraft/profile")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $mcToken")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val response = conn.inputStream.bufferedReader().readText()
            Log.d("MinecraftSkinManager", "Profile response: $response")

            if (conn.responseCode in 200..299) {
                val json = JSONObject(response)
                val skinsArray = json.optJSONArray("skins")

                if (skinsArray != null && skinsArray.length() > 0) {
                    val firstSkin = skinsArray.getJSONObject(0)
                    val skinUrl = firstSkin.optString("url", null)
                    Log.d("MinecraftSkinManager", "Skin URL: $skinUrl")
                    skinUrl
                } else {
                    Log.e("MinecraftSkinManager", "No skin data found in response")
                    null
                }
            } else {
                Log.e("MinecraftSkinManager", "HTTP Error: ${conn.responseCode}")
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

            val result = conn.responseCode in 200..299
            Log.d("MinecraftSkinManager", "Upload result: $result (${conn.responseCode})")
            result
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}