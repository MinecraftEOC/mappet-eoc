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

public class SpriteSheetTest
{
    private SpriteSheet load(String name) throws Exception
    {
        String path = "/assets/mappet/textures/gui/dice/" + name;
        MetadataSerializer serializer = new MetadataSerializer();

        serializer.registerMetadataSectionType(new AnimationMetadataSectionSerializer(), AnimationMetadataSection.class);

        try (InputStream imageStream = getClass().getResourceAsStream(path);
             InputStream metadataStream = getClass().getResourceAsStream(path + ".mcmeta");
             InputStreamReader reader = new InputStreamReader(metadataStream, "UTF-8"))
        {
            BufferedImage image = ImageIO.read(imageStream);
            AnimationMetadataSection metadata = serializer.parseMetadataSection("animation", new JsonParser().parse(reader).getAsJsonObject());

            return new SpriteSheet(image.getWidth(), image.getHeight(), metadata);
        }
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
