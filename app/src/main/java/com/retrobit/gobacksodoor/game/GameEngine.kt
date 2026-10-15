package com.retrobit.gobacksodoor.game

import kotlin.math.abs
import kotlin.random.Random

// Konstanta papan & aturan (sama dengan preview/index.html).
const val COLS = 13
const val ROWS = 22
const val CENTER_X = 6
const val END_Y = 0          // garis finish (atas)
const val START_Y = 20       // garis start (hijau)
const val BELOW_Y = 21       // baris coklat, tempat penyerang dipasang
const val WIN_SCORE = 5
const val TOTAL_ROUNDS = 6   // tiap tim 3 babak sebagai penyerang
const val HAND_SIZE = 3      // tangan awal dan batas tangan di awal giliran
const val TOUCH_COST = 3
const val STAR_COST = 5
const val MAX_STEPS = 5
val H_LANES = listOf(1, 7, 13, 19)

enum class Team { A, B; fun other(): Team = if (this == A) B else A }
enum class Side { ATTACKER, GUARD }
enum class Zone { WALL, START, BELOW, END, LANE, FIELD }
enum class Dir { VERTICAL, HORIZONTAL, CROSS }
enum class GuardType { H, V }
enum class Facing { LEFT, RIGHT, UP, DOWN }
enum class Phase { SETUP, PLAY, OVER, WON, ENDED }
enum class SetupTool { ATTACKER, GUARD, ERASE }

data class Cell(val x: Int, val y: Int)

/** Jenis rintangan yang muncul di area permainan tiap giliran. */
enum class ObstacleKind { HOLE, ROCK, BUSH }

/** Rintangan di papan. [left] = sisa giliran sebelum hilang. */
class Obstacle(val x: Int, val y: Int, val kind: ObstacleKind, var left: Int)

const val OBSTACLE_TURNS = 4

fun obstacleName(k: ObstacleKind): String = when (k) {
    ObstacleKind.HOLE -> "lubang"
    ObstacleKind.ROCK -> "batu"
    ObstacleKind.BUSH -> "semak"
}

/** Karakter: satu efek, satu tipe utama (penyerang atau penjaga). */
data class Character(
    val name: String,
    val tribe: String,
    val rarity: String,
    val stamina: Int,
    val effect: String,
)

val ATTACKER_ROSTER = listOf(
    Character("Slamet", "Jawa", "Umum", 4, "Langkah ganda: angka kartu gerak dikali 2."),
    Character("Sutan", "Minangkabau", "Langka", 3, "Melompati satu penjaga di garis lurus tanpa tersentuh."),
    Character("Ekot", "Dayak Ngaju", "Umum", 5, "Sentuhan pertama dibatalkan tanpa kartu menghindar."),
    Character("Wayan", "Bali", "Umum", 4, "Tukar posisi dengan penyerang rekan di sebelahnya."),
    Character("Andi", "Bugis", "Epik", 3, "Gerak kanan-kiri atau silang melanjutkan ke sisi lain saat kena dinding."),
)

val GUARD_ROSTER = listOf(
    Character("Togar", "Batak", "Umum", 5, "Area merah bertambah satu kotak ke depan."),
    Character("Dalle", "Toraja", "Umum", 3, "Satu penyerang lawan tidak bisa dipilih pada giliran berikutnya."),
    Character("Pitung", "Betawi", "Langka", 4, "Penyerang yang menghindar tetap tersentuh."),
    Character("Lalu", "Sasak", "Umum", 4, "Penjaga yang dipilih boleh bergerak satu kotak tambahan."),
    Character("Wanggai", "Papua", "Epik", 3, "Dapat menyentuh penyerang yang berjarak dua kotak lurus."),
)

sealed class Card {
    abstract val uid: Int
    /** Nama karakter pemilik kartu; null untuk kartu netral (stamina dan ambil). */
    abstract val owner: String?

    /** Kartu arah tanpa angka; jumlah kotak = stamina yang dibayar (1 per kotak). */
    data class Move(override val uid: Int, val dir: Dir, override val owner: String) : Card()
    /** Kartu sentuh (penjaga), biaya 3. */
    data class Touch(override val uid: Int, override val owner: String) : Card()
    /** Kartu menghindar (penyerang), biaya 3. */
    data class Evade(override val uid: Int, override val owner: String) : Card()
    /** Kartu bintang, biaya 5. Efek dari karakter (belum aktif). */
    data class Star(override val uid: Int, val id: Int, override val owner: String) : Card()
    /** Kartu stamina ±N, biaya 0. Pilih karakter tujuan di papan. */
    data class Stamina(override val uid: Int, val value: Int) : Card() {
        override val owner: String? get() = null
    }
    /** Kartu ambil +N, biaya 0. Mengambil N kartu dari teratas deck. */
    data class Draw(override val uid: Int, val value: Int) : Card() {
        override val owner: String? get() = null
    }
}

/** Event visual untuk tampilan: jenis ("touch", "hit", "hole", "bush"), sel, dan waktu (ms). */
data class Fx(val kind: String, val x: Int, val y: Int, val at: Long)

