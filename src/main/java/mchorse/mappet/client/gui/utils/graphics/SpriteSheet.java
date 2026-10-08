package mchorse.mappet.client.gui.utils.graphics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A PNG sheet with frame order and durations parsed by Minecraft's metadata API.
 * Only sheet information is cached; playback clocks belong to GUI elements.
 */
public class SpriteSheet
{
    private static final Logger LOGGER = LogManager.getLogger("Mappet dice");
    private static final Map<ResourceLocation, SpriteSheet> CACHE = new HashMap<ResourceLocation, SpriteSheet>();
    private static IResourceManager resourceManager;

    public final int width;
    public final int height;
    public final int frameWidth;
    public final int frameHeight;
    public final long duration;

    private final int[] frames;
    private final long[] frameEnds;
    private Float inkCenterX;

    public static SpriteSheet get(Minecraft mc, ResourceLocation texture)
    {
        IResourceManager manager = mc.getResourceManager();

        if (resourceManager != manager)
        {
            resourceManager = manager;
            CACHE.clear();

            if (manager instanceof IReloadableResourceManager)
            {
                ((IReloadableResourceManager) manager).registerReloadListener(reloaded -> CACHE.clear());
            }
        }

        if (!CACHE.containsKey(texture))
        {
            SpriteSheet sheet = null;

            try (IResource resource = manager.getResource(texture))
            {
                BufferedImage image = ImageIO.read(resource.getInputStream());

                if (image == null)
                {
                    throw new IOException("Not a readable image");
                }

                sheet = new SpriteSheet(image.getWidth(), image.getHeight(), resource.getMetadata("animation"));
                sheet.captureInkCenter(image);
            }
            catch (IOException | RuntimeException e)
            {
                LOGGER.warn("Cannot load dice sprite sheet {}", texture, e);
            }

            CACHE.put(texture, sheet);
        }

        return CACHE.get(texture);
    }

    public SpriteSheet(int width, int height, AnimationMetadataSection metadata)
    {
        this.width = width;
        this.height = height;
        this.frameWidth = metadata == null ? width : metadata.getFrameWidth() > 0 ? metadata.getFrameWidth() : width;
        this.frameHeight = metadata == null ? height : metadata.getFrameHeight() > 0 ? metadata.getFrameHeight() : this.frameWidth;

        if (width <= 0 || height <= 0 || width % this.frameWidth != 0 || height % this.frameHeight != 0)
        {
            throw new IllegalArgumentException("Sprite sheet dimensions must be multiples of the frame dimensions");
        }
        if (metadata != null && metadata.isInterpolate())
        {
            throw new IllegalArgumentException("Dice sprite sheets require interpolate: false");
        }

        int cells = width / this.frameWidth * (height / this.frameHeight);
        boolean explicitFrames = metadata != null && metadata.getFrameCount() > 0;
        int count = explicitFrames ? metadata.getFrameCount() : cells;

        this.frames = new int[count];
        this.frameEnds = new long[count];

        long end = 0;

        for (int i = 0; i < count; i++)
        {
            int frame = explicitFrames ? metadata.getFrameIndex(i) : i;
            int ticks = metadata == null ? 1 : explicitFrames ? metadata.getFrameTimeSingle(i) : metadata.getFrameTime();

            if (frame < 0 || frame >= cells || ticks <= 0)
            {
                throw new IllegalArgumentException("Invalid animation frame index or duration");
            }

            end += ticks * 50L;
            this.frames[i] = frame;
            this.frameEnds[i] = end;
        }

        this.duration = end;
    }

    public int frameAt(long elapsed, boolean loop)
    {
        long time = Math.max(0, elapsed);

        if (loop)
        {
            time %= this.duration;
        }

        for (int i = 0; i < this.frames.length; i++)
        {
            if (time < this.frameEnds[i])
            {
                return this.frames[i];
            }
        }

        return this.frames[this.frames.length - 1];
    }

    /** Inspect the final frame only. This also works with rectangular animated digit layers. */
    public void captureInkCenter(BufferedImage image)
    {
        int frame = this.frames[this.frames.length - 1];
        int u = frame % (this.width / this.frameWidth) * this.frameWidth;
        int v = frame / (this.width / this.frameWidth) * this.frameHeight;
        int left = this.frameWidth;
        int right = -1;

        for (int y = 0; y < this.frameHeight; y++)
        {
            for (int x = 0; x < this.frameWidth; x++)
            {
                if ((image.getRGB(u + x, v + y) >>> 24) == 0) continue;

                left = Math.min(left, x);
                right = Math.max(right, x);
            }
        }

        if (right >= left) this.inkCenterX = (left + right + 1) / 2F;
    }

    public float centeredOffsetX(float target, float fallback)
    {
        // Keep the asset pack's two-pixel grid, even for narrow digits such as 1.
        return this.inkCenterX == null ? fallback : Math.round((target - this.inkCenterX) / 2F) * 2F;
    }
}
