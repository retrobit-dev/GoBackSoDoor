package com.retrobit.gobacksodoor.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import com.retrobit.gobacksodoor.game.COLS
import com.retrobit.gobacksodoor.game.Card
import com.retrobit.gobacksodoor.game.Facing
import com.retrobit.gobacksodoor.game.GameEngine
import com.retrobit.gobacksodoor.game.GuardType
import com.retrobit.gobacksodoor.game.HAND_SIZE
import com.retrobit.gobacksodoor.game.MAX_STEPS
import com.retrobit.gobacksodoor.game.Phase
import com.retrobit.gobacksodoor.game.ROWS
import com.retrobit.gobacksodoor.game.SetupTool
import com.retrobit.gobacksodoor.game.Side
import com.retrobit.gobacksodoor.game.TOTAL_ROUNDS
import com.retrobit.gobacksodoor.game.Zone
import com.retrobit.gobacksodoor.game.zoneOf
import kotlin.math.min

/** Tombol aksi (langkah, ambil, buang, dan lain-lain). */
private class Btn(
    val label: String,
    val enabled: Boolean,
    val active: Boolean,
    val action: () -> Unit,
) {
    var rect: RectF = RectF()
}

/**
 * Layar permainan: papan arena di tengah, dek di kanan, kartu terbuang di kiri,
 * dan tangan kartu di bawah. Tombol aksi berada di antara papan dan tangan.
 */
class GameView(context: Context) : View(context) {

    private val engine = GameEngine()

