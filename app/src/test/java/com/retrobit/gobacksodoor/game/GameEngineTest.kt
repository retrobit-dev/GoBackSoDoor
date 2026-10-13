package com.retrobit.gobacksodoor.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private lateinit var e: GameEngine

    /** Pemasangan standar: penyerang di baris coklat, penjaga di garis batas. */
    @Before
    fun setUp() {
        e = GameEngine(Random(1))
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
    fun moveCostsOneStaminaPerStep() {
        setHand(Side.ATTACKER, Card.Move(900, Dir.VERTICAL), Card.Move(901, Dir.HORIZONTAL), Card.Move(902, Dir.CROSS))
        e.onCell(1, BELOW_Y)                 // pilih penyerang A1
        e.selectCard(hand(Side.ATTACKER)[0]) // atas-bawah
        e.onCell(1, 18)                      // 2 kotak ke atas; garis batas (1,19) tidak dihitung
        assertEquals(Cell(1, 18), Cell(attacker("A1").x, attacker("A1").y))
        assertEquals(2, attacker("A1").stamina)
        assertEquals(Side.GUARD, e.turn)
    }

    @Test
    fun wallBounceReversesWithoutCost() {
        val a = attacker("A1")
        a.x = 11
        a.y = 10
        setHand(Side.ATTACKER, Card.Move(910, Dir.HORIZONTAL), Card.Move(911, Dir.CROSS), Card.Move(912, Dir.CROSS))
        e.onCell(11, 10)
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
        setHand(Side.ATTACKER, Card.Move(920, Dir.VERTICAL), Card.Move(921, Dir.CROSS), Card.Move(922, Dir.CROSS))
        e.onCell(1, 12)
        e.selectCard(hand(Side.ATTACKER)[0])
        e.onCell(1, 13)
        assertEquals(12, a.y)
        assertEquals(Side.ATTACKER, e.turn)
    }

    @Test
    fun starHoldsTurnAfterMoveAndReturnsToDeck() {
        attacker("A1").stamina = 6
        val star = Card.Star(930, 1)
        setHand(Side.ATTACKER, Card.Move(931, Dir.VERTICAL), star, Card.Move(932, Dir.CROSS))
        e.onCell(1, BELOW_Y)
        e.selectCard(hand(Side.ATTACKER)[0])
        e.onCell(1, 20)
        assertEquals(Side.ATTACKER, e.turn)
        assertEquals("A1", e.postMove)
        e.selectCard(star)
        assertEquals(Side.GUARD, e.turn)
        assertTrue(e.decks.getValue(Side.ATTACKER).deck.contains(star) || hand(Side.ATTACKER).contains(star))
    }

    @Test
    fun touchThenEvadeCostsThreeEach() {
        val v = guard("V1")
        v.stamina = 5
        attacker("A1").x = 7
        attacker("A1").y = 10
        e.skipTurn()                          // giliran penyerang dilewati -> penjaga
        assertEquals(Side.GUARD, e.turn)
        setHand(Side.ATTACKER, Card.Evade(940), Card.Move(941, Dir.VERTICAL), Card.Move(942, Dir.CROSS))
        setHand(Side.GUARD, Card.Touch(943), Card.Move(944, Dir.VERTICAL), Card.Move(945, Dir.CROSS))
        e.onCell(6, 10)
        e.selectCard(hand(Side.GUARD)[0])
        e.onCell(7, 10)
        assertNotNull(e.pending)
        assertEquals(3, v.stamina)               // 5 + 1 (awal giliran) - 3 (sentuh)
        e.evade()
        assertNull(e.pending)
        assertEquals(Side.ATTACKER, e.turn)
        assertEquals(2, attacker("A1").stamina)  // 4 - 3 hindar + 1 saat giliran kembali
    }

    @Test
    fun touchWithoutEvadeEndsRound() {
        attacker("A1").x = 7
        attacker("A1").y = 10
        attacker("A1").stamina = 2
        e.skipTurn()
        setHand(Side.ATTACKER, Card.Move(953, Dir.VERTICAL), Card.Move(954, Dir.CROSS), Card.Move(955, Dir.HORIZONTAL))
        setHand(Side.GUARD, Card.Touch(950), Card.Move(951, Dir.VERTICAL), Card.Move(952, Dir.CROSS))
        e.onCell(6, 10)
        e.selectCard(hand(Side.GUARD)[0])
        e.onCell(7, 10)
        assertEquals(Phase.OVER, e.phase)
    }

    @Test
    fun excessCardsMustBeDiscardedAtTurnEnd() {
        setHand(
            Side.ATTACKER,
            Card.Move(960, Dir.VERTICAL), Card.Move(961, Dir.CROSS), Card.Move(962, Dir.HORIZONTAL),
            Card.Move(963, Dir.VERTICAL), Card.Move(964, Dir.CROSS),
        )
        // Tangan 5: setelah bermain 1 kartu tersisa 4, jadi wajib buang 1.
        e.onCell(1, BELOW_Y)
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
        setHand(Side.ATTACKER, Card.Move(970, Dir.VERTICAL))
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
        setHand(Side.ATTACKER, Card.Draw(980, 2), Card.Move(981, Dir.VERTICAL))
        e.onCell(1, BELOW_Y)
        e.selectCard(hand(Side.ATTACKER)[0])
        assertEquals(Side.ATTACKER, e.turn)           // tidak mengakhiri giliran
        assertEquals(HAND_SIZE, hand(Side.ATTACKER).size)
        assertTrue(hand(Side.ATTACKER).containsAll(top))
        // Setelah ambil, pemain tetap bisa bergerak dengan kartu yang tersisa.
        e.selectCard(hand(Side.ATTACKER).first { it is Card.Move })
        e.onCell(1, 20)
        assertEquals(Side.GUARD, e.turn)
    }

    @Test
    fun drawThenEndTurnWithTooManyCardsRequiresDiscard() {
        setHand(Side.ATTACKER, Card.Draw(985, 3), Card.Move(986, Dir.VERTICAL), Card.Move(987, Dir.CROSS))
        e.onCell(1, BELOW_Y)
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
        setHand(Side.GUARD, Card.Stamina(990, 3), Card.Move(991, Dir.VERTICAL), Card.Move(992, Dir.CROSS))
        e.onCell(2, 1)
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
        setHand(Side.ATTACKER, Card.Move(1000, Dir.VERTICAL), Card.Move(1001, Dir.CROSS), Card.Move(1002, Dir.CROSS))
        e.onCell(1, 2)
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
        setHand(Side.GUARD, Card.Move(1010, Dir.CROSS), Card.Move(1011, Dir.VERTICAL), Card.Move(1012, Dir.CROSS))
        e.onCell(2, 1)
        e.selectCard(hand(Side.GUARD)[0])
        e.onCell(3, 1)
        assertEquals(3, guard("H1").x)
        assertEquals(1, guard("H1").y)
    }

    @Test
    fun stepsAreChosenByDestinationCell() {
        setHand(Side.ATTACKER, Card.Move(1100, Dir.VERTICAL), Card.Move(1101, Dir.CROSS), Card.Move(1102, Dir.CROSS))
        e.onCell(1, BELOW_Y)
        e.selectCard(hand(Side.ATTACKER)[0])
        e.onCell(1, 20)                                // 1 kotak
        assertEquals(3, attacker("A1").stamina)        // 4 - 1
        assertEquals(Side.GUARD, e.turn)
    }

    @Test
    fun selectingAnotherCharacterCancelsCardSelection() {
        setHand(Side.ATTACKER, Card.Move(1110, Dir.VERTICAL), Card.Move(1111, Dir.CROSS), Card.Move(1112, Dir.CROSS))
        e.onCell(1, BELOW_Y)
        e.selectCard(hand(Side.ATTACKER)[0])
        assertNotNull(e.activeCard)
        e.onCell(3, BELOW_Y)                           // pilih penyerang lain
        assertEquals("A2", e.activeId)
        assertNull(e.activeCard)
    }

    @Test
    fun discardCardMovesHandCardToDiscardPile() {
        setHand(Side.ATTACKER, Card.Move(1120, Dir.VERTICAL), Card.Move(1121, Dir.CROSS), Card.Move(1122, Dir.CROSS))
        val before = e.decks.getValue(Side.ATTACKER).discard.size
        e.discardCard(hand(Side.ATTACKER)[0])
        assertEquals(2, hand(Side.ATTACKER).size)
        assertEquals(before + 1, e.decks.getValue(Side.ATTACKER).discard.size)
        assertEquals(Side.ATTACKER, e.turn)            // membuang tidak mengakhiri giliran
    }
}
