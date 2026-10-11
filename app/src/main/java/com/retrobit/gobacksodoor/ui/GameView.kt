package com.retrobit.gobacksodoor.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import com.retrobit.gobacksodoor.game.Cell
import com.retrobit.gobacksodoor.game.Direction
import com.retrobit.gobacksodoor.game.GameEngine
import com.retrobit.gobacksodoor.game.RoundPhase
import kotlin.math.min

/** Menggambar lapangan Gobak Sodor dan menangani sentuhan pemain. */
class GameView(context: Context) : View(context) {

    private val engine = GameEngine()

    private var cellSize = 0f
    private var boardLeft = 0f
    private var boardTop = 0f
    private var margin = 0f
    private var hudHeight = 0f
    private val buttonRects = mutableMapOf<Direction, RectF>()

    private val backgroundColor = Color.rgb(33, 33, 33)
    private val fieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(245, 222, 179)
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(110, 75, 40)
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    private val zonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(110, 46, 125, 50)
        style = Paint.Style.FILL
    }
    private val attackerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(33, 150, 243)
        style = Paint.Style.FILL
    }
    private val defenderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(211, 47, 47)
        style = Paint.Style.FILL
    }
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 90, 90, 90)
        style = Paint.Style.FILL
    }
    private val overlayPaint = Paint().apply {
        color = Color.argb(170, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
        isFakeBoldText = true
    }
    private val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    init {
        setBackgroundColor(backgroundColor)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        margin = w * 0.05f
        hudHeight = h * 0.12f

        // Tombol panah (D-pad) di bagian bawah layar.
        val btn = min(w / 6f, h * 0.1f)
        val cx = w / 2f
        val cy = h - 1.45f * btn - margin
        buttonRects.clear()
        buttonRects[Direction.UP] = squareAt(cx, cy - btn, btn * 0.9f)
        buttonRects[Direction.DOWN] = squareAt(cx, cy + btn, btn * 0.9f)
        buttonRects[Direction.LEFT] = squareAt(cx - btn, cy, btn * 0.9f)
        buttonRects[Direction.RIGHT] = squareAt(cx + btn, cy, btn * 0.9f)

        // Area papan di antara HUD dan D-pad.
        val controlTop = cy - 1.45f * btn
        val available = controlTop - hudHeight - margin
        cellSize = min(
            (w - 2 * margin) / engine.columns,
            available / engine.rows,
        )
        boardLeft = (w - cellSize * engine.columns) / 2f
        boardTop = hudHeight + (available - cellSize * engine.rows) / 2f

        textPaint.textSize = hudHeight * 0.4f
        centerTextPaint.textSize = min(w, h) * 0.05f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawHud(canvas)
        drawField(canvas)
        drawPieces(canvas)
        if (engine.phase == RoundPhase.RUNNING) drawControls(canvas) else drawRoundMessage(canvas)
    }

    private fun drawHud(canvas: Canvas) {
        val text = "Skor: ${engine.score}    Nyawa: ${engine.lives}"
        canvas.drawText(text, margin, hudHeight * 0.65f, textPaint)
    }

    private fun drawField(canvas: Canvas) {
        val right = boardLeft + cellSize * engine.columns
        val bottom = boardTop + cellSize * engine.rows
        canvas.drawRect(boardLeft, boardTop, right, bottom, fieldPaint)

        // Zona garis awal (kiri) dan garis sodor (kanan).
        canvas.drawRect(boardLeft, boardTop, boardLeft + cellSize, bottom, zonePaint)
        canvas.drawRect(right - cellSize, boardTop, right, bottom, zonePaint)

        for (i in 0..engine.columns) {
            val x = boardLeft + i * cellSize
            canvas.drawLine(x, boardTop, x, bottom, linePaint)
        }
        for (j in 0..engine.rows) {
            val y = boardTop + j * cellSize
            canvas.drawLine(boardLeft, y, right, y, linePaint)
        }
    }

    private fun drawPieces(canvas: Canvas) {
        val radius = cellSize * 0.35f
        for (defender in engine.defenders) {
            val (cx, cy) = centerOf(defender)
            canvas.drawCircle(cx, cy, radius, defenderPaint)
        }
        val (ax, ay) = centerOf(engine.attacker)
        canvas.drawCircle(ax, ay, radius, attackerPaint)
    }

    private fun drawControls(canvas: Canvas) {
        val arrows = mapOf(
            Direction.UP to "▲",
            Direction.DOWN to "▼",
            Direction.LEFT to "◀",
            Direction.RIGHT to "▶",
        )
        for ((direction, rect) in buttonRects) {
            canvas.drawRoundRect(rect, 24f, 24f, buttonPaint)
            drawCenteredText(canvas, arrows.getValue(direction), rect.centerX(), rect.centerY(), centerTextPaint)
        }
    }

    private fun drawRoundMessage(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        val message = when {
            engine.isGameOver -> "Game Over\nKetuk layar untuk main lagi"
            engine.phase == RoundPhase.CAUGHT -> "Tersentuh!\nKetuk layar untuk lanjut"
            else -> "Berhasil!\nKetuk layar untuk lanjut"
        }
        val cx = width / 2f
        val cy = height / 2f
        val lineHeight = centerTextPaint.textSize * 1.4f
        message.split("\n").forEachIndexed { index, line ->
            val y = cy + (index - 0.5f) * lineHeight
            drawCenteredText(canvas, line, cx, y, centerTextPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            handleTap(event.x, event.y)
            performClick()
        }
        return true
    }

    private fun handleTap(x: Float, y: Float) {
        if (engine.phase != RoundPhase.RUNNING) {
            engine.continueAfterRound()
        } else {
            val direction = buttonRects.entries.firstOrNull { it.value.contains(x, y) }?.key
            if (direction != null) engine.moveAttacker(direction)
        }
        invalidate()
    }

    private fun centerOf(cell: Cell): Pair<Float, Float> {
        val cx = boardLeft + (cell.x + 0.5f) * cellSize
        val cy = boardTop + (cell.y + 0.5f) * cellSize
        return cx to cy
    }

    private fun squareAt(cx: Float, cy: Float, size: Float): RectF {
        val half = size / 2f
        return RectF(cx - half, cy - half, cx + half, cy + half)
    }

    private fun drawCenteredText(canvas: Canvas, text: String, cx: Float, cy: Float, paint: Paint) {
        val offset = (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, cx, cy - offset, paint)
    }
}
