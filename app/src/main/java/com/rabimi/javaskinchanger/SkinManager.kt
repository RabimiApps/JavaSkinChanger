package com.rabimi.javaskinchanger

import android.content.Context
import android.net.Uri
import android.widget.Toast

object SkinManager {
    fun uploadSkin(context: Context, skinUri: Uri, isSteve: Boolean) {
        // TODO: Mojang API 認証処理 & スキンアップロードを実装
        // 現在は仮の処理
        Toast.makeText(context, "スキンをアップロード中…", Toast.LENGTH_SHORT).show()

        // 成功メッセージ
        Toast.makeText(context, "スキンを変更しました！", Toast.LENGTH_SHORT).show()
    }
}