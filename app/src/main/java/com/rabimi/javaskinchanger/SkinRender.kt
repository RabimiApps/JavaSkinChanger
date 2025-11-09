package com.rabimi.javaskinchanger

import android.content.Context
import android.net.Uri
import org.rajawali3d.renderer.Renderer
import org.rajawali3d.Object3D
import org.rajawali3d.materials.Material
import org.rajawali3d.materials.textures.Texture
import org.rajawali3d.math.vector.Vector3

class SkinRenderer(context: Context) : Renderer(context) {

    private var character: Object3D? = null

    init {
        setFrameRate(60)
    }

    override fun initScene() {
        character = Object3D()
        val material = Material()
        material.color = 0xFFFFFF
        character?.material = material
        character?.position = Vector3(0.0, 0.0, -5.0)
        currentScene.addChild(character)
    }

    fun loadSkinFromUri(uri: Uri) {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return
        loadTextureFromStream(inputStream)
    }

    fun loadSkinFromUrl(url: String) {
        // ネットから PNG をダウンロードして適用する処理
        // ここは必要に応じて Coroutine 内で実装
    }

    private fun loadTextureFromStream(stream: java.io.InputStream) {
        val texture = Texture("skin", stream)
        character?.material?.addTexture(texture)
    }
}