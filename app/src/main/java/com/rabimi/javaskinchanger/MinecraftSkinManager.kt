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

                // "skins" 配列を取得
                val skinsArray = json.optJSONArray("skins")
                if (skinsArray != null && skinsArray.length() > 0) {
                    for (i in 0 until skinsArray.length()) {
                        val skin = skinsArray.getJSONObject(i)
                        val state = skin.optString("state")
                        if (state == "ACTIVE") { // 現在のスキンを優先
                            val skinUrl = skin.optString("url", null)
                            Log.d("MinecraftSkinManager", "Active Skin URL: $skinUrl")
                            return skinUrl
                        }
                    }
                    // ACTIVEがなければ最初のスキンを返す
                    val firstSkinUrl = skinsArray.getJSONObject(0).optString("url", null)
                    Log.d("MinecraftSkinManager", "Fallback Skin URL: $firstSkinUrl")
                    firstSkinUrl
                } else {
                    Log.e("MinecraftSkinManager", "No skins array in profile")
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