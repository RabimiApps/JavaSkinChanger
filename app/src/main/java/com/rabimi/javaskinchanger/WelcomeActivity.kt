package com.rabimi.javaskinchanger

import android.content.Context
import android.view.MotionEvent
import org.rajawali3d.Object3D
import org.rajawali3d.loader.LoaderOBJ
import org.rajawali3d.loader.ParsingException
import org.rajawali3d.materials.Material
import org.rajawali3d.materials.textures.Texture
import org.rajawali3d.renderer.Renderer

class SkinRenderer(context: Context, private val skinUrl: String) : Renderer(context) {

    private var skinModel: Object3D? = null
    private var previousX = 0f
    private var rotationSpeed = 0.5f

    override fun initScene() {
        try {
            val loader = LoaderOBJ(mContext.resources, mTextureManager, R.raw.minecraft_skin)
            loader.parse()
            skinModel = loader.parsedObject
            skinModel?.material = Material().apply {
                addTexture(Texture("skin", skinUrl))
                colorInfluence = 0f
            }
            currentScene.addChild(skinModel)
            currentCamera.z = 6.0
        } catch (e: ParsingException) {
            e.printStackTrace()
        }
    }

    override fun onRenderFrame(glUnused: javax.microedition.khronos.opengles.GL10?) {
        super.onRenderFrame(glUnused)
        skinModel?.rotateY(0.5)
    }

    override fun onTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - previousX
                skinModel?.rotateY(dx * rotationSpeed)
            }
        }
        previousX = event.x
    }
}