    private var cell = 0f
    private var boardLeft = 0f
    private var boardTop = 0f
    private var margin = 0f
    private var gap = 0f
    private var hudH = 0f
    private var actionTop = 0f
    private var handTop = 0f
    private var cardW = 0f
    private var cardH = 0f
    private var pileW = 0f
    private var discardRect = RectF()
    private var deckRect = RectF()
    private var buttons: List<Btn> = emptyList()
    private val handHits = mutableListOf<Pair<RectF, Card>>()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(110, 40, 25, 10)
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 235, 59)
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
    }
    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(110, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val leftText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        isFakeBoldText = true
        textAlign = Paint.Align.LEFT
    }
    private val hudPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    init {
        setBackgroundColor(Color.rgb(18, 24, 38))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        margin = w * 0.03f
        gap = margin * 0.6f
        hudH = h * 0.14f

        val boardBottom = h * 0.60f
        val availH = boardBottom - hudH - margin
        cell = min((w * 0.7f) / COLS, availH / ROWS)
        boardLeft = (w - cell * COLS) / 2f
        boardTop = hudH + (availH - cell * ROWS) / 2f

        actionTop = h * 0.615f
        handTop = h * 0.79f
        val handH = h - margin - handTop
        cardW = min((w - 2 * margin - 4 * gap) / 5f, (handH - margin) * CardArt.W / CardArt.H)
        cardH = cardW * CardArt.H / CardArt.W

        val boardRight = boardLeft + cell * COLS
        val midY = boardTop + cell * ROWS / 2f
        pileW = min(boardLeft - 2 * margin, cardW)
        val pileH = pileW * CardArt.H / CardArt.W
        val leftCenter = boardLeft / 2f
        val rightCenter = boardRight + (w - boardRight) / 2f
        discardRect = RectF(leftCenter - pileW / 2f, midY - pileH / 2f, leftCenter + pileW / 2f, midY + pileH / 2f)
        deckRect = RectF(rightCenter - pileW / 2f, midY - pileH / 2f, rightCenter + pileW / 2f, midY + pileH / 2f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawHud(canvas)
        drawBoard(canvas)
        drawPieces(canvas)
        drawPiles(canvas)
        drawActions(canvas)
        drawHand(canvas)
    }

    // ---- HUD ----

    private fun drawHud(canvas: Canvas) {
        val w = (width - 2 * margin).toInt()
        hudPaint.textSize = hudH * 0.13f
        hudPaint.isFakeBoldText = true
        val status = layoutOf(engine.statusLine(), w)
        canvas.save()
        canvas.translate(margin, margin * 0.5f)
        status.draw(canvas)
        canvas.restore()

        hudPaint.isFakeBoldText = false
        val msg = layoutOf(engine.message, w)
        canvas.save()
        canvas.translate(margin, margin * 0.5f + status.height + 6f)
        msg.draw(canvas)
        canvas.restore()
    }

    private fun layoutOf(s: String, w: Int): StaticLayout =
        StaticLayout.Builder.obtain(s, 0, s.length, hudPaint, w)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()

    // ---- Papan ----

    private fun zoneColor(z: Zone): Int = when (z) {
        Zone.WALL -> Color.rgb(70, 50, 30)
        Zone.START -> Color.rgb(102, 187, 106)
        Zone.BELOW -> Color.rgb(141, 110, 99)
        Zone.END -> Color.rgb(255, 213, 79)
        Zone.LANE -> Color.rgb(255, 236, 200)
        Zone.FIELD -> Color.rgb(232, 210, 165)
    }

    private fun cellRect(x: Int, y: Int) = RectF(
        boardLeft + x * cell, boardTop + y * cell,
        boardLeft + (x + 1) * cell, boardTop + (y + 1) * cell,
    )

    private fun drawBoard(canvas: Canvas) {
        for (x in 0 until COLS) for (y in 0 until ROWS) {
            fill.color = zoneColor(zoneOf(x, y))
            canvas.drawRect(cellRect(x, y), fill)
        }
        if (engine.phase == Phase.PLAY && engine.turn == Side.GUARD) {
            fill.color = Color.argb(120, 229, 57, 53)
            for (g in engine.guards) for (c in engine.threatCells(g)) {
                canvas.drawRect(cellRect(c.x, c.y), fill)
            }
        }
        if (engine.phase == Phase.PLAY) {
            fill.color = Color.argb(190, 255, 235, 59)
            for (c in engine.moveTargets().keys) {
                canvas.drawCircle(boardLeft + (c.x + 0.5f) * cell, boardTop + (c.y + 0.5f) * cell, cell * 0.18f, fill)
            }
        }
        for (x in 0..COLS) {
            val px = boardLeft + x * cell
            canvas.drawLine(px, boardTop, px, boardTop + cell * ROWS, gridPaint)
        }
        for (y in 0..ROWS) {
            val py = boardTop + y * cell
            canvas.drawLine(boardLeft, py, boardLeft + cell * COLS, py, gridPaint)
        }
    }

    private fun drawPieces(canvas: Canvas) {
        for (p in engine.attackers + engine.guards) {
            val cx = boardLeft + (p.x + 0.5f) * cell
            val cy = boardTop + (p.y + 0.5f) * cell
            val r = cell * 0.36f

            fill.color = Color.argb(90, 0, 0, 0)
            canvas.drawCircle(cx + 2f, cy + 3f, r, fill)

            if (p.side == Side.ATTACKER) {
                fill.color = Color.rgb(41, 121, 255)
                canvas.drawCircle(cx, cy, r, fill)
            } else {
                fill.color = Color.rgb(229, 57, 53)
                canvas.drawRoundRect(RectF(cx - r, cy - r, cx + r, cy + r), r * 0.3f, r * 0.3f, fill)
            }
            fill.color = Color.argb(70, 255, 255, 255)
            canvas.drawCircle(cx, cy - r * 0.2f, r * 0.55f, fill)

            borderPaint.strokeWidth = cell * 0.05f
            if (p.side == Side.ATTACKER) {
                canvas.drawCircle(cx, cy, r, borderPaint)
            } else {
                canvas.drawRoundRect(RectF(cx - r, cy - r, cx + r, cy + r), r * 0.3f, r * 0.3f, borderPaint)
            }
            if (engine.activeId == p.id) {
                ringPaint.strokeWidth = cell * 0.06f
                canvas.drawCircle(cx, cy, r + cell * 0.08f, ringPaint)
            }

            text.textSize = cell * 0.3f
            drawCentered(canvas, p.stamina.toString(), cx, cy, text)

            // Batang stamina di atas bidak.
            val barW = cell * 0.7f
            val barH = cell * 0.08f
            val barTop = cy - r - cell * 0.14f
            fill.color = Color.argb(180, 0, 0, 0)
            canvas.drawRect(RectF(cx - barW / 2, barTop, cx + barW / 2, barTop + barH), fill)
            val max = p.char.stamina.coerceAtLeast(1)
            val frac = (p.stamina.toFloat() / max).coerceIn(0f, 1f)
            fill.color = if (frac > 0.4f) Color.rgb(102, 187, 106) else Color.rgb(255, 112, 67)
            canvas.drawRect(RectF(cx - barW / 2, barTop, cx - barW / 2 + barW * frac, barTop + barH), fill)

            // Nama karakter di bawah bidak.
            text.textSize = cell * 0.2f
            drawCentered(canvas, p.char.name, cx, cy + r + cell * 0.2f, text)

            if (p.guardType == GuardType.V) {
                text.textSize = cell * 0.26f
                val arrow = if (p.facing == Facing.LEFT) "◀" else "▶"
                val ax = if (p.facing == Facing.LEFT) cx - r - cell * 0.22f else cx + r + cell * 0.22f
                drawCentered(canvas, arrow, ax, cy, text)
            }
        }
    }

    // ---- Dek dan buangan ----

    private fun drawPiles(canvas: Canvas) {
        val side = engine.turn
        val d = engine.decks[side] ?: return
        val who = if (side == Side.ATTACKER) "Penyerang" else "Penjaga"
        val labelSize = pileW * 0.1f
        text.textSize = labelSize

        // Dek di kanan: tumpukan punggung kartu.
        if (d.deck.isNotEmpty()) {
            for (k in 3 downTo 1) {
                val r = RectF(deckRect)
                r.offset(k * 4f, -k * 4f)
                CardArt.drawBack(canvas, r)
            }
            CardArt.drawBack(canvas, deckRect)
        } else {
            canvas.drawRoundRect(deckRect, 12f, 12f, dashPaint)
        }
        drawCentered(canvas, "DEK $who", deckRect.centerX(), deckRect.bottom + labelSize * 1.4f, text)
        drawCentered(canvas, "${d.deck.size} kartu", deckRect.centerX(), deckRect.bottom + labelSize * 2.8f, text)

        // Buangan di kiri: kartu terakhir yang dipakai.
        val last = d.discard.lastOrNull()
        if (last != null) {
            for (k in 2 downTo 1) {
                val r = RectF(discardRect)
                r.offset(-k * 3f, k * 3f)
                CardArt.drawBack(canvas, r)
            }
            CardArt.draw(canvas, last, discardRect)
        } else {
            canvas.drawRoundRect(discardRect, 12f, 12f, dashPaint)
        }
        drawCentered(canvas, "BUANGAN", discardRect.centerX(), discardRect.bottom + labelSize * 1.4f, text)
        drawCentered(canvas, "${d.discard.size} kartu", discardRect.centerX(), discardRect.bottom + labelSize * 2.8f, text)
    }

    // ---- Tangan ----

    private fun usable(c: Card): Boolean = when {
        engine.owed != null -> true
        engine.pending != null -> c is Card.Evade
        engine.postMove != null -> c is Card.Star
        c is Card.Star -> false
        c is Card.Evade -> false
        c is Card.Touch -> engine.turn == Side.GUARD
        else -> true
    }

    private fun drawHand(canvas: Canvas) {
        handHits.clear()
        if (engine.phase != Phase.PLAY) return
        val hand = engine.decks[engine.turn]?.hand ?: return
        if (hand.isEmpty()) return
        val total = hand.size * cardW + (hand.size - 1) * gap
        var x = (width - total) / 2f
        val baseY = handTop + margin * 0.5f
        for (c in hand) {
            val selected = engine.activeCard == c
            val lift = if (selected) cardH * 0.08f else 0f
            val rect = RectF(x, baseY - lift, x + cardW, baseY - lift + cardH)
            CardArt.draw(canvas, c, rect, enabled = usable(c), selected = selected)
            handHits += rect to c
            x += cardW + gap
        }
    }

    // ---- Aksi ----

    private fun infoText(): String = when (engine.phase) {
        Phase.SETUP -> "Pemasangan: ketuk papan sesuai pilihan di bawah."
        Phase.PLAY -> {
            val p = engine.pieceById(engine.activeId)
            val base = p?.let { "Dipilih: ${it.char.name} · stamina ${it.stamina}" } ?: "Ketuk karakter di papan."
            val hand = engine.decks[engine.turn]?.hand?.size ?: 0
            "$base · Tangan $hand/$HAND_SIZE"
        }
        else -> ""
    }

    private fun buildActionRows(): List<List<Btn>> {
        val rows = mutableListOf<List<Btn>>()
        when (engine.phase) {
            Phase.SETUP -> rows += listOf(
                Btn("Pasang Penyerang", true, engine.setupTool == SetupTool.ATTACKER) { engine.setupTool = SetupTool.ATTACKER },
                Btn("Pasang Penjaga", true, engine.setupTool == SetupTool.GUARD) { engine.setupTool = SetupTool.GUARD },
                Btn("Hapus", true, engine.setupTool == SetupTool.ERASE) { engine.setupTool = SetupTool.ERASE },
            )
            Phase.OVER -> rows += listOf(
                Btn(if (engine.round >= TOTAL_ROUNDS) "Lihat Hasil" else "Babak Berikutnya", true, false) { engine.nextRound() },
            )
            Phase.WON, Phase.ENDED -> rows += listOf(
                Btn("Main Lagi", true, false) { engine.newGame() },
            )
            Phase.PLAY -> {
                val hand = engine.decks[engine.turn]?.hand ?: emptyList<Card>()
                if (engine.owed != null) {
                    // Pilih kartu di tangan untuk dibuang.
                } else if (engine.pending != null) {
                    rows += listOf(
                        Btn("Hindar (3)", engine.decks[Side.ATTACKER]?.hand?.any { it is Card.Evade } == true, false) { engine.evade() },
                        Btn("Terima Sentuhan", true, false) { engine.acceptTouch() },
                    )
                } else if (engine.postMove != null) {
                    rows += listOf(Btn("Akhiri Giliran", true, false) { engine.endPostMove() })
                } else {
                    if (!engine.drew && !engine.played && hand.size < HAND_SIZE) {
                        rows += (0..(HAND_SIZE - hand.size)).map { n ->
                            Btn(if (n == 0) "Tidak Ambil" else "Ambil $n", true, false) { engine.chooseDraw(n) }
                        }
                    }
                    rows += (1..MAX_STEPS).map { n ->
                        Btn("Langkah $n", true, engine.steps == n) { engine.setSteps(n) }
                    }
                    val actions = mutableListOf(
                        Btn("Buang Kartu", true, false) { engine.discardSelected() },
                        Btn("Akhiri Giliran", true, false) { engine.skipTurn() },
                    )
                    val sel = engine.pieceById(engine.activeId)
                    if (engine.turn == Side.GUARD && sel?.guardType == GuardType.V) {
                        actions += Btn("Putar Hadap", true, false) { engine.turnFace() }
                    }
                    rows += actions
                }
            }
        }
        return rows
    }

    private fun drawActions(canvas: Canvas) {
        val info = infoText()
        val infoSize = hudH * 0.12f
        leftText.textSize = fitSize(info, width - 2 * margin, infoSize, leftText)
        canvas.drawText(info, margin, actionTop + infoSize, leftText)

        val rows = buildActionRows()
        if (rows.isEmpty()) {
            buttons = emptyList()
            return
        }
        val rowsTop = actionTop + infoSize * 1.8f
        val rowsBottom = handTop - margin
        val rowH = min(height * 0.075f, (rowsBottom - rowsTop) / rows.size)
        val all = mutableListOf<Btn>()
        rows.forEachIndexed { ri, row ->
            val top = rowsTop + ri * rowH + gap / 2f
            val bottom = top + rowH - gap
            val slot = (width - 2 * margin - gap * (row.size - 1)) / row.size
            row.forEachIndexed { ci, b ->
                val l = margin + ci * (slot + gap)
                b.rect = RectF(l, top, l + slot, bottom)
                all += b
            }
        }
        buttons = all
        for (b in all) {
            fill.color = when {
                !b.enabled -> Color.rgb(45, 45, 55)
                b.active -> Color.rgb(255, 193, 7)
                else -> Color.rgb(40, 84, 150)
            }
            canvas.drawRoundRect(b.rect, 16f, 16f, fill)
            text.color = when {
                b.active -> Color.BLACK
                b.enabled -> Color.WHITE
                else -> Color.GRAY
            }
            text.textSize = fitSize(b.label, b.rect.width() - 8f, b.rect.height() * 0.34f, text)
            drawCentered(canvas, b.label, b.rect.centerX(), b.rect.centerY(), text)
        }
        text.color = Color.WHITE
    }

    // ---- Input ----

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            handleTap(event.x, event.y)
            performClick()
        }
        return true
    }

    private fun handleTap(x: Float, y: Float) {
        val boardRight = boardLeft + cell * COLS
        val boardBottom = boardTop + cell * ROWS
        if (x >= boardLeft && x < boardRight && y >= boardTop && y < boardBottom) {
            val cx = ((x - boardLeft) / cell).toInt().coerceIn(0, COLS - 1)
            val cy = ((y - boardTop) / cell).toInt().coerceIn(0, ROWS - 1)
            engine.onCell(cx, cy)
        } else {
            val card = handHits.lastOrNull { it.first.contains(x, y) }?.second
            if (card != null) {
                engine.selectCard(card)
            } else {
                buttons.firstOrNull { it.rect.contains(x, y) }?.let { if (it.enabled) it.action() }
            }
        }
        invalidate()
    }

    // ---- Util ----

    private fun drawCentered(canvas: Canvas, s: String, cx: Float, cy: Float, p: Paint) {
        val offset = (p.descent() + p.ascent()) / 2f
        canvas.drawText(s, cx, cy - offset, p)
    }

    private fun fitSize(s: String, maxW: Float, start: Float, p: Paint): Float {
        p.textSize = start
        val w = p.measureText(s)
        return if (w > maxW && w > 0f) start * maxW / w else start
    }
}
