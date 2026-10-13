package com.retrobit.gobacksodoor.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
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
import com.retrobit.gobacksodoor.game.dirName
import com.retrobit.gobacksodoor.game.zoneOf
import kotlin.math.min

/** Tombol di area kontrol (kartu tangan, langkah, aksi). */
private class Btn(
    val label: String,
    val enabled: Boolean,
    val active: Boolean,
    val action: () -> Unit,
) {
    var rect: RectF = RectF()
}

/** Menggambar papan Gobak Sodor versi kartu & stamina, serta menangani sentuhan. */
class GameView(context: Context) : View(context) {

    private val engine = GameEngine()

    private var cell = 0f
    private var boardLeft = 0f
    private var boardTop = 0f
    private var margin = 0f
    private var hudH = 0f
    private var controlsTop = 0f
    private var buttons: List<Btn> = emptyList()

    private val bg = Color.rgb(28, 28, 28)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(110, 75, 40)
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 235, 59)
        strokeWidth = 5f
        style = Paint.Style.STROKE
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
    private val msgPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(220, 220, 220) }

    init {
        setBackgroundColor(bg)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        margin = w * 0.04f
        hudH = h * 0.17f
        controlsTop = h * 0.60f
        val availH = controlsTop - hudH - margin
        cell = min((w - 2 * margin) / COLS, availH / ROWS)
        boardLeft = (w - cell * COLS) / 2f
        boardTop = hudH + (availH - cell * ROWS) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawHud(canvas)
        drawBoard(canvas)
        drawPieces(canvas)
        drawControls(canvas)
    }

    // ---- HUD ----

    private fun drawHud(canvas: Canvas) {
        leftText.textSize = fitSize(engine.statusLine(), width - 2 * margin, hudH * 0.2f, leftText)
        canvas.drawText(engine.statusLine(), margin, hudH * 0.3f, leftText)

        msgPaint.textSize = hudH * 0.15f
        val layout = StaticLayout.Builder
            .obtain(engine.message, 0, engine.message.length, msgPaint, (width - 2 * margin).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(margin, hudH * 0.4f)
        layout.draw(canvas)
        canvas.restore()
    }

    // ---- Papan ----

    private fun zoneColor(z: Zone): Int = when (z) {
        Zone.WALL -> Color.rgb(70, 50, 30)
        Zone.START -> Color.rgb(129, 199, 132)
        Zone.BELOW -> Color.rgb(141, 110, 99)
        Zone.END -> Color.rgb(255, 213, 79)
        Zone.LANE -> Color.rgb(255, 236, 200)
        Zone.FIELD -> Color.rgb(245, 222, 179)
    }

    private fun drawBoard(canvas: Canvas) {
        for (x in 0 until COLS) for (y in 0 until ROWS) {
            val l = boardLeft + x * cell
            val t = boardTop + y * cell
            fill.color = zoneColor(zoneOf(x, y))
            canvas.drawRect(l, t, l + cell, t + cell, fill)
        }
        // Area sentuh penjaga saat giliran penjaga.
        if (engine.phase == Phase.PLAY && engine.turn == Side.GUARD) {
            fill.color = Color.argb(110, 229, 57, 53)
            for (g in engine.guards) for (c in engine.threatCells(g)) {
                canvas.drawRect(cellRect(c.x, c.y), fill)
            }
        }
        // Kotak tujuan kartu gerak.
        if (engine.phase == Phase.PLAY) {
            fill.color = Color.argb(170, 255, 235, 59)
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

    private fun cellRect(x: Int, y: Int) = RectF(
        boardLeft + x * cell, boardTop + y * cell,
        boardLeft + (x + 1) * cell, boardTop + (y + 1) * cell,
    )

    private fun drawPieces(canvas: Canvas) {
        for (p in engine.attackers + engine.guards) {
            val cx = boardLeft + (p.x + 0.5f) * cell
            val cy = boardTop + (p.y + 0.5f) * cell
            val r = cell * 0.36f
            if (p.side == Side.ATTACKER) {
                fill.color = Color.rgb(33, 150, 243)
                canvas.drawCircle(cx, cy, r, fill)
            } else {
                fill.color = Color.rgb(211, 47, 47)
                canvas.drawRoundRect(RectF(cx - r, cy - r, cx + r, cy + r), cell * 0.1f, cell * 0.1f, fill)
            }
            if (engine.activeId == p.id) canvas.drawCircle(cx, cy, cell * 0.44f, ringPaint)
            text.textSize = cell * 0.3f
            drawCentered(canvas, p.stamina.toString(), cx, cy, text)
            if (p.guardType == GuardType.V) {
                text.textSize = cell * 0.22f
                val arrow = if (p.facing == Facing.LEFT) "◀" else "▶"
                drawCentered(canvas, arrow, cx, cy - r - cell * 0.18f, text)
            }
        }
    }

    // ---- Kontrol ----

    private fun handLabel(c: Card): String = when (c) {
        is Card.Move -> dirName(c.dir)
        is Card.Touch -> "Sentuh (3)"
        is Card.Evade -> "Hindar (3)"
        is Card.Star -> "Bintang ${c.id} (5)"
        is Card.Stamina -> "Stamina ±${c.value}"
        is Card.Draw -> "Ambil +${c.value}"
    }

    /** Susun tombol per baris sesuai fase permainan. */
    private fun buildRows(): List<List<Btn>> {
        val rows = mutableListOf<List<Btn>>()
        val hand = engine.decks[engine.turn]?.hand ?: emptyList<Card>()
        fun handRow(enabled: Boolean = true) = hand.map { c ->
            Btn(handLabel(c), enabled, engine.activeCard == c) { engine.selectCard(c) }
        }
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
            Phase.PLAY -> when {
                engine.owed != null -> {
                    rows += handRow()
                }
                engine.pending != null -> rows += listOf(
                    Btn("Hindar (3)", engine.decks[Side.ATTACKER]?.hand?.any { it is Card.Evade } == true, false) { engine.evade() },
                    Btn("Terima Sentuhan", true, false) { engine.acceptTouch() },
                )
                engine.postMove != null -> {
                    rows += handRow()
                    rows += listOf(Btn("Akhiri Giliran", true, false) { engine.endPostMove() })
                }
                else -> {
                    if (!engine.drew && !engine.played && hand.size < HAND_SIZE) {
                        rows += (0..(HAND_SIZE - hand.size)).map { n ->
                            Btn(if (n == 0) "Tidak Ambil" else "Ambil $n", true, false) { engine.chooseDraw(n) }
                        }
                    }
                    rows += handRow()
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

    private fun drawControls(canvas: Canvas) {
        val info = if (engine.phase == Phase.PLAY) {
            engine.pieceById(engine.activeId)?.let { "Dipilih: ${it.char.name} · stamina ${it.stamina}" }
                ?: "Ketuk karakter di papan."
        } else if (engine.phase == Phase.SETUP) {
            "Pemasangan: ketuk papan sesuai pilihan di bawah."
        } else {
            ""
        }
        val infoSize = hudH * 0.2f
        leftText.textSize = fitSize(info, width - 2 * margin, infoSize, leftText)
        canvas.drawText(info, margin, controlsTop + infoSize, leftText)

        val rows = buildRows()
        val rowsTop = controlsTop + infoSize * 1.8f
        val rowsBottom = height - margin
        if (rows.isEmpty()) {
            buttons = emptyList()
            return
        }
        val gap = margin * 0.4f
        val rowH = min(height * 0.085f, (rowsBottom - rowsTop) / rows.size)
        val all = mutableListOf<Btn>()
        rows.forEachIndexed { ri, row ->
            val top = rowsTop + ri * rowH + gap / 2
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
                !b.enabled -> Color.rgb(40, 40, 40)
                b.active -> Color.rgb(255, 193, 7)
                else -> Color.rgb(75, 75, 75)
            }
            canvas.drawRoundRect(b.rect, 16f, 16f, fill)
            text.color = if (b.active) Color.BLACK else if (b.enabled) Color.WHITE else Color.GRAY
            text.textSize = fitSize(b.label, b.rect.width() - 8f, b.rect.height() * 0.32f, text)
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
        val onBoard = x >= boardLeft && x < boardLeft + cell * COLS && y >= boardTop && y < boardTop + cell * ROWS
        if (onBoard) {
            val cx = ((x - boardLeft) / cell).toInt().coerceIn(0, COLS - 1)
            val cy = ((y - boardTop) / cell).toInt().coerceIn(0, ROWS - 1)
            engine.onCell(cx, cy)
        } else {
            buttons.firstOrNull { it.rect.contains(x, y) }?.let { if (it.enabled) it.action() }
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
