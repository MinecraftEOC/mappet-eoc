package mchorse.mappet.api.ui.components;

import mchorse.mappet.api.scripts.user.mappet.IMappetUIBuilder;
import mchorse.mappet.api.ui.UIContext;
import mchorse.mappet.api.ui.utils.DiscardMethod;
import mchorse.mappet.api.ui.utils.DiceRollResult;
import mchorse.mappet.client.gui.utils.GuiDice;
import mchorse.mclib.client.gui.framework.elements.GuiElement;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Dice check with looping spin, result, base transfer and outcome caption.
 *
 * <p>Created with {@link IMappetUIBuilder#dice()}, {@code ui.dice(color)}
 * or {@code ui.create("dice")}.
 * Frame order and timing come from the textures' Minecraft animation metadata.
 * Each component has its own client timer, independent of server tick rate.</p>
 *
 * <p>The component frame is the entire panel, not just the die. The default
 * panel is 240 by 300 GUI pixels. All widgets and animations fit inside it,
 * preserving proportions. {@code wh(128, 128)} makes the whole panel 128 by
 * 128, while {@code background(0)} disables its translucent background.</p>
 *
 * <pre>{@code
 *    var ui = mappet.createUI(c, "handler").background();
 *    var dice = ui.dice();
 *    dice.difficulty(15).base(5).id("dice");
 *    dice.roll(2400, 15, 9, 5);
 *    c.getSubject().openUI(ui);
 *
 *    // From a UI handler, restart even with the same duration:
 *    c.getSubject().getUIContext().get("dice").roll(2400, 15, 12, 5);
 *
 *    // Completion or left-click skip invokes the existing GUI handler.
 *    // uiContext.getLast() === "dice"
 *    // uiContext.getData().getLong("dice.total") is the final value.
 * }</pre>
 */
public class UIDiceComponent extends UIComponent
{
    public static final int DEFAULT_WIDTH = 240;
    public static final int DEFAULT_HEIGHT = 300;
    public static final String TEXTURES = "mappet:textures/gui/dice/gui_components_480x600/";

    public String spinTexture = TEXTURES + "dice/original/spin.png";
    public String stopTexture = TEXTURES + "dice/original/stop.png";
    public String restTexture = TEXTURES + "dice/original/rest.png";
    public String variant = "original";

    public long duration = 2000;
    public boolean playing;
    public Integer difficulty;
    public Integer base;
    public boolean checkRoll;
    public int result;
    public int rollDifficulty;
    public int rollBase;
    public long rollSequence;
    public int background = 0x88000000;

    public UIDiceComponent()
    {
        this.w.offset = DEFAULT_WIDTH;
        this.h.offset = DEFAULT_HEIGHT;
        this.x.value = this.y.value = 0.5F;
        this.x.anchor = this.y.anchor = 0.5F;
    }

    /**
     * Set the background of the entire panel as ARGB, like a label background.
     * Use zero for a transparent panel. This does not darken the rest of the screen.
     */
    public UIDiceComponent background(int color)
    {
        this.background = color;
        this.change("Background");

        return this;
    }

    /**
     * Select a die color by folder name. A string obtained from player states is
     * supported just like a literal. Empty or null selects original.
     *
     * <p>Available variants: original, blue, burgundy, green, purple, red, violet,
     * yellow. This only changes the die, not the success/failure number colors.</p>
     */
    public UIDiceComponent variant(String name)
    {
        if (name == null || name.trim().isEmpty())
        {
            name = "original";
        }
        else
        {
            name = name.trim().toLowerCase(java.util.Locale.ROOT);
        }

        switch (name)
        {
            case "original": case "blue": case "burgundy": case "green":
            case "purple": case "red": case "violet": case "yellow":
                break;
            default:
                throw new IllegalArgumentException("Unknown dice variant: " + name);
        }

        this.variant = name;
        this.textures(TEXTURES + "dice/" + name + "/spin.png",
                TEXTURES + "dice/" + name + "/stop.png", TEXTURES + "dice/" + name + "/rest.png");

        return this;
    }

    /**
     * Show a difficulty label and an integer from 0 to 99 above and left of the die.
     */
    public UIDiceComponent difficulty(int value)
    {
        if (value < 0 || value > 99)
        {
            throw new IllegalArgumentException("Dice difficulty must be an integer from 0 to 99");
        }

        this.difficulty = value;
        this.change("Difficulty");

        return this;
    }

    /**
     * Show the parameter/skill label and a plus sign followed by an integer from 0 to 99.
     */
    public UIDiceComponent base(int value)
    {
        if (value < 0 || value > 99)
        {
            throw new IllegalArgumentException("Dice base must be an integer from 0 to 99");
        }

        this.base = value;
        this.change("Base");

        return this;
    }

    /**
     * Run a complete check. Duration controls spin and stop as in {@link #roll(long)};
     * the result reveal, 600 ms base transfer, sum reveal and caption follow afterwards.
     *
     * <p>Natural 1 and 20 ignore base and difficulty and keep their original value.
     * Difficulty, base and noncritical totals must fit two digits (0 to 99).
     * Left clicking anywhere in the open GUI skips directly to the final outcome.
     * Completion invokes the existing GUI handler once per roll, with this component's
     * ID in {@code uiContext.getLast()}. Set an ID to receive events.</p>
     */
    public UIDiceComponent roll(long durationMs, int difficulty, int result, int base)
    {
        DiceRollResult.resolve(difficulty, result, base);
        this.roll(durationMs);
        this.difficulty(difficulty);
        this.base(base);
        this.result = result;
        this.rollDifficulty = difficulty;
        this.rollBase = base;
        this.checkRoll = true;

        return this;
    }

    /**
     * Set Minecraft resource locations for the spin sheet, stop sheet and rest image.
     *
     * <p>The two sheets require adjacent {@code .png.mcmeta} files. They must have
     * compatible poses at the end of a spin cycle and the beginning of the stop.
     * The layers must use the same 480 by 600 canvas with the die already positioned.
     * The supplied D20 textures use rectangular frames and no interpolation.</p>
     */
    public UIDiceComponent textures(String spin, String stop, String rest)
    {
        if (spin == null || spin.isEmpty() || stop == null || stop.isEmpty() || rest == null || rest.isEmpty())
        {
            throw new IllegalArgumentException("Dice texture resource locations must not be empty");
        }

        this.spinTexture = spin;
        this.stopTexture = stop;
        this.restTexture = rest;
        this.change("Textures");

        return this;
    }

    /**
     * Start or restart the entire dice animation with a requested duration in milliseconds.
     *
     * <p>The stop plays at its metadata-defined speed, once. Spin time is the requested
     * duration minus stop time, rounded up to a complete spin cycle for a seamless
     * handoff. A duration shorter than the stop plays only the complete stop.
     * Negative durations are rejected; zero plays the stop immediately.</p>
     *
     * <p>For the bundled textures: {@code roll(2400)} plays two 400 ms spin cycles
     * and the 1600 ms stop, then holds the rest image indefinitely.
     * {@code roll(2500)} takes 2800 ms because it needs three full spin cycles.</p>
     */
    public UIDiceComponent roll(long durationMs)
    {
        if (durationMs < 0)
        {
            throw new IllegalArgumentException("Dice duration must not be negative");
        }

        this.duration = durationMs;
        this.playing = true;
        this.checkRoll = false;
        this.rollSequence++;
        this.change("Roll");

        return this;
    }

    /**
     * Immediately show the resting image, cancelling any current animation.
     */
    public UIDiceComponent reset()
    {
        this.playing = false;
        this.checkRoll = false;
        this.change("Roll");

        return this;
    }

    @Override
    @DiscardMethod
    @SideOnly(Side.CLIENT)
    public GuiElement create(Minecraft mc, UIContext context)
    {
        return this.apply(new GuiDice(mc, this, context), context);
    }

    @Override
    @DiscardMethod
    @SideOnly(Side.CLIENT)
    protected boolean isDataReserved()
    {
        return true;
    }

    @Override
    @DiscardMethod
    public void populateData(NBTTagCompound tag)
    {
        if (!this.id.isEmpty())
        {
            tag.setInteger(this.id, 0);
        }
    }

    @DiscardMethod
    public void populateCompletionData(NBTTagCompound tag, DiceRollResult outcome, boolean skipped)
    {
        if (this.id.isEmpty())
        {
            return;
        }

        tag.setInteger(this.id, tag.getInteger(this.id) + 1);
        tag.setLong(this.id + ".sequence", this.rollSequence);
        tag.setInteger(this.id + ".result", outcome == null ? 0 : outcome.result);
        tag.setLong(this.id + ".total", outcome == null ? 0 : outcome.total);
        tag.setInteger(this.id + ".difficulty", outcome == null ? 0 : outcome.difficulty);
        tag.setInteger(this.id + ".base", outcome == null ? 0 : outcome.base);
        tag.setBoolean(this.id + ".success", outcome != null && outcome.success);
        tag.setBoolean(this.id + ".critical", outcome != null && outcome.critical);
        tag.setString(this.id + ".outcome", outcome == null ? "none" : outcome.caption);
        tag.setBoolean(this.id + ".skipped", skipped);
    }

    @Override
    @DiscardMethod
    @SideOnly(Side.CLIENT)
    protected void applyProperty(UIContext context, String key, GuiElement element)
    {
        super.applyProperty(context, key, element);

        if (key.equals("Roll"))
        {
            ((GuiDice) element).restart();
        }
    }

    @Override
    @DiscardMethod
    public void serializeNBT(NBTTagCompound tag)
    {
        super.serializeNBT(tag);
        tag.setInteger("Background", this.background);

        NBTTagCompound textures = new NBTTagCompound();

        textures.setString("Spin", this.spinTexture);
        textures.setString("Stop", this.stopTexture);
        textures.setString("Rest", this.restTexture);
        textures.setString("Variant", this.variant);
        tag.setTag("Textures", textures);

        if (this.difficulty != null)
        {
            tag.setInteger("Difficulty", this.difficulty);
        }
        if (this.base != null)
        {
            tag.setInteger("Base", this.base);
        }

        NBTTagCompound roll = new NBTTagCompound();

        roll.setLong("Duration", this.duration);
        roll.setBoolean("Playing", this.playing);
        roll.setBoolean("Check", this.checkRoll);
        roll.setLong("Sequence", this.rollSequence);
        roll.setInteger("Difficulty", this.rollDifficulty);
        roll.setInteger("Result", this.result);
        roll.setInteger("Base", this.rollBase);
        tag.setTag("Roll", roll);
    }

    @Override
    @DiscardMethod
    public void deserializeNBT(NBTTagCompound tag)
    {
        super.deserializeNBT(tag);

        if (tag.hasKey("Background"))
        {
            this.background = tag.getInteger("Background");
        }

        if (tag.hasKey("Textures"))
        {
            NBTTagCompound textures = tag.getCompoundTag("Textures");

            this.spinTexture = textures.getString("Spin");
            this.stopTexture = textures.getString("Stop");
            this.restTexture = textures.getString("Rest");
            if (textures.hasKey("Variant"))
            {
                this.variant = textures.getString("Variant");
            }
        }

        if (tag.hasKey("Roll"))
        {
            NBTTagCompound roll = tag.getCompoundTag("Roll");

            this.duration = Math.max(0, roll.getLong("Duration"));
            this.playing = roll.getBoolean("Playing");
            this.checkRoll = roll.getBoolean("Check");
            this.rollSequence = roll.getLong("Sequence");
            this.rollDifficulty = Math.max(0, roll.getInteger("Difficulty"));
            this.result = roll.getInteger("Result");
            this.rollBase = Math.max(0, roll.getInteger("Base"));
        }

        if (tag.hasKey("Difficulty"))
        {
            this.difficulty = Math.max(0, tag.getInteger("Difficulty"));
        }
        if (tag.hasKey("Base"))
        {
            this.base = Math.max(0, tag.getInteger("Base"));
        }
    }
}
