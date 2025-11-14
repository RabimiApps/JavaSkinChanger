package dev.storeforminecraft.skinviewandroid.library.threedimension.renderer

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Skin3DRenderer : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "Skin3DRenderer"
    }

    private var skinBitmap: Bitmap? = null
    private var variant: String = "classic"

    fun setSkin(bitmap: Bitmap) {
        skinBitmap = bitmap
        Log.d(TAG, "Skin set: ${bitmap.width}x${bitmap.height}")
    }

    fun setVariant(variant: String) {
        this.variant = variant
        Log.d(TAG, "Variant set: $variant")
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated")
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: ${width}x${height}")
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        // 🔹 簡易版なのでビットマップを描画は省略
        // 将来的にはテクスチャとして OpenGL で貼り付ける
    }
}
