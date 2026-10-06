package mchorse.mappet.api.ui.components;

import mchorse.mappet.api.scripts.code.mappet.MappetUIBuilder;
import mchorse.mappet.api.ui.UI;
import mchorse.mappet.api.ui.UIContext;
import mchorse.mappet.api.ui.utils.DiceRollResult;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

import static org.junit.Assert.*;

public class UIDiceComponentTest
{
    @Test
    public void builderAddsDiceToTheCurrentLayout()
    {
        UI ui = new UI();
        MappetUIBuilder builder = new MappetUIBuilder(ui, "", "");
        UIDiceComponent dice = builder.dice();

        assertSame(dice, ui.root.children.get(0));
        assertFalse(dice.playing);
        assertEquals(64, dice.w.offset);
        assertEquals(64, dice.h.offset);
        assertEquals(0.5F, dice.x.value, 0F);
        assertEquals(0.5F, dice.y.anchor, 0F);
        assertNull(dice.difficulty);
        assertNull(dice.base);
    }

    @Test
    public void roundTripPreservesTexturesPlaybackAndBaseProperties()
    {
        UIDiceComponent original = new UIDiceComponent();

        original.id("dice").wh(128, 96).visible(false);
        original.textures("test:spin.png", "test:stop.png", "test:rest.png").roll(2400);

        UIDiceComponent copy = new UIDiceComponent();

        copy.deserializeNBT(original.serializeNBT());

        assertEquals("dice", copy.id);
        assertEquals(128, copy.w.offset);
        assertFalse(copy.visible);
        assertEquals("test:spin.png", copy.spinTexture);
        assertEquals("test:stop.png", copy.stopTexture);
        assertEquals("test:rest.png", copy.restTexture);
        assertEquals(2400, copy.duration);
        assertTrue(copy.playing);
    }

    @Test
    public void sameDurationRollIsSentAgainAndBaseUpdatesKeepPlaybackConfiguration()
    {
        UI ui = new UI();
        UIDiceComponent dice = new MappetUIBuilder(ui, "", "").dice();
        UIContext context = new UIContext(ui);

        dice.id("dice");
        dice.roll(2400);
        assertTrue(context.compileChanges().getCompoundTag("dice").hasKey("Roll"));
        context.clearChanges();

        dice.roll(2400);
        NBTTagCompound changes = context.compileChanges().getCompoundTag("dice");

        assertTrue(changes.hasKey("Roll"));
        assertFalse(changes.hasKey("Textures"));

        UIDiceComponent client = new UIDiceComponent();

        client.deserializeNBT(changes);
        context.clearChanges();
        dice.visible(false);
        client.deserializeNBT(context.compileChanges().getCompoundTag("dice"));

        assertTrue(client.playing);
        assertEquals(2400, client.duration);
        assertFalse(client.visible);

        context.clearChanges();
        dice.reset();
        client.deserializeNBT(context.compileChanges().getCompoundTag("dice"));
        assertFalse(client.playing);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeRollTime()
    {
        new UIDiceComponent().roll(-1);
    }

    @Test
    public void checkRoundTripAndDisplayChangesPreserveImmutableRollParameters()
    {
        UIDiceComponent server = new UIDiceComponent();

        server.difficulty(10).base(2).roll(2400, 15, 12, 5);

        UIDiceComponent client = new UIDiceComponent();

        client.deserializeNBT(server.serializeNBT());
        assertTrue(client.checkRoll);
        assertEquals(Integer.valueOf(15), client.difficulty);
        assertEquals(Integer.valueOf(5), client.base);
        assertEquals(12, client.result);
        assertEquals(15, client.rollDifficulty);
        assertEquals(5, client.rollBase);

        server.clearChanges();
        server.base(7).difficulty(16);
        assertFalse(server.getChanges().contains("Roll"));
        NBTTagCompound displayChange = new NBTTagCompound();

        displayChange.setInteger("Base", 7);
        client.deserializeNBT(displayChange);
        assertEquals(Integer.valueOf(7), client.base);
        assertEquals(5, client.rollBase);
        assertEquals(12, client.result);
    }

    @Test
    public void completionPopulatesStandardEventCounterAndOutcomeData()
    {
        UIDiceComponent dice = new UIDiceComponent();
        NBTTagCompound data = new NBTTagCompound();

        dice.id("dice");
        dice.populateData(data);
        assertEquals(0, data.getInteger("dice"));
        dice.roll(2400, 15, 12, 5);
        dice.populateCompletionData(data, DiceRollResult.resolve(15, 12, 5), true);

        assertEquals(1, data.getInteger("dice"));
        assertEquals(12, data.getInteger("dice.result"));
        assertEquals(17, data.getLong("dice.total"));
        assertEquals(15, data.getInteger("dice.difficulty"));
        assertEquals(5, data.getInteger("dice.base"));
        assertEquals(1, data.getLong("dice.sequence"));
        assertTrue(data.getBoolean("dice.success"));
        assertTrue(data.getBoolean("dice.skipped"));
        assertFalse(data.getBoolean("dice.critical"));
        assertEquals("success", data.getString("dice.outcome"));
    }

    @Test
    public void invalidCheckDoesNotPartiallyChangeTheComponent()
    {
        UIDiceComponent dice = new UIDiceComponent();

        dice.roll(2400, 15, 12, 5);

        try
        {
            dice.roll(-1, 10, 10, 2);
            fail("Negative duration should be rejected");
        }
        catch (IllegalArgumentException expected)
        {
            assertEquals(2400, dice.duration);
            assertEquals(12, dice.result);
            assertEquals(Integer.valueOf(15), dice.difficulty);
            assertEquals(1, dice.rollSequence);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeDifficulty()
    {
        new UIDiceComponent().difficulty(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeBase()
    {
        new UIDiceComponent().base(-1);
    }
}
