package mchorse.mappet.api.ui.utils;

/** Per-roll timeline. Rendering and event emission can each run without advancing it twice. */
public class DicePlayback
{
    public static final long TRANSFER_DURATION = 600;

    public enum Phase { SPIN, STOP, RESULT, BASE, SUM, CAPTION, COMPLETE }

    public final DiceRollResult result;
    public final long spinDuration;
    public final long stopDuration;
    public final long resultDuration;
    public final long sumDuration;
    public final long captionDuration;
    public final long totalDuration;

    private Phase phase;
    private long phaseElapsed;
    private boolean skipped;
    private boolean reported;

    public DicePlayback(long requested, long spinCycle, long stop, DiceRollResult result, long reveal, long sumReveal, long caption)
    {
        if (reveal <= 0 || sumReveal <= 0 || caption <= 0)
        {
            throw new IllegalArgumentException("Dice effect durations must be positive");
        }

        this.result = result;
        this.spinDuration = DiceAnimation.spinDuration(requested, spinCycle, stop);
        this.stopDuration = stop;
        this.resultDuration = reveal;
        this.sumDuration = sumReveal;
        this.captionDuration = caption;

        long effects = result == null ? 0 : Math.addExact(reveal, caption);

        if (result != null && !result.critical)
        {
            effects = Math.addExact(effects, Math.addExact(TRANSFER_DURATION, sumReveal));
        }

        this.totalDuration = Math.addExact(Math.addExact(this.spinDuration, stop), effects);
        this.advance(0);
    }

    public void advance(long elapsed)
    {
        if (this.phase == Phase.COMPLETE)
        {
            return;
        }

        long time = Math.max(0, elapsed);

        if (this.select(Phase.SPIN, time, this.spinDuration)) return;
        time -= this.spinDuration;
        if (this.select(Phase.STOP, time, this.stopDuration)) return;
        time -= this.stopDuration;

        if (this.result != null)
        {
            if (this.select(Phase.RESULT, time, this.resultDuration)) return;
            time -= this.resultDuration;

            if (!this.result.critical)
            {
                if (this.select(Phase.BASE, time, TRANSFER_DURATION)) return;
                time -= TRANSFER_DURATION;
                if (this.select(Phase.SUM, time, this.sumDuration)) return;
                time -= this.sumDuration;
            }

            if (this.select(Phase.CAPTION, time, this.captionDuration)) return;
        }

        this.phase = Phase.COMPLETE;
        this.phaseElapsed = 0;
    }

    private boolean select(Phase phase, long elapsed, long duration)
    {
        if (elapsed < duration)
        {
            this.phase = phase;
            this.phaseElapsed = elapsed;

            return true;
        }

        return false;
    }

    public Phase phase()
    {
        return this.phase;
    }

    public long phaseElapsed()
    {
        return this.phaseElapsed;
    }

    public boolean skip()
    {
        if (this.phase == Phase.COMPLETE)
        {
            return false;
        }

        this.skipped = true;
        this.phase = Phase.COMPLETE;
        this.phaseElapsed = 0;

        return true;
    }

    public boolean skipped()
    {
        return this.skipped;
    }

    public boolean consumeCompletion()
    {
        if (this.phase != Phase.COMPLETE || this.reported)
        {
            return false;
        }

        this.reported = true;

        return true;
    }
}
