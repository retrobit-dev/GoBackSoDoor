package com.retrobit.gobacksodoor.game

import kotlin.math.abs
import kotlin.random.Random

/** Posisi sebuah kotak pada lapangan (x = kolom, y = baris). */
data class Cell(val x: Int, val y: Int) {
    fun move(direction: Direction): Cell = Cell(x + direction.dx, y + direction.dy)

    fun distanceTo(other: Cell): Int = abs(x - other.x) + abs(y - other.y)

    fun neighbors(): List<Cell> = Direction.values().map { move(it) }
}

enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0),
}

enum class RoundPhase {
    /** Ronde sedang berjalan. */
    RUNNING,
    /** Penyerang tersentuh penjaga. */
    CAUGHT,
    /** Penyerang sukses kembali ke garis awal setelah menyentuh garis akhir. */
    COMPLETED,
}

/**
 * Logika inti Gobak Sodor (tanpa dependensi Android sehingga mudah diuji).
 *
 * Aturan sederhana versi ini:
 * - Pemain adalah penyerang. Ia harus mencapai kolom paling kanan (garis sodor),
 *   lalu kembali ke kolom paling kiri (garis awal).
 * - Setiap langkah penyerang diikuti satu langkah serentak oleh para penjaga.
 * - Penyerang gugur jika ada penjaga yang berada tepat di sebelahnya (4 arah).
 * - Penyerang tidak boleh masuk ke kotak yang ditempati penjaga.
 * - Pemain punya 3 nyawa. Jumlah penjaga bertambah seiring skor.
 */
class GameEngine(
    val columns: Int = 9,
    val rows: Int = 5,
    private val random: Random = Random.Default,
) {
    var attacker: Cell = Cell(0, rows / 2)
        private set

    var defenders: List<Cell> = emptyList()
        private set

    var phase: RoundPhase = RoundPhase.RUNNING
        private set

    var score: Int = 0
        private set

    var lives: Int = MAX_LIVES
        private set

    private var reachedGoal = false

    init {
        startRound()
    }

    val isGameOver: Boolean
        get() = phase == RoundPhase.CAUGHT && lives <= 0

    fun defenderCountForScore(): Int = (2 + score / 3).coerceAtMost(MAX_DEFENDERS)

    /** Menggerakkan penyerang satu langkah. Mengembalikan false jika gerakan tidak valid. */
    fun moveAttacker(direction: Direction): Boolean {
        if (phase != RoundPhase.RUNNING) return false

        val target = attacker.move(direction)
        if (!target.isInside() || target in defenders) return false

        attacker = target
        if (attacker.x == columns - 1) reachedGoal = true

        if (reachedGoal && attacker.x == 0) {
            phase = RoundPhase.COMPLETED
            score++
            return true
        }

        moveDefenders()
        if (isTouched()) {
            phase = RoundPhase.CAUGHT
            lives--
        }
        return true
    }

    /** Dipanggil setelah ronde berakhir (ketuk layar). Memulai ronde baru atau game baru. */
    fun continueAfterRound() {
        if (phase == RoundPhase.RUNNING) return
        if (lives <= 0) reset() else startRound()
    }

    fun reset() {
        score = 0
        lives = MAX_LIVES
        startRound()
    }

    private fun startRound() {
        attacker = Cell(0, rows / 2)
        reachedGoal = false
        phase = RoundPhase.RUNNING
        defenders = spawnDefenders(defenderCountForScore())
    }

    private fun spawnDefenders(count: Int): List<Cell> {
        // Kolom 0 (garis awal) dan kolom terakhir (garis sodor) dibiarkan kosong.
        val candidates = (2 until columns - 1).flatMap { x -> (0 until rows).map { y -> Cell(x, y) } }
        return candidates.shuffled(random).take(count)
    }

    private fun moveDefenders() {
        val occupied = defenders.toMutableSet()
        val moved = mutableListOf<Cell>()

        for (defender in defenders) {
            occupied.remove(defender)
            val current = defender.distanceTo(attacker)

            val next = if (current == 1) {
                defender // sudah menempel, tetap di tempat
            } else {
                defender.neighbors()
                    .filter { it.isInside() && it != attacker && it !in occupied }
                    .filter { it.distanceTo(attacker) <= current }
                    .shuffled(random)
                    .minByOrNull { it.distanceTo(attacker) }
                    ?: defender
            }

            occupied.add(next)
            moved.add(next)
        }
        defenders = moved
    }

    private fun isTouched(): Boolean = defenders.any { it.distanceTo(attacker) == 1 }

    private fun Cell.isInside(): Boolean = x in 0 until columns && y in 0 until rows

    /** Hanya untuk pengujian: menempatkan pemain dan penjaga secara manual. */
    internal fun placeForTest(attackerAt: Cell, defendersAt: List<Cell>) {
        attacker = attackerAt
        defenders = defendersAt
        reachedGoal = false
        phase = RoundPhase.RUNNING
    }

    companion object {
        const val MAX_LIVES = 3
        const val MAX_DEFENDERS = 5
    }
}
