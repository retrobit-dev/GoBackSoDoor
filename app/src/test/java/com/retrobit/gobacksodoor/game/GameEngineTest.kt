package com.retrobit.gobacksodoor.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * Pemasangan standar: A1 Slamet, A2 Sutan, A3 Ekot, A4 Wayan, A5 Andi (baris coklat);
 * H1 Togar, H2 Dalle, H3 Pitung, H4 Lalu (garis batas baris), V1 Wanggai (kolom tengah).
 * Karakter dipilih lewat kartunya: kartu dengan nama karakter itu.
 */
class GameEngineTest {

    private lateinit var e: GameEngine

    @Before
    fun setUp() {
        e = GameEngine(Random(1), obstaclesOn = false)
        e.setupTool = SetupTool.ATTACKER
        listOf(1, 3, 5, 7, 9).forEach { e.onCell(it, BELOW_Y) }
        e.setupTool = SetupTool.GUARD
        e.onCell(2, 1)
        e.onCell(9, 7)
        e.onCell(4, 13)
        e.onCell(10, 19)
        e.onCell(CENTER_X, 10)
    }

    private fun hand(side: Side): MutableList<Card> = e.decks.getValue(side).hand

    private fun setHand(side: Side, vararg cards: Card) {
        hand(side).clear()
        hand(side).addAll(cards)
    }

    private fun attacker(id: String): Piece = e.attackers.first { it.id == id }
    private fun guard(id: String): Piece = e.guards.first { it.id == id }

    @Test
    fun setupFinishesWithCharactersAndHands() {
        assertEquals(Phase.PLAY, e.phase)
        assertEquals(Side.ATTACKER, e.turn)
        assertEquals("Slamet", attacker("A1").char.name)
        assertEquals(4, attacker("A1").stamina)
        assertEquals("Togar", guard("H1").char.name)
        assertEquals(HAND_SIZE, hand(Side.ATTACKER).size)
        assertEquals(35, e.decks.getValue(Side.ATTACKER).let { it.deck.size + it.hand.size })
    }

    @Test
    fun combatCardsBelongToFiveCharactersEach() {
        val d = e.decks.getValue(Side.ATTACKER)
        val combat = (d.deck + d.hand).filter { it.owner != null }
        assertEquals(25, combat.size)
        assertEquals(ATTACKER_ROSTER.map { it.name }.toSet(), combat.mapNotNull { it.owner }.toSet())
        ATTACKER_ROSTER.forEach { c -> assertEquals(5, combat.count { it.owner == c.name }) }
    }

    @Test
    fun moveCostsOneStaminaPerStep() {
        setHand(Side.ATTACKER, Card.Move(900, Dir.VERTICAL, "Slamet"), Card.Move(901, Dir.HORIZONTAL, "Slamet"), Card.Move(902, Dir.CROSS, "Slamet"))
        e.selectCard(hand(Side.ATTACKER)[0]) // Slamet, atas-bawah
        e.onCell(1, 19)                      // 2 kotak ke atas; garis batas (1,19) ikut dihitung
        assertEquals(Cell(1, 19), Cell(attacker("A1").x, attacker("A1").y))
        assertEquals(2, attacker("A1").stamina)
        assertEquals(Side.GUARD, e.turn)
    }

    @Test
    fun wallBounceReversesWithoutCost() {
        val a = attacker("A1")
        a.x = 11
        a.y = 10
        setHand(Side.ATTACKER, Card.Move(910, Dir.HORIZONTAL, "Slamet"), Card.Move(911, Dir.CROSS, "Slamet"), Card.Move(912, Dir.CROSS, "Slamet"))
        e.selectCard(hand(Side.ATTACKER)[0])
        e.onCell(8, 10)                      // kanan 3 memantul di dinding (12,10), tanpa biaya angka
        assertEquals(8, a.x)
        assertEquals(10, a.y)
        assertEquals(1, a.stamina)
    }