/** Pemain di papan: penyerang atau penjaga, lengkap dengan karakter dan stamina. */
class Piece(
    val id: String,
    val side: Side,
    val char: Character,
    var x: Int,
    var y: Int,
    var stamina: Int,
    val guardType: GuardType? = null,
) {
    var facing: Facing = if (guardType == GuardType.V) Facing.RIGHT else Facing.DOWN
    var visited: MutableSet<Int>? = null
    /** Terkunci: tidak bisa dimainkan pada giliran pemiliknya (karena jatuh ke lubang). */
    var locked: Boolean = false
    /** Akan terkunci pada giliran pemilik berikutnya. */
    var lockPending: Boolean = false
    var touchedH: Boolean = false
    /** Penyerang: baris garis batas terdekat yang sudah dilewati; tidak boleh kembali melewatinya. */
    var barrier: Int = ROWS
}

class Deck {
    val deck: MutableList<Card> = mutableListOf()
    val hand: MutableList<Card> = mutableListOf()
    val discard: MutableList<Card> = mutableListOf()
}

/** Hasil simulasi gerak. */
data class MoveResult(val path: List<Int>, val end: Cell, val scored: Boolean = false, val flag: Boolean = false, val steps: Int = 0, val barrier: Int = ROWS)

/** Kewajiban membuang kartu di akhir giliran sampai tangan berisi HAND_SIZE. */
class Owed(val side: Side, val text: String, var need: Int)

private fun key(x: Int, y: Int): Int = y * COLS + x
private fun isWallX(x: Int): Boolean = x == 0 || x == COLS - 1
private fun inBounds(x: Int, y: Int): Boolean = x in 0 until COLS && y in 0 until ROWS

fun zoneOf(x: Int, y: Int): Zone = when {
    isWallX(x) -> Zone.WALL
    y == START_Y -> Zone.START
    y == BELOW_Y -> Zone.BELOW
    y == END_Y -> Zone.END
    y in H_LANES || x == CENTER_X -> Zone.LANE
    else -> Zone.FIELD
}

fun dirName(dir: Dir): String = when (dir) {
    Dir.VERTICAL -> "Atas-Bawah"
    Dir.HORIZONTAL -> "Kanan-Kiri"
    Dir.CROSS -> "Silang"
}

/**
 * Logika inti Gobak Sodor versi kartu & stamina (tanpa dependensi Android).
 * Aturan mengikuti preview/index.html.
 */
class GameEngine(private val random: Random = Random.Default, private val obstaclesOn: Boolean = true) {

    var phase: Phase = Phase.SETUP
        private set
    var setupTool: SetupTool = SetupTool.ATTACKER
    var round: Int = 1
        private set
    var attTeam: Team = Team.A
        private set
    val scores: MutableMap<Team, Int> = mutableMapOf(Team.A to 0, Team.B to 0)
    var winner: Team? = null
        private set
    var turn: Side = Side.ATTACKER
        private set
    var activeId: String? = null
        private set
    var activeCard: Card? = null
        private set
    var pending: Pair<Piece, Piece>? = null
        private set
    var postMove: String? = null
        private set
    var drew: Boolean = false
        private set
    var played: Boolean = false
        private set
    var owed: Owed? = null
        private set
    var message: String = ""
        private set

    /** Event efek terbaru untuk tampilan. Tidak dipakai oleh aturan permainan. */
    val effects = mutableListOf<Fx>()

    private fun fx(kind: String, x: Int, y: Int) {
        effects.add(Fx(kind, x, y, System.currentTimeMillis()))
        if (effects.size > 12) effects.removeAt(0)
    }

    val attackers: MutableList<Piece> = mutableListOf()
    val guards: MutableList<Piece> = mutableListOf()
    val decks: MutableMap<Side, Deck> = mutableMapOf()
    val obstacles: MutableList<Obstacle> = mutableListOf()
    /** Teks rintangan yang muncul pada giliran terakhir. */
    var lastSpawn: String = ""
        private set

    private var uidCounter = 0

    init {
        newGame()
    }

    // ---- Utilitas ----

    private fun nextUid(): Int { uidCounter++; return uidCounter }

    fun pieceAt(x: Int, y: Int): Piece? = (attackers + guards).firstOrNull { it.x == x && it.y == y }

    fun pieceById(id: String?): Piece? = (attackers + guards).firstOrNull { it.id == id }

    fun obstacleAt(x: Int, y: Int): Obstacle? = obstacles.firstOrNull { it.x == x && it.y == y }

    /** Muncul satu rintangan acak di kotak lapangan kosong. */
    private fun spawnObstacle() {
        if (!obstaclesOn) return
        val free = mutableListOf<Cell>()
        for (x in 0 until COLS) for (y in 0 until ROWS) {
            if (zoneOf(x, y) == Zone.FIELD && pieceAt(x, y) == null && obstacleAt(x, y) == null) free.add(Cell(x, y))
        }
        if (free.isEmpty()) return
        val c = free[random.nextInt(free.size)]
        val kind = ObstacleKind.values()[random.nextInt(ObstacleKind.values().size)]
        obstacles.add(Obstacle(c.x, c.y, kind, OBSTACLE_TURNS))
        lastSpawn = "Muncul ${obstacleName(kind)} di kotak (${c.x + 1},${ROWS - c.y})."
    }

