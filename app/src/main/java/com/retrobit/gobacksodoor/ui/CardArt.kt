package com.retrobit.gobacksodoor.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.retrobit.gobacksodoor.game.ATTACKER_ROSTER
import com.retrobit.gobacksodoor.game.Card
import com.retrobit.gobacksodoor.game.GUARD_ROSTER
import com.retrobit.gobacksodoor.game.dirName
import kotlin.math.min

/**
 * Kartu bergaya pixel art: bingkai tegas tanpa sudut membulat, judul di kiri atas,
 * gambar karakter pixel, pita nama, dan baris nilai berbentuk kotak.
 * Semua ukuran dalam satuan 200x290 lalu diskalakan ke ukuran layar.
 */
object CardArt {
    const val W = 200f
    const val H = 290f

    private val CREAM = 0xFFFBE9D2.toInt()
    private val BANNER = 0xFFF5A03A.toInt()
    private val INK = 0xFF3B1E08.toInt()
    private val PILL = 0xFFBDEFF7.toInt()
    private val PILL_EDGE = 0xFF2B8C9E.toInt()

    private var face: Typeface = Typeface.MONOSPACE

    private class Look(val frame: Int, val title: String, val name: String, val rows: List<Pair<String, String>>, val effect: String)

    fun draw(canvas: Canvas, context: Context, card: Card, rect: RectF, enabled: Boolean = true, selected: Boolean = false) {
        face = PixelFont.get(context)
        val lk = look(card)
        val n = begin(canvas, rect)

        // Bingkai luar: dua lapis, gelap di luar dan cokelat di dalam.
        canvas.drawRect(RectF(0f, 0f, W, H), fill(INK))
        canvas.drawRect(RectF(4f, 4f, W - 4f, H - 4f), fill(lk.frame))
        canvas.drawRect(RectF(10f, 10f, W - 10f, H - 10f), fill(CREAM))

        // Judul kategori (kotak di kiri atas).
        canvas.drawRect(RectF(16f, 16f, 126f, 40f), fill(BANNER))
        canvas.drawRect(RectF(16f, 16f, 126f, 40f), stroke(INK, 3f))
        canvas.drawText(lk.title, 71f, 33f, text(INK, fit(lk.title, 98f, 12f)))

        // Jendela gambar.
        val art = RectF(16f, 46f, 184f, 174f)
        canvas.save()
        canvas.clipRect(art)
        canvas.drawRect(art, fill(0xFFFFF4E4.toInt()))
        val owner = card.owner
        val bmp = if (owner != null) CharacterArt.get(context, owner) else null
        if (bmp != null) {
            // Potong bagian tengah sesuai rasio jendela, tanpa penghalusan (pixel tetap tajam).
            val sw = bmp.width.toFloat()
            val sh = sw * art.height() / art.width()
            val top = ((bmp.height - sh) / 2f).coerceAtLeast(0f)
            val src = android.graphics.Rect(0, top.toInt(), bmp.width, (top + sh).toInt().coerceAtMost(bmp.height))
            canvas.drawBitmap(bmp, src, art, pixelPaint())
        } else {
            canvas.drawText(lk.name.substringAfter(' ', lk.name), 100f, 150f, text(INK, 40f))
        }
        canvas.restore()
        canvas.drawRect(art, stroke(INK, 3f))

        // Pita nama.
        val banner = RectF(20f, 158f, 180f, 184f)
        canvas.drawRect(banner, fill(BANNER))
        canvas.drawRect(banner, stroke(INK, 3f))
        canvas.drawText(lk.name, 100f, 176f, text(INK, fit(lk.name, 146f, 10f)))

        // Baris nilai: penanda kotak di kiri, label, dan pil nilai di kanan.
        var y = 192f
        for ((label, value) in lk.rows) {
            canvas.drawRect(RectF(22f, y + 4f, 32f, y + 14f), fill(BANNER))
            canvas.drawRect(RectF(22f, y + 4f, 32f, y + 14f), stroke(INK, 2f))
            canvas.drawText(label, 38f, y + 13f, text(INK, 7f, Paint.Align.LEFT))
            // Font pixel: lebar tiap huruf kira-kira sama dengan ukuran teksnya.
            val pillW = (value.length * 7f + 14f).coerceIn(44f, 112f)
            val pill = RectF(184f - pillW, y + 2f, 184f, y + 19f)
            canvas.drawRect(pill, fill(PILL))
            canvas.drawRect(pill, stroke(PILL_EDGE, 2f))
            val vs = fit(value, pillW - 10f, 7f)
            canvas.drawText(value, pill.centerX(), y + 13f, text(INK, vs))
            y += 22f
        }

        // Efek karakter (atau keterangan kartu netral), dibungkus manual per baris.
        canvas.drawText("EFEK", 22f, y + 8f, text(INK, 7f, Paint.Align.LEFT))
        val lines = wrap(lk.effect, 22)
        var ly = y + 18f
        for (line in lines) {
            canvas.drawText(line, 22f, ly, text(INK, 6f, Paint.Align.LEFT))
            ly += 9f
        }

        if (!enabled) canvas.drawRect(RectF(0f, 0f, W, H), fill(Color.argb(150, 0, 0, 0)))
        if (selected) canvas.drawRect(RectF(2f, 2f, W - 2f, H - 2f), stroke(Color.rgb(255, 235, 59), 5f))
        canvas.restoreToCount(n)
    }

