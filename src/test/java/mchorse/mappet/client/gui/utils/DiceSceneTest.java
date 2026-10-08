package mchorse.mappet.client.gui.utils;

import com.google.gson.JsonParser;
import mchorse.mappet.api.ui.components.UIDiceComponent;
import mchorse.mappet.api.ui.utils.DicePlayback;
import mchorse.mappet.api.ui.utils.DiceRollResult;
import mchorse.mappet.client.gui.utils.graphics.SpriteSheet;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.client.resources.data.AnimationMetadataSectionSerializer;
import net.minecraft.client.resources.data.MetadataSerializer;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DiceSceneTest
{
    private DicePlayback roll(int difficulty, int result, int base)
    {
        return new DicePlayback(2400, 400, 1600, DiceRollResult.resolve(difficulty, result, base), 500, 500, 500);
    }

    @Test
    public void sceneChangesFromRedRawNumberToGoldSumAndKeepsBaseBlock()
    {
        UIDiceComponent component = new UIDiceComponent().difficulty(15).base(5);
        DicePlayback roll = roll(15, 12, 5);
        List<String> paths = new ArrayList<String>();
        DiceScene scene = new DiceScene((path, time, loop, u, v, sw, sh, x, y, w, h, a) -> paths.add(path));

        roll.advance(2650);
        scene.draw(component, roll, 0, 0, 1);
        assertTrue(paths.contains(DiceScene.digitTexture('1', false, 1, true)));
        assertTrue(paths.contains(DiceScene.digitTexture('2', false, 2, true)));
        assertFalse(paths.contains(DiceScene.captionTexture(roll.result)));

        paths.clear();
        roll.advance(4500);
        scene.draw(component, roll, 0, 0, 1);
        assertTrue(paths.contains(DiceScene.digitTexture('1', true, 1, false)));
        assertTrue(paths.contains(DiceScene.digitTexture('7', true, 2, false)));
        assertTrue(paths.contains(DiceScene.captionTexture(roll.result, false)));
        assertEquals(1, paths.stream().filter(path -> path.endsWith("parameter_skill/plus.png")).count());
    }

    @Test
    public void flightAddsASecondPlusAndCriticalsDoNot()
    {
        List<String> paths = new ArrayList<String>();
        DiceScene scene = new DiceScene((path, time, loop, u, v, sw, sh, x, y, w, h, a) -> paths.add(path));
        UIDiceComponent component = new UIDiceComponent().difficulty(15).base(5);
        DicePlayback normal = roll(15, 12, 5);

        normal.advance(3200);
        scene.draw(component, normal, 0, 0, 1);
        assertEquals(2, paths.stream().filter(path -> path.endsWith("parameter_skill/plus.png")).count());

        paths.clear();
        DicePlayback critical = roll(99, 20, 5);

        critical.advance(3200);
        scene.draw(component, critical, 0, 0, 1);
        assertEquals(1, paths.stream().filter(path -> path.endsWith("parameter_skill/plus.png")).count());
        assertTrue(paths.contains(DiceScene.digitTexture('2', true, 1, false)));
        assertTrue(paths.contains(DiceScene.digitTexture('0', true, 2, false)));
    }

    @Test
    public void idleDoesNotShowAnyResultAndSkipFreezesTheFinalNumberAndCaption()
    {
        UIDiceComponent component = new UIDiceComponent().difficulty(15).base(5);
        List<String> paths = new ArrayList<String>();
        List<Long> times = new ArrayList<Long>();
        DiceScene scene = new DiceScene((path, time, loop, u, v, sw, sh, x, y, w, h, a) ->
        {
            paths.add(path);
            times.add(time);
        });

        scene.draw(component, null, 0, 0, 1);
        assertFalse(paths.stream().anyMatch(path -> path.contains("die_numbers/") || path.contains("results/")));
        assertTrue(paths.contains(component.restTexture));

        paths.clear();
        times.clear();
        DicePlayback roll = roll(15, 9, 5);

        roll.skip();
        scene.draw(component, roll, 0, 0, 1);
        assertTrue(paths.contains(DiceScene.digitTexture('1', false, 1, false)));
        assertTrue(paths.contains(DiceScene.digitTexture('4', false, 2, false)));
        assertTrue(paths.contains(DiceScene.captionTexture(roll.result, false)));

        for (int i = 0; i < paths.size(); i++)
        {
            assertFalse(paths.get(i).contains("/animated/") || paths.get(i).endsWith("_fade.png"));
            assertEquals(Long.valueOf(0), times.get(i));
        }
    }

    @Test
    public void singleDieAndDifficultyAreCenteredButBaseStaysInFirstPosition()
    {
        UIDiceComponent component = new UIDiceComponent().difficulty(8).base(5);
        DicePlayback single = roll(8, 9, 5);
        single.advance(2650);
        Map<String, float[]> bounds = new HashMap<String, float[]>();
        DiceScene scene = new DiceScene((path, time, loop, u, v, sw, sh, x, y, w, h, alpha) ->
                bounds.put(path, new float[] {x, y, w, h}));
        scene.draw(component, single, 0, 0, 1);
        assertEquals(10, bounds.get(DiceScene.WIDGETS + "values/difficulty/position_1/digit_8.png")[0], 0F);
        assertEquals(14, bounds.get(DiceScene.digitTexture('9', true, 1, true))[0], 0F);
        assertEquals(0, bounds.get(DiceScene.WIDGETS + "values/parameter_skill/position_1/digit_5.png")[0], 0F);
        assertFalse(bounds.keySet().stream().anyMatch(path -> path.contains("position_2/")));

        bounds.clear();
        single.advance(3750);
        scene.draw(component, single, 0, 0, 1);
        assertEquals(0, bounds.get(DiceScene.digitTexture('1', true, 1, true))[0], 0F);
        assertEquals(0, bounds.get(DiceScene.digitTexture('4', true, 2, true))[0], 0F);
    }

    /** Render the very same scene commands offscreen, so layout can be inspected without launching Minecraft. */
    @Test
    public void renderLayoutAndPhasePreviewsWithRealBundledAssets() throws Exception
    {
        File directory = new File("dice-previews");

        assertTrue(directory.isDirectory() || directory.mkdirs());
        UIDiceComponent component = new UIDiceComponent().difficulty(15).base(5);
        String[] names = {"idle", "raw-red", "base-flight", "sum-gold", "success", "critical-success"};
        long[] times = {-1, 2650, 3200, 3750, 4500, 3400};
        BufferedImage contact = new BufferedImage(1440, 1252, BufferedImage.TYPE_INT_RGB);
        Graphics2D contactPainter = contact.createGraphics();

        contactPainter.setColor(Color.BLACK);
        contactPainter.fillRect(0, 0, contact.getWidth(), contact.getHeight());

        for (int i = 0; i < names.length; i++)
        {
            DicePlayback playback = times[i] < 0 ? null : roll(i == 5 ? 99 : 15, i == 5 ? 20 : 12, 5);

            if (playback != null) playback.advance(times[i]);
            BufferedImage preview = render(i == 5 ? new UIDiceComponent().difficulty(99).base(5) : component, playback, 480, 600);

            ImageIO.write(preview, "png", new File(directory, names[i] + ".png"));
            int x = i % 3 * 480;
            int y = i / 3 * 626;

            contactPainter.setColor(Color.LIGHT_GRAY);
            contactPainter.drawString(names[i], x + 10, y + 17);
            contactPainter.drawImage(preview, x, y + 26, null);
        }

        contactPainter.dispose();
        ImageIO.write(contact, "png", new File(directory, "phases.png"));

        DicePlayback failure = roll(15, 9, 5);
        failure.skip();
        ImageIO.write(render(component, failure, 480, 600), "png", new File(directory, "failure.png"));
        DicePlayback criticalFailure = roll(0, 1, 99);
        criticalFailure.skip();
        ImageIO.write(render(new UIDiceComponent().difficulty(0).base(99), criticalFailure, 480, 600), "png", new File(directory, "critical-failure.png"));
        DicePlayback big = roll(25, 19, 50);
        big.skip();
        ImageIO.write(render(new UIDiceComponent().difficulty(25).base(50), big, 480, 600), "png", new File(directory, "scaled.png"));
        ImageIO.write(render(component, failure, 240, 300), "png", new File(directory, "default-panel.png"));
        ImageIO.write(render(component, failure, 128, 128), "png", new File(directory, "panel-128.png"));
        DicePlayback flight = roll(15, 12, 5);
        flight.advance(3200);
        BufferedImage bounded = render(component, flight, 512, 320, 192, 96, 128, 128);
        ImageIO.write(bounded, "png", new File(directory, "bounded-panel-128.png"));
        assertEquals(new Color(86, 98, 112).getRGB(), bounded.getRGB(191, 96));
        assertNotEquals(bounded.getRGB(191, 96), bounded.getRGB(192, 96));
        assertEquals(new Color(86, 98, 112).getRGB(), bounded.getRGB(320, 223));
        assertEquals(new Color(86, 98, 112).getRGB(), bounded.getRGB(192, 224));
        ImageIO.write(render(component, failure, 512, 320, 96, 60, 320, 200), "png", new File(directory, "bounded-panel-320.png"));

        DicePlayback single = roll(8, 9, 0);
        single.skip();
        ImageIO.write(render(new UIDiceComponent().difficulty(8).base(0), single, 480, 600), "png", new File(directory, "single-digit.png"));
        BufferedImage variants = new BufferedImage(960, 652, BufferedImage.TYPE_INT_RGB);
        Graphics2D variantPainter = variants.createGraphics();
        String[] colors = {"original", "blue", "burgundy", "green", "purple", "red", "violet", "yellow"};
        for (int i = 0; i < colors.length; i++)
        {
            int x = i % 4 * 240;
            int y = i / 4 * 326;
            variantPainter.setColor(Color.LIGHT_GRAY);
            variantPainter.drawString(colors[i], x + 10, y + 17);
            variantPainter.drawImage(render(new UIDiceComponent().variant(colors[i]).difficulty(15).base(5), failure, 240, 300), x, y + 26, null);
        }
        variantPainter.dispose();
        ImageIO.write(variants, "png", new File(directory, "variants.png"));
    }

    private BufferedImage render(UIDiceComponent component, DicePlayback playback, int width, int height)
    {
        return this.render(component, playback, width, height, 0, 0, width, height);
    }

    private BufferedImage render(UIDiceComponent component, DicePlayback playback, int width, int height,
                                 int panelX, int panelY, int panelWidth, int panelHeight)
    {
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        Map<String, BufferedImage> images = new HashMap<String, BufferedImage>();
        Map<String, SpriteSheet> sheets = new HashMap<String, SpriteSheet>();
        MetadataSerializer serializer = new MetadataSerializer();

        graphics.setColor(new Color(86, 98, 112));
        graphics.fillRect(0, 0, width, height);
        graphics.setClip(panelX, panelY, panelWidth, panelHeight);
        serializer.registerMetadataSectionType(new AnimationMetadataSectionSerializer(), AnimationMetadataSection.class);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        DiceScene scene = new DiceScene(new DiceScene.Painter()
        {
            private SpriteSheet load(String path)
            {
                if (!images.containsKey(path))
                {
                    String resource = "/assets/" + path.replace(':', '/');
                    try (InputStream imageStream = getClass().getResourceAsStream(resource);
                         InputStream metadataStream = getClass().getResourceAsStream(resource + ".mcmeta"))
                    {
                        assertNotNull("Missing bundled image " + path, imageStream);
                        BufferedImage image = ImageIO.read(imageStream);
                        AnimationMetadataSection metadata = null;
                        if (metadataStream != null)
                        {
                            try (InputStreamReader reader = new InputStreamReader(metadataStream, "UTF-8"))
                            {
                                metadata = serializer.parseMetadataSection("animation", new JsonParser().parse(reader).getAsJsonObject());
                            }
                        }
                        SpriteSheet sheet = new SpriteSheet(image.getWidth(), image.getHeight(), metadata);
                        sheet.captureInkCenter(image);
                        images.put(path, image);
                        sheets.put(path, sheet);
                    }
                    catch (Exception e)
                    {
                        throw new AssertionError("Cannot load " + path, e);
                    }
                }
                return sheets.get(path);
            }

            @Override
            public float centerOffsetX(String path, float center, float fallback)
            {
                return this.load(path).centeredOffsetX(center, fallback);
            }

            @Override
            public void background(int x, int y, int w, int h, int color)
            {
                graphics.setComposite(AlphaComposite.SrcOver);
                graphics.setColor(new Color(color, true));
                graphics.fillRect(x, y, w, h);
            }

            @Override
            public void sprite(String path, long time, boolean loop, int u, int v, int sw, int sh,
                               float x, float y, float w, float h, float alpha)
            {
                try
                {
                    SpriteSheet sheet = this.load(path);
                    BufferedImage image = images.get(path);
                    int frame = sheet.frameAt(time, loop);
                    int cropWidth = sw == 0 ? sheet.frameWidth : sw;
                    int cropHeight = sh == 0 ? sheet.frameHeight : sh;
                    int left = frame % (sheet.width / sheet.frameWidth) * sheet.frameWidth + u;
                    int top = frame / (sheet.width / sheet.frameWidth) * sheet.frameHeight + v;

                    assertTrue(left + cropWidth <= image.getWidth());
                    assertTrue(top + cropHeight <= image.getHeight());
                    graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    graphics.drawImage(image, Math.round(x), Math.round(y), Math.round(x) + Math.max(1, Math.round(w)), Math.round(y) + Math.max(1, Math.round(h)),
                            left, top, left + cropWidth, top + cropHeight, null);
                }
                catch (Exception e)
                {
                    throw new AssertionError("Cannot paint " + path, e);
                }
            }
        });

        scene.drawPanel(component, playback, panelX, panelY, panelWidth, panelHeight);
        graphics.dispose();

        return canvas;
    }
}