    private fun occupiedBy(x: Int, y: Int, ignoreId: String): Boolean {
        val p = pieceAt(x, y) ?: return false
        return p.id != ignoreId
    }

    fun turnName(): String =
        (if (turn == Side.ATTACKER) "Penyerang" else "Penjaga") + " (" + "Tim " + turnTeam().name + ")"

    private fun turnTeam(): Team = if (turn == Side.ATTACKER) attTeam else attTeam.other()

    /** Area sentuh penjaga: kotak di depan, dua sisinya, dan dua sisi penjaga (kiri-kanan sesuai hadap). */
    fun threatCells(g: Piece): List<Cell> {
        val (dx, dy) = when (g.guardType) {
            GuardType.H -> if (g.facing == Facing.UP) 0 to -1 else 0 to 1
            else -> if (g.facing == Facing.LEFT) -1 to 0 else 1 to 0
        }
        val fx = g.x + dx
        val fy = g.y + dy
        val frontSides = if (dx == 0) listOf(Cell(fx - 1, fy), Cell(fx + 1, fy)) else listOf(Cell(fx, fy - 1), Cell(fx, fy + 1))
        val selfSides = if (dx == 0) listOf(Cell(g.x - 1, g.y), Cell(g.x + 1, g.y)) else listOf(Cell(g.x, g.y - 1), Cell(g.x, g.y + 1))
        return (listOf(Cell(fx, fy)) + frontSides + selfSides).filter { inBounds(it.x, it.y) }
    }

    // ---- Kartu & dek ----

    private fun buildDeck(side: Side): Deck {
        val d = Deck()
        val roster = if (side == Side.ATTACKER) ATTACKER_ROSTER else GUARD_ROSTER
        // 25 kartu karakter (15 arah, 5 sentuh/hindar, 5 bintang). Tiap karakter mendapat 5 kartu.
        val makers = mutableListOf<(Int, String) -> Card>()
        for (dir in Dir.values()) repeat(5) { makers.add { uid, owner -> Card.Move(uid, dir, owner) } }
        repeat(5) { makers.add { uid, owner -> if (side == Side.ATTACKER) Card.Evade(uid, owner) else Card.Touch(uid, owner) } }
        for (i in 1..5) makers.add { uid, owner -> Card.Star(uid, i, owner) }
        makers.shuffle(random)
        makers.forEachIndexed { i, mk -> d.deck.add(mk(nextUid(), roster[i / 5].name)) }
        for (v in 1..5) d.deck.add(Card.Stamina(nextUid(), v))
        for (n in listOf(1, 1, 2, 2, 3)) d.deck.add(Card.Draw(nextUid(), n))
        d.deck.shuffle(random)
        return d
    }

    /** Ambil 1 kartu dari teratas deck (kocok buangan jika dek habis). */
    private fun drawOne(side: Side): Card? {
        val d = decks[side] ?: return null
        if (d.deck.isEmpty()) {
            if (d.discard.isEmpty()) return null
            d.deck.addAll(d.discard.shuffled(random))
            d.discard.clear()
        }
        val c = d.deck.removeAt(d.deck.lastIndex)
        d.hand.add(c)
        return c
    }

    private fun drawToFull(side: Side) {
        while ((decks[side]?.hand?.size ?: HAND_SIZE) < HAND_SIZE) {
            if (drawOne(side) == null) break
        }
    }

    /** Kartu gerak/sentuh/hindar/stamina/ambil masuk buangan. Kartu bintang kembali ke dek. */
    private fun consume(side: Side, card: Card) {
        val d = decks[side] ?: return
        if (!d.hand.remove(card)) return
        if (card is Card.Star) {
            d.deck.add(card)
            d.deck.shuffle(random)
        } else {
            d.discard.add(card)
        }
        if (side == turn) played = true
    }

    // ---- Alur permainan ----

    fun newGame() {
        effects.clear()
        round = 1
        attTeam = Team.A
        scores[Team.A] = 0
        scores[Team.B] = 0
        winner = null
        startRound()
    }

    private fun startRound() {
        phase = Phase.SETUP
        setupTool = SetupTool.ATTACKER
        turn = Side.ATTACKER
        activeId = null
        activeCard = null
        pending = null
        postMove = null
        drew = false
        played = false
        owed = null
        attackers.clear()
        guards.clear()
        obstacles.clear()
        lastSpawn = ""
        decks[Side.ATTACKER] = buildDeck(Side.ATTACKER)
        decks[Side.GUARD] = buildDeck(Side.GUARD)
        drawToFull(Side.ATTACKER)
        drawToFull(Side.GUARD)
        message = "Babak $round dari $TOTAL_ROUNDS: Tim ${attTeam.name} jadi penyerang. ${setupText()}"
    }

    fun nextRound() {
        if (phase == Phase.WON || phase == Phase.ENDED) return newGame()
        if (phase != Phase.OVER) return
        if (round >= TOTAL_ROUNDS) return endGame()
        round++
        attTeam = attTeam.other()
        startRound()
    }

