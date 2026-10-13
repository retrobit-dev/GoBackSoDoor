package com.retrobit.gobacksodoor.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import com.retrobit.gobacksodoor.game.CENTER_X
import com.retrobit.gobacksodoor.game.COLS
import com.retrobit.gobacksodoor.game.H_LANES
import com.retrobit.gobacksodoor.game.Card
import com.retrobit.gobacksodoor.game.Facing
import com.retrobit.gobacksodoor.game.GameEngine
import com.retrobit.gobacksodoor.game.GuardType
import com.retrobit.gobacksodoor.game.HAND_SIZE
import com.retrobit.gobacksodoor.game.Phase
import com.retrobit.gobacksodoor.game.ROWS
import com.retrobit.gobacksodoor.game.SetupTool
import com.retrobit.gobacksodoor.game.Side
import com.retrobit.gobacksodoor.game.TOTAL_ROUNDS
import com.retrobit.gobacksodoor.game.Team
import com.retrobit.gobacksodoor.game.Zone
import com.retrobit.gobacksodoor.game.zoneOf
import kotlin.math.abs
import kotlin.math.min

/** Tombol aksi (ambil, hindar, terima, pasang, dan lain-lain). */
private class Btn(
    val label: String,
    val enabled: Boolean,
    val active: Boolean,
    val action: () -> Unit,
) {
    var rect: RectF = RectF()
}

/**
 * Layar permainan bergaya Clash Royale:
 * banner lawan + nama babak di atas, papan besar di tengah,
 * buangan (kiri), tangan kartu (tengah), dek (kanan), dan tombol akhiri giliran di bawah.
 * Kartu dibuang dengan menyeret kartu ke buangan.
 */
class GameView(context: Context) : View(context) {

    private val engine = GameEngine()

    private var margin = 0f
    private var gap = 0f
    private var topH = 0f
    private var cell = 0f
    private var boardLeft = 0f
    private var boardTop = 0f
    private var actionTop = 0f
    private var handTop = 0f
    private var handH = 0f
    private var bottomTop = 0f
    private var cardW = 0f
    private var cardH = 0f
    private var discardRect = RectF()
    private var deckRect = RectF()
    private var endRect = RectF()
    private var buttons: List<Btn> = emptyList()
    private val handHits = mutableListOf<Pair<RectF, Card>>()

