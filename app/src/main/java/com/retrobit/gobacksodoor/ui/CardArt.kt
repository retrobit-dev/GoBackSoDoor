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
 * Kartu bergaya Clash Royale (pixel font): bingkai emas tebal dengan sudut membulat,
 * lencana biaya ungu di kiri atas, judul kategori, jendela gambar biru langit,
 * pita nama emas, baris nilai, dan area efek krem.
 * Semua ukuran dalam satuan 200x290 lalu diskalakan ke ukuran layar.
 */
object CardArt {
    const val W = 200f
    const val H = 290f

    private val NAVY = 0xFF14215A.toInt()
    private val GOLD = 0xFFFFC83D.toInt()
    private val BODY = 0xFFE9F4FF.toInt()
    private val SKY = 0xFF7CCBFF.toInt()
    private val GRASS = 0xFF62C15A.toInt()
    private val CREAM = 0xFFFFF8E7.toInt()
    private val ELIXIR = 0xFF9B3FD8.toInt()
    private val PILL = 0xFFFFFFFF.toInt()

    private var face: Typeface = Typeface.MONOSPACE

    private class Look(val frame: Int, val title: String, val name: String, val cost: String, val rows: List<Pair<String, String>>, val effect: String)

    fun draw(canvas: Canvas, context: Context, card: Card, rect: RectF, enabled: Boolean = true, selected: Boolean = false) {
        face = PixelFont.get(context)
        val lk = look(card)
        val n = begin(canvas, rect)

        // Bingkai: navy di luar, emas tebal, lalu badan kartu biru muda.
        canvas.drawRoundRect(RectF(0f, 0f, W, H), 22f, 22f, fill(NAVY))
        canvas.drawRoundRect(RectF(3f, 3f, W - 3f, H - 3f), 20f, 20f, fill(GOLD))
        canvas.drawRoundRect(RectF(9f, 9f, W - 9f, H - 9f), 16f, 16f, fill(lk.frame))
        canvas.drawRoundRect(RectF(12f, 12f, W - 12f, H - 12f), 13f, 13f, fill(BODY))

        // Lencana biaya (kiri atas) dan judul kategori (pita).
        canvas.drawCircle(30f, 32f, 17f, fill(NAVY))
        canvas.drawCircle(30f, 32f, 14f, fill(ELIXIR))
        canvas.drawText(lk.cost, 30f, 37f, text(0xFFFFFFFF.toInt(), fit(lk.cost, 22f, 12f)))
        val title = RectF(52f, 20f, 186f, 44f)
        canvas.drawRoundRect(title, 8f, 8f, fill(lk.frame))
        canvas.drawRoundRect(title, 8f, 8f, stroke(NAVY, 3f))
        canvas.drawText(lk.title, title.centerX(), 36f, text(0xFFFFFFFF.toInt(), fit(lk.title, 118f, 11f)))

        // Jendela gambar: langit biru dengan rumput di bawah; karakter di tengah.
        val art = RectF(18f, 52f, 182f, 172f)
        val artPath = Path().apply { addRoundRect(art, 10f, 10f, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(artPath)
        canvas.drawRect(art, fill(SKY))
        canvas.drawRect(RectF(art.left, art.centerY(), art.right, art.bottom), fill(GRASS))
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
            canvas.drawText(lk.name.substringAfter(' ', lk.name), 100f, 120f, text(NAVY, 40f))
        }
        canvas.restore()
        canvas.drawRoundRect(art, 10f, 10f, stroke(NAVY, 3f))

        // Pita nama emas.
        val banner = RectF(14f, 170f, 186f, 194f)
        canvas.drawRoundRect(banner, 8f, 8f, fill(GOLD))
        canvas.drawRoundRect(banner, 8f, 8f, stroke(NAVY, 3f))
        canvas.drawRoundRect(RectF(banner.left + 3f, banner.top + 2f, banner.right - 3f, banner.centerY()), 4f, 4f, fill(0x55FFFFFF))
        canvas.drawText(lk.name, 100f, 186f, text(NAVY, fit(lk.name, 150f, 11f)))

        // Baris nilai: ikon kotak emas, label, dan pil putih di kanan.
        var y = 200f
        for ((label, value) in lk.rows) {
            canvas.drawRoundRect(RectF(20f, y + 3f, 32f, y + 15f), 4f, 4f, fill(GOLD))
            canvas.drawRoundRect(RectF(20f, y + 3f, 32f, y + 15f), 4f, 4f, stroke(NAVY, 2f))
            canvas.drawText(label, 38f, y + 13f, text(NAVY, 7f, Paint.Align.LEFT))
            // Font pixel: lebar tiap huruf kira-kira sama dengan ukuran teksnya.
            val pillW = (value.length * 7f + 14f).coerceIn(44f, 112f)
            val pill = RectF(182f - pillW, y + 1f, 182f, y + 17f)
            canvas.drawRoundRect(pill, 8f, 8f, fill(PILL))
            canvas.drawRoundRect(pill, 8f, 8f, stroke(NAVY, 2f))
            val vs = fit(value, pillW - 10f, 7f)
            canvas.drawText(value, pill.centerX(), y + 12f, text(NAVY, vs))
            y += 20f
        }

        // Efek karakter (atau keterangan kartu netral) dalam kotak krem.
        val box = RectF(14f, 244f, 186f, 280f)
        canvas.drawRoundRect(box, 8f, 8f, fill(CREAM))
        canvas.drawRoundRect(box, 8f, 8f, stroke(NAVY, 2f))
        var ly = 254f
        for (line in wrap(lk.effect, 24)) {
            canvas.drawText(line, 21f, ly, text(NAVY, 6f, Paint.Align.LEFT))
            ly += 9f
        }

        if (!enabled) canvas.drawRoundRect(RectF(0f, 0f, W, H), 22f, 22f, fill(Color.argb(150, 0, 0, 0)))
        if (selected) canvas.drawRoundRect(RectF(2f, 2f, W - 2f, H - 2f), 22f, 22f, stroke(Color.rgb(255, 235, 59), 5f))
        canvas.restoreToCount(n)
    }

    /** Punggung kartu (dek): biru dengan pola emas, tanpa label. */
    fun drawBack(canvas: Canvas, rect: RectF) {
        val n = begin(canvas, rect)
        canvas.drawRoundRect(RectF(0f, 0f, W, H), 22f, 22f, fill(NAVY))
        canvas.drawRoundRect(RectF(4f, 4f, W - 4f, H - 4f), 19f, 19f, fill(GOLD))
        canvas.drawRoundRect(RectF(12f, 12f, W - 12f, H - 12f), 14f, 14f, fill(0xFF2C6FD6.toInt()))
        val step = 16f
        var yy = 0f
        var row = 0
        while (yy < H) {
            var xx = if (row % 2 == 0) 0f else step / 2f
            while (xx < W) {
                canvas.drawRect(RectF(xx + 3f, yy + 3f, xx + step - 3f, yy + step - 3f), fill(Color.argb(60, 255, 255, 255)))
                xx += step
            }
            yy += step
            row++
        }
        val diamond = Path().apply {
            moveTo(100f, 110f); lineTo(140f, 150f); lineTo(100f, 190f); lineTo(60f, 150f); close()
        }
        canvas.drawPath(diamond, fill(GOLD))
        canvas.drawPath(diamond, stroke(NAVY, 3f))
        canvas.restoreToCount(n)
    }

    private fun look(card: Card): Look {
        val roster = ATTACKER_ROSTER + GUARD_ROSTER
        return when (card) {
            is Card.Move -> {
                val c = roster.first { it.name == card.owner }
                Look(
                    frame = Color.rgb(230, 120, 40), title = "GERAK", name = c.name.uppercase(), cost = "1",
                    rows = listOf("GERAKAN" to dirName(card.dir), "LANGKAH" to "1-${c.stamina} KOTAK"),
                    effect = c.effect,
                )
            }
            is Card.Touch -> {
                val c = roster.first { it.name == card.owner }
                Look(Color.rgb(220, 50, 47), "SENTUH", c.name.uppercase(), "3", listOf("GERAKAN" to "Sentuh", "BIAYA" to "3 STAMINA"), c.effect)
            }
            is Card.Evade -> {
                val c = roster.first { it.name == card.owner }
                Look(Color.rgb(30, 130, 230), "HINDAR", c.name.uppercase(), "3", listOf("GERAKAN" to "Hindar", "BIAYA" to "3 STAMINA"), c.effect)
            }
            is Card.Star -> {
                val c = roster.first { it.name == card.owner }
                Look(Color.rgb(255, 170, 0), "BINTANG", c.name.uppercase(), "5", listOf("GERAKAN" to "Bintang", "BIAYA" to "5 STAMINA"), c.effect)
            }
            is Card.Stamina -> Look(
                Color.rgb(60, 170, 70), "STAMINA", "STAMINA ${if (card.value > 0) "+" else "-"}${kotlin.math.abs(card.value)}", "0", emptyList(),
                "Ketuk karakter tujuan: sekutu +${card.value}, lawan -${card.value} stamina.",
            )
            is Card.Draw -> Look(
                Color.rgb(150, 80, 220), "AMBIL", "AMBIL +${card.value}", "0", emptyList(),
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
        val w = text(NAVY, size, Paint.Align.LEFT).measureText(s)
        return if (w > maxW && w > 0f) size * maxW / w else size
    }
}