    private fun endGame() {
        phase = Phase.ENDED
        activeId = null
        activeCard = null
        val a = scores[Team.A] ?: 0
        val b = scores[Team.B] ?: 0
        if (a == b) {
            winner = null
            message = "Semua babak selesai. Skor seri $a–$b."
        } else {
            val win = if (a > b) Team.A else Team.B
            winner = win
            message = "Semua babak selesai. Tim ${win.name} menang dengan ${maxOf(a, b)} poin."
        }
    }

    private fun gameWon() {
        fx("win", CENTER_X, ROWS / 2)
        phase = Phase.WON
        winner = attTeam
        activeId = null
        activeCard = null
        pending = null
        postMove = null
        message = "Tim ${attTeam.name} mencapai ${scores[attTeam]} poin dan menang!"
    }

    private fun roundOver(text: String) {
        fx("round", CENTER_X, ROWS / 2)
        phase = Phase.OVER
        pending = null
        postMove = null
        activeId = null
        activeCard = null
        message = text
    }

    /** Pergantian giliran: karakter tim yang mulai bergerak mendapat +1 stamina. */
    private fun endTurn(text: String) {
        val excess = (decks[turn]?.hand?.size ?: 0) - HAND_SIZE
        if (owed == null && excess > 0) {
            owed = Owed(turn, text, excess)
            activeCard = null
            activeId = null
            postMove = null
            message = "Akhir giliran: buang $excess kartu sampai tangan $HAND_SIZE. Pilih kartu lalu buang."
            return
        }
        owed = null
        turn = if (turn == Side.ATTACKER) Side.GUARD else Side.ATTACKER
        activeId = null
        activeCard = null
        postMove = null
        drew = false
        played = false
        guards.forEach { it.visited = null; it.touchedH = false }
        val pieces = if (turn == Side.ATTACKER) attackers else guards
        pieces.forEach { it.stamina += 1 }
        // Rintangan: umur berkurang, lalu muncul satu rintangan baru.
        for (o in obstacles) o.left--
        obstacles.removeAll { it.left <= 0 }
        lastSpawn = ""
        spawnObstacle()
        // Piece yang jatuh ke lubang terkunci pada giliran pemiliknya berikutnya.
        pieces.forEach { it.locked = it.lockPending; it.lockPending = false }
        val spawn = if (lastSpawn.isNotEmpty()) " $lastSpawn" else ""
        message = (if (text.isNotEmpty()) "$text " else "") + "Giliran ${turnName()}. Stamina +1 untuk karakter tim ini.$spawn"
    }

    /** Setelah kartu gerak: jika bisa, tahan giliran agar bisa memakai bintang. */
    private fun afterMove(piece: Piece, text: String) {
        val hand = decks[turn]?.hand ?: emptyList<Card>()
        if (piece.stamina >= STAR_COST && hand.any { it is Card.Star && it.owner == piece.char.name }) {
            postMove = piece.id
            activeCard = null
            message = "$text Pakai bintang (5 stamina) atau akhiri giliran."
            return
        }
        endTurn(text)
    }

    // ---- Pemasangan ----

    fun setupText(): String {
        val a = attackers.size
        val g = guards.size
        val hint = when {
            a < 5 -> "Penyerang pasang dulu: klik baris coklat (${5 - a} lagi)."
            guards.count { it.guardType == GuardType.H } < 4 -> "Penjaga pasang: klik garis batas baris (${4 - guards.count { it.guardType == GuardType.H }} lagi)."
            guards.count { it.guardType == GuardType.V } < 1 -> "Penjaga pasang: klik garis batas tengah (kolom)."
            else -> "Pemasangan selesai."
        }
        return "Penyerang $a/5 · Penjaga $g/5 · $hint"
    }

    private fun placeAttacker(x: Int, y: Int): String {
        if (y != BELOW_Y || isWallX(x)) return "Penyerang hanya boleh di baris coklat (bukan dinding)."
        if (attackers.size >= 5) return "Penyerang sudah 5."
        if (pieceAt(x, y) != null) return "Kotak sudah terisi."
        val char = ATTACKER_ROSTER[attackers.size]
        attackers.add(Piece("A${attackers.size + 1}", Side.ATTACKER, char, x, y, char.stamina))
        return ""
    }

    private fun placeGuard(x: Int, y: Int): String {
        if (attackers.size < 5) return "Penyerang pasang dulu (5 penyerang)."
        if (isWallX(x)) return "Dinding tidak bisa diisi."
        if (pieceAt(x, y) != null) return "Kotak sudah terisi."
        val char = GUARD_ROSTER[guards.size]
        if (y in H_LANES) {
            if (guards.any { it.guardType == GuardType.H && it.y == y }) return "Batas baris ini sudah ada penjaga."
            guards.add(Piece("H${H_LANES.indexOf(y) + 1}", Side.GUARD, char, x, y, char.stamina, GuardType.H))
            return ""
        }
        if (x == CENTER_X) {
            if (guards.any { it.guardType == GuardType.V }) return "Penjaga kolom sudah ada."
            guards.add(Piece("V1", Side.GUARD, char, x, y, char.stamina, GuardType.V))
            return ""
        }
        return "Penjaga hanya boleh di garis batas (baris atau kolom tengah)."
    }

