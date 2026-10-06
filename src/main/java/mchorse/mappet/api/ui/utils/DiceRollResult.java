package mchorse.mappet.api.ui.utils;

/** Immutable check parameters and outcome, independent of rendering. */
public class DiceRollResult
{
    public final int difficulty;
    public final int result;
    public final int base;
    public final long total;
    public final boolean critical;
    public final boolean success;
    public final String caption;

    public static DiceRollResult resolve(int difficulty, int result, int base)
    {
        if (difficulty < 0 || base < 0 || result < 1 || result > 20)
        {
            throw new IllegalArgumentException("Dice checks require nonnegative difficulty/base and a result from 1 to 20");
        }

        return new DiceRollResult(difficulty, result, base);
    }

    private DiceRollResult(int difficulty, int result, int base)
    {
        this.difficulty = difficulty;
        this.result = result;
        this.base = base;
        this.critical = result == 1 || result == 20;
        this.total = this.critical ? result : (long) result + base;
        this.success = result == 20 || result != 1 && this.total >= difficulty;
        this.caption = this.critical ? this.success ? "critical_success" : "critical_failure" : this.success ? "success" : "failure";
    }

    public boolean initialSuccess()
    {
        return this.critical ? this.success : this.result >= this.difficulty;
    }
}
