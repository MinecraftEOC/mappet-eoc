package mchorse.mappet.client.gui.utils;

import mchorse.mappet.api.ui.UIContext;
import mchorse.mappet.api.ui.components.UIDiceComponent;
import mchorse.mappet.api.ui.utils.DicePlayback;
import mchorse.mappet.api.ui.utils.DiceRollResult;
import mchorse.mappet.client.gui.utils.graphics.SpriteSheet;
import mchorse.mclib.client.gui.framework.elements.GuiElement;
import mchorse.mclib.client.gui.framework.elements.utils.GuiContext;
import mchorse.mclib.client.gui.framework.elements.utils.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;

/** A client playback clock and painter, with completion using the existing UI data channel. */
public class GuiDice extends GuiElement
{
    private final UIDiceComponent component;
    private final UIContext uiContext;
    private final DiceScene scene;
    private DicePlayback playback;
    private long startedAt;

    public GuiDice(Minecraft mc, UIDiceComponent component, UIContext context)
    {
        super(mc);

        this.component = component;
        this.uiContext = context;
        this.scene = new DiceScene(this::drawSprite);
    }

    public void restart()
    {
        this.playback = null;
    }

    private SpriteSheet sheet(String texture)
    {
        return SpriteSheet.get(this.mc, new ResourceLocation(texture));
    }

    private long duration(String texture, long fallback)
    {
        SpriteSheet sheet = this.sheet(texture);

        return sheet == null ? fallback : sheet.duration;
    }

    private long numberDuration(long value, boolean gold)
    {
        String digits = Long.toString(value);
        long duration = 0;

        for (int i = 0; i < digits.length(); i++)
        {
            duration = Math.max(duration, this.duration(DiceScene.digitTexture(digits.charAt(i), gold), 500));
        }

        return duration;
    }

    private void ensurePlayback()
    {
        if (!this.component.playing || this.playback != null)
        {
            return;
        }

        DiceRollResult result = this.component.checkRoll ? DiceRollResult.resolve(this.component.rollDifficulty, this.component.result, this.component.rollBase) : null;
        long reveal = result == null ? 500 : this.numberDuration(result.result, result.initialSuccess());
        long sumReveal = result == null ? 500 : this.numberDuration(result.total, result.success);
        long caption = result == null ? 500 : this.duration(DiceScene.captionTexture(result), 500);

        this.playback = new DicePlayback(this.component.duration, this.duration(this.component.spinTexture, 400),
                this.duration(this.component.stopTexture, 1600), result, reveal, sumReveal, caption);
        this.startedAt = System.nanoTime();
    }

    /** Return true only when this click actually skips a visible, enabled active roll. */
    public boolean skip()
    {
        if (!this.isEnabled() || !this.canBeSeen() || !this.component.playing)
        {
            return false;
        }

        this.ensurePlayback();

        if (this.playback.skip())
        {
            this.reportCompletion();

            return true;
        }

        return false;
    }

    private void reportCompletion()
    {
        if (this.playback != null && this.playback.consumeCompletion() && !this.component.id.isEmpty())
        {
            this.component.populateCompletionData(this.uiContext.data, this.playback.result, this.playback.skipped());
            this.uiContext.dirty(this.component.id, this.component.updateDelay);
        }
    }

    @Override
    public void draw(GuiContext context)
    {
        this.ensurePlayback();

        if (this.playback != null)
        {
            this.playback.advance((System.nanoTime() - this.startedAt) / 1000000L);
        }

        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        float cx = this.area.x + this.area.w / 2F;
        float cy = this.area.y + this.area.h / 2F;
        float scale = Math.max(0.01F, Math.min(this.area.w, this.area.h) / 64F);

        if (this.component.difficulty != null || this.component.base != null)
        {
            scale = DiceScene.fitScale(scale, cx, cy, context.screen.width, context.screen.height);
        }

        this.scene.draw(this.component, this.playback, cx, cy, scale);
        GlStateManager.color(1F, 1F, 1F, 1F);

        super.draw(context);
        this.reportCompletion();
    }

    private void drawSprite(String texture, long elapsed, boolean loop, int cropX, int cropY, int cropW, int cropH,
                            float x, float y, float w, float h, float alpha)
    {
        SpriteSheet sheet = this.sheet(texture);
        int width = sheet == null ? 16 : sheet.width;
        int height = sheet == null ? 16 : sheet.height;
        int frameW = sheet == null ? width : sheet.frameWidth;
        int frameH = sheet == null ? height : sheet.frameHeight;
        int frame = sheet == null ? 0 : sheet.frameAt(elapsed, loop);
        int u = frame % (width / frameW) * frameW;
        int v = frame / (width / frameW) * frameH;
        int regionW = frameW;
        int regionH = frameH;

        if (sheet == null)
        {
            GlStateManager.bindTexture(TextureUtil.MISSING_TEXTURE.getGlTextureId());
        }
        else
        {
            this.mc.renderEngine.bindTexture(new ResourceLocation(texture));

            if (cropW > 0 && cropH > 0)
            {
                u += cropX;
                v += cropY;
                regionW = cropW;
                regionH = cropH;
            }
        }

        GlStateManager.color(1F, 1F, 1F, alpha);
        GuiDraw.drawBillboard(Math.round(x), Math.round(y), u, v, Math.max(1, Math.round(w)), Math.max(1, Math.round(h)),
                width, height, u + regionW, v + regionH);
    }
}
