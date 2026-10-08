package mchorse.mappet.client.gui.utils;

import mchorse.mappet.api.ui.components.UIDiceComponent;
import mchorse.mappet.api.ui.utils.DicePlayback;
import mchorse.mappet.api.ui.utils.DiceRollResult;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class DicePanelTest
{
    @Test
    public void fullCanvasLayersFitThePanelAndFlightUsesItsClip()
    {
        int[][] sizes = {{240, 300}, {480, 600}, {300, 300}, {128, 128}, {128, 32}, {32, 128}, {1, 1}};
        int[][] checks = {{15, 12, 5}, {15, 9, 5}, {0, 1, 99}, {99, 20, 99}, {99, 19, 80}};

        for (int[] size : sizes)
        {
            for (int[] check : checks)
            {
                UIDiceComponent component = new UIDiceComponent().difficulty(check[0]).base(check[2]);
                DicePlayback roll = new DicePlayback(2400, 400, 1600, DiceRollResult.resolve(check[0], check[1], check[2]), 500, 500, 500);
                DiceScene scene = new DiceScene((path, time, loop, u, v, sw, sh, x, y, w, h, alpha) ->
                {
                    assertTrue(Float.isFinite(x) && Float.isFinite(y));
                    assertTrue(w > 0 && h > 0);
                    assertTrue(alpha >= 0 && alpha <= 1);
                    // The flight's transparent canvas is transformed, and clipped by GuiDice.
                    if (alpha < 1) return;
                    assertTrue(path + " left", x >= 37 - 0.001F);
                    assertTrue(path + " top", y >= 23 - 0.001F);
                    assertTrue(path + " right", x + w <= 37 + size[0] + 0.001F);
                    assertTrue(path + " bottom", y + h <= 23 + size[1] + 0.001F);
                });

                scene.drawPanel(component, null, 37, 23, size[0], size[1]);
                for (long time = 0; time <= 4600; time += 50)
                {
                    roll.advance(time);
                    scene.drawPanel(component, roll, 37, 23, size[0], size[1]);
                }
            }
        }
    }

    @Test
    public void panelBackgroundUsesExactFrameAndCanBeTransparent()
    {
        UIDiceComponent component = new UIDiceComponent();
        List<Integer> colors = new ArrayList<Integer>();
        DiceScene scene = new DiceScene(new DiceScene.Painter()
        {
            @Override
            public void background(int x, int y, int width, int height, int color)
            {
                assertEquals(73, x);
                assertEquals(19, y);
                assertEquals(128, width);
                assertEquals(128, height);
                colors.add(color);
            }

            @Override
            public void sprite(String path, long time, boolean loop, int u, int v, int sw, int sh,
                               float x, float y, float w, float h, float alpha)
            {}
        });

        scene.drawPanel(component, null, 73, 19, 128, 128);
        component.background(0);
        scene.drawPanel(component, null, 73, 19, 128, 128);
        assertEquals(Integer.valueOf(0x88000000), colors.get(0));
        assertEquals(Integer.valueOf(0), colors.get(1));
    }

    @Test
    public void translatingPanelDoesNotChangeItsScaleOrPlacementWithinFrame()
    {
        UIDiceComponent component = new UIDiceComponent().difficulty(15).base(5);
        List<float[]> bounds = new ArrayList<float[]>();
        DiceScene scene = new DiceScene((path, time, loop, u, v, sw, sh, x, y, w, h, alpha) ->
                bounds.add(new float[] {x, y, w, h}));
        scene.drawPanel(component, null, 0, 0, 128, 128);
        int count = bounds.size();
        scene.drawPanel(component, null, 1900, -100, 128, 128);
        assertEquals(count * 2, bounds.size());

        for (int i = 0; i < count; i++)
        {
            float[] first = bounds.get(i);
            float[] shifted = bounds.get(i + count);
            assertEquals(first[0] + 1900, shifted[0], 0.001F);
            assertEquals(first[1] - 100, shifted[1], 0.001F);
            assertEquals(first[2], shifted[2], 0F);
            assertEquals(first[3], shifted[3], 0F);
        }

        float[] die = bounds.get(count - 1);
        assertEquals(102.4F, die[2], 0.001F);
        assertEquals(128F, die[3], 0.001F);
    }

    @Test
    public void twoDigitLayersShareExactlyTheSameCanvasWithoutLayoutOffsets()
    {
        UIDiceComponent component = new UIDiceComponent().difficulty(15).base(15);
        DicePlayback roll = new DicePlayback(2400, 400, 1600, DiceRollResult.resolve(15, 12, 15), 500, 500, 500);
        roll.skip();
        DiceScene scene = new DiceScene((path, time, loop, u, v, sw, sh, x, y, w, h, alpha) ->
        {
            assertEquals(37, x, 0F);
            assertEquals(23, y, 0F);
            assertEquals(240, w, 0F);
            assertEquals(300, h, 0F);
            assertEquals(0, u);
            assertEquals(0, v);
        });
        scene.drawPanel(component, roll, 37, 23, 240, 300);
    }

    @Test
    public void zeroOrNegativeFrameDoesNotDraw()
    {
        DiceScene scene = new DiceScene(new DiceScene.Painter()
        {
            @Override
            public void background(int x, int y, int width, int height, int color)
            {
                fail("An empty panel must not draw a background");
            }

            @Override
            public void sprite(String path, long time, boolean loop, int u, int v, int sw, int sh,
                               float x, float y, float w, float h, float alpha)
            {
                fail("An empty panel must not draw sprites");
            }
        });

        UIDiceComponent component = new UIDiceComponent();
        scene.drawPanel(component, null, 0, 0, 0, 128);
        scene.drawPanel(component, null, 0, 0, 128, 0);
        scene.drawPanel(component, null, 0, 0, -1, 128);
        scene.drawPanel(component, null, 0, 0, 128, -1);
    }
}
