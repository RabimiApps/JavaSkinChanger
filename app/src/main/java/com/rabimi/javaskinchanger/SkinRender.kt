package com.rabimi.javaskinchanger

import android.content.Context
import android.graphics.Bitmap
import org.rajawali3d.materials.Material
import org.rajawali3d.materials.textures.Texture
import org.rajawali3d.primitives.Cube
import org.rajawali3d.renderer.Renderer

class SkinRenderer(context: Context) : Renderer(context) {

    private lateinit var cube: Cube

    override fun initScene() {
        cube = Cube(1.0f)
        val material = Material()
        cube.material = material
        currentScene.addChild(cube)
        currentCamera.z = 4.0
    }

    fun updateSkin(bitmap: Bitmap) {
        val texture = Texture("skin", bitmap)
        cube.material.addTexture(texture)
    }

    override fun onOffsetsChanged(
        xOffset: Float,
        yOffset: Float,
        xOffsetStep: Float,
        yOffsetStep: Float,
        xPixelOffset: Int,
        yPixelOffset: Int
    ) {
        // 空実装でOK
    }
}