    @Test
    fun attackerCannotRunIntoGuard() {
        val a = attacker("A1")
        a.x = 1
        a.y = 12
        guard("H1").x = 1
        guard("H1").y = 13
        setHand(Side.ATTACKER, Card.Move(920, Dir.VERTICAL, "Slamet"), Card.Move(921, Dir.CROSS, "Slamet"), Card.Move(922, Dir.CROSS, "Slamet"))
        e.selectCard(hand(Side.ATTACKER)[0])
        e.onCell(1, 13)
        assertEquals(12, a.y)
        assertEquals(Side.ATTACKER, e.turn)
    }

    @Test
    fun starHoldsTurnAfterMoveAndReturnsToDeck() {
        attacker("A1").stamina = 6
        val star = Card.Star(930, 1, "Slamet")
        setHand(Side.ATTACKER, Card.Move(931, Dir.VERTICAL, "Slamet"), star, Card.Move(932, Dir.CROSS, "Slamet"))
        e.selectCard(hand(Side.ATTACKER)[0])
        e.onCell(1, 20)
        assertEquals(Side.ATTACKER, e.turn)
        assertEquals("A1", e.postMove)
        e.selectCard(star)
        assertEquals(Side.GUARD, e.turn)
        assertTrue(e.decks.getValue(Side.ATTACKER).deck.contains(star) || hand(Side.ATTACKER).contains(star))
    }

    @Test
    fun starOfAnotherCharacterCannotBeUsed() {
        attacker("A1").stamina = 6
        val star = Card.Star(933, 1, "Sutan")
        setHand(Side.ATTACKER, Card.Move(934, Dir.VERTICAL, "Slamet"), star, Card.Star(936, 2, "Slamet"))
        e.selectCard(hand(Side.ATTACKER)[0])
        e.onCell(1, 20)
        assertEquals("A1", e.postMove)
        e.selectCard(star)
        assertEquals(Side.ATTACKER, e.turn)          // bintang milik Sutan tidak bisa dipakai Slamet
    }

    @Test
    fun touchThenEvadeCostsThreeEach() {
        val v = guard("V1")
        v.stamina = 5
        attacker("A1").x = 7
        attacker("A1").y = 10
        e.skipTurn()                          // giliran penyerang dilewati -> penjaga
        assertEquals(Side.GUARD, e.turn)
        setHand(Side.ATTACKER, Card.Evade(940, "Slamet"), Card.Move(941, Dir.VERTICAL, "Slamet"), Card.Move(942, Dir.CROSS, "Slamet"))
        setHand(Side.GUARD, Card.Touch(943, "Wanggai"), Card.Move(944, Dir.VERTICAL, "Wanggai"), Card.Move(945, Dir.CROSS, "Wanggai"))
        e.selectCard(hand(Side.GUARD)[0])     // Wanggai (V1) dipilih lewat kartu sentuh
        e.onCell(7, 10)
        assertNotNull(e.pending)
        assertEquals(3, v.stamina)               // 5 + 1 (awal giliran) - 3 (sentuh)
        e.evade()
        assertNull(e.pending)
        assertEquals(Side.ATTACKER, e.turn)
        assertEquals(2, attacker("A1").stamina)  // 4 - 3 hindar + 1 saat giliran kembali
    }

    @Test
    fun evadeMustBelongToTouchedCharacter() {
        attacker("A1").x = 7
        attacker("A1").y = 10
        e.skipTurn()
        val wrong = Card.Evade(945, "Sutan")
        val right = Card.Evade(946, "Slamet")
        setHand(Side.ATTACKER, wrong, right, Card.Move(947, Dir.CROSS, "Slamet"))
        setHand(Side.GUARD, Card.Touch(948, "Wanggai"), Card.Move(949, Dir.VERTICAL, "Wanggai"), Card.Move(950, Dir.CROSS, "Wanggai"))
        e.selectCard(hand(Side.GUARD)[0])
        e.onCell(7, 10)
        assertNotNull(e.pending)
        assertEquals(right, e.evadeCard())
        e.selectCard(wrong)                      // hindar milik Sutan tidak berlaku untuk Slamet
        assertNotNull(e.pending)
        e.selectCard(right)
        assertNull(e.pending)
    }

