package com.rabimi.javaskinchanger

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.ModelBatch as GdxModelBatch
import com.badlogic.gdx.graphics.g3d.RenderableProvider
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.Pixmap as GdxPixmap
import android.graphics.Bitmap as AndroidBitmap

class Skin3DApp : ApplicationAdapter() {

    private lateinit var modelBatch: GdxModelBatch
    private lateinit var environment: Environment
    private lateinit var camera: PerspectiveCamera

    private var headModel: Model? = null
    private var bodyModel: Model? = null
    private var leftArmModel: Model? = null
    private var rightArmModel: Model? = null
    private var leftLegModel: Model? = null
    private var rightLegModel: Model? = null

    private var headInstance: ModelInstance? = null
    private var bodyInstance: ModelInstance? = null
    private var leftArmInstance: ModelInstance? = null
    private var rightArmInstance: ModelInstance? = null
    private var leftLegInstance: ModelInstance? = null
    private var rightLegInstance: ModelInstance? = null

    private var texture: Texture? = null
    private var pendingBitmap: AndroidBitmap? = null
    private var currentModelType: String = "classic"
    private var angle = 0f

    override fun create() {
        modelBatch = GdxModelBatch()
        environment = Environment().apply {
            set(ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f))
            add(DirectionalLight().set(1f, 1f, 1f, -0.5f, -1f, -0.3f))
        }
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
            position.set(0f, 1.5f, 4f)
            lookAt(0f, 1f, 0f)
            near = 0.1f
            far = 100f
            update()
        }

        buildAllParts()
        pendingBitmap?.let { applyTexture(it) }
    }

    override fun render() {
        angle += Gdx.graphics.deltaTime * 20f

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        val rotationY = com.badlogic.gdx.math.Matrix4().setToRotation(0f, 1f, 0f, angle)
        headInstance?.transform?.set(rotationY)?.translate(0f, 1.6f, 0f)
        bodyInstance?.transform?.set(rotationY)?.translate(0f, 0.8f, 0f)
        leftArmInstance?.transform?.set(rotationY)?.translate(-0.75f, 0.9f, 0f)
        rightArmInstance?.transform?.set(rotationY)?.translate(0.75f, 0.9f, 0f)
        leftLegInstance?.transform?.set(rotationY)?.translate(-0.25f, -0.8f, 0f)
        rightLegInstance?.transform?.set(rotationY)?.translate(0.25f, -0.8f, 0f)

        modelBatch.begin(camera)
        listOf(headInstance, bodyInstance, leftArmInstance, rightArmInstance, leftLegInstance, rightLegInstance)
            .forEach { it?.let { mi -> modelBatch.render(mi as RenderableProvider, environment) } }
        modelBatch.end()
    }

    fun updateSkin(bitmap: AndroidBitmap) {
        pendingBitmap = bitmap
        applyTexture(bitmap)
    }

    fun setModelType(type: String) {
        if (type != "classic" && type != "slim") return
        if (currentModelType == type) return
        currentModelType = type
        rebuildPartsKeepTexture()
    }

    private fun buildAllParts() {
        disposeModels()
        val baseMaterial = com.badlogic.gdx.graphics.g3d.Material()
        val armWidth = if (currentModelType == "slim") 0.3f else 0.4f

        headModel = createBoxModel(0.8f, 0.8f, 0.8f, baseMaterial).also { headInstance = ModelInstance(it) }
        bodyModel = createBoxModel(0.8f, 1.2f, 0.4f, baseMaterial).also { bodyInstance = ModelInstance(it) }
        leftArmModel = createBoxModel(armWidth, 1.2f, 0.4f, baseMaterial).also { leftArmInstance = ModelInstance(it) }
        rightArmModel = createBoxModel(armWidth, 1.2f, 0.4f, baseMaterial).also { rightArmInstance = ModelInstance(it) }
        leftLegModel = createBoxModel(0.4f, 1.2f, 0.4f, baseMaterial).also { leftLegInstance = ModelInstance(it) }
        rightLegModel = createBoxModel(0.4f, 1.2f, 0.4f, baseMaterial).also { rightLegInstance = ModelInstance(it) }

        headInstance?.transform?.idt()?.translate(0f, 1.6f, 0f)
        bodyInstance?.transform?.idt()?.translate(0f, 0.8f, 0f)
        leftArmInstance?.transform?.idt()?.translate(-0.75f, 0.9f, 0f)
        rightArmInstance?.transform?.idt()?.translate(0.75f, 0.9f, 0f)
        leftLegInstance?.transform?.idt()?.translate(-0.25f, -0.8f, 0f)
        rightLegInstance?.transform?.idt()?.translate(0.25f, -0.8f, 0f)
    }

    private fun rebuildPartsKeepTexture() {
        val keptTexture = texture
        buildAllParts()
        keptTexture?.let { applyTextureToModels(it) }
    }

    private fun createBoxModel(width: Float, height: Float, depth: Float, material: com.badlogic.gdx.graphics.g3d.Material): Model {
        val mb = ModelBuilder()
        val attr = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.TextureCoordinates).toLong()
        return mb.createBox(width, height, depth, material, attr)
    }

    private fun applyTextureToModels(tex: Texture) {
        val ta = TextureAttribute.createDiffuse(tex)
        listOf(headModel, bodyModel, leftArmModel, rightArmModel, leftLegModel, rightLegModel)
            .forEach { it?.materials?.firstOrNull()?.set(ta) }
    }

    private fun applyTexture(bitmap: AndroidBitmap) {
        texture?.dispose()
        val w = bitmap.width
        val h = bitmap.height
        val pixmap = GdxPixmap(w, h, GdxPixmap.Format.RGBA8888)
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val color = pixels[x + y * w]
                val a = (color ushr 24) and 0xFF
                val r = (color ushr 16) and 0xFF
                val g = (color ushr 8) and 0xFF
                val b = color and 0xFF
                val rgba = (r shl 24) or (g shl 16) or (b shl 8) or a
                pixmap.drawPixel(x, h - 1 - y, rgba) // ← 修正ポイント
            }
        }

        texture = Texture(pixmap)
        pixmap.dispose()
        texture?.let { applyTextureToModels(it) }
    }

    private fun disposeModels() {
        listOf(
            headModel to headInstance,
            bodyModel to bodyInstance,
            leftArmModel to leftArmInstance,
            rightArmModel to rightArmInstance,
            leftLegModel to leftLegInstance,
            rightLegModel to rightLegInstance
        ).forEach { (model, _) -> model?.dispose() }

        headModel = null; headInstance = null
        bodyModel = null; bodyInstance = null
        leftArmModel = null; leftArmInstance = null
        rightArmModel = null; rightArmInstance = null
        leftLegModel = null; leftLegInstance = null
        rightLegModel = null; rightLegInstance = null
    }

    override fun dispose() {
        modelBatch.dispose()
        texture?.dispose(); texture = null
        disposeModels()
    }
}