package mchorse.mappet.api.ui.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class DicePlaybackTest
{
    private DicePlayback playback(int difficulty, int result, int base)
    {
        return new DicePlayback(2400, 400, 1600, DiceRollResult.resolve(difficulty, result, base), 500, 500, 500);
    }

    @Test
    public void normalCheckFollowsEveryPhaseAndFinishesAfterCaption()
    {
        DicePlayback roll = playback(15, 12, 5);

        assertEquals(4500, roll.totalDuration);
        assertEquals(DicePlayback.Phase.SPIN, roll.phase());
        roll.advance(800);
        assertEquals(DicePlayback.Phase.STOP, roll.phase());
        roll.advance(2400);
        assertEquals(DicePlayback.Phase.RESULT, roll.phase());
        roll.advance(2900);
        assertEquals(DicePlayback.Phase.BASE, roll.phase());
        roll.advance(3200);
        assertEquals(300, roll.phaseElapsed());
        roll.advance(3500);
        assertEquals(DicePlayback.Phase.SUM, roll.phase());
        roll.advance(4000);
        assertEquals(DicePlayback.Phase.CAPTION, roll.phase());
        assertFalse(roll.consumeCompletion());
        roll.advance(4499);
        assertFalse(roll.consumeCompletion());
        roll.advance(4500);
        assertEquals(DicePlayback.Phase.COMPLETE, roll.phase());
        assertTrue(roll.consumeCompletion());
        assertFalse(roll.consumeCompletion());
        assertFalse(roll.skipped());
    }

    @Test
    public void criticalsIgnoreBaseDifficultyAndTransferPhases()
    {
        DicePlayback failure = playback(0, 1, 99);
        DicePlayback success = playback(99, 20, 99);

        assertEquals(1, failure.result.total);
        assertEquals(20, success.result.total);
        assertFalse(failure.result.success);
        assertFalse(failure.result.initialSuccess());
        assertTrue(success.result.success);
        assertTrue(success.result.initialSuccess());
        assertEquals("critical_failure", failure.result.caption);
        assertEquals("critical_success", success.result.caption);
        assertEquals(3400, failure.totalDuration);
        failure.advance(2900);
        success.advance(2900);
        assertEquals(DicePlayback.Phase.CAPTION, failure.phase());
        assertEquals(DicePlayback.Phase.CAPTION, success.phase());
    }

    @Test
    public void addingBaseCanTurnRedIntoGoldAndEqualityPasses()
    {
        DiceRollResult result = DiceRollResult.resolve(15, 10, 5);

        assertFalse(result.initialSuccess());
        assertTrue(result.success);
        assertEquals(15, result.total);
        assertEquals("success", result.caption);
        assertFalse(DiceRollResult.resolve(15, 9, 5).success);
        assertTrue(DiceRollResult.resolve(0, 2, 0).success);
    }

    @Test
    public void skipFromEveryPhaseHasOneCompletionAndTheSameFinalOutcome()
    {
        for (long time : new long[] {0, 1000, 2500, 3000, 3700, 4200})
        {
            DicePlayback roll = playback(15, 12, 5);

            roll.advance(time);
            assertTrue(roll.skip());
            assertEquals(DicePlayback.Phase.COMPLETE, roll.phase());
            assertEquals(17, roll.result.total);
            assertTrue(roll.result.success);
            assertTrue(roll.skipped());
            assertTrue(roll.consumeCompletion());
            assertFalse(roll.skip());
            roll.advance(100000);
            assertFalse(roll.consumeCompletion());
        }
    }

    @Test
    public void skippingCriticalsKeepsNaturalNumbers()
    {
        for (int natural : new int[] {1, 20})
        {
            DicePlayback roll = playback(99, natural, 99);

            roll.skip();
            assertEquals(natural, roll.result.total);
            assertTrue(roll.result.critical);
        }
    }

    @Test
    public void lowFrameRateCanJumpToFinishAndIndependentRollsDoNotShareCompletion()
    {
        DicePlayback first = playback(15, 12, 5);
        DicePlayback second = playback(15, 12, 5);

        first.advance(100000);
        assertTrue(first.consumeCompletion());
        assertEquals(DicePlayback.Phase.SPIN, second.phase());
        second.advance(4500);
        assertTrue(second.consumeCompletion());
    }

    @Test
    public void readsEffectDurationsInsteadOfAssuming500Milliseconds()
    {
        DicePlayback roll = new DicePlayback(2400, 400, 1600, DiceRollResult.resolve(15, 12, 5), 700, 900, 1100);

        assertEquals(5700, roll.totalDuration);
        roll.advance(3100);
        assertEquals(DicePlayback.Phase.BASE, roll.phase());
        roll.advance(3700);
        assertEquals(DicePlayback.Phase.SUM, roll.phase());
        roll.advance(4600);
        assertEquals(DicePlayback.Phase.CAPTION, roll.phase());
    }

    @Test
    public void legacySpinAndStopFinishWithoutCheckEffects()
    {
        DicePlayback roll = new DicePlayback(2400, 400, 1600, null, 500, 500, 500);

        assertEquals(2400, roll.totalDuration);
        roll.advance(2400);
        assertEquals(DicePlayback.Phase.COMPLETE, roll.phase());
        assertTrue(roll.consumeCompletion());
    }

    @Test
    public void zeroBaseUsesTheNormalSequenceAndTwoDigitBoundaryIsSupported()
    {
        DicePlayback zero = playback(15, 12, 0);

        zero.advance(2900);
        assertEquals(DicePlayback.Phase.BASE, zero.phase());
        assertEquals(99, DiceRollResult.resolve(99, 19, 80).total);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsResultOutsideD20Range()
    {
        DiceRollResult.resolve(15, 21, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsThreeDigitSum()
    {
        DiceRollResult.resolve(99, 19, 81);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsThreeDigitDifficultyEvenForCriticals()
    {
        DiceRollResult.resolve(100, 20, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsThreeDigitBaseEvenForCriticals()
    {
        DiceRollResult.resolve(0, 1, 100);
    }
}
