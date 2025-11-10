package com.rabimi.javaskinchanger

import android.content.Context
import android.graphics.Bitmap
import android.view.MotionEvent
import org.rajawali3d.lights.DirectionalLight
import org.rajawali3d.materials.Material
import org.rajawali3d.materials.textures.Texture
import org.rajawali3d.primitives.Cube
import org.rajawali3d.renderer.Renderer

class SkinRenderer(context: Context) : Renderer(context) {

    private var skinBitmap: Bitmap? = null
    private lateinit var skinCube: Cube
    private lateinit var light: DirectionalLight

    init {
        setFrameRate(60)
    }

    override fun initScene() {
        // ライト設定
        light = DirectionalLight(1.0, -0.5, -1.0)
        light.setColor(1.0f, 1.0f, 1.0f)
        light.power = 2.0f
        currentScene.addLight(light)

        // スキンを貼る立方体
        skinCube = Cube(2.0f)
        val material = Material()

        skinBitmap?.let {
            val texture = Texture("skinTexture", it)
            material.addTexture(texture)
        }

        skinCube.material = material
        currentScene.addChild(skinCube)

        // カメラ位置設定
        currentCamera.z = 6.0
        currentCamera.y = 0.0
    }

    fun updateSkin(bitmap: Bitmap) {
        skinBitmap = bitmap
        if (::skinCube.isInitialized) {
            val material = Material()
            val texture = Texture("skinTexture", bitmap)
            material.addTexture(texture)
            skinCube.material = material
        }
    }

    // RendererのonTouchEventはUnitを返す
    override fun onTouchEvent(event: MotionEvent) {
        // ここで回転などを追加できる
    }

    override fun onOffsetsChanged(
        xOffset: Float,
        yOffset: Float,
        xOffsetStep: Float,
        yOffsetStep: Float,
        xPixelOffset: Int,
        yPixelOffset: Int
    ) {
        // 未使用
    }
}