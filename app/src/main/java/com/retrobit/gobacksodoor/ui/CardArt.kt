package com.retrobit.gobacksodoor.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.retrobit.gobacksodoor.game.Card
import com.retrobit.gobacksodoor.game.Dir
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Kartu bergaya trading card (gaya Yu-Gi-Oh): bingkai berwarna, nama, orb biaya,
 * ilustrasi, jenis kartu, kotak teks, dan footer. Semua ukuran dalam satuan 200x290
 * lalu diskalakan ke ukuran layar.
 */
object CardArt {
    const val W = 200f
    const val H = 290f

    private class Look(
        val frame: Int,
        val dark: Int,
        val title: String,
        val kind: String,
        val orb: String,
        val footer: String,
        val body: String,
    )

    private fun look(c: Card): Look = when (c) {
        is Card.Move -> when (c.dir) {
            Dir.VERTICAL -> Look(
                Color.rgb(30, 136, 229), Color.rgb(10, 50, 120), "Gerak Atas-Bawah", "KARTU GERAK", "1+",
                "Biaya: 1 stamina / kotak",
                "Bayar 1 stamina per kotak, termasuk garis batas. Penyerang memantul di dinding tanpa biaya.",
            )
            Dir.HORIZONTAL -> Look(
                Color.rgb(0, 150, 136), Color.rgb(0, 70, 60), "Gerak Kanan-Kiri", "KARTU GERAK", "1+",
                "Biaya: 1 stamina / kotak",
                "Bayar 1 stamina per kotak, termasuk garis batas. Penyerang memantul di dinding tanpa biaya.",
            )
            Dir.CROSS -> Look(
                Color.rgb(142, 36, 170), Color.rgb(60, 15, 90), "Gerak Silang", "KARTU GERAK", "1+",
                "Biaya: 1 stamina / kotak",
                "Penyerang bergerak diagonal, penjaga ortogonal. Pilih 1 sampai stamina karakter.",
            )
        }
        is Card.Touch -> Look(
            Color.rgb(229, 57, 53), Color.rgb(110, 10, 10), "Sentuh", "KARTU PENJAGA", "3",
            "Biaya: 3 stamina",
            "Sentuh penyerang di area merah penjagamu. Penyerang bisa membalas dengan Hindar.",
        )
        is Card.Evade -> Look(
            Color.rgb(0, 172, 193), Color.rgb(0, 80, 95), "Hindar", "KARTU PENYERANG", "3",
            "Biaya: 3 stamina",
            "Balas sentuhan yang tertunda. Hanya bisa dipakai saat disentuh.",
        )
        is Card.Star -> Look(
            Color.rgb(255, 193, 7), Color.rgb(150, 100, 0), "Bintang", "KARTU BINTANG", "5",
            "Biaya: 5 stamina",
            "Dipakai setelah satu gerak jika stamina 5+. Kembali ke dek. Efek karakter belum aktif.",
        )
        is Card.Stamina -> Look(
            Color.rgb(67, 160, 71), Color.rgb(20, 80, 30), "Stamina ±${c.value}", "KARTU STAMINA", "0",
            "Biaya: 0",
            "Pilih karakter: sekutu +${c.value}, lawan −${c.value} stamina.",
        )
        is Card.Draw -> Look(
            Color.rgb(123, 31, 162), Color.rgb(60, 15, 90), "Ambil +${c.value}", "KARTU AMBIL", "0",
            "Biaya: 0",
            "Ambil ${c.value} kartu dari teratas dek. Giliran tetap berjalan.",
        )
    }

