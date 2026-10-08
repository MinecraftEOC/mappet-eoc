package mchorse.mappet.client.gui.utils.graphics;

import com.google.gson.JsonParser;
import net.minecraft.client.resources.data.AnimationFrame;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.client.resources.data.AnimationMetadataSectionSerializer;
import net.minecraft.client.resources.data.MetadataSerializer;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

public class SpriteSheetTest
{
    private SpriteSheet load(String name) throws Exception
    {
        String path = "/assets/mappet/textures/gui/dice/" + name;
        MetadataSerializer serializer = new MetadataSerializer();

        serializer.registerMetadataSectionType(new AnimationMetadataSectionSerializer(), AnimationMetadataSection.class);

        try (InputStream imageStream = getClass().getResourceAsStream(path);
             InputStream metadataStream = getClass().getResourceAsStream(path + ".mcmeta"))
        {
            assertNotNull(path, imageStream);
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
            return sheet;
        }
    }

    private void assertFinalFrameMatches(String animated, String rest) throws Exception
    {
        String root = "/assets/mappet/textures/gui/dice/gui_components_480x600/";
        SpriteSheet sheet = this.load("gui_components_480x600/" + animated);
        try (InputStream animationStream = getClass().getResourceAsStream(root + animated);
             InputStream restStream = getClass().getResourceAsStream(root + rest))
        {
            BufferedImage animation = ImageIO.read(animationStream);
            BufferedImage still = ImageIO.read(restStream);
            int frame = sheet.frameAt(Long.MAX_VALUE, false);
            assertEquals(480, still.getWidth());
            assertEquals(600, still.getHeight());
            assertArrayEquals(animated, still.getRGB(0, 0, 480, 600, null, 0, 480),
                    animation.getRGB(0, frame * 600, 480, 600, null, 0, 480));
        }
    }

    @Test
    public void allColorVariantsUseRectangularFramesAndKeepTheExactRestImage() throws Exception
    {
        for (String color : Arrays.asList("original", "blue", "burgundy", "green", "purple", "red", "violet", "yellow"))
        {
            String directory = "gui_components_480x600/dice/" + color + "/";
            SpriteSheet spin = this.load(directory + "spin.png");
            SpriteSheet stop = this.load(directory + "stop.png");
            assertEquals(480, spin.frameWidth);
            assertEquals(600, spin.frameHeight);
            assertEquals(400, spin.duration);
            assertEquals(7, spin.frameAt(399, true));
            assertEquals(0, spin.frameAt(400, true));
            assertEquals(480, stop.frameWidth);
            assertEquals(600, stop.frameHeight);
            assertEquals(1600, stop.duration);
            assertEquals(22, stop.frameAt(1100, false));
            this.assertFinalFrameMatches("dice/" + color + "/stop.png", "dice/" + color + "/rest.png");
        }
    }

    @Test
    public void everyPositionedDigitAndCaptionEndsInItsStaticLayer() throws Exception
    {
        for (String color : Arrays.asList("gold", "red"))
        {
            for (int position = 1; position <= 2; position++)
            {
                for (int digit = 0; digit <= 9; digit++)
                {
                    String directory = "die_numbers/" + color + "/position_" + position + "/digit_" + digit;
                    SpriteSheet sheet = this.load("gui_components_480x600/" + directory + "_fade.png");
                    assertEquals(480, sheet.frameWidth);
                    assertEquals(600, sheet.frameHeight);
                    assertEquals(500, sheet.duration);
                    assertEquals(9, sheet.frameAt(500, false));
                    this.assertFinalFrameMatches(directory + "_fade.png", directory + ".png");
                }
            }
        }
        for (String caption : Arrays.asList("success", "failure", "critical_success", "critical_failure"))
        {
            SpriteSheet sheet = this.load("gui_components_480x600/results/animated/" + caption + ".png");
            assertEquals(500, sheet.duration);
            assertEquals(480, sheet.frameWidth);
            assertEquals(600, sheet.frameHeight);
            this.assertFinalFrameMatches("results/animated/" + caption + ".png", "results/static/" + caption + ".png");
        }
    }

    @Test
    public void centeringUsesFinalInkBoundsAndTheTwoPixelAssetGrid() throws Exception
    {
        String root = "gui_components_480x600/";
        assertEquals(18, this.load(root + "die_numbers/gold/position_1/digit_1.png").centeredOffsetX(240, 0), 0F);
        assertEquals(18, this.load(root + "die_numbers/gold/position_1/digit_1_fade.png").centeredOffsetX(240, 0), 0F);
        assertEquals(14, this.load(root + "die_numbers/red/position_1/digit_8.png").centeredOffsetX(240, 0), 0F);
        assertEquals(14, this.load(root + "values/difficulty/position_1/digit_1.png").centeredOffsetX(114, 0), 0F);
        assertEquals(10, this.load(root + "values/difficulty/position_1/digit_8.png").centeredOffsetX(114, 0), 0F);
        assertEquals(12, new SpriteSheet(480, 600, null).centeredOffsetX(240, 12), 0F);
    }

