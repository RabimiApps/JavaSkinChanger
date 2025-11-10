package com.rabimi.javaskinchanger

import android.content.Context
import android.graphics.Bitmap
import android.view.MotionEvent
import org.rajawali3d.Object3D
import org.rajawali3d.lights.DirectionalLight
import org.rajawali3d.materials.Material
import org.rajawali3d.materials.textures.Texture
import org.rajawali3d.renderer.Renderer
import org.rajawali3d.scene.Scene

class SkinRenderer(context: Context) : Renderer(context) {

    private var skinBitmap: Bitmap? = null
    private lateinit var skinCube: Object3D
    private lateinit var light: DirectionalLight

    init {
        // 必ず RajawaliRenderer を setSurfaceRenderer した直後に initScene が呼ばれる
        setFrameRate(60)
    }

    override fun initScene() {
        // ライト
        light = DirectionalLight(1.0, -0.5, -1.0)
        light.setColor(1.0f, 1.0f, 1.0f)
        light.power = 2.0f
        currentScene.addLight(light)

        // 3Dオブジェクト（立方体で代用）
        skinCube = Object3D()
        skinCube.geometry = org.rajawali3d.primitives.Cube(2.0f)
        val material = Material()
        skinBitmap?.let {
            val texture = Texture("skinTexture", it)
            material.addTexture(texture)
        }
        skinCube.material = material
        currentScene.addChild(skinCube)

        // カメラ位置
        currentCamera.z = 6.0
        currentCamera.y = 0.0
    }

    fun updateSkin(bitmap: Bitmap) {
        skinBitmap = bitmap
        // 新しいスキンをテクスチャに反映
        if (::skinCube.isInitialized) {
            val material = Material()
            val texture = Texture("skinTexture", bitmap)
            material.addTexture(texture)
            skinCube.material = material
        }
    }

    // 必須の抽象メソッドの実装
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // 今は何もしない（後で回転操作など追加可能）
        return true
    }

    override fun onOffsetsChanged(
        xOffset: Float,
        yOffset: Float,
        xOffsetStep: Float,
        yOffsetStep: Float,
        xPixelOffset: Int,
        yPixelOffset: Int
    ) {
        // ライブウォールペーパーなどで呼ばれるが、今回は未使用
    }
}
