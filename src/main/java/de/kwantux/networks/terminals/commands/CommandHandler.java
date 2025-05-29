package de.kwantux.networks.terminals.commands;


import de.kwantux.networks.terminals.TerminalsPlugin;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.CommandManager;

public abstract class CommandHandler {
    protected final TerminalsPlugin plugin;
    protected final CommandManager<CommandSender> cmd;

    protected CommandHandler(TerminalsPlugin plugin, CommandManager<CommandSender> commandManager) {
        this.plugin = plugin;
        this.cmd = commandManager;
    }

    public abstract void register();
}