    private fun checkSetupDone() {
        if (attackers.size == 5 && guards.size == 5) {
            phase = Phase.PLAY
            turn = Side.ATTACKER
            message = "Pemasangan selesai. Giliran ${turnName()}: ambil kartu, lalu mainkan kartu karakter."
        }
    }

    // ---- Input ----

    /** Ketukan pada kotak papan. */
    fun onCell(x: Int, y: Int) {
        when (phase) {
            Phase.SETUP -> onSetupCell(x, y)
            Phase.PLAY -> onPlayCell(x, y)
            else -> message = "Babak selesai. Tekan tombol berikutnya."
        }
    }

    private fun onSetupCell(x: Int, y: Int) {
        if (setupTool == SetupTool.ERASE) {
            guards.removeAll { it.x == x && it.y == y }
            attackers.removeAll { it.x == x && it.y == y }
            message = setupText()
        } else {
            val err = if (setupTool == SetupTool.GUARD) placeGuard(x, y) else placeAttacker(x, y)
            message = if (err.isEmpty()) setupText() else err
        }
        checkSetupDone()
    }

    private fun onPlayCell(x: Int, y: Int) {
        if (owed != null) { message = "Buang kartu dulu sampai tangan $HAND_SIZE."; return }
        if (pending != null) { message = "Pilih Hindar atau Terima Sentuhan dulu."; return }
        if (postMove != null) { message = "Setelah bergerak: pakai bintang atau akhiri giliran."; return }
        val card = activeCard
        if (card is Card.Stamina) {
            val target = pieceAt(x, y)
            if (target == null) { message = "Pilih karakter di papan sebagai tujuan."; return }
            applyStamina(card, target)
            return
        }
        if (turn == Side.ATTACKER) attackerAct(x, y) else guardAct(x, y)
    }

    /** Efek rintangan setelah bergerak: semak mengurangi stamina, lubang mengunci. */
    private fun hazard(p: Piece, path: List<Int>, end: Cell): String {
        val pen = path.count { obstacleAt(it % COLS, it / COLS)?.kind == ObstacleKind.BUSH }
        var text = ""
        if (pen > 0) {
            fx("bush", end.x, end.y)
            p.stamina = (p.stamina - pen).coerceAtLeast(0)
            text += " Semak: −$pen stamina."
        }
        if (obstacleAt(end.x, end.y)?.kind == ObstacleKind.HOLE) {
            fx("hole", end.x, end.y)
            p.lockPending = true
            text += " ${p.char.name} jatuh ke lubang, terkunci sampai giliran berikutnya."
        }
        return text
    }

    /** Pemilik kartu karakter di papan (null jika karakter sudah tidak di papan). */
    fun ownerOf(card: Card): Piece? {
        val n = card.owner ?: return null
        return (attackers + guards).firstOrNull { it.char.name == n }
    }

    /** Kartu menghindar di tangan penyerang untuk karakter yang sedang disentuh. */
    fun evadeCard(): Card.Evade? {
        val a = pending?.second ?: return null
        return decks[Side.ATTACKER]?.hand?.firstOrNull { it is Card.Evade && it.owner == a.char.name && !a.locked } as? Card.Evade
    }

    /** Karakter dipilih lewat kartunya (tidak ada pemilihan langsung di papan). */
    private fun selectPiece(p: Piece) {
        activeId = p.id
        if (p.side == Side.GUARD && p.visited == null) p.visited = mutableSetOf(key(p.x, p.y))
    }

    /** Pilih kartu di tangan (ketuk kartu). Kartu karakter langsung memilih karakter pemiliknya. */
    fun selectCard(card: Card) {
        if (owed != null) { discardCard(card); return }
        if (phase != Phase.PLAY) return
        val pend = pending
        if (pend != null) {
            if (card is Card.Evade && card.owner == pend.second.char.name) evade()
            else message = "Pilih Hindar untuk ${pend.second.char.name}, atau terima sentuhan."
            return
        }
        val after = pieceById(postMove)
        if (after != null) {
            if (card is Card.Star && card.owner == after.char.name) useStar(card)
            else message = "Setelah bergerak hanya bintang ${after.char.name} yang bisa dipakai. Tekan Akhiri Giliran."
            return
        }
        when {
            card is Card.Evade -> message = "Kartu menghindar hanya dipakai saat karakter itu disentuh."
            card is Card.Touch && turn != Side.GUARD -> message = "Kartu sentuh hanya dipakai penjaga."
            card is Card.Star -> message = "Kartu bintang dipakai setelah kartu gerak karakter yang sama."
            card is Card.Draw -> playDraw(card)
            card is Card.Stamina -> {
                activeCard = if (activeCard == card) null else card
                message = "Ketuk karakter tujuan: sekutu +${card.value}, lawan −${card.value} stamina."
            }
            else -> selectCharacterCard(card)
        }
    }

    private fun selectCharacterCard(card: Card) {
        val p = ownerOf(card)
        if (p == null) { message = "Karakter ${card.owner} tidak ada di papan."; return }
        if (p.side != turn) { message = "${p.char.name} bukan milik giliran ini."; return }
        if (p.locked) { message = "${p.char.name} terkunci di lubang sampai giliran berikutnya."; return }
        if (activeCard == card) {
            activeCard = null
            message = "Kartu dibatalkan."
            return
        }
        selectPiece(p)
        activeCard = card
        message = if (card is Card.Touch) "${p.char.name} dipilih. Ketuk penyerang di area merah."
        else "${p.char.name} dipilih. Ketuk kotak tujuan untuk bergerak."
    }

    /** Ambil 0..n kartu di awal giliran (sekali per giliran, sampai tangan HAND_SIZE). */
    fun chooseDraw(n: Int) {
        if (phase != Phase.PLAY || pending != null || postMove != null || owed != null) return
        if (drew || played) { message = "Kartu hanya bisa diambil di awal giliran."; return }
        var got = 0
        val hand = decks[turn]?.hand ?: return
        while (got < n && hand.size < HAND_SIZE) {
            if (drawOne(turn) == null) break
            got++
        }
        drew = true
        message = "Mengambil $got kartu. Tangan ${hand.size}/$HAND_SIZE. Pilih kartu karakter."
    }

    private fun attackerAct(x: Int, y: Int) {
        val card = activeCard
        val act = pieceById(activeId)
        if (act == null || act.side != Side.ATTACKER) { message = "Pilih penyerang dulu."; return }
        if (card == null) { message = "Pilih kartu dulu."; return }
        if (card !is Card.Move) { message = "Kartu ini tidak bisa dipakai penyerang."; return }
        val res = attackerTargets(act, card)[Cell(x, y)]
        if (res == null) { message = "Kotak itu tidak bisa dicapai dengan kartu dan stamina ini."; return }
        act.x = res.end.x
        act.y = res.end.y
        act.barrier = res.barrier
        act.stamina -= res.steps
        val hz = hazard(act, res.path, res.end)
        consume(Side.ATTACKER, card)
        if (res.scored) {
            attackers.remove(act)
            scores[attTeam] = (scores[attTeam] ?: 0) + 1
            fx("score", act.x, act.y)
            if ((scores[attTeam] ?: 0) >= WIN_SCORE) { gameWon(); return }
            if (attackers.isEmpty()) { roundOver("Semua penyerang sudah mencapai finish. Babak selesai."); return }
            endTurn("${act.char.name} mencapai finish: +1 poin untuk Tim ${attTeam.name}.")
            return
        }
        afterMove(act, "${act.char.name} bergerak ${dirName(card.dir)} ${res.steps} kotak.$hz")
    }

    private fun guardAct(x: Int, y: Int) {
        val g = pieceById(activeId)
        if (g == null || g.side != Side.GUARD) { message = "Pilih penjaga dulu."; return }
        val card = activeCard ?: run { message = "Pilih kartu dulu."; return }
        val target = pieceAt(x, y)
        if (card is Card.Touch) {
            if (target == null || target.side != Side.ATTACKER) { message = "Pilih penyerang di area merah untuk disentuh."; return }
            if (g.stamina < TOUCH_COST) { message = "Stamina ${g.char.name} kurang untuk menyentuh (butuh $TOUCH_COST)."; return }
            if (threatCells(g).none { it.x == target.x && it.y == target.y }) { message = "Penyerang itu tidak ada di area merah penjaga."; return }
            declareTouch(g, target, card)
            return
        }
        if (card !is Card.Move) { message = "Kartu ini tidak bisa dipakai penjaga."; return }
        if (target != null) { message = "Kotak itu ditempati penyerang."; return }
        val res = guardTargets(g, card)[Cell(x, y)]
        if (res == null) { message = "Kotak itu tidak bisa dicapai dengan kartu dan stamina ini."; return }
        val visited = g.visited ?: mutableSetOf(key(g.x, g.y))
        visited.addAll(res.path)
        g.visited = visited
        g.touchedH = res.flag
        g.x = res.end.x
        g.y = res.end.y
        g.stamina -= res.steps
        val hz = hazard(g, res.path, res.end)
        consume(Side.GUARD, card)
        afterMove(g, "${g.char.name} bergerak ${dirName(card.dir)} ${res.steps} kotak.$hz")
    }

    private fun declareTouch(g: Piece, a: Piece, card: Card.Touch) {
        fx("touch", a.x, a.y)
        g.stamina -= TOUCH_COST
        consume(Side.GUARD, card)
        val hasEvade = decks[Side.ATTACKER]?.hand?.any { it is Card.Evade && it.owner == a.char.name } ?: false
        if (!hasEvade || a.stamina < TOUCH_COST) {
            roundOver("Penjaga ${g.char.name} menyentuh ${a.char.name}. Babak selesai.")
            return
        }
        pending = g to a
        message = "Penjaga ${g.char.name} menyentuh ${a.char.name}. Penyerang: Hindar (3 stamina) atau Terima Sentuhan."
    }

