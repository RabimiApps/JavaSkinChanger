package com.rabimi.javaskinchanger

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.Environment
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes
import android.graphics.Bitmap as AndroidBitmap

/**
 * Minecraft風プレイヤーモデル（head/body/arms/legs）、slim/classic対応版
 *
 * - MainActivity から:
 *     skinApp.updateSkin(bitmap)   // スキン差し替え（Android Bitmap 64x64 推奨）
 *     skinApp.setModelType("slim"|"classic")
 *
 * 注意: UV は簡易的にテクスチャ全体をボックスに貼る方式です（完全な Minecraft UV マッピングは別実装）。
 */
class Skin3DApp : ApplicationAdapter() {

    private lateinit var modelBatch: ModelBatch
    private lateinit var environment: Environment
    private lateinit var camera: PerspectiveCamera

    // 個別モデル（1つの Model を各パーツで再利用してても良いが、簡潔に分ける）
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
    private var currentModelType: String = "classic" // "classic" or "slim"

    // 自動回転用
    private var angle = 0f

    override fun create() {
        modelBatch = ModelBatch()

        // ライト環境
        environment = Environment()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.8f, 0.8f, 0.8f, 1f))
        environment.add(DirectionalLight().set(1f, 1f, 1f, -0.5f, -1f, -0.3f))

        // カメラ
        camera = PerspectiveCamera(67f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position.set(0f, 1.5f, 4f)
        camera.lookAt(0f, 1f, 0f)
        camera.near = 0.1f
        camera.far = 100f
        camera.update()

        buildAllParts()
        pendingBitmap?.let { applyTexture(it) }
    }

    override fun render() {
        // ゆっくり自動回転（必要無ければ angle 更新を消す）
        angle += Gdx.graphics.deltaTime * 20f

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // 回転を各パーツに適用（体を中心に回す）
        val rotationY = com.badlogic.gdx.math.Matrix4().setToRotation(0f, 1f, 0f, angle)
        // 位置・回転はモデルインスタンスの transform に毎フレーム適用
        headInstance?.transform?.set(rotationY)?.translate(0f, 1.6f, 0f)
        bodyInstance?.transform?.set(rotationY)?.translate(0f, 0.8f, 0f)
        leftArmInstance?.transform?.set(rotationY)?.translate(-0.75f, 0.9f, 0f)
        rightArmInstance?.transform?.set(rotationY)?.translate(0.75f, 0.9f, 0f)
        leftLegInstance?.transform?.set(rotationY)?.translate(-0.25f, -0.8f, 0f)
        rightLegInstance?.transform?.set(rotationY)?.translate(0.25f, -0.8f, 0f)

        modelBatch.begin(camera)
        headInstance?.let { modelBatch.render(it, environment) }
        bodyInstance?.let { modelBatch.render(it, environment) }
        leftArmInstance?.let { modelBatch.render(it, environment) }
        rightArmInstance?.let { modelBatch.render(it, environment) }
        leftLegInstance?.let { modelBatch.render(it, environment) }
        rightLegInstance?.let { modelBatch.render(it, environment) }
        modelBatch.end()
    }

    /**
     * MainActivity からスキンを更新する（Bitmap は ARGB_8888 推奨, 64x64 がベスト）
     */
    fun updateSkin(bitmap: AndroidBitmap) {
        pendingBitmap = bitmap
        applyTexture(bitmap)
    }

    /**
     * classic / slim 切替。呼ぶとパーツを再構築して既存テクスチャを再適用する
     */
    fun setModelType(type: String) {
        if (type != "classic" && type != "slim") return
        if (currentModelType == type) return
        currentModelType = type
        rebuildPartsKeepTexture()
    }

    // ---------- モデル構築関連 ----------

    private fun buildAllParts() {
        disposeModels() // 既存あるなら破棄

        // 基本材質（テクスチャは後でセット）
        val baseMaterial = Material()
        // 頭 (8x8x8) – サイズは Minecraft スケールに近い（任意調整可）
        headModel = createBoxModel(0.8f, 0.8f, 0.8f, baseMaterial)
        headInstance = ModelInstance(headModel)

        // 体 (8x12x4)
        bodyModel = createBoxModel(0.8f, 1.2f, 0.4f, baseMaterial)
        bodyInstance = ModelInstance(bodyModel)

        // 腕は幅がモデルタイプによる（classic: 0.4, slim: 0.3）
        val armWidth = if (currentModelType == "slim") 0.3f else 0.4f
        leftArmModel = createBoxModel(armWidth, 1.2f, 0.4f, baseMaterial)
        rightArmModel = createBoxModel(armWidth, 1.2f, 0.4f, baseMaterial)
        leftArmInstance = ModelInstance(leftArmModel)
        rightArmInstance = ModelInstance(rightArmModel)

        // 足
        leftLegModel = createBoxModel(0.4f, 1.2f, 0.4f, baseMaterial)
        rightLegModel = createBoxModel(0.4f, 1.2f, 0.4f, baseMaterial)
        leftLegInstance = ModelInstance(leftLegModel)
        rightLegInstance = ModelInstance(rightLegModel)

        // 初期配置（transform は render で毎フレーム上書きするが、念のためセット）
        headInstance?.transform?.idt()?.translate(0f, 1.6f, 0f)
        bodyInstance?.transform?.idt()?.translate(0f, 0.8f, 0f)
        leftArmInstance?.transform?.idt()?.translate(-0.75f, 0.9f, 0f)
        rightArmInstance?.transform?.idt()?.translate(0.75f, 0.9f, 0f)
        leftLegInstance?.transform?.idt()?.translate(-0.25f, -0.8f, 0f)
        rightLegInstance?.transform?.idt()?.translate(0.25f, -0.8f, 0f)
    }

    private fun rebuildPartsKeepTexture() {
        // テクスチャを保持しておいて再適用する
        val keptTexture = texture
        buildAllParts()
        if (keptTexture != null) {
            // 各モデルに既存テクスチャを適用
            applyTextureToModels(keptTexture)
        }
    }

    private fun createBoxModel(width: Float, height: Float, depth: Float, material: Material): Model {
        val mb = ModelBuilder()
        val attr = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.TextureCoordinates).toLong()
        // createBox は中心が原点の箱を作る
        return mb.createBox(width, height, depth, material, attr)
    }

    // テクスチャを Model のマテリアルに反映させる（Texture を直接渡すオーバーロード）
    private fun applyTextureToModels(tex: Texture) {
        val ta = TextureAttribute.createDiffuse(tex)
        // head
        headModel?.materials?.firstOrNull()?.set(ta)
        bodyModel?.materials?.firstOrNull()?.set(ta)
        leftArmModel?.materials?.firstOrNull()?.set(ta)
        rightArmModel?.materials?.firstOrNull()?.set(ta)
        leftLegModel?.materials?.firstOrNull()?.set(ta)
        rightLegModel?.materials?.firstOrNull()?.set(ta)
    }

    // Bitmap -> Pixmap(RGBA8888) に変換して Texture 作成、モデルに適用
    private fun applyTexture(bitmap: AndroidBitmap) {
        // dispose 旧テクスチャを差し替える
        texture?.dispose()

        // Android Bitmap は ARGB_8888 (A R G B)
        val w = bitmap.width
        val h = bitmap.height
        val pixmap = Pixmap(w, h, Pixmap.Format.RGBA8888)
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        // libGDX Pixmap.setPixel は RGBA8888 の順 (r<<24 | g<<16 | b<<8 | a)
        // Android の int は ARGB (a<<24 | r<<16 | g<<8 | b)
        var idx = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val color = pixels[idx++]
                val a = (color ushr 24) and 0xFF
                val r = (color ushr 16) and 0xFF
                val g = (color ushr 8) and 0xFF
                val b = (color) and 0xFF
                val rgba = (r shl 24) or (g shl 16) or (b shl 8) or a
                // Y 軸反転して貼る（Android と OpenGL の Y 方向差）
                pixmap.setPixel(x, h - 1 - y, rgba)
            }
        }

        texture = Texture(pixmap)
        // pixmap はテクスチャ生成後に破棄して OK
        pixmap.dispose()

        // 全パーツにテクスチャを適用
        texture?.let { applyTextureToModels(it) }
    }

    // ---------- cleanup ----------
    private fun disposeModels() {
        headModel?.dispose(); headModel = null; headInstance = null
        bodyModel?.dispose(); bodyModel = null; bodyInstance = null
        leftArmModel?.dispose(); leftArmModel = null; leftArmInstance = null
        rightArmModel?.dispose(); rightArmModel = null; rightArmInstance = null
        leftLegModel?.dispose(); leftLegModel = null; leftLegInstance = null
        rightLegModel?.dispose(); rightLegModel = null; rightLegInstance = null
    }

    override fun dispose() {
        modelBatch.dispose()
        texture?.dispose(); texture = null
        disposeModels()
    }
}
