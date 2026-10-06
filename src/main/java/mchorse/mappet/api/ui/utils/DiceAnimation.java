package mchorse.mappet.api.ui.utils;

/**
 * Timing calculations shared by dice rendering and headless checks.
 */
public class DiceAnimation
{
    /**
     * Round the spin portion up to a full cycle without overflowing long values.
     */
    public static long spinDuration(long requestedDuration, long cycleDuration, long stopDuration)
    {
        if (requestedDuration < 0 || cycleDuration <= 0 || stopDuration <= 0)
        {
            throw new IllegalArgumentException("Invalid dice animation durations");
        }

        long spin = Math.max(0, requestedDuration - stopDuration);
        long remainder = spin % cycleDuration;

        if (remainder == 0)
        {
            return spin;
        }

        long extra = cycleDuration - remainder;

        if (spin > Long.MAX_VALUE - stopDuration - extra)
        {
            throw new IllegalArgumentException("Dice duration is too large to round to a complete cycle");
        }

        return spin + extra;
    }
}
