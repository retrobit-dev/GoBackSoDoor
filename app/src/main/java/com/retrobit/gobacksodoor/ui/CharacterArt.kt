package com.retrobit.gobacksodoor.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Gambar karakter low-poly (drawable/char_<nama>.jpg). Dimuat sekali lalu di-cache.
 * Mengembalikan null jika gambar tidak ada, sehingga UI memakai lingkaran berwarna.
 */
object CharacterArt {
    private val cache = HashMap<String, Bitmap?>()

    fun get(context: Context, name: String): Bitmap? {
        val key = name.lowercase()
        return cache.getOrPut(key) {
            val id = context.resources.getIdentifier("char_$key", "drawable", context.packageName)
            if (id == 0) null else BitmapFactory.decodeResource(context.resources, id)
        }
    }
}
