package com.retrobit.gobacksodoor.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.retrobit.gobacksodoor.game.ATTACKER_ROSTER
import com.retrobit.gobacksodoor.game.Card
import com.retrobit.gobacksodoor.game.GUARD_ROSTER
import com.retrobit.gobacksodoor.game.dirName
import kotlin.math.min

/**
 * Kartu karakter bergaya kartu koleksi (referensi: bingkai oranye, judul di kiri atas,
 * ilustrasi besar, pita nama, dan baris berisi pil nilai). Hanya baris yang dipakai
 * yang ditampilkan: gerakan, langkah/biaya, dan efek. Semua ukuran dalam satuan 200x290.
 */
object CardArt {
    const val W = 200f
    const val H = 290f

    private val CREAM = 0xFFFBE9D2.toInt()
    private val BANNER = 0xFFF5A03A.toInt()
    private val INK = 0xFF3B1E08.toInt()
    private val PILL = 0xFFBDEFF7.toInt()
    private val PILL_EDGE = 0xFF2B8C9E.toInt()

    private class Look(val frame: Int, val title: String, val name: String, val rows: List<Pair<String, String>>, val effect: String)

    fun draw(canvas: Canvas, context: Context, card: Card, rect: RectF, enabled: Boolean = true, selected: Boolean = false) {
        val lk = look(card)
        val n = begin(canvas, rect)

        // Bingkai luar.
        canvas.drawRoundRect(RectF(0f, 0f, W, H), 16f, 16f, fill(lk.frame))
        canvas.drawRoundRect(RectF(0f, 0f, W, H), 16f, 16f, stroke(INK, 3f))
        canvas.drawRoundRect(RectF(7f, 7f, W - 7f, H - 7f), 12f, 12f, fill(CREAM))

        // Judul kategori (gelembung di kiri atas).
        canvas.drawRoundRect(RectF(14f, 14f, 124f, 40f), 13f, 13f, fill(BANNER))
        canvas.drawRoundRect(RectF(14f, 14f, 124f, 40f), 13f, 13f, stroke(INK, 2f))
        canvas.drawText(lk.title, 69f, 33f, text(INK, fit(lk.title, 96f, 15f)))

        // Jendela ilustrasi.
        val art = RectF(14f, 46f, 186f, 182f)
        canvas.save()
        canvas.clipRect(art)
        canvas.drawRect(art, fill(0xFFFFF4E4.toInt()))
        canvas.drawCircle(100f, 110f, 70f, fill(0x33FFFFFF))
        val bmp = if (card.owner != null) CharacterArt.get(context, card.owner!!) else null
        if (bmp != null) {
            val sw = bmp.width.toFloat()
            val sh = sw * art.height() / art.width()
            val top = ((bmp.height - sh) / 2f).coerceAtLeast(0f)
            val src = android.graphics.Rect(0, top.toInt(), bmp.width, (top + sh).toInt().coerceAtMost(bmp.height))
            canvas.drawBitmap(bmp, src, art, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else {
            canvas.drawText(lk.name.substringAfter(' ', lk.name), 100f, 150f, text(INK, 58f))
        }
        canvas.restore()
        canvas.drawRoundRect(art, 10f, 10f, stroke(INK, 2.5f))

        // Pita nama.
        val banner = RectF(20f, 166f, 180f, 192f)
        canvas.drawRoundRect(banner, 9f, 9f, fill(BANNER))
        canvas.drawRoundRect(banner, 9f, 9f, stroke(INK, 2.5f))
        canvas.drawText(lk.name, 100f, 185f, text(INK, fit(lk.name, 146f, 17f)))

        // Baris nilai: label kiri, pil kanan.
        var y = 200f
        for ((label, value) in lk.rows) {
            canvas.drawCircle(28f, y + 9f, 5f, fill(BANNER))
            canvas.drawCircle(28f, y + 9f, 5f, stroke(INK, 1.5f))
            canvas.drawText(label, 38f, y + 13f, text(INK, 9f, Paint.Align.LEFT))
            val pillW = (value.length * 5.6f + 18f).coerceIn(44f, 120f)
            val pill = RectF(186f - pillW, y + 2f, 186f, y + 19f)
            canvas.drawRoundRect(pill, 9f, 9f, fill(PILL))
            canvas.drawRoundRect(pill, 9f, 9f, stroke(PILL_EDGE, 1.5f))
            canvas.drawText(value, pill.centerX(), y + 14f, text(INK, fit(value, pillW - 10f, 9.5f)))
            y += 22f
        }

        // Efek karakter (atau keterangan kartu netral) di bagian bawah.
        canvas.drawText("EFEK", 24f, y + 9f, text(INK, 8.5f, Paint.Align.LEFT))
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; textSize = 9f }
        val layout = StaticLayout.Builder.obtain(lk.effect, 0, lk.effect.length, tp, 150)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(24f, y + 13f)
        layout.draw(canvas)
        canvas.restore()

        if (!enabled) canvas.drawRoundRect(RectF(0f, 0f, W, H), 16f, 16f, fill(Color.argb(150, 0, 0, 0)))
        if (selected) canvas.drawRoundRect(RectF(2f, 2f, W - 2f, H - 2f), 15f, 15f, stroke(Color.rgb(255, 235, 59), 5f))
        canvas.restoreToCount(n)
    }

    /** Punggung kartu (dek): tanpa label, hanya pola. */
    fun drawBack(canvas: Canvas, rect: RectF) {
        val n = begin(canvas, rect)
        canvas.drawRoundRect(RectF(0f, 0f, W, H), 16f, 16f, fill(Color.rgb(24, 34, 78)))
        canvas.save()
        canvas.clipRect(RectF(7f, 7f, W - 7f, H - 7f))
        val pat = stroke(Color.argb(60, 255, 215, 0), 2f)
        var x = -H
        while (x < W + H) {
            canvas.drawLine(x, 0f, x + H, H, pat)
            x += 16f
        }
        canvas.restore()
        canvas.drawRoundRect(RectF(7f, 7f, W - 7f, H - 7f), 12f, 12f, stroke(Color.rgb(255, 215, 0), 4f))
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
                    rows = listOf("GERAKAN" to dirName(card.dir), "LANGKAH" to "1–${c.stamina} kotak"),
                    effect = c.effect,
                )
            }
            is Card.Touch -> {
                val c = roster.first { it.name == card.owner }
                Look(Color.rgb(179, 38, 30), "SENTUH", c.name.uppercase(), listOf("GERAKAN" to "Sentuh", "BIAYA" to "3 stamina"), c.effect)
            }
            is Card.Evade -> {
                val c = roster.first { it.name == card.owner }
                Look(Color.rgb(30, 111, 184), "HINDAR", c.name.uppercase(), listOf("GERAKAN" to "Hindar", "BIAYA" to "3 stamina"), c.effect)
            }
            is Card.Star -> {
                val c = roster.first { it.name == card.owner }
                Look(Color.rgb(184, 134, 11), "BINTANG", c.name.uppercase(), listOf("GERAKAN" to "Bintang", "BIAYA" to "5 stamina"), c.effect)
            }
            is Card.Stamina -> Look(
                Color.rgb(46, 125, 50), "STAMINA", "STAMINA ${if (card.value > 0) "+" else "−"}${kotlin.math.abs(card.value)}", emptyList(),
                "Ketuk karakter tujuan: sekutu +${card.value}, lawan −${card.value} stamina.",
            )
            is Card.Draw -> Look(
                Color.rgb(106, 63, 160), "AMBIL", "AMBIL +${card.value}", emptyList(),
                "Ambil ${card.value} kartu teratas dari dek. Tidak mengakhiri giliran.",
            )
        }
    }

    private fun begin(canvas: Canvas, rect: RectF): Int {
        val s = min(rect.width() / W, rect.height() / H)
        val n = canvas.save()
        canvas.translate(rect.left, rect.top)
        canvas.scale(s, s)
        return n
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
    }

    private fun text(color: Int, size: Float, align: Paint.Align = Paint.Align.CENTER) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            isFakeBoldText = true
            textAlign = align
        }

    private fun fit(s: String, maxW: Float, size: Float): Float {
        val w = text(INK, size, Paint.Align.LEFT).measureText(s)
        return if (w > maxW && w > 0f) size * maxW / w else size
    }
}
