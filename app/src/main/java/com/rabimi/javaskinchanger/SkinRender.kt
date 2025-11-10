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
    private lateinit var light: DirectionalLight

    private lateinit var head: Cube
    private lateinit var body: Cube
    private lateinit var leftArm: Cube
    private lateinit var rightArm: Cube
    private lateinit var leftLeg: Cube
    private lateinit var rightLeg: Cube

    private var lastX = 0f
    private var lastY = 0f
    private var angleX = 0f
    private var angleY = 0f

    init {
        setFrameRate(60)
    }

    override fun initScene() {
        // ライト
        light = DirectionalLight(1.0f, -0.5f, -1.0f)
        light.setColor(1.0f, 1.0f, 1.0f)
        light.power = 2.0f
        currentScene.addLight(light)

        // 各部位をCubeで作成（すべてFloat）
        head = Cube(1.0f, 1.0f, 1.0f)
        body = Cube(1.0f, 1.5f, 0.5f)
        leftArm = Cube(0.5f, 1.5f, 0.5f)
        rightArm = Cube(0.5f, 1.5f, 0.5f)
        leftLeg = Cube(0.5f, 1.5f, 0.5f)
        rightLeg = Cube(0.5f, 1.5f, 0.5f)

        // 配置
        head.y = 2.25f
        body.y = 0.75f
        leftArm.y = 0.75f; leftArm.x = -0.75f
        rightArm.y = 0.75f; rightArm.x = 0.75f
        leftLeg.y = -0.75f; leftLeg.x = -0.25f
        rightLeg.y = -0.75f; rightLeg.x = 0.25f

        // デフォルトマテリアル
        val defaultMat = Material()
        defaultMat.color = 0x999999
        head.material = defaultMat
        body.material = defaultMat
        leftArm.material = defaultMat
        rightArm.material = defaultMat
        leftLeg.material = defaultMat
        rightLeg.material = defaultMat

        // シーンに追加
        currentScene.addChild(head)
        currentScene.addChild(body)
        currentScene.addChild(leftArm)
        currentScene.addChild(rightArm)
        currentScene.addChild(leftLeg)
        currentScene.addChild(rightLeg)

        // カメラ
        currentCamera.z = 6.0f
        currentCamera.y = 1.0f
    }

    // スキンを適用
    fun updateSkin(bitmap: Bitmap) {
        skinBitmap = bitmap
        skinBitmap?.let { bmp ->
            val scaleX = bmp.width / 64f
            val scaleY = bmp.height / 64f

            fun crop(x: Int, y: Int, w: Int, h: Int) =
                Bitmap.createBitmap(bmp, (x * scaleX).toInt(), (y * scaleY).toInt(), (w * scaleX).toInt(), (h * scaleY).toInt())

            head.material = Material().apply { addTexture(Texture("head", crop(8, 8, 8, 8))) }
            body.material = Material().apply { addTexture(Texture("body", crop(20, 20, 8, 12))) }
            leftArm.material = Material().apply { addTexture(Texture("leftArm", crop(44, 20, 4, 12))) }
            rightArm.material = Material().apply { addTexture(Texture("rightArm", crop(44, 20, 4, 12))) }
            leftLeg.material = Material().apply { addTexture(Texture("leftLeg", crop(4, 20, 4, 12))) }
            rightLeg.material = Material().apply { addTexture(Texture("rightLeg", crop(4, 20, 4, 12))) }
        }
    }

    override fun onTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                angleY += dx * 0.5f
                angleX += dy * 0.5f

                // 各パーツを回転
                head.rotation.x = angleX
                head.rotation.y = angleY
                body.rotation.x = angleX
                body.rotation.y = angleY
                leftArm.rotation.x = angleX
                leftArm.rotation.y = angleY
                rightArm.rotation.x = angleX
                rightArm.rotation.y = angleY
                leftLeg.rotation.x = angleX
                leftLeg.rotation.y = angleY
                rightLeg.rotation.x = angleX
                rightLeg.rotation.y = angleY

                lastX = event.x
                lastY = event.y
            }
        }
    }

    override fun onOffsetsChanged(
        xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float,
        xPixelOffset: Int, yPixelOffset: Int
    ) {}
}