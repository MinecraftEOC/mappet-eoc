package mchorse.mappet.client.gui.utils;

import mchorse.mappet.api.ui.components.UIDiceComponent;
import mchorse.mappet.api.ui.utils.DicePlayback;
import mchorse.mappet.api.ui.utils.DiceRollResult;

/** Layout shared by the game painter and offscreen visual checks. Coordinates scale with the die. */
public class DiceScene
{
    public static final String WIDGETS = "mappet:textures/gui/dice/widgets_v2/";
    public static final long LAST_FRAME = Long.MAX_VALUE;

    public interface Painter
    {
        void sprite(String texture, long elapsed, boolean loop, int cropX, int cropY, int cropW, int cropH,
                    float x, float y, float w, float h, float alpha);
    }

    private final Painter painter;

    public DiceScene(Painter painter)
    {
        this.painter = painter;
    }

    /** Fit the surrounding widgets on small GUI resolutions as well. */
    public static float fitScale(float requested, float centerX, float centerY, int screenWidth, int screenHeight)
    {
        float horizontal = Math.max(1, Math.min(centerX, screenWidth - centerX) - 8) / 216F;
        float vertical = Math.min(Math.max(1, centerY - 8) / 154F, Math.max(1, screenHeight - centerY - 8) / 120F);

        return Math.max(0.01F, Math.min(requested, Math.min(horizontal, vertical)));
    }

    public void draw(UIDiceComponent component, DicePlayback playback, float cx, float cy, float scale)
    {
        if (component.difficulty != null)
        {
            this.full(WIDGETS + "labels/difficulty_label.png", 0, false, cx - 216 * scale, cy - 154 * scale, 128 * scale, 128 * scale);
            this.blockNumber(component.difficulty, false, cx - 152 * scale, cy - 68 * scale, scale, 1F);
        }
        if (component.base != null)
        {
            this.full(WIDGETS + "labels/base_label.png", 0, false, cx + 88 * scale, cy - 146 * scale, 128 * scale, 128 * scale);
            this.blockNumber(component.base, true, cx + 152 * scale, cy - 68 * scale, scale, 1F);
        }

        DicePlayback.Phase phase = playback == null ? null : playback.phase();
        long elapsed = playback == null ? 0 : playback.phaseElapsed();
        String die = phase == DicePlayback.Phase.SPIN ? component.spinTexture : phase == DicePlayback.Phase.STOP ? component.stopTexture : component.restTexture;

        this.full(die, elapsed, phase == DicePlayback.Phase.SPIN, cx - 32 * scale, cy - 32 * scale, 64 * scale, 64 * scale);

        if (playback == null || playback.result == null || phase.ordinal() < DicePlayback.Phase.RESULT.ordinal())
        {
            return;
        }

        DiceRollResult result = playback.result;
        boolean summed = phase == DicePlayback.Phase.SUM || phase == DicePlayback.Phase.CAPTION || phase == DicePlayback.Phase.COMPLETE;
        long value = summed ? result.total : result.result;
        boolean gold = summed ? result.success : result.initialSuccess();
        long numberElapsed = phase == DicePlayback.Phase.RESULT || phase == DicePlayback.Phase.SUM ? elapsed : LAST_FRAME;
        long numberDuration = phase == DicePlayback.Phase.SUM ? playback.sumDuration : playback.resultDuration;

        this.resultNumber(value, gold, numberElapsed, numberDuration, cx, cy + scale, scale);

        if (phase == DicePlayback.Phase.BASE)
        {
            float progress = elapsed / (float) DicePlayback.TRANSFER_DURATION;
            float ease = progress * progress * (3 - 2 * progress);
            float flightScale = scale * (1 - 0.65F * ease);
            float x = cx + 152 * scale * (1 - ease);
            float y = cy - 52 * scale * (1 - ease) - (float) Math.sin(Math.PI * progress) * 24 * scale;
            float alpha = 1 - ease * ease;

            this.blockNumber(result.base, true, x, y - 16 * flightScale, flightScale, alpha);
        }

        if (phase == DicePlayback.Phase.CAPTION || phase == DicePlayback.Phase.COMPLETE)
        {
            this.full(captionTexture(result), phase == DicePlayback.Phase.COMPLETE ? LAST_FRAME : elapsed, false,
                    cx - 64 * scale, cy - 8 * scale, 128 * scale, 128 * scale);
        }
    }

    public static String captionTexture(DiceRollResult result)
    {
        return WIDGETS + "captions/animated/d20_" + result.caption + ".png";
    }

    public static String digitTexture(char digit, boolean gold)
    {
        return WIDGETS + "numbers/animated/" + (gold ? "gold" : "red") + "/d20_number_0" + digit + ".png";
    }

    private void full(String texture, long elapsed, boolean loop, float x, float y, float w, float h)
    {
        this.painter.sprite(texture, elapsed, loop, 0, 0, 0, 0, x, y, w, h, 1F);
    }

    private void blockNumber(int value, boolean plus, float cx, float top, float scale, float alpha)
    {
        String digits = Integer.toString(value);
        int count = digits.length() + (plus ? 1 : 0);
        float glyphScale = scale * Math.min(1F, 112F / (count * 26 + 2));
        float x = cx - (count * 26 + 2) * glyphScale / 2;

        if (plus)
        {
            this.painter.sprite(WIDGETS + "digits/base/base_plus.png", 0, false, 18, 24, 28, 32,
                    x, top, 28 * glyphScale, 32 * glyphScale, alpha);
            x += 26 * glyphScale;
        }

        String atlas = WIDGETS + "digits/" + (plus ? "base" : "difficulty") + "_digits_0_9.png";

        for (int i = 0; i < digits.length(); i++)
        {
            int digit = digits.charAt(i) - '0';

            this.painter.sprite(atlas, 0, false, digit * 64 + 18, 24, 28, 32,
                    x + i * 26 * glyphScale, top, 28 * glyphScale, 32 * glyphScale, alpha);
        }
    }

    private void resultNumber(long value, boolean gold, long elapsed, long duration, float cx, float cy, float scale)
    {
        String digits = Long.toString(value);
        float glyphScale = scale * Math.min(1F, 26F / ((digits.length() - 1) * 9 + 8));
        float progress = elapsed == LAST_FRAME ? 1F : Math.min(1F, elapsed / (float) duration);
        float spacing = (9 + 13 * (1 - progress) * (1 - progress)) * glyphScale;

        for (int i = 0; i < digits.length(); i++)
        {
            float x = cx + (i - (digits.length() - 1) / 2F) * spacing;

            this.painter.sprite(digitTexture(digits.charAt(i), gold), elapsed, false, 18, 18, 28, 30,
                    x - 14 * glyphScale, cy - 15 * glyphScale, 28 * glyphScale, 30 * glyphScale, 1F);
        }
    }
}
