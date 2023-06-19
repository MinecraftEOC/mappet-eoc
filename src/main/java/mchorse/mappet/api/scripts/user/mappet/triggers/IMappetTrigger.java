package mchorse.mappet.api.scripts.user.mappet.triggers;

import mchorse.mappet.api.scripts.code.mappet.triggers.MappetTrigger;
import mchorse.mappet.api.scripts.code.mappet.triggers.triggerBlocks.MappetTriggerBlockCommand;
import mchorse.mappet.api.scripts.code.mappet.triggers.triggerBlocks.MappetTriggerBlockScript;
import mchorse.mappet.api.triggers.Trigger;

import java.util.List;

/**
 * This section covers Mappet's triggers. Here you can get or add different trigger types blocks to given triggers.
 */
public interface IMappetTrigger
{
    /**
     * Gets the Minecraft trigger.
     */
    Trigger getMinecraftTrigger();

    /**
     * Sets the Minecraft trigger from the given trigger. (copy)
     *
     * <pre>{@code
     * // This example copies the region block's `on enter trigger` to the region block's `on exit trigger`
     * var regionBlock = c.getWorld().getRegionBlock(0, 4, 0);
     * var regionBlockOnEnterTrigger = regionBlock.getOnEnterTrigger();
     * var regionBlockOnExitTrigger = regionBlock.getOnExitTrigger();
     * regionBlockOnExitTrigger.set(regionBlockOnEnterTrigger);
     * }</pre>
     *
     * @param trigger trigger to copy
     * @return the trigger instance
     */
    MappetTrigger set(MappetTrigger trigger);

    /**
     * Gets all trigger blocks.
     *
     * <pre>{@code
     * // This example removes all script trigger blocks from the given region block's `on enter trigger`
     * var regionBlock = c.getWorld().getRegionBlock(0, 4, 0);
     * var regionBlockTrigger = regionBlock.getOnEnterTrigger();
     * var allTriggerBlocks = regionBlockTrigger.getAllBlocks();
     * for each (var triggerBlock in allTriggerBlocks) {
     *     var triggerType = triggerBlock.getType();
     *     if (triggerType === "script") {
     *         triggerBlock.remove();
     *     }
     * }
     * }</pre>
     *
     * @return list of all trigger blocks
     */
    List<IMappetTriggerBlock> getAllBlocks();

    /**
     * Adds a script trigger block.
     *
     * <pre>{@code
     * // This example adds a script trigger block to the given region block's `on enter trigger`
     * var regionBlock = c.getWorld().getRegionBlock(0, 4, 0);
     * var regionBlockTrigger = regionBlock.getOnEnterTrigger();
     * var scriptTriggerBlock = regionBlockTrigger.addScriptBlock();
     * scriptTriggerBlock.setInlineCode(function() {
     *     c.getSubject().send("Welcome to this region!");
     * });
     * }</pre>
     *
     * @return script trigger block
     */
    public MappetTriggerBlockScript addScriptBlock();

    /**
     * Adds a command trigger block.
     *
     * <pre>{@code
     * // This example adds a command trigger block to the given region block's `on enter trigger`
     * var regionBlock = c.getWorld().getRegionBlock(0, 4, 0);
     * var regionBlockTrigger = regionBlock.getOnEnterTrigger();
     * var commandTriggerBlock = regionBlockTrigger.addCommandBlock();
     * commandTriggerBlock.set("/say @s Welcome to this region!");
     * }</pre>
     *
     * @return command trigger block
     */
    public MappetTriggerBlockCommand addCommandBlock();
}
