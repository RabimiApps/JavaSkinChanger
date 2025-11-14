package dev.storeforminecraft.skinviewandroid.library.threedimension.ui

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import dev.storeforminecraft.skinviewandroid.library.threedimension.renderer.Skin3DRenderer

class SkinView3DSurfaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    companion object {
        private const val TAG = "SkinView3D"
    }

    private val renderer: Skin3DRenderer

    init {
        // OpenGL ES 2.0
        setEGLContextClientVersion(2)
        renderer = Skin3DRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun render(bitmap: Bitmap) {
        Log.d(TAG, "render called")
        renderer.setSkin(bitmap)
        requestRender()
    }

    fun setVariant(variant: String) {
        Log.d(TAG, "setVariant called: $variant")
        renderer.setVariant(variant)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "GLSurfaceView onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "GLSurfaceView onPause")
    }
}
