package mchorse.mappet.api.ui;

import mchorse.mappet.Mappet;
import mchorse.mappet.api.ui.components.UIComponent;
import mchorse.mappet.api.utils.DataContext;
import mchorse.mappet.network.Dispatcher;
import mchorse.mappet.network.common.ui.PacketUIData;
import mchorse.mclib.client.gui.framework.elements.GuiElement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UIContext
{
    public NBTTagCompound data = new NBTTagCompound();
    public EntityPlayer player;
    public UI ui;

    private String script = "";
    private String function = "";

    @SideOnly(Side.CLIENT)
    public Map<String, GuiElement> elements = new HashMap<String, GuiElement>();

    private String last = "";
    private boolean closed;
    private Long dirty;

    public UIContext(UI ui)
    {
        this.ui = ui;
    }

    public UIContext(UI ui, EntityPlayer player, String script, String function)
    {
        this.ui = ui;
        this.player = player;
        this.script = script == null ? "" : script;
        this.function = function == null ? "" : function;
    }

    /* Data sync code */

    public UIComponent getById(String id)
    {
        return this.getByIdRecursive(id, this.ui.root);
    }

    private UIComponent getByIdRecursive(String id, UIComponent component)
    {
        for (UIComponent child : component.getChildComponents())
        {
            if (child.id.equals(id))
            {
                return child;
            }

            UIComponent result = this.getByIdRecursive(id, child);

            if (result != null)
            {
                return result;
            }
        }

        return null;
    }

    public void clearChanges()
    {
        this.clearChangesRecursive(this.ui.root);
    }

    private void clearChangesRecursive(UIComponent component)
    {
        for (UIComponent child : component.getChildComponents())
        {
            child.clearChanges();

            this.clearChangesRecursive(child);
        }
    }

    public NBTTagCompound compileChanges()
    {
        NBTTagCompound tag = new NBTTagCompound();

        this.compileChangesRecursive(tag, this.ui.root);

        return tag;
    }

    private void compileChangesRecursive(NBTTagCompound tag, UIComponent component)
    {
        for (UIComponent child : component.getChildComponents())
        {
            if (!child.id.isEmpty())
            {
                this.compileComponent(tag, child);
            }

            this.compileChangesRecursive(tag, child);
        }
    }

    private void compileComponent(NBTTagCompound tag, UIComponent component)
    {
        Set<String> changes = component.getChanges();

        if (changes.isEmpty())
        {
            return;
        }

        NBTTagCompound full = component.serializeNBT();
        NBTTagCompound partial = new NBTTagCompound();

        for (String key : changes)
        {
            if (full.hasKey(key))
            {
                partial.setTag(key, full.getTag(key));
            }
        }

        tag.setTag(component.id, partial);
    }

    /* Getters */

    public String getLast()
    {
        return this.last;
    }

    public boolean isClosed()
    {
        return this.closed;
    }

    public boolean isDirty()
    {
        if (this.dirty == null)
        {
            return false;
        }

        return System.currentTimeMillis() >= this.dirty;
    }

    /* Client side code */

    @SideOnly(Side.CLIENT)
    public void dirty(String id, long delay)
    {
        this.last = id;

        if (delay <= 0)
        {
            this.dirty = null;
            this.sendToServer();
        }
        else
        {
            this.dirty = System.currentTimeMillis() + delay;
        }
    }

    @SideOnly(Side.CLIENT)
    public void sendToServer()
    {
        this.dirty = null;

        NBTTagCompound tag = new NBTTagCompound();

        tag.setTag("Data", this.data);
        tag.setString("Last", this.last);

        this.data = new NBTTagCompound();

        Dispatcher.sendToServer(new PacketUIData(tag));
    }

    /* Server side code */

    public void handleNewData(NBTTagCompound data)
    {
        if (this.player == null)
        {
            return;
        }

        this.data.merge(data.getCompoundTag("Data"));
        this.last = data.getString("Last");

        if (this.handleScript(this.player))
        {
            this.sendToPlayer();
        }
        else
        {
            this.clearChanges();
        }
    }

    public void sendToPlayer()
    {
        NBTTagCompound changes = this.compileChanges();

        if (!changes.getKeySet().isEmpty())
        {
            Dispatcher.sendTo(new PacketUIData(changes), (EntityPlayerMP) this.player);
        }

        this.clearChanges();
    }

    public void close()
    {
        if (this.player == null)
        {
            return;
        }

        this.closed = true;

        this.handleScript(this.player);
    }

    private boolean handleScript(EntityPlayer player)
    {
        if (this.script.isEmpty() || this.function.isEmpty())
        {
            return false;
        }

        try
        {
            Mappet.scripts.execute(this.script, this.function, new DataContext(player));

            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return false;
    }
}