    @Test
    fun touchWithoutEvadeEndsRound() {
        attacker("A1").x = 7
        attacker("A1").y = 10
        attacker("A1").stamina = 2
        e.skipTurn()
        setHand(Side.ATTACKER, Card.Move(953, Dir.VERTICAL, "Slamet"), Card.Move(954, Dir.CROSS, "Slamet"), Card.Move(955, Dir.HORIZONTAL, "Slamet"))
        setHand(Side.GUARD, Card.Touch(950, "Wanggai"), Card.Move(951, Dir.VERTICAL, "Wanggai"), Card.Move(952, Dir.CROSS, "Wanggai"))
        e.selectCard(hand(Side.GUARD)[0])
        e.onCell(7, 10)
        assertEquals(Phase.OVER, e.phase)
    }

    @Test
    fun excessCardsMustBeDiscardedAtTurnEnd() {
        setHand(
            Side.ATTACKER,
            Card.Move(960, Dir.VERTICAL, "Slamet"), Card.Move(961, Dir.CROSS, "Slamet"), Card.Move(962, Dir.HORIZONTAL, "Slamet"),
            Card.Move(963, Dir.VERTICAL, "Slamet"), Card.Move(964, Dir.CROSS, "Slamet"),
        )
        // Tangan 5: setelah bermain 1 kartu tersisa 4, jadi wajib buang 1.
        e.selectCard(hand(Side.ATTACKER)[0])
        e.onCell(1, 20)
        assertEquals(Side.ATTACKER, e.turn)
        assertNotNull(e.owed)
        e.selectCard(hand(Side.ATTACKER)[0])
        assertEquals(HAND_SIZE, hand(Side.ATTACKER).size)
        assertEquals(Side.GUARD, e.turn)
        assertNull(e.owed)
    }

    @Test
    fun chooseDrawFillsUpToThreeOncePerTurn() {
        setHand(Side.ATTACKER, Card.Move(970, Dir.VERTICAL, "Slamet"))
        e.chooseDraw(5)
        assertEquals(HAND_SIZE, hand(Side.ATTACKER).size)
        val before = hand(Side.ATTACKER).size
        e.chooseDraw(1)
        assertEquals(before, hand(Side.ATTACKER).size)
    }

    @Test
    fun drawCardTakesTopCardsAndKeepsTurn() {
        val d = e.decks.getValue(Side.ATTACKER)
        val top = d.deck.takeLast(2)
        setHand(Side.ATTACKER, Card.Draw(980, 2), Card.Move(981, Dir.VERTICAL, "Slamet"))
        e.selectCard(hand(Side.ATTACKER)[0])
        assertEquals(Side.ATTACKER, e.turn)           // tidak mengakhiri giliran
        assertEquals(HAND_SIZE, hand(Side.ATTACKER).size)
        assertTrue(hand(Side.ATTACKER).containsAll(top))
        // Setelah ambil, pemain tetap bisa bergerak dengan kartu yang tersisa.
        e.selectCard(hand(Side.ATTACKER).first { it.uid == 981 })   // kartu gerak Slamet yang tersisa
        e.onCell(1, 20)
        assertEquals(Side.GUARD, e.turn)
    }

