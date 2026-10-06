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
        scene.draw(component, roll, 256, 160, 1);
        assertTrue(paths.contains(DiceScene.digitTexture('1', false)));
        assertTrue(paths.contains(DiceScene.digitTexture('2', false)));
        assertFalse(paths.contains(DiceScene.captionTexture(roll.result)));

        paths.clear();
        roll.advance(4500);
        scene.draw(component, roll, 256, 160, 1);
        assertTrue(paths.contains(DiceScene.digitTexture('1', true)));
        assertTrue(paths.contains(DiceScene.digitTexture('7', true)));
        assertTrue(paths.contains(DiceScene.captionTexture(roll.result)));
        assertEquals(1, paths.stream().filter(path -> path.endsWith("base_plus.png")).count());
    }

    @Test
    public void flightAddsASecondPlusAndCriticalsDoNot()
    {
        List<String> paths = new ArrayList<String>();
        DiceScene scene = new DiceScene((path, time, loop, u, v, sw, sh, x, y, w, h, a) -> paths.add(path));
        UIDiceComponent component = new UIDiceComponent().difficulty(15).base(5);
        DicePlayback normal = roll(15, 12, 5);

        normal.advance(3200);
        scene.draw(component, normal, 256, 160, 1);
        assertEquals(2, paths.stream().filter(path -> path.endsWith("base_plus.png")).count());

        paths.clear();
        DicePlayback critical = roll(100, 20, 5);

        critical.advance(3200);
        scene.draw(component, critical, 256, 160, 1);
        assertEquals(1, paths.stream().filter(path -> path.endsWith("base_plus.png")).count());
        assertTrue(paths.contains(DiceScene.digitTexture('2', true)));
        assertTrue(paths.contains(DiceScene.digitTexture('0', true)));
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

        scene.draw(component, null, 256, 160, 1);
        assertFalse(paths.stream().anyMatch(path -> path.contains("numbers/") || path.contains("captions/")));
        assertTrue(paths.contains(component.restTexture));

        paths.clear();
        times.clear();
        DicePlayback roll = roll(15, 9, 5);

        roll.skip();
        scene.draw(component, roll, 256, 160, 1);
        assertTrue(paths.contains(DiceScene.digitTexture('4', false)));
        assertTrue(paths.contains(DiceScene.captionTexture(roll.result)));

        for (int i = 0; i < paths.size(); i++)
        {
            if (paths.get(i).contains("/animated/"))
            {
                assertEquals(Long.valueOf(DiceScene.LAST_FRAME), times.get(i));
            }
        }
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
        BufferedImage contact = new BufferedImage(1536, 692, BufferedImage.TYPE_INT_RGB);
        Graphics2D contactPainter = contact.createGraphics();

        contactPainter.setColor(Color.BLACK);
        contactPainter.fillRect(0, 0, contact.getWidth(), contact.getHeight());

        for (int i = 0; i < names.length; i++)
        {
            DicePlayback playback = times[i] < 0 ? null : roll(i == 5 ? 100 : 15, i == 5 ? 20 : 12, 5);

            if (playback != null) playback.advance(times[i]);
            BufferedImage preview = render(component, playback, 512, 320, 1);

            ImageIO.write(preview, "png", new File(directory, names[i] + ".png"));
            int x = i % 3 * 512;
            int y = i / 3 * 346;

            contactPainter.setColor(Color.LIGHT_GRAY);
            contactPainter.drawString(names[i], x + 10, y + 17);
            contactPainter.drawImage(preview, x, y + 26, null);
        }

        contactPainter.dispose();
        ImageIO.write(contact, "png", new File(directory, "phases.png"));

        DicePlayback failure = roll(15, 9, 5);
        failure.skip();
        ImageIO.write(render(component, failure, 512, 320, 1), "png", new File(directory, "failure.png"));
        DicePlayback criticalFailure = roll(0, 1, 100);
        criticalFailure.skip();
        ImageIO.write(render(new UIDiceComponent().difficulty(0).base(100), criticalFailure, 512, 320, 1), "png", new File(directory, "critical-failure.png"));
        DicePlayback big = roll(25, 19, 50);
        big.skip();
        ImageIO.write(render(new UIDiceComponent().difficulty(25).base(50), big, 1024, 640, 2), "png", new File(directory, "scaled.png"));
        ImageIO.write(render(component, failure, 320, 240, 1), "png", new File(directory, "small-screen.png"));
    }

    private BufferedImage render(UIDiceComponent component, DicePlayback playback, int width, int height, float scale)
    {
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = canvas.createGraphics();
        Map<String, BufferedImage> images = new HashMap<String, BufferedImage>();
        Map<String, SpriteSheet> sheets = new HashMap<String, SpriteSheet>();
        MetadataSerializer serializer = new MetadataSerializer();

        serializer.registerMetadataSectionType(new AnimationMetadataSectionSerializer(), AnimationMetadataSection.class);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        DiceScene scene = new DiceScene((path, time, loop, u, v, sw, sh, x, y, w, h, alpha) ->
        {
            try
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

                        images.put(path, image);
                        sheets.put(path, new SpriteSheet(image.getWidth(), image.getHeight(), metadata));
                    }
                }

                BufferedImage image = images.get(path);
                SpriteSheet sheet = sheets.get(path);
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
        });

        scene.draw(component, playback, width / 2F, height / 2F, DiceScene.fitScale(scale, width / 2F, height / 2F, width, height));
        graphics.dispose();

        return canvas;
    }
}
