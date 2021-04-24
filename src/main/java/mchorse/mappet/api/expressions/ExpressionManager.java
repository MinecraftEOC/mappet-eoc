package mchorse.mappet.api.expressions;

import mchorse.mappet.api.expressions.functions.InventoryArmor;
import mchorse.mappet.api.expressions.functions.InventoryHas;
import mchorse.mappet.api.expressions.functions.InventoryHolds;
import mchorse.mappet.api.expressions.functions.QuestPresent;
import mchorse.mappet.api.expressions.functions.State;
import mchorse.mclib.math.IValue;
import mchorse.mclib.math.MathBuilder;
import mchorse.mclib.math.Variable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.server.MinecraftServer;

public class ExpressionManager
{
    public MathBuilder builder;
    public Variable value;

    public MinecraftServer server;
    public EntityLivingBase subject;
    public EntityLivingBase object;

    public ExpressionManager()
    {
        this.builder = new MathBuilder();
        this.builder.register(this.value = new Variable("value", 0));

        /* Functions */
        this.builder.functions.put("quest_present", QuestPresent.class);

        this.builder.functions.put("state", State.class);

        this.builder.functions.put("inv_has", InventoryHas.class);
        this.builder.functions.put("inv_holds", InventoryHolds.class);
        this.builder.functions.put("inv_armor", InventoryArmor.class);
    }

    /* TODO: look into caching these values or something */

    public IValue evalute(String expression, MinecraftServer server, EntityLivingBase subject, double value)
    {
        this.server = server;
        this.subject = subject;
        this.object = null;
        this.value.set(value);

        try
        {
            return this.builder.parse(expression);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public IValue evalute(String expression, MinecraftServer server, EntityLivingBase subject)
    {
        this.server = server;
        this.subject = subject;
        this.object = null;
        this.value.set(0);

        try
        {
            return this.builder.parse(expression);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }
}