package mchorse.mappet.network.common.ui;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import mchorse.mappet.api.ui.UI;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class PacketOpenUI implements IMessage
{
    private static final int MAX_UI_BYTES = 8 * 1024 * 1024;

    private UI ui;
    private byte[] payload;

    public PacketOpenUI()
    {}

    public PacketOpenUI(UI ui)
    {
        this.ui = ui;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        int length = buf.readableBytes();

        if (length > MAX_UI_BYTES)
        {
            throw new DecoderException("GUI packet exceeds " + MAX_UI_BYTES + " bytes");
        }

        /* Forge shares its decoder across both sides. Only the client handler reads the NBT. */
        this.payload = new byte[length];
        buf.readBytes(this.payload);
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        ByteBufUtils.writeTag(buf, this.ui.serializeNBT());
    }

    public void decodeUI()
    {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(this.payload)))
        {
            UI decoded = new UI();

            decoded.deserializeNBT(CompressedStreamTools.read(input, new NBTSizeTracker(MAX_UI_BYTES)));
            this.ui = decoded;
            this.payload = null;
        }
        catch (IOException e)
        {
            throw new DecoderException("Unable to read GUI packet NBT", e);
        }
    }

    public UI getUI()
    {
        return this.ui;
    }
}
