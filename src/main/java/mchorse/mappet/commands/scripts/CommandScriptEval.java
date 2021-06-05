package mchorse.mappet.commands.scripts;

import mchorse.mappet.Mappet;
import mchorse.mappet.api.utils.DataContext;
import mchorse.mclib.commands.SubCommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import javax.script.ScriptException;
import java.util.logging.Level;

public class CommandScriptEval extends CommandScriptBase
{
    @Override
    public String getName()
    {
        return "eval";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "mappet.commands.mp.script.eval";
    }

    @Override
    public String getSyntax()
    {
        return "{l}{6}/{r}mp {8}script eval{r} {7}<player> <id> [function] [data]{r}";
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index)
    {
        return index == 0;
    }

    @Override
    public void executeCommand(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        EntityPlayerMP player = getPlayer(server, sender, args[0]);
        DataContext context = new DataContext(player);
        String function = args.length > 2 ? args[2] : "main";

        if (args.length > 3)
        {
            context.parse(String.join(" ", SubCommandBase.dropFirstArguments(args, 3)));
        }

        try
        {
            Mappet.scripts.execute(args[1], function, context);
        }
        catch (ScriptException e)
        {
            throw new CommandException("script.error", args[1], e.getColumnNumber(), e.getLineNumber(), e.getMessage());
        }
        catch (Exception e)
        {
            StringBuilder stack = new StringBuilder();

            for (StackTraceElement element : e.getStackTrace())
            {
                stack.append(element.toString()).append("\n");
            }

            throw new CommandException("script.empty", args[1], stack.toString());
        }
    }
}