package com.retrobit.gobacksodoor.ui

import android.content.Context
import android.graphics.Typeface

/** Font pixel (Press Start 2P, OFL) dari assets/fonts. Dimuat sekali. */
object PixelFont {
    private var face: Typeface? = null

    fun get(context: Context): Typeface {
        face?.let { return it }
        val tf = try {
            Typeface.createFromAsset(context.assets, "fonts/press_start.ttf")
        } catch (e: Exception) {
            Typeface.MONOSPACE
        }
        face = tf
        return tf
    }
}
