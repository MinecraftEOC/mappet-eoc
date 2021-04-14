package mchorse.mappet.client.gui.quests;

import mchorse.mappet.api.quests.objectives.CollectObjective;
import mchorse.mappet.api.quests.objectives.KillObjective;
import mchorse.mappet.api.quests.rewards.IReward;
import mchorse.mappet.api.quests.rewards.ItemStackReward;
import mchorse.mappet.client.gui.quests.rewards.GuiItemStackReward;
import mchorse.mappet.client.gui.quests.rewards.GuiReward;
import mchorse.mclib.client.gui.framework.elements.GuiElement;
import mchorse.mclib.client.gui.framework.elements.context.GuiSimpleContextMenu;
import mchorse.mclib.client.gui.framework.elements.utils.GuiInventoryElement;
import mchorse.mclib.client.gui.utils.Icons;
import mchorse.mclib.client.gui.utils.keys.IKey;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.function.Supplier;

public class GuiRewards extends GuiElement
{
    public List<IReward> rewards;
    public Supplier<GuiInventoryElement> inventory;

    public GuiRewards(Minecraft mc)
    {
        super(mc);

        this.flex().column(5).vertical().stretch();
    }

    public GuiSimpleContextMenu getAdds()
    {
        return new GuiSimpleContextMenu(Minecraft.getMinecraft())
            .action(Icons.ADD, IKey.str("Add item stack reward"), () -> this.addReward(new ItemStackReward(), true));
    }

    private void addReward(IReward reward, boolean add)
    {
        GuiReward element = null;

        if (reward instanceof ItemStackReward)
        {
            element = new GuiItemStackReward(this.mc, (ItemStackReward) reward, inventory);
        }

        if (element != null)
        {
            this.add(element);

            final GuiReward finalElement = element;

            element.context(() -> new GuiSimpleContextMenu(Minecraft.getMinecraft())
                .action(Icons.REMOVE, IKey.str("Remove this reward"), () -> this.removeReward(finalElement)));

            if (add)
            {
                this.rewards.add(reward);
                this.getParentContainer().resize();
            }
        }
    }

    private void removeReward(GuiReward element)
    {
        if (this.rewards.remove(element.reward))
        {
            element.removeFromParent();
            this.getParentContainer().resize();
        }
    }

    public void set(List<IReward> rewards, Supplier<GuiInventoryElement> inventory)
    {
        this.rewards = rewards;
        this.inventory = inventory;

        this.removeAll();

        for (IReward reward : this.rewards)
        {
            this.addReward(reward, false);
        }

        this.getParentContainer().resize();
    }
}