    /** Penyerang membalas sentuhan dengan kartu menghindar (biaya 3). */
    fun evade() {
        val (_, a) = pending ?: return
        val card = evadeCard()
        if (card == null) { message = "Tidak punya kartu menghindar untuk ${a.char.name}."; return }
        if (a.stamina < TOUCH_COST) { message = "Stamina tidak cukup untuk menghindar."; return }
        a.stamina -= TOUCH_COST
        consume(Side.ATTACKER, card)
        pending = null
        endTurn("Sentuhan dihindari.")
    }

    fun acceptTouch() {
        val (g, a) = pending ?: return
        fx("hit", a.x, a.y)
        roundOver("Penjaga ${g.char.name} menyentuh ${a.char.name}. Babak selesai.")
    }

    private fun applyStamina(card: Card.Stamina, target: Piece) {
        val own = target.side == turn
        val delta = card.value * (if (own) 1 else -1)
        target.stamina = (target.stamina + delta).coerceAtLeast(0)
        consume(turn, card)
        activeCard = null
        endTurn("${target.char.name} ${if (delta > 0) "+" else ""}$delta stamina.")
    }

    /** Kartu ambil +N: ambil N kartu dari teratas deck. Biaya 0, tidak mengakhiri giliran. */
    private fun playDraw(card: Card.Draw) {
        consume(turn, card)
        var got = 0
        repeat(card.value) { if (drawOne(turn) != null) got++ }
        message = "Kartu +${card.value}: mengambil $got kartu. Giliran masih berjalan: lanjutkan atau akhiri giliran."
    }

    /** Kartu bintang: efek karakter belum aktif; kartu kembali ke dek dan giliran berakhir. */
    private fun useStar(card: Card.Star) {
        val piece = pieceById(postMove) ?: return
        piece.stamina -= STAR_COST
        consume(turn, card)
        endTurn("${piece.char.name} memakai bintang (efek belum aktif).")
    }

    fun endPostMove() {
        if (postMove != null) endTurn("Giliran diakhiri.")
    }

    fun skipTurn() {
        if (phase != Phase.PLAY || pending != null || postMove != null || owed != null) return
        endTurn("Giliran dilewati.")
    }

    /** Penjaga di garis batas (baris atau kolom tengah) berputar hadap dengan kartu arah apa pun. Memakan giliran. */
    fun turnFace() {
        if (phase != Phase.PLAY || pending != null || postMove != null || owed != null || turn != Side.GUARD) return
        val g = pieceById(activeId)
        if (g == null || g.side != Side.GUARD) { message = "Pilih penjaga dulu."; return }
        if (!(g.y in H_LANES || g.x == CENTER_X)) {
            message = "Penjaga hanya bisa berputar hadap di garis batas (baris atau kolom tengah)."
            return
        }
        val card = activeCard as? Card.Move ?: run { message = "Pilih kartu arah dulu, lalu putar hadap."; return }
        if (g.guardType != GuardType.V) {
            message = "Penjaga baris selalu menghadap start."
            return
        }
        consume(Side.GUARD, card)
        g.facing = if (g.facing == Facing.LEFT) Facing.RIGHT else Facing.LEFT
        activeCard = null
        endTurn("${g.char.name} berputar hadap dengan kartu ${dirName(card.dir)}.")
    }

    /** Buang kartu ke buangan (seret kartu ke buangan). Saat kewajiban buang, ini memenuhi kewajiban. */
    fun discardCard(card: Card) {
        if (phase != Phase.PLAY) return
        if (owed != null) { discardOwed(card); return }
        if (pending != null || postMove != null) {
            message = "Selesaikan aksi dulu sebelum membuang kartu."
            return
        }
        val d = decks[turn] ?: return
        if (!d.hand.remove(card)) return
        d.discard.add(card)
        if (activeCard == card) activeCard = null
        message = "Kartu dibuang ke buangan. Tangan ${d.hand.size}/$HAND_SIZE."
    }

    private fun discardOwed(card: Card) {
        val o = owed ?: return
        val d = decks[o.side] ?: return
        if (!d.hand.remove(card)) { message = "Pilih kartu di tangan untuk dibuang."; return }
        d.discard.add(card)
        activeCard = null
        o.need--
        if (o.need <= 0) {
            owed = null
            endTurn(o.text)
        } else {
            message = "Buang ${o.need} kartu lagi."
        }
    }

    // ---- Simulasi gerak ----

