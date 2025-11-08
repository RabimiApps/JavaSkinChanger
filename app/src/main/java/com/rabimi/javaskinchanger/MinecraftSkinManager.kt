package com.rabimi.javaskinchanger

import android.content.Context
import android.net.Uri

object MinecraftSkinManager {

    fun getCurrentSkinUrl(token: String): String {
        // ここで Minecraft API 呼び出してスキンURLを返す
        return "https://minecraft.net/skin/placeholder.png"
    }

    fun uploadSkin(context: Context, uri: Uri, token: String) {
        // Minecraft API を呼んでアップロード
    }
}