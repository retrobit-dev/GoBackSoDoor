package com.retrobit.gobacksodoor.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    @Test
    fun attackerCannotMoveIntoDefender() {
        val engine = GameEngine()
        engine.placeForTest(Cell(3, 2), listOf(Cell(4, 2)))

        assertFalse(engine.moveAttacker(Direction.RIGHT))
        assertEquals(Cell(3, 2), engine.attacker)
    }

    @Test
    fun attackerCompletesRoundByReturningFromGoal() {
        val engine = GameEngine(columns = 9, rows = 5)
        engine.placeForTest(Cell(0, 2), emptyList())

        repeat(8) { engine.moveAttacker(Direction.RIGHT) }
        assertEquals(RoundPhase.RUNNING, engine.phase)

        repeat(8) { engine.moveAttacker(Direction.LEFT) }
        assertEquals(RoundPhase.COMPLETED, engine.phase)
        assertEquals(1, engine.score)
        assertEquals(GameEngine.MAX_LIVES, engine.lives)
    }

    @Test
    fun attackerIsCaughtWhenDefenderIsAdjacentAfterMove() {
        val engine = GameEngine()
        engine.placeForTest(Cell(3, 2), listOf(Cell(5, 2)))

        assertTrue(engine.moveAttacker(Direction.RIGHT))
        assertEquals(RoundPhase.CAUGHT, engine.phase)
        assertEquals(GameEngine.MAX_LIVES - 1, engine.lives)
    }

    @Test
    fun defendersApproachAttacker() {
        val engine = GameEngine()
        engine.placeForTest(Cell(0, 2), listOf(Cell(6, 2)))

        engine.moveAttacker(Direction.DOWN) // penyerang kini di (0, 3)
        val distanceBeforeDefenderMove = Cell(6, 2).distanceTo(engine.attacker)
        val distanceAfter = engine.defenders.single().distanceTo(engine.attacker)
        assertTrue(distanceAfter < distanceBeforeDefenderMove)
    }

    @Test
    fun gameOverResetsScoreAndLives() {
        val engine = GameEngine()
        repeat(GameEngine.MAX_LIVES) {
            engine.placeForTest(Cell(3, 2), listOf(Cell(5, 2)))
            engine.moveAttacker(Direction.RIGHT)
            if (engine.lives > 0) engine.continueAfterRound()
        }
        assertTrue(engine.isGameOver)

        engine.continueAfterRound()
        assertEquals(0, engine.score)
        assertEquals(GameEngine.MAX_LIVES, engine.lives)
        assertEquals(RoundPhase.RUNNING, engine.phase)
    }
}