    private fun signs(dir: Dir, side: Side): List<Pair<Int, Int>> = when (dir) {
        Dir.VERTICAL -> listOf(0 to 1, 0 to -1)
        Dir.HORIZONTAL -> listOf(1 to 0, -1 to 0)
        Dir.CROSS -> if (side == Side.ATTACKER) listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
        else listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)
    }

    /** Penyerang: setiap kotak (termasuk garis batas) membayar 1 stamina; dinding memantulkan arah tanpa biaya; tidak menabrak penjaga. */
    private fun simAttacker(a: Piece, sdx: Int, sdy: Int, n: Int): MoveResult? {
        var x = a.x
        var y = a.y
        var dx = sdx
        var dy = sdy
        var rem = n
        var barrier = a.barrier
        val path = mutableListOf<Int>()
        var guard = 0
        while (rem > 0 && guard++ < 80) {
            val nx = x + dx
            val ny = y + dy
            if (isWallX(nx)) { dx = -dx; continue }
            if (ny < 0 || ny >= ROWS) { dy = -dy; continue }
            if (occupiedBy(nx, ny, a.id)) return null
            if (obstacleAt(nx, ny)?.kind == ObstacleKind.ROCK) return null
            // Tidak boleh kembali melewati garis batas yang sudah dilewati.
            if (ny > barrier) return null
            x = nx
            y = ny
            path.add(key(x, y))
            H_LANES.firstOrNull { y <= it }?.let { if (it < barrier) barrier = it }
            rem--
            if (y == END_Y) return MoveResult(path, Cell(x, y), scored = true, steps = n - rem, barrier = barrier)
            // Lubang menghentikan gerak di kotak itu.
            if (obstacleAt(x, y)?.kind == ObstacleKind.HOLE) return MoveResult(path, Cell(x, y), steps = n - rem, barrier = barrier)
        }
        if (rem > 0) return null
        if (x == a.x && y == a.y) return null
        return MoveResult(path, Cell(x, y), steps = n, barrier = barrier)
    }

    private fun canRevisit(g: Piece, x: Int, flag: Boolean): Boolean =
        if (g.guardType == GuardType.H) x == 1 || x == COLS - 2 else flag

    /** Penjaga: lurus sesuai garisnya; dinding memantulkan arah tanpa biaya; revisit hanya dalam aturan. */
    private fun simGuard(g: Piece, sdx: Int, sdy: Int, n: Int): MoveResult? {
        var x = g.x
        var y = g.y
        var dx = sdx
        var dy = sdy
        var rem = n
        val visited = HashSet<Int>(g.visited ?: setOf(key(g.x, g.y)))
        var flag = g.touchedH
        val path = mutableListOf<Int>()
        var guard = 0
        while (rem > 0 && guard++ < 80) {
            val nx = x + dx
            val ny = y + dy
            if (isWallX(nx)) { dx = -dx; continue }
            if (ny < 0 || ny >= ROWS) { dy = -dy; continue }
            if (occupiedBy(nx, ny, g.id)) return null
            if (obstacleAt(nx, ny)?.kind == ObstacleKind.ROCK) return null
            val kk = key(nx, ny)
            val seen = visited.contains(kk)
            if (seen && !canRevisit(g, x, flag)) return null
            if (!seen) visited.add(kk)
            x = nx
            y = ny
            path.add(kk)
            rem--
            if (g.guardType == GuardType.V && y in H_LANES) flag = true
            if (obstacleAt(x, y)?.kind == ObstacleKind.HOLE) return MoveResult(path, Cell(x, y), flag = flag, steps = n - rem)
        }
        if (rem > 0) return null
        if (x == g.x && y == g.y) return null
        return MoveResult(path, Cell(x, y), flag = flag, steps = n)
    }

    fun attackerTargets(a: Piece, card: Card.Move): Map<Cell, MoveResult> {
        val map = linkedMapOf<Cell, MoveResult>()
        for (n in 1..a.stamina) for ((dx, dy) in signs(card.dir, Side.ATTACKER)) {
            simAttacker(a, dx, dy, n)?.let { map.putIfAbsent(it.end, it) }
        }
        return map
    }

    fun guardTargets(g: Piece, card: Card.Move): Map<Cell, MoveResult> {
        val map = linkedMapOf<Cell, MoveResult>()
        if (card.dir != Dir.CROSS) {
            if (g.guardType == GuardType.H && card.dir != Dir.HORIZONTAL) return map
            if (g.guardType == GuardType.V && card.dir != Dir.VERTICAL) return map
        }
        for (n in 1..g.stamina) for ((dx, dy) in signs(card.dir, Side.GUARD)) {
            simGuard(g, dx, dy, n)?.let { map.putIfAbsent(it.end, it) }
        }
        return map
    }

    /** Kotak tujuan untuk kartu arah yang sedang dipilih. */
    fun moveTargets(): Map<Cell, MoveResult> {
        if (phase != Phase.PLAY || pending != null || postMove != null) return emptyMap()
        val card = activeCard as? Card.Move ?: return emptyMap()
        val p = pieceById(activeId) ?: return emptyMap()
        return if (turn == Side.ATTACKER) attackerTargets(p, card) else guardTargets(p, card)
    }

    fun statusLine(): String {
        val phaseName = when (phase) {
            Phase.SETUP -> "Pemasangan"
            Phase.PLAY -> "Permainan"
            Phase.OVER -> "Babak selesai"
            Phase.WON, Phase.ENDED -> "Permainan selesai"
        }
        val turnText = if (phase == Phase.PLAY) " · Giliran: ${turnName()}" else ""
        return "Babak $round/$TOTAL_ROUNDS · $phaseName · Penyerang: Tim ${attTeam.name}$turnText · " +
            "Skor Tim A ${scores[Team.A]} – Tim B ${scores[Team.B]}"
    }
}