    @Test
    public void bundledSpinLoopsAtExactly400Milliseconds() throws Exception
    {
        SpriteSheet spin = this.load("d20_spin.png");

        assertEquals(64, spin.frameWidth);
        assertEquals(64, spin.frameHeight);
        assertEquals(400, spin.duration);
        assertEquals(0, spin.frameAt(0, true));
        assertEquals(0, spin.frameAt(49, true));
        assertEquals(1, spin.frameAt(50, true));
        assertEquals(7, spin.frameAt(399, true));
        assertEquals(0, spin.frameAt(400, true));
        assertEquals(0, spin.frameAt(800, true));
    }

    @Test
    public void bundledStopHoldsFinalFrameAndNeverLoops() throws Exception
    {
        SpriteSheet stop = this.load("d20_stop.png");

        assertEquals(1600, stop.duration);
        assertEquals(0, stop.frameAt(0, false));
        assertEquals(21, stop.frameAt(1099, false));
        assertEquals(22, stop.frameAt(1100, false));
        assertEquals(22, stop.frameAt(1599, false));
        assertEquals(22, stop.frameAt(1600, false));
        assertEquals(22, stop.frameAt(100000, false));
    }

    @Test
    public void bundledHandoffAndRestPixelsMatchExactly() throws Exception
    {
        String path = "/assets/mappet/textures/gui/dice/";

        try (InputStream spinStream = getClass().getResourceAsStream(path + "d20_spin.png");
             InputStream stopStream = getClass().getResourceAsStream(path + "d20_stop.png");
             InputStream restStream = getClass().getResourceAsStream(path + "d20_rest.png"))
        {
            BufferedImage spin = ImageIO.read(spinStream);
            BufferedImage stop = ImageIO.read(stopStream);
            BufferedImage rest = ImageIO.read(restStream);

            for (int y = 0; y < 64; y++)
            {
                for (int x = 0; x < 64; x++)
                {
                    assertEquals(spin.getRGB(x, y), stop.getRGB(x, y));
                    assertEquals(rest.getRGB(x, y), stop.getRGB(x, y + 22 * 64));
                }
            }
        }
    }

    @Test
    public void respectsReorderedRepeatedFramesAndIndividualTimes()
    {
        AnimationMetadataSection metadata = new AnimationMetadataSection(
                Arrays.asList(new AnimationFrame(2, 2), new AnimationFrame(0), new AnimationFrame(2, 3)), 16, 16, 1, false);
        SpriteSheet sheet = new SpriteSheet(16, 48, metadata);

        assertEquals(300, sheet.duration);
        assertEquals(2, sheet.frameAt(99, false));
        assertEquals(0, sheet.frameAt(100, false));
        assertEquals(2, sheet.frameAt(150, false));
        assertEquals(2, sheet.frameAt(300, false));
    }

    @Test
    public void emptyFrameListUsesAllSheetCells()
    {
        AnimationMetadataSection metadata = new AnimationMetadataSection(Collections.emptyList(), -1, -1, 2, false);
        SpriteSheet sheet = new SpriteSheet(16, 48, metadata);

        assertEquals(300, sheet.duration);
        assertEquals(1, sheet.frameAt(100, true));
        assertEquals(2, sheet.frameAt(200, true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOutOfBoundsFrames()
    {
        new SpriteSheet(16, 48, new AnimationMetadataSection(Arrays.asList(new AnimationFrame(3)), 16, 16, 1, false));
    }

    @Test
    public void allBundledWidgetAnimationsHaveReadableMetadataAndFinishIn500Milliseconds() throws Exception
    {
        for (String color : Arrays.asList("gold", "red"))
        {
            for (int digit = 0; digit <= 10; digit++)
            {
                SpriteSheet sheet = this.load("widgets_v2/numbers/animated/" + color + String.format("/d20_number_%02d.png", digit));

                assertEquals(500, sheet.duration);
                assertEquals(64, sheet.frameWidth);
                assertEquals(9, sheet.frameAt(10000, false));
            }
        }

        for (String caption : Arrays.asList("success", "failure", "critical_success", "critical_failure"))
        {
            SpriteSheet sheet = this.load("widgets_v2/captions/animated/d20_" + caption + ".png");

            assertEquals(500, sheet.duration);
            assertEquals(128, sheet.frameWidth);
        }
    }
}
