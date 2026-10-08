package mchorse.mappet.client.gui.utils;

import mchorse.mappet.api.ui.components.UIDiceComponent;
import mchorse.mappet.api.ui.utils.DicePlayback;
import mchorse.mappet.api.ui.utils.DiceRollResult;

/** Compose positioned 480 by 600 layers; only single digits and the base flight need offsets. */
public class DiceScene
{
    public static final int REFERENCE_WIDTH = 480;
    public static final int REFERENCE_HEIGHT = 600;
    public static final String WIDGETS = UIDiceComponent.TEXTURES;
    public static final long LAST_FRAME = Long.MAX_VALUE;

    public interface Painter
    {
        default void background(int x, int y, int width, int height, int color)
        {}

        /** Offset in source-canvas pixels, based on the final digit's visible bounds. */
        default float centerOffsetX(String texture, float center, float fallback)
        {
            return fallback;
        }

        void sprite(String texture, long elapsed, boolean loop, int cropX, int cropY, int cropW, int cropH,
                    float x, float y, float w, float h, float alpha);
    }

    private final Painter painter;

    public DiceScene(Painter painter)
    {
        this.painter = painter;
    }

    public void drawPanel(UIDiceComponent component, DicePlayback playback, int x, int y, int width, int height)
    {
        if (width <= 0 || height <= 0) return;

        this.painter.background(x, y, width, height, component.background);
        float scale = Math.min(width / (float) REFERENCE_WIDTH, height / (float) REFERENCE_HEIGHT);

        this.draw(component, playback, x + (width - REFERENCE_WIDTH * scale) / 2,
                y + (height - REFERENCE_HEIGHT * scale) / 2, scale);
    }

    /** x and y are the shared upper-left corner of the full-canvas layers. */
    public void draw(UIDiceComponent component, DicePlayback playback, float x, float y, float scale)
    {
        if (component.difficulty != null)
        {
            this.layer(WIDGETS + "labels/difficulty.png", 0, false, x, y, scale, 1F);
            this.blockNumber(component.difficulty, false, x, y, scale, 1F);
        }
        if (component.base != null)
        {
            this.layer(WIDGETS + "labels/parameter_skill.png", 0, false, x, y, scale, 1F);
            this.blockNumber(component.base, true, x, y, scale, 1F);
        }

        DicePlayback.Phase phase = playback == null ? null : playback.phase();
        long elapsed = playback == null ? 0 : playback.phaseElapsed();
        String die = phase == DicePlayback.Phase.SPIN ? component.spinTexture :
                phase == DicePlayback.Phase.STOP ? component.stopTexture : component.restTexture;

        this.layer(die, elapsed, phase == DicePlayback.Phase.SPIN, x, y, scale, 1F);

        if (playback == null || playback.result == null || phase.ordinal() < DicePlayback.Phase.RESULT.ordinal()) return;

        DiceRollResult result = playback.result;
        boolean summed = phase == DicePlayback.Phase.SUM || phase == DicePlayback.Phase.CAPTION || phase == DicePlayback.Phase.COMPLETE;
        int value = (int) (summed ? result.total : result.result);
        boolean gold = summed ? result.success : result.initialSuccess();
        boolean revealing = phase == DicePlayback.Phase.RESULT || phase == DicePlayback.Phase.SUM;

        this.resultNumber(value, gold, revealing, elapsed, x, y, scale);

        if (phase == DicePlayback.Phase.BASE)
        {
            float progress = elapsed / (float) DicePlayback.TRANSFER_DURATION;
            float ease = progress * progress * (3 - 2 * progress);
            float flightScale = scale * (1 - 0.65F * ease);
            float pivotX = 366 + (240 - 366) * ease;
            float pivotY = 114 + (288 - 114) * ease - (float) Math.sin(Math.PI * progress) * 24;
            float alpha = 1 - ease * ease;

            // Transform the plus and all digits as one copy around their shared source pivot.
            this.blockNumber(result.base, true, x + pivotX * scale - 366 * flightScale,
                    y + pivotY * scale - 114 * flightScale, flightScale, alpha);
        }

        if (phase == DicePlayback.Phase.CAPTION || phase == DicePlayback.Phase.COMPLETE)
        {
            boolean animated = phase == DicePlayback.Phase.CAPTION;
            this.layer(captionTexture(result, animated), animated ? elapsed : 0, false, x, y, scale, 1F);
        }
    }

    public static String captionTexture(DiceRollResult result)
    {
        return captionTexture(result, true);
    }

    public static String captionTexture(DiceRollResult result, boolean animated)
    {
        return WIDGETS + "results/" + (animated ? "animated/" : "static/") + result.caption + ".png";
    }

    public static String digitTexture(char digit, boolean gold, int position, boolean animated)
    {
        return WIDGETS + "die_numbers/" + (gold ? "gold" : "red") + "/position_" + position
                + "/digit_" + digit + (animated ? "_fade" : "") + ".png";
    }

    private void layer(String texture, long elapsed, boolean loop, float x, float y, float scale, float alpha)
    {
        this.painter.sprite(texture, elapsed, loop, 0, 0, 0, 0, x, y,
                REFERENCE_WIDTH * scale, REFERENCE_HEIGHT * scale, alpha);
    }

    private void centeredLayer(String texture, long elapsed, float center, float fallback,
                               float x, float y, float scale, float alpha)
    {
        int shift = Math.round(this.painter.centerOffsetX(texture, center, fallback));
        int cropX = Math.max(0, -shift);
        int cropW = REFERENCE_WIDTH - Math.abs(shift);

        // Clip only the translated transparent margin, keeping the quad inside the original canvas.
        this.painter.sprite(texture, elapsed, false, cropX, 0, cropW, REFERENCE_HEIGHT,
                x + Math.max(0, shift) * scale, y, cropW * scale, REFERENCE_HEIGHT * scale, alpha);
    }

    private void blockNumber(int value, boolean plus, float x, float y, float scale, float alpha)
    {
        String directory = WIDGETS + "values/" + (plus ? "parameter_skill" : "difficulty") + "/";
        if (plus) this.layer(directory + "plus.png", 0, false, x, y, scale, alpha);

        int first = value < 10 ? value : value / 10;
        String texture = directory + "position_1/digit_" + first + ".png";

        if (!plus && value < 10)
        {
            this.centeredLayer(texture, 0, 114, 10, x, y, scale, alpha);
        }
        else
        {
            this.layer(texture, 0, false, x, y, scale, alpha);
        }
        if (value >= 10)
        {
            this.layer(directory + "position_2/digit_" + value % 10 + ".png", 0, false, x, y, scale, alpha);
        }
    }

    private void resultNumber(int value, boolean gold, boolean animated, long elapsed, float x, float y, float scale)
    {
        int first = value < 10 ? value : value / 10;
        String texture = digitTexture((char) ('0' + first), gold, 1, animated);

        if (value < 10)
        {
            this.centeredLayer(texture, elapsed, 240, 14, x, y, scale, 1F);
        }
        else
        {
            this.layer(texture, elapsed, false, x, y, scale, 1F);
            this.layer(digitTexture((char) ('0' + value % 10), gold, 2, animated), elapsed, false, x, y, scale, 1F);
        }
    }
}
