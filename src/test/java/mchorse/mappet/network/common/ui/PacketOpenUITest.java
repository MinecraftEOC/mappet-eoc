package mchorse.mappet.network.common.ui;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mchorse.mappet.CommonProxy;
import mchorse.mappet.api.scripts.code.mappet.MappetUIBuilder;
import mchorse.mappet.api.ui.UI;
import mchorse.mappet.api.ui.components.UIComponent;
import mchorse.mappet.api.ui.components.UILabelComponent;
import mchorse.mappet.api.utils.factory.MapFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class PacketOpenUITest
{
    private Field factoryField;
    private Object previousFactory;

    @Before
    public void registerLabelComponent() throws ReflectiveOperationException
    {
        this.factoryField = CommonProxy.class.getDeclaredField("uiComponents");
        this.factoryField.setAccessible(true);
        this.previousFactory = this.factoryField.get(null);
        this.factoryField.set(null, new MapFactory<UIComponent>().register("label", UILabelComponent.class, 0xffffff));
    }

    @After
    public void restoreComponentFactory() throws IllegalAccessException
    {
        this.factoryField.set(null, this.previousFactory);
    }

    @Test
    public void opensGuiAboveVanillaNbtLimit()
    {
        int labelCount = 1000;
        UI source = createGui(labelCount);
        ByteBuf bytes = Unpooled.buffer();

        try
        {
            new PacketOpenUI(source).toBytes(bytes);
            RuntimeException error = assertThrows(RuntimeException.class, () -> new PacketUI().fromBytes(bytes.duplicate()));
            assertTrue(error.getMessage().contains("Tried to read NBT tag that was too big"));

            PacketOpenUI received = new PacketOpenUI();
            received.fromBytes(bytes);
            received.decodeUI();

            assertEquals(source.getUIId(), received.getUI().getUIId());
            assertEquals(labelCount, received.getUI().root.children.size());
        }
        finally
        {
            bytes.release();
        }
    }

    @Test
    public void rejectsGuiAboveRaisedNbtLimit()
    {
        ByteBuf bytes = Unpooled.buffer();

        try
        {
            new PacketOpenUI(createGui(5000)).toBytes(bytes);
            PacketOpenUI received = new PacketOpenUI();
            received.fromBytes(bytes);
            RuntimeException error = assertThrows(RuntimeException.class, received::decodeUI);
            assertTrue(error.getMessage().contains("max allowed: 8388608"));
        }
        finally
        {
            bytes.release();
        }
    }

    private static UI createGui(int labelCount)
    {
        UI ui = new UI();
        MappetUIBuilder builder = new MappetUIBuilder(ui, "test", "click");

        for (int i = 0; i < labelCount; i++)
        {
            builder.label("Row " + i);
        }

        return ui;
    }
}
