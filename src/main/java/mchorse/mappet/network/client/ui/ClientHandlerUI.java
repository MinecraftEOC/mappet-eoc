package mchorse.mappet.network.client.ui;

import mchorse.mappet.client.gui.GuiUserInterface;
import mchorse.mappet.network.common.ui.PacketOpenUI;
import mchorse.mclib.network.ClientMessageHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ClientHandlerUI extends ClientMessageHandler<PacketOpenUI>
{
    @Override
    public IMessage handleClientMessage(PacketOpenUI message)
    {
        message.decodeUI();

        return super.handleClientMessage(message);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void run(EntityPlayerSP player, PacketOpenUI message)
    {
        Minecraft mc = Minecraft.getMinecraft();

        mc.displayGuiScreen(new GuiUserInterface(mc, message.getUI()));
    }
}