    /** Menggambar kartu wajah ke dalam [rect]. */
    fun draw(canvas: Canvas, card: Card, rect: RectF, enabled: Boolean = true, selected: Boolean = false) {
        val lk = look(card)
        val n = begin(canvas, rect)

        // Bingkai luar dan garis emas tipis.
        canvas.drawRoundRect(RectF(0f, 0f, W, H), 14f, 14f, fillPaint(lk.frame))
        canvas.drawRoundRect(RectF(4f, 4f, W - 4f, H - 4f), 11f, 11f, strokePaint(lk.dark, 3f))
        canvas.drawRoundRect(RectF(1.5f, 1.5f, W - 1.5f, H - 1.5f), 13f, 13f, strokePaint(Color.rgb(255, 215, 0), 1.5f))

        // Nama kartu.
        canvas.drawRoundRect(RectF(42f, 9f, 192f, 41f), 6f, 6f, fillPaint(Color.argb(210, 12, 12, 22)))
        val titleSize = fit(lk.title, 132f, 15f)
        canvas.drawText(lk.title, 50f, 30f, textPaint(Color.WHITE, titleSize, Paint.Align.LEFT))

        // Orb biaya.
        canvas.drawCircle(24f, 25f, 17f, fillPaint(Color.rgb(12, 12, 28)))
        canvas.drawCircle(24f, 25f, 17f, strokePaint(Color.rgb(255, 215, 0), 3f))
        canvas.drawText(lk.orb, 24f, 31f, textPaint(Color.WHITE, if (lk.orb.length > 1) 12f else 17f))

        // Jendela ilustrasi.
        val art = RectF(12f, 46f, 188f, 150f)
        canvas.drawRoundRect(art, 8f, 8f, fillPaint(lk.dark))
        canvas.save()
        canvas.clipRect(art)
        canvas.drawCircle(100f, 98f, 70f, fillPaint(Color.argb(50, 255, 255, 255)))
        canvas.restore()
        icon(canvas, card)
        canvas.drawRoundRect(art, 8f, 8f, strokePaint(Color.rgb(255, 215, 0), 2f))

        // Baris jenis kartu.
        canvas.drawRect(RectF(12f, 155f, 188f, 170f), fillPaint(lk.dark))
        canvas.drawText(lk.kind, 100f, 166f, textPaint(Color.WHITE, 9.5f))

        // Kotak teks.
        val box = RectF(12f, 174f, 188f, 262f)
        canvas.drawRoundRect(box, 8f, 8f, fillPaint(Color.rgb(255, 248, 225)))
        canvas.drawRoundRect(box, 8f, 8f, strokePaint(lk.dark, 2f))
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 25, 15)
            textSize = 10.5f
        }
        val layout = StaticLayout.Builder
            .obtain(lk.body, 0, lk.body.length, tp, 166)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()
        canvas.save()
        canvas.translate(17f, 180f)
        layout.draw(canvas)
        canvas.restore()

        // Footer.
        canvas.drawText(lk.footer, 100f, 282f, textPaint(Color.WHITE, 9f))

        if (!enabled) canvas.drawRoundRect(RectF(0f, 0f, W, H), 14f, 14f, fillPaint(Color.argb(150, 0, 0, 0)))
        if (selected) canvas.drawRoundRect(RectF(2.5f, 2.5f, W - 2.5f, H - 2.5f), 13f, 13f, strokePaint(Color.rgb(255, 235, 59), 5f))
        canvas.restoreToCount(n)
    }

    /** Punggung kartu (dek). */
    fun drawBack(canvas: Canvas, rect: RectF) {
        val n = begin(canvas, rect)
        canvas.drawRoundRect(RectF(0f, 0f, W, H), 14f, 14f, fillPaint(Color.rgb(24, 34, 78)))
        canvas.save()
        canvas.clipRect(RectF(6f, 6f, W - 6f, H - 6f))
        val pat = strokePaint(Color.argb(60, 255, 215, 0), 2f)
        var x = -H
        while (x < W + H) {
            canvas.drawLine(x, 0f, x + H, H, pat)
            x += 16f
        }
        canvas.restore()
        canvas.drawRoundRect(RectF(6f, 6f, W - 6f, H - 6f), 10f, 10f, strokePaint(Color.rgb(255, 215, 0), 4f))
        val diamond = Path().apply {
            moveTo(100f, 100f)
            lineTo(140f, 145f)
            lineTo(100f, 190f)
            lineTo(60f, 145f)
            close()
        }
        canvas.drawPath(diamond, fillPaint(Color.rgb(255, 193, 7)))
        canvas.drawText("GB", 100f, 157f, textPaint(Color.rgb(24, 34, 78), 30f))
        canvas.restoreToCount(n)
    }

    private fun icon(canvas: Canvas, c: Card) {
        when (c) {
            is Card.Move -> when (c.dir) {
                Dir.VERTICAL -> shape(
                    canvas,
                    poly(
                        listOf(
                            100f to 56f, 118f to 78f, 107f to 78f, 107f to 120f, 118f to 120f,
                            100f to 142f, 82f to 120f, 93f to 120f, 93f to 78f, 82f to 78f,
                        ),
                    ),
                )
                Dir.HORIZONTAL -> shape(
                    canvas,
                    poly(
                        listOf(
                            44f to 98f, 66f to 80f, 66f to 90f, 134f to 90f, 134f to 80f,
                            156f to 98f, 134f to 116f, 134f to 106f, 66f to 106f, 66f to 116f,
                        ),
                    ),
                )
                Dir.CROSS -> {
                    val p = Path().apply {
                        moveTo(50f, 50f)
                        lineTo(150f, 146f)
                        moveTo(150f, 50f)
                        lineTo(50f, 146f)
                    }
                    line(canvas, p, 9f)
                    for ((x, y) in listOf(50f to 50f, 150f to 50f, 50f to 146f, 150f to 146f)) {
                        canvas.drawCircle(x, y, 11f, fillPaint(Color.WHITE))
                    }
                }
            }
            is Card.Touch -> {
                line(canvas, Path().apply { addCircle(100f, 98f, 38f, Path.Direction.CW) }, 7f)
                val cross = Path().apply {
                    moveTo(100f, 48f); lineTo(100f, 62f)
                    moveTo(100f, 134f); lineTo(100f, 148f)
                    moveTo(50f, 98f); lineTo(64f, 98f)
                    moveTo(136f, 98f); lineTo(150f, 98f)
                }
                line(canvas, cross, 7f)
                canvas.drawCircle(100f, 98f, 9f, fillPaint(Color.WHITE))
            }
            is Card.Evade -> {
                val shield = Path().apply {
                    moveTo(100f, 52f)
                    lineTo(146f, 68f)
                    lineTo(146f, 100f)
                    quadTo(146f, 138f, 100f, 152f)
                    quadTo(54f, 138f, 54f, 100f)
                    lineTo(54f, 68f)
                    close()
                }
                shape(canvas, shield)
            }
            is Card.Star -> shape(canvas, starPath())
            is Card.Stamina -> outlined(canvas, "±${c.value}", 100f, 120f, 64f)
            is Card.Draw -> outlined(canvas, "+${c.value}", 100f, 120f, 64f)
        }
    }

    private fun starPath(): Path {
        val pts = (0 until 10).map { i ->
            val r = if (i % 2 == 0) 50f else 22f
            val a = -PI / 2 + i * PI / 5
            Pair(100f + r * cos(a).toFloat(), 98f + r * sin(a).toFloat())
        }
        return poly(pts)
    }

    private fun poly(pts: List<Pair<Float, Float>>): Path = Path().apply {
        moveTo(pts[0].first, pts[0].second)
        for (i in 1 until pts.size) lineTo(pts[i].first, pts[i].second)
        close()
    }

    private fun shape(canvas: Canvas, path: Path) {
        canvas.drawPath(path, strokePaint(Color.BLACK, 5f))
        canvas.drawPath(path, fillPaint(Color.WHITE))
    }

    private fun line(canvas: Canvas, path: Path, width: Float) {
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = width + 4f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        })
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = width
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        })
    }

    private fun outlined(canvas: Canvas, s: String, x: Float, y: Float, size: Float) {
        val edge = textPaint(Color.BLACK, size).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.12f
        }
        canvas.drawText(s, x, y, edge)
        canvas.drawText(s, x, y, textPaint(Color.WHITE, size))
    }

    private fun begin(canvas: Canvas, rect: RectF): Int {
        val s = min(rect.width() / W, rect.height() / H)
        val n = canvas.save()
        canvas.translate(rect.left, rect.top)
        canvas.scale(s, s)
        return n
    }

    private fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun strokePaint(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
    }

    private fun textPaint(color: Int, size: Float, align: Paint.Align = Paint.Align.CENTER) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            isFakeBoldText = true
            textAlign = align
        }

    private fun fit(s: String, maxW: Float, size: Float): Float {
        val w = textPaint(Color.WHITE, size, Paint.Align.LEFT).measureText(s)
        return if (w > maxW && w > 0f) size * maxW / w else size
    }
}