    /** Punggung kartu (dek): tanpa label, hanya pola piksel. */
    fun drawBack(canvas: Canvas, rect: RectF) {
        val n = begin(canvas, rect)
        canvas.drawRect(RectF(0f, 0f, W, H), fill(Color.rgb(24, 34, 78)))
        val step = 16f
        var yy = 0f
        var row = 0
        while (yy < H) {
            var xx = if (row % 2 == 0) 0f else step / 2f
            while (xx < W) {
                canvas.drawRect(RectF(xx + 3f, yy + 3f, xx + step - 3f, yy + step - 3f), fill(Color.argb(70, 255, 215, 0)))
                xx += step
            }
            yy += step
            row++
        }
        canvas.drawRect(RectF(7f, 7f, W - 7f, H - 7f), stroke(Color.rgb(255, 215, 0), 5f))
        val diamond = Path().apply {
            moveTo(100f, 110f); lineTo(140f, 150f); lineTo(100f, 190f); lineTo(60f, 150f); close()
        }
        canvas.drawPath(diamond, fill(Color.rgb(255, 193, 7)))
        canvas.restoreToCount(n)
    }

    private fun look(card: Card): Look {
        val roster = ATTACKER_ROSTER + GUARD_ROSTER
        return when (card) {
            is Card.Move -> {
                val c = roster.first { it.name == card.owner }
                Look(
                    frame = Color.rgb(200, 100, 42), title = "GERAK", name = c.name.uppercase(),
                    rows = listOf("GERAKAN" to dirName(card.dir), "LANGKAH" to "1-${c.stamina} KOTAK"),
                    effect = c.effect,
                )
            }
            is Card.Touch -> {
                val c = roster.first { it.name == card.owner }
                Look(Color.rgb(179, 38, 30), "SENTUH", c.name.uppercase(), listOf("GERAKAN" to "Sentuh", "BIAYA" to "3 STAMINA"), c.effect)
            }
            is Card.Evade -> {
                val c = roster.first { it.name == card.owner }
                Look(Color.rgb(30, 111, 184), "HINDAR", c.name.uppercase(), listOf("GERAKAN" to "Hindar", "BIAYA" to "3 STAMINA"), c.effect)
            }
            is Card.Star -> {
                val c = roster.first { it.name == card.owner }
                Look(Color.rgb(184, 134, 11), "BINTANG", c.name.uppercase(), listOf("GERAKAN" to "Bintang", "BIAYA" to "5 STAMINA"), c.effect)
            }
            is Card.Stamina -> Look(
                Color.rgb(46, 125, 50), "STAMINA", "STAMINA ${if (card.value > 0) "+" else "-"}${kotlin.math.abs(card.value)}", emptyList(),
                "Ketuk karakter tujuan: sekutu +${card.value}, lawan -${card.value} stamina.",
            )
            is Card.Draw -> Look(
                Color.rgb(106, 63, 160), "AMBIL", "AMBIL +${card.value}", emptyList(),
                "Ambil ${card.value} kartu teratas dari dek. Tidak mengakhiri giliran.",
            )
        }
    }

    /** Pemotongan kata sederhana untuk font pixel (lebar tetap per karakter). */
    private fun wrap(s: String, maxChars: Int): List<String> {
        val out = mutableListOf<String>()
        var cur = ""
        for (word in s.split(" ")) {
            val next = if (cur.isEmpty()) word else "$cur $word"
            if (next.length > maxChars && cur.isNotEmpty()) {
                out.add(cur)
                cur = word
            } else {
                cur = next
            }
        }
        if (cur.isNotEmpty()) out.add(cur)
        return out.take(3)
    }

    private fun begin(canvas: Canvas, rect: RectF): Int {
        val s = min(rect.width() / W, rect.height() / H)
        val n = canvas.save()
        canvas.translate(rect.left, rect.top)
        canvas.scale(s, s)
        return n
    }

    private fun pixelPaint() = Paint().apply { isFilterBitmap = false }

    private fun fill(color: Int) = Paint().apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun stroke(color: Int, width: Float) = Paint().apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
    }

    private fun text(color: Int, size: Float, align: Paint.Align = Paint.Align.CENTER) =
        Paint().apply {
            this.color = color
            textSize = size
            typeface = face
            textAlign = align
        }

    private fun fit(s: String, maxW: Float, size: Float): Float {
        val w = text(INK, size, Paint.Align.LEFT).measureText(s)
        return if (w > maxW && w > 0f) size * maxW / w else size
    }
}
