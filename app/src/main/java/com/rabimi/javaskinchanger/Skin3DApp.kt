package com.rabimi.javaskinchanger.render

import android.graphics.Bitmap
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.Environment
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.utils.GdxRuntimeException

class Skin3DApp : ApplicationListener {

    private lateinit var camera: PerspectiveCamera
    private lateinit var batch: ModelBatch
    private lateinit var model: Model
    private lateinit var instance: ModelInstance
    private lateinit var env: Environment

    private var skinTexture: Texture? = null

    override fun create() {
        // camera
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position.set(0f, 1.6f, 3f)
        camera.lookAt(0f, 1.4f, 0f)
        camera.near = 0.1f
        camera.far = 100f
        camera.update()

        // environment
        env = Environment().apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f))
            add(DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f))
        }

        batch = ModelBatch()

        // 最初はダミーテクスチャ（灰色）
        val pix = com.badlogic.gdx.graphics.Pixmap(64, 64, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)
        pix.setColor(0.5f, 0.5f, 0.5f, 1f)
        pix.fill()
        skinTexture = Texture(pix)
        pix.dispose()

        // model（頭だけ）
        val builder = ModelBuilder()
        model = builder.createBox(
            1f, 1f, 1f,
            Material(TextureAttribute.createDiffuse(skinTexture)),
            (VertexAttributes.Usage.Position
                    or VertexAttributes.Usage.Normal
                    or VertexAttributes.Usage.TextureCoordinates).toLong()
        )

        instance = ModelInstance(model)
        instance.transform.setToTranslation(0f, 1.5f, 0f)
    }

    override fun render() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // rotate
        instance.transform.rotate(0f, 1f, 0f, 1f)

        batch.begin(camera)
        batch.render(instance, env)
        batch.end()
    }

    override fun resize(width: Int, height: Int) {}
    override fun pause() {}
    override fun resume() {}

    override fun dispose() {
        batch.dispose()
        model.dispose()
        skinTexture?.dispose()
    }

    // ───────────────────────
    // ★ 外部からスキン差し替え
    // ───────────────────────
    fun setSkin(bitmap: Bitmap) {
        try {
            // 1. 前のテクスチャ破棄
            skinTexture?.dispose()

            // 2. Bitmap → Pixmap に変換
            val pixmap = com.badlogic.gdx.graphics.Pixmap(bitmap.width, bitmap.height, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) {
                    pixmap.drawPixel(x, y, pixels[y * bitmap.width + x])
                }
            }

            skinTexture = Texture(pixmap)
            pixmap.dispose()

            // 3. Material に反映
            instance.materials.first().set(TextureAttribute.createDiffuse(skinTexture))

        } catch (e: GdxRuntimeException) {
            e.printStackTrace()
        }
    }
}