    @Test
    fun drawThenEndTurnWithTooManyCardsRequiresDiscard() {
        setHand(Side.ATTACKER, Card.Draw(985, 3), Card.Move(986, Dir.VERTICAL, "Slamet"), Card.Move(987, Dir.CROSS, "Slamet"))
        e.selectCard(hand(Side.ATTACKER)[0])          // tangan 2 + 3 = 5
        assertEquals(5, hand(Side.ATTACKER).size)
        e.skipTurn()                                  // akhiri giliran -> wajib buang 2
        assertEquals(Side.ATTACKER, e.turn)
        assertNotNull(e.owed)
        e.selectCard(hand(Side.ATTACKER)[0])
        e.selectCard(hand(Side.ATTACKER)[0])
        assertEquals(Side.GUARD, e.turn)
        assertEquals(HAND_SIZE, hand(Side.ATTACKER).size)
    }

    @Test
    fun staminaCardTargetsOwnOrOpponent() {
        e.skipTurn()                          // penjaga
        guard("H1").stamina = 4
        setHand(Side.GUARD, Card.Stamina(990, 3), Card.Move(991, Dir.VERTICAL, "Togar"), Card.Move(992, Dir.CROSS, "Togar"))
        e.selectCard(hand(Side.GUARD)[0])
        e.onCell(2, 1)                        // +3 untuk karakter sendiri
        assertEquals(7, guard("H1").stamina)
    }

    @Test
    fun finishScoresOnePointAndWinsAtFive() {
        val a = attacker("A1")
        a.x = 1
        a.y = 2
        a.stamina = 4
        e.scores[Team.A] = 4
        setHand(Side.ATTACKER, Card.Move(1000, Dir.VERTICAL, "Slamet"), Card.Move(1001, Dir.CROSS, "Slamet"), Card.Move(1002, Dir.CROSS, "Slamet"))
        e.selectCard(hand(Side.ATTACKER)[0])
        e.onCell(1, 0)
        assertEquals(Phase.WON, e.phase)
        assertEquals(Team.A, e.winner)
        assertEquals(5, e.scores[Team.A])
    }

    @Test
    fun guardCrossCardMovesOrthogonally() {
        // Kartu silang untuk penjaga bergerak ortogonal (tidak diagonal).
        e.skipTurn()
        setHand(Side.GUARD, Card.Move(1010, Dir.CROSS, "Togar"), Card.Move(1011, Dir.VERTICAL, "Togar"), Card.Move(1012, Dir.CROSS, "Togar"))
        e.selectCard(hand(Side.GUARD)[0])
        e.onCell(3, 1)
        assertEquals(3, guard("H1").x)
        assertEquals(1, guard("H1").y)
    }

    @Test
    fun stepsAreChosenByDestinationCell() {
        setHand(Side.ATTACKER, Card.Move(1100, Dir.VERTICAL, "Slamet"), Card.Move(1101, Dir.CROSS, "Slamet"), Card.Move(1102, Dir.CROSS, "Slamet"))
        e.selectCard(hand(Side.ATTACKER)[0])
        e.onCell(1, 20)                                // 1 kotak
        assertEquals(3, attacker("A1").stamina)        // 4 - 1
        assertEquals(Side.GUARD, e.turn)
    }

    @Test
    fun selectingAnotherCharacterCardSwitchesCharacter() {
        setHand(Side.ATTACKER, Card.Move(1110, Dir.VERTICAL, "Slamet"), Card.Move(1111, Dir.CROSS, "Sutan"), Card.Move(1112, Dir.CROSS, "Slamet"))
        e.selectCard(hand(Side.ATTACKER)[0])
        assertEquals("A1", e.activeId)
        assertNotNull(e.activeCard)
        e.selectCard(hand(Side.ATTACKER)[1])           // kartu Sutan
        assertEquals("A2", e.activeId)
        assertEquals(hand(Side.ATTACKER)[1], e.activeCard)
    }

    @Test
    fun discardCardMovesHandCardToDiscardPile() {
        setHand(Side.ATTACKER, Card.Move(1120, Dir.VERTICAL, "Slamet"), Card.Move(1121, Dir.CROSS, "Slamet"), Card.Move(1122, Dir.CROSS, "Slamet"))
        val before = e.decks.getValue(Side.ATTACKER).discard.size
        e.discardCard(hand(Side.ATTACKER)[0])
        assertEquals(2, hand(Side.ATTACKER).size)
        assertEquals(before + 1, e.decks.getValue(Side.ATTACKER).discard.size)
        assertEquals(Side.ATTACKER, e.turn)            // membuang tidak mengakhiri giliran
    }