    // Status seret kartu.
    private var pressed: Card? = null
    private var dragging = false
    private var downX = 0f
    private var downY = 0f
    private var dragX = 0f
    private var dragY = 0f
    private var endEnabled = false

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
        color = Color.argb(120, 255, 255, 255)
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
    private val infoPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(120, 220, 255)
        isFakeBoldText = true
    }
    private val msgPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(225, 225, 235) }

    init {
        setBackgroundColor(Color.rgb(18, 24, 38))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        margin = w * 0.025f
        gap = margin * 0.8f

        // Banner atas, lalu papan besar.
        topH = h * 0.085f
        val boardBottom = h * 0.70f
        val boardAvail = boardBottom - topH - margin * 0.5f
        cell = min((w - 2 * margin) / COLS, boardAvail / ROWS)
        boardLeft = (w - cell * COLS) / 2f
        boardTop = topH + margin * 0.5f + (boardAvail - cell * ROWS) / 2f

        // Area aksi, tangan, lalu tombol akhiri giliran.
        actionTop = h * 0.70f
        handTop = h * 0.80f
        bottomTop = h * 0.915f
        handH = bottomTop - handTop
        cardW = min(w * 0.165f, (handH - margin) * CardArt.W / CardArt.H)
        cardH = cardW * CardArt.H / CardArt.W
        val handY = handTop + (handH - cardH) / 2f
        discardRect = RectF(margin, handY, margin + cardW, handY + cardH)
        deckRect = RectF(w - margin - cardW, handY, w - margin, handY + cardH)
        endRect = RectF(w * 0.52f, bottomTop + margin, w - margin, h - margin)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawTopBanner(canvas)
        drawBoard(canvas)
        drawPieces(canvas)
        drawActionArea(canvas)
        drawPiles(canvas)
        drawHand(canvas)
        drawBottomBar(canvas)
        drawDragged(canvas)
    }

    // ---- Banner ----

    private fun drawTopBanner(canvas: Canvas) {
        val w = width.toFloat()
        drawTeamBanner(canvas, Team.B, RectF(margin, margin * 0.5f, w * 0.58f, topH - margin * 0.25f))
        drawRoundBadge(canvas, RectF(w * 0.61f, margin * 0.5f, w - margin, topH - margin * 0.25f))
    }

    private fun drawBottomBar(canvas: Canvas) {
        drawTeamBanner(canvas, Team.A, RectF(margin, bottomTop + margin, width * 0.48f, height - margin))
        endEnabled = engine.phase == Phase.PLAY && engine.owed == null && engine.pending == null
        fill.color = if (endEnabled) Color.rgb(67, 160, 71) else Color.rgb(60, 60, 68)
        canvas.drawRoundRect(endRect, 24f, 24f, fill)
        borderPaint.strokeWidth = 4f
        borderPaint.color = if (endEnabled) Color.rgb(165, 214, 167) else Color.rgb(90, 90, 100)
        canvas.drawRoundRect(endRect, 24f, 24f, borderPaint)
        borderPaint.color = Color.WHITE
        text.color = if (endEnabled) Color.WHITE else Color.GRAY
        text.textSize = fitSize("Akhiri Giliran", endRect.width() - 16f, endRect.height() * 0.36f, text)
        drawCentered(canvas, "Akhiri Giliran", endRect.centerX(), endRect.centerY(), text)
        text.color = Color.WHITE
    }

    /** Banner tim: avatar, nama, peran, dan mahkota (poin). */
    private fun drawTeamBanner(canvas: Canvas, team: Team, r: RectF) {
        fill.color = Color.argb(160, 8, 12, 24)
        canvas.drawRoundRect(r, 18f, 18f, fill)
        borderPaint.strokeWidth = 2f
        borderPaint.color = Color.argb(140, 255, 255, 255)
        canvas.drawRoundRect(r, 18f, 18f, borderPaint)
        borderPaint.color = Color.WHITE

        val avR = r.height() * 0.38f
        val avX = r.left + r.height() * 0.1f + avR
        val avY = r.centerY()
        fill.color = if (team == Team.A) Color.rgb(33, 150, 243) else Color.rgb(229, 57, 53)
        canvas.drawCircle(avX, avY, avR, fill)
        borderPaint.strokeWidth = 3f
        canvas.drawCircle(avX, avY, avR, borderPaint)
        text.textSize = avR * 1.0f
        drawCentered(canvas, team.name, avX, avY, text)

        val textX = avX + avR + r.height() * 0.15f
        val role = if (engine.attTeam == team) "Penyerang" else "Penjaga"
        leftText.textSize = fitSize("Tim ${team.name} · $role", r.right - textX - 12f, r.height() * 0.22f, leftText)
        canvas.drawText("Tim ${team.name} · $role", textX, r.top + r.height() * 0.4f, leftText)

        val score = engine.scores[team] ?: 0
        val crownS = r.height() * 0.13f
        for (i in 0 until WIN_CROWNS) {
            val cx = textX + crownS + i * crownS * 2.3f
            val cy = r.top + r.height() * 0.72f
            fill.color = if (i < score) Color.rgb(255, 193, 7) else Color.rgb(70, 70, 82)
            canvas.drawPath(crownPath(cx, cy, crownS), fill)
        }
    }

    private fun drawRoundBadge(canvas: Canvas, r: RectF) {
        fill.color = Color.argb(200, 20, 28, 50)
        canvas.drawRoundRect(r, 18f, 18f, fill)
        borderPaint.strokeWidth = 3f
        borderPaint.color = Color.rgb(255, 193, 7)
        canvas.drawRoundRect(r, 18f, 18f, borderPaint)
        borderPaint.color = Color.WHITE

        text.color = Color.rgb(255, 213, 79)
        text.textSize = fitSize("BABAK ${engine.round}/$TOTAL_ROUNDS", r.width() - 16f, r.height() * 0.3f, text)
        drawCentered(canvas, "BABAK ${engine.round}/$TOTAL_ROUNDS", r.centerX(), r.top + r.height() * 0.36f, text)

        val sub = when (engine.phase) {
            Phase.SETUP -> "Pemasangan"
            Phase.PLAY -> engine.turnName()
            Phase.OVER -> "Babak selesai"
            Phase.WON -> "Tim ${engine.winner?.name ?: "-"} menang"
            Phase.ENDED -> "Permainan selesai"
        }
        text.color = Color.WHITE
        text.textSize = fitSize(sub, r.width() - 16f, r.height() * 0.2f, text)
        drawCentered(canvas, sub, r.centerX(), r.top + r.height() * 0.78f, text)
    }

    private fun crownPath(cx: Float, cy: Float, s: Float): Path = Path().apply {
        moveTo(cx - s, cy + s * 0.6f)
        lineTo(cx - s, cy - s * 0.4f)
        lineTo(cx - s * 0.5f, cy + s * 0.1f)
        lineTo(cx, cy - s * 0.7f)
        lineTo(cx + s * 0.5f, cy + s * 0.1f)
        lineTo(cx + s, cy - s * 0.4f)
        lineTo(cx + s, cy + s * 0.6f)
        close()
    }

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
        for (x in 0..COLS) {
            val px = boardLeft + x * cell
            canvas.drawLine(px, boardTop, px, boardTop + cell * ROWS, gridPaint)
        }
        for (y in 0..ROWS) {
            val py = boardTop + y * cell
            canvas.drawLine(boardLeft, py, boardLeft + cell * COLS, py, gridPaint)
        }
        // Kotak tujuan: jumlah kotak (langkah) ditampilkan di dalam titik.
        if (engine.phase == Phase.PLAY) {
            for ((c, res) in engine.moveTargets()) {
                val cx = boardLeft + (c.x + 0.5f) * cell
                val cy = boardTop + (c.y + 0.5f) * cell
                fill.color = Color.rgb(255, 235, 59)
                canvas.drawCircle(cx, cy, cell * 0.22f, fill)
                text.color = Color.BLACK
                text.textSize = cell * 0.26f
                drawCentered(canvas, res.steps.toString(), cx, cy, text)
                text.color = Color.WHITE
            }
        }
    }

    private fun drawPieces(canvas: Canvas) {
        for (p in engine.attackers + engine.guards) {
            val cx = boardLeft + (p.x + 0.5f) * cell
            val cy = boardTop + (p.y + 0.5f) * cell
            val r = cell * 0.36f

            fill.color = Color.argb(90, 0, 0, 0)
            canvas.drawCircle(cx + 2f, cy + 3f, r, fill)

            fill.color = if (p.side == Side.ATTACKER) Color.rgb(41, 121, 255) else Color.rgb(229, 57, 53)
            if (p.side == Side.ATTACKER) {
                canvas.drawCircle(cx, cy, r, fill)
            } else {
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

            text.textSize = cell * 0.26f
            if (p.guardType == GuardType.V) {
                val arrow = if (p.facing == Facing.LEFT) "◀" else "▶"
                val ax = if (p.facing == Facing.LEFT) cx - r - cell * 0.22f else cx + r + cell * 0.22f
                drawCentered(canvas, arrow, ax, cy, text)
            } else if (p.guardType == GuardType.H) {
                val arrow = if (p.facing == Facing.UP) "▲" else "▼"
                drawCentered(canvas, arrow, cx + r + cell * 0.22f, cy, text)
            }
        }
    }

    // ---- Aksi & info ----

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

    private fun buildButtons(): List<Btn> {
        val list = mutableListOf<Btn>()
        when (engine.phase) {
            Phase.SETUP -> list += listOf(
                Btn("Pasang Penyerang", true, engine.setupTool == SetupTool.ATTACKER) { engine.setupTool = SetupTool.ATTACKER },
                Btn("Pasang Penjaga", true, engine.setupTool == SetupTool.GUARD) { engine.setupTool = SetupTool.GUARD },
                Btn("Hapus", true, engine.setupTool == SetupTool.ERASE) { engine.setupTool = SetupTool.ERASE },
            )
            Phase.OVER -> list += Btn(
                if (engine.round >= TOTAL_ROUNDS) "Lihat Hasil" else "Babak Berikutnya", true, false,
            ) { engine.nextRound() }
            Phase.WON, Phase.ENDED -> list += Btn("Main Lagi", true, false) { engine.newGame() }
            Phase.PLAY -> {
                if (engine.owed == null) {
                    if (engine.pending != null) {
                        list += Btn("Hindar (3)", engine.decks[Side.ATTACKER]?.hand?.any { it is Card.Evade } == true, false) { engine.evade() }
                        list += Btn("Terima Sentuhan", true, false) { engine.acceptTouch() }
                    } else if (engine.postMove == null) {
                        val hand = engine.decks[engine.turn]?.hand ?: emptyList<Card>()
                        if (!engine.drew && !engine.played && hand.size < HAND_SIZE) {
                            for (n in 0..(HAND_SIZE - hand.size)) {
                                list += Btn(if (n == 0) "Tidak Ambil" else "Ambil $n", true, false) { engine.chooseDraw(n) }
                            }
                        }
                        val sel = engine.pieceById(engine.activeId)
                        if (engine.turn == Side.GUARD && sel?.guardType == GuardType.V && (sel.y in H_LANES || sel.x == CENTER_X)) {
                            list += Btn("Putar Hadap", engine.activeCard is Card.Move, false) { engine.turnFace() }
                        }
                    }
                }
            }
        }
        return list
    }

    private fun drawActionArea(canvas: Canvas) {
        val w = width.toFloat()
        val size = height * 0.011f
        infoPaint.textSize = size
        msgPaint.textSize = size
        val info = infoText()
        canvas.drawText(info, margin, actionTop + size * 1.4f, infoPaint.apply { textAlign = Paint.Align.LEFT })

        val msg = StaticLayout.Builder
            .obtain(engine.message, 0, engine.message.length, msgPaint, (w - 2 * margin).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(margin, actionTop + size * 2.4f)
        msg.draw(canvas)
        canvas.restore()

        // Tombol aksi satu baris, tepat di atas tangan.
        val btns = buildButtons()
        buttons = btns
        if (btns.isEmpty()) return
        val btnH = min(height * 0.05f, (handTop - actionTop) * 0.36f)
        val btnTop = handTop - margin * 0.5f - btnH
        val count = btns.size
        val slot = min(w * 0.3f, (w - 2 * margin - gap * (count - 1)) / count)
        val totalW = slot * count + gap * (count - 1)
        var x = (w - totalW) / 2f
        for (b in btns) {
            b.rect = RectF(x, btnTop, x + slot, btnTop + btnH)
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
            text.textSize = fitSize(b.label, b.rect.width() - 8f, b.rect.height() * 0.36f, text)
            drawCentered(canvas, b.label, b.rect.centerX(), b.rect.centerY(), text)
            x += slot + gap
        }
        text.color = Color.WHITE
    }

    // ---- Dek & buangan ----

    private fun drawPiles(canvas: Canvas) {
        val d = engine.decks[engine.turn] ?: return

        // Dek di kanan: tumpukan punggung kartu.
        if (d.deck.isNotEmpty()) {
            for (k in 3 downTo 1) {
                val r = RectF(deckRect)
                r.offset(k * 3f, -k * 3f)
                CardArt.drawBack(canvas, r)
            }
            CardArt.drawBack(canvas, deckRect)
        } else {
            canvas.drawRoundRect(deckRect, 12f, 12f, dashPaint)
        }
        countOverlay(canvas, deckRect, d.deck.size)

        // Buangan di kiri: kartu terakhir yang dibuang, menghadap ke atas.
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
        countOverlay(canvas, discardRect, d.discard.size)

        // Sorotan saat kartu diseret di atas buangan.
        if (dragging) {
            val zone = discardZone()
            if (zone.contains(dragX, dragY)) {
                borderPaint.strokeWidth = 6f
                borderPaint.color = Color.rgb(255, 235, 59)
                canvas.drawRoundRect(RectF(discardRect).apply { inset(-6f, -6f) }, 16f, 16f, borderPaint)
                borderPaint.color = Color.WHITE
            }
        }
    }

    /** Overlay jumlah kartu di atas tumpukan. */
    private fun countOverlay(canvas: Canvas, r: RectF, n: Int) {
        val pw = r.width() * 0.55f
        val ph = r.height() * 0.17f
        val pill = RectF(r.centerX() - pw / 2f, r.centerY() - ph / 2f, r.centerX() + pw / 2f, r.centerY() + ph / 2f)
        fill.color = Color.argb(220, 10, 12, 20)
        canvas.drawRoundRect(pill, ph / 2f, ph / 2f, fill)
        borderPaint.strokeWidth = ph * 0.08f
        borderPaint.color = Color.rgb(255, 213, 79)
        canvas.drawRoundRect(pill, ph / 2f, ph / 2f, borderPaint)
        borderPaint.color = Color.WHITE
        text.textSize = ph * 0.62f
        drawCentered(canvas, n.toString(), pill.centerX(), pill.centerY(), text)
    }

    private fun discardZone(): RectF = RectF(discardRect).apply { inset(-margin, -margin) }

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
        val n = hand.size
        if (n == 0) return

        val regionL = discardRect.right + gap * 2f
        val regionR = deckRect.left - gap * 2f
        val regionW = regionR - regionL
        val spacing = if (n == 1) 0f else min(cardW + gap, (regionW - cardW) / (n - 1))
        val total = cardW + spacing * (n - 1)
        val startX = regionL + (regionW - total) / 2f
        val baseY = handTop + (handH - cardH) / 2f

        // Kartu terpilih digambar terakhir agar berada di atas.
        val order = hand.withIndex().sortedBy { if (engine.activeCard == it.value) 1 else 0 }
        for ((i, c) in order) {
            if (dragging && pressed == c) continue
            val selected = engine.activeCard == c
            val lift = if (selected) cardH * 0.12f else 0f
            val x = startX + i * spacing
            val rect = RectF(x, baseY - lift, x + cardW, baseY - lift + cardH)
            CardArt.draw(canvas, c, rect, enabled = usable(c), selected = selected)
            handHits += rect to c
        }
    }

    private fun drawDragged(canvas: Canvas) {
        val card = pressed ?: return
        if (!dragging) return
        val s = cardW * 1.08f
        val rect = RectF(dragX - s / 2f, dragY - s * CardArt.H / CardArt.W / 2f, dragX + s / 2f, dragY + s * CardArt.H / CardArt.W / 2f)
        CardArt.draw(canvas, card, rect, enabled = true, selected = true)
    }

    // ---- Input ----

    private fun slop(): Float = resources.displayMetrics.density * 10f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                downY = y
                dragging = false
                pressed = handHits.lastOrNull { it.first.contains(x, y) }?.second
            }
            MotionEvent.ACTION_MOVE -> {
                if (pressed != null) {
                    if (!dragging && (abs(x - downX) > slop() || abs(y - downY) > slop())) dragging = true
                    if (dragging) {
                        dragX = x
                        dragY = y
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                val card = pressed
                if (card != null && dragging) {
                    if (discardZone().contains(x, y)) engine.discardCard(card)
                } else if (card != null) {
                    engine.selectCard(card)
                } else {
                    handleTap(x, y)
                }
                pressed = null
                dragging = false
                performClick()
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                pressed = null
                dragging = false
                invalidate()
            }
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
        } else if (endRect.contains(x, y) && endEnabled) {
            if (engine.postMove != null) engine.endPostMove() else engine.skipTurn()
        } else {
            buttons.firstOrNull { it.rect.contains(x, y) }?.let { if (it.enabled) it.action() }
        }
        invalidate()
    }

    override fun performClick(): Boolean = super.performClick()

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

    companion object {
        private const val WIN_CROWNS = 5
    }
}
