package com.rabimi.javaskinchanger

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.Environment
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.PerspectiveCamera
import android.graphics.Bitmap as AndroidBitmap

class Skin3DApp : ApplicationAdapter() {

    private lateinit var modelBatch: ModelBatch
    private lateinit var environment: Environment
    private lateinit var modelInstance: ModelInstance
    private var texture: Texture? = null
    private var model: Model? = null
    private lateinit var camera: PerspectiveCamera

    private var pendingBitmap: AndroidBitmap? = null
    private var currentModelType: String = "classic" // default Steve

    override fun create() {
        modelBatch = ModelBatch()

        // 環境ライト
        environment = Environment()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f))
        environment.add(DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f))

        // カメラ
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position.set(0f, 1.5f, 3f)
        camera.lookAt(0f, 1f, 0f)
        camera.near = 0.1f
        camera.far = 100f
        camera.update()

        buildModel()

        // pendingBitmap があれば初期スキンとして反映
        pendingBitmap?.let { applyTexture(it) }
    }

    override fun render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        modelBatch.begin(camera)
        modelBatch.render(modelInstance, environment)
        modelBatch.end()
    }

    /** MainActivity からスキンを更新 */
    fun updateSkin(bitmap: AndroidBitmap) {
        pendingBitmap = bitmap
        applyTexture(bitmap)
    }

    /** MainActivity からモデルタイプを切り替え */
    fun setModelType(type: String) {
        if (type != "classic" && type != "slim") return
        if (currentModelType == type) return
        currentModelType = type
        buildModel()
        // 既存テクスチャがあれば再適用
        pendingBitmap?.let { applyTexture(it) }
    }

    /** Steve/Alex モデル生成 */
    private fun buildModel() {
        model?.dispose()

        val modelBuilder = ModelBuilder()
        val width = if (currentModelType == "slim") 0.9f else 1f
        val height = 2f
        val depth = 0.5f

        model = modelBuilder.createBox(
            width, height, depth,
            Material(),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.TextureCoordinates).toLong()
        )
        modelInstance = ModelInstance(model)
    }

    private fun applyTexture(bitmap: AndroidBitmap) {
        texture?.dispose()

        val pixmap = Pixmap(bitmap.width, bitmap.height, Pixmap.Format.RGBA8888)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val color = pixels[x + y * bitmap.width]
                pixmap.setPixel(x, bitmap.height - 1 - y, color)
            }
        }
        texture = Texture(pixmap)
        pixmap.dispose()

        model?.materials?.firstOrNull()?.set(TextureAttribute.createDiffuse(texture))
    }

    override fun dispose() {
        modelBatch.dispose()
        texture?.dispose()
        model?.dispose()
    }
}