    @Test
    fun attackerCannotJumpOverBoundaryLanes() {
        // Dari baris coklat (y=21) ke y=18 harus melewati garis batas y=19 dengan langkah: 3 kotak.
        attacker("A1").stamina = 4
        setHand(Side.ATTACKER, Card.Move(1130, Dir.VERTICAL, "Slamet"), Card.Move(1131, Dir.CROSS, "Slamet"), Card.Move(1132, Dir.CROSS, "Slamet"))
        e.selectCard(hand(Side.ATTACKER)[0])
        e.onCell(1, 18)
        assertEquals(Cell(1, 18), Cell(attacker("A1").x, attacker("A1").y))
        assertEquals(1, attacker("A1").stamina)       // 4 - 3
    }

    @Test
    fun touchAreaIncludesGuardSides() {
        val v1 = guard("V1")                           // (6,10) menghadap kanan
        val cells = e.threatCells(v1)
        assertTrue(cells.contains(Cell(7, 10)))        // depan
        assertTrue(cells.contains(Cell(6, 9)))         // sisi penjaga (atas)
        assertTrue(cells.contains(Cell(6, 11)))        // sisi penjaga (bawah)
        val h1 = guard("H1")                           // (2,1) menghadap bawah
        val hCells = e.threatCells(h1)
        assertTrue(hCells.contains(Cell(2, 2)))
        assertTrue(hCells.contains(Cell(1, 1)))        // sisi penjaga (kiri)
        assertTrue(hCells.contains(Cell(3, 1)))        // sisi penjaga (kanan)
    }

    @Test
    fun guardOnBoundaryTurnsFaceWithAnyMoveCard() {
        e.skipTurn()                                   // penjaga
        setHand(Side.GUARD, Card.Move(1140, Dir.CROSS, "Wanggai"), Card.Move(1141, Dir.VERTICAL, "Wanggai"), Card.Move(1142, Dir.HORIZONTAL, "Wanggai"))
        e.selectCard(hand(Side.GUARD)[0])              // Wanggai (V1) di kolom tengah, garis batas
        e.turnFace()
        assertEquals(Facing.LEFT, guard("V1").facing)
        assertEquals(Side.ATTACKER, e.turn)            // memakan giliran
    }

    @Test
    fun guardOffBoundaryCannotTurnFace() {
        e.skipTurn()
        guard("V1").x = 5                              // bukan garis batas
        setHand(Side.GUARD, Card.Move(1150, Dir.CROSS, "Wanggai"), Card.Move(1151, Dir.VERTICAL, "Wanggai"), Card.Move(1152, Dir.HORIZONTAL, "Wanggai"))
        e.selectCard(hand(Side.GUARD)[0])
        e.turnFace()
        assertEquals(Facing.RIGHT, guard("V1").facing)
        assertEquals(Side.GUARD, e.turn)
    }

    @Test
    fun attackerCannotMoveBackPastPassedBoundary() {
        val a = attacker("A1")
        a.x = 1
        a.y = 18
        a.barrier = 19                                 // sudah melewati garis batas baris 19
        setHand(Side.ATTACKER, Card.Move(1160, Dir.VERTICAL, "Slamet"), Card.Move(1161, Dir.CROSS, "Slamet"), Card.Move(1162, Dir.CROSS, "Slamet"))
        e.selectCard(hand(Side.ATTACKER)[0])
        e.onCell(1, 20)                                // kembali ke baris 20 dilarang
        assertEquals(18, a.y)
        assertEquals(Side.ATTACKER, e.turn)
    }

