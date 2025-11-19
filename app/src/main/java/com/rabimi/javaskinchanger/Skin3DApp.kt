package com.rabimi.javaskinchanger

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.Environment
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.TextureProvider
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.Texture.TextureWrap
import com.badlogic.gdx.graphics.Pixmap
import android.graphics.Bitmap
import android.os.Environment
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.PerspectiveCamera

class Skin3DApp : ApplicationAdapter() {

    private lateinit var modelBatch: ModelBatch
    private lateinit var environment: Environment
    private lateinit var model: Model
    private lateinit var instance: ModelInstance
    private lateinit var camera: PerspectiveCamera
    private var texture: Texture? = null
    private var cameraController: CameraInputController? = null

    override fun create() {
        modelBatch = ModelBatch()
        environment = Environment().apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f))
            add(DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f))
        }

        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
            position.set(0f, 1.5f, 3f)
            lookAt(0f, 1f, 0f)
            near = 0.1f
            far = 100f
            update()
        }

        cameraController = CameraInputController(camera)
        Gdx.input.inputProcessor = cameraController

        // 初期モデル（立方体を簡易スキンモデルとして使用）
        val builder = ModelBuilder()
        model = builder.createBox(
            1f, 2f, 0.5f,
            Material(ColorAttribute.createDiffuse(Color.WHITE)),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.TextureCoordinates).toLong()
        )
        instance = ModelInstance(model)
    }

    override fun render() {
        cameraController?.update()

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        modelBatch.begin(camera)
        modelBatch.render(instance, environment)
        modelBatch.end()
    }

    override fun dispose() {
        modelBatch.dispose()
        model.dispose()
        texture?.dispose()
    }

    fun updateSkin(bitmap: Bitmap) {
        Gdx.app.postRunnable {
            texture?.dispose()
            texture = Texture(Pixmap(bitmap))
            instance.materials[0].set(TextureAttribute.createDiffuse(texture))
        }
    }
}
