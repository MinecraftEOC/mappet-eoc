package mchorse.mappet.api.ui.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DiceAnimationTest
{
    @Test
    public void subtractsStopAndRoundsSpinUpToCompleteCycles()
    {
        assertEquals(400, DiceAnimation.spinDuration(2000, 400, 1600));
        assertEquals(800, DiceAnimation.spinDuration(2400, 400, 1600));
        assertEquals(1200, DiceAnimation.spinDuration(2500, 400, 1600));
        assertEquals(400, DiceAnimation.spinDuration(1601, 400, 1600));
    }

    @Test
    public void shortRequestsStillPlayTheWholeStop()
    {
        assertEquals(0, DiceAnimation.spinDuration(0, 400, 1600));
        assertEquals(0, DiceAnimation.spinDuration(1000, 400, 1600));
        assertEquals(0, DiceAnimation.spinDuration(1600, 400, 1600));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeDuration()
    {
        DiceAnimation.spinDuration(-1, 400, 1600);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroCycle()
    {
        DiceAnimation.spinDuration(2000, 0, 1600);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOverflowDuringRounding()
    {
        DiceAnimation.spinDuration(Long.MAX_VALUE, 400, 1600);
    }
}