    @Test
    fun guardRowCannotTurnFace() {
        e.skipTurn()
        setHand(Side.GUARD, Card.Move(1170, Dir.CROSS, "Togar"), Card.Move(1171, Dir.VERTICAL, "Togar"), Card.Move(1172, Dir.HORIZONTAL, "Togar"))
        e.selectCard(hand(Side.GUARD)[0])              // Togar (H1) penjaga baris
        e.turnFace()
        assertEquals(Facing.DOWN, guard("H1").facing)
        assertEquals(Side.GUARD, e.turn)
    }

    @Test
    fun holeStopsAttackerAndLocksItUntilNextTurn() {
        e.obstacles.add(Obstacle(1, 19, ObstacleKind.HOLE, 4))
        setHand(Side.ATTACKER, Card.Move(1200, Dir.VERTICAL, "Slamet"), Card.Move(1201, Dir.CROSS, "Slamet"), Card.Move(1202, Dir.CROSS, "Slamet"))
        e.selectCard(hand(Side.ATTACKER)[0])
        e.onCell(1, 19)                                 // lubang: gerak berhenti di sana (2 kotak)
        assertEquals(Cell(1, 19), Cell(attacker("A1").x, attacker("A1").y))
        assertEquals(2, attacker("A1").stamina)
        assertEquals(Side.GUARD, e.turn)
        e.skipTurn()                                    // giliran penjaga dilewati -> giliran penyerang berikutnya
        assertEquals(Side.ATTACKER, e.turn)
        assertTrue(attacker("A1").locked)
        e.selectCard(hand(Side.ATTACKER)[0])            // Slamet terkunci: tidak bisa dipilih
        assertNull(e.activeCard)
        e.skipTurn()                                    // giliran penyerang lewat
        e.skipTurn()                                    // giliran penjaga lewat
        assertEquals(Side.ATTACKER, e.turn)
        assertEquals(false, attacker("A1").locked)      // terbuka lagi pada giliran berikutnya
    }

    @Test
    fun rockBlocksAttackerPath() {
        e.obstacles.add(Obstacle(1, 19, ObstacleKind.ROCK, 4))
        setHand(Side.ATTACKER, Card.Move(1210, Dir.VERTICAL, "Slamet"), Card.Move(1211, Dir.CROSS, "Slamet"), Card.Move(1212, Dir.CROSS, "Slamet"))
        e.selectCard(hand(Side.ATTACKER)[0])
        e.onCell(1, 18)                                 // harus melewati batu di (1,19)
        assertEquals(21, attacker("A1").y)
        assertEquals(Side.ATTACKER, e.turn)
    }

    @Test
    fun bushCostsOneExtraStaminaWhenPassed() {
        e.obstacles.add(Obstacle(1, 20, ObstacleKind.BUSH, 4))
        setHand(Side.ATTACKER, Card.Move(1220, Dir.VERTICAL, "Slamet"), Card.Move(1221, Dir.CROSS, "Slamet"), Card.Move(1222, Dir.CROSS, "Slamet"))
        e.selectCard(hand(Side.ATTACKER)[0])
        e.onCell(1, 19)                                 // 2 kotak + 1 semak = 3 stamina
        assertEquals(1, attacker("A1").stamina)
        assertEquals(Side.GUARD, e.turn)
    }

    @Test
    fun obstacleAppearsEachTurnOnFieldCell() {
        val g = GameEngine(Random(5), obstaclesOn = true)
        g.setupTool = SetupTool.ATTACKER
        listOf(1, 3, 5, 7, 9).forEach { g.onCell(it, BELOW_Y) }
        g.setupTool = SetupTool.GUARD
        g.onCell(2, 1)
        g.onCell(9, 7)
        g.onCell(4, 13)
        g.onCell(10, 19)
        g.onCell(CENTER_X, 10)
        g.skipTurn()
        assertEquals(1, g.obstacles.size)
        val o = g.obstacles[0]
        assertEquals(Zone.FIELD, zoneOf(o.x, o.y))
    }
}
