package de.kwantux.networks.terminals.commands;

import de.kwantux.networks.Network;
import de.kwantux.networks.terminals.TerminalsPlugin;
import de.kwantux.networks.terminals.inventory.InventoryMenuManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.setting.ManagerSetting;
import org.jetbrains.annotations.Nullable;

import static de.kwantux.networks.Main.lang;
import static de.kwantux.networks.Main.mgr;

public class InterfaceCommand extends CommandHandler {

    public InterfaceCommand(TerminalsPlugin plugin, CommandManager<CommandSender> commandManager) {
        super(plugin, commandManager);
    }

    @Override
    public void register() {
        cmd.settings().set(ManagerSetting.ALLOW_UNSAFE_REGISTRATION, true);

        cmd.command(cmd.commandBuilder("networksterminals", "networkterminals", "networksterminal", "netterm", "nt")
                .senderType(Player.class)
                .handler(this::openMenu)
                .permission("networks.terminals.use")
        );
    }

    private @Nullable Network selection(CommandSender sender) {
        Network network = mgr.selection(sender);
        if (network == null) {
            lang.message(sender, "select.noselection");
            return null;
        }
        return network;
    }

    private void openMenu(CommandContext<Player> context) {
        Player player = context.sender();
        Network network = selection(player);
        if (network == null) return;
        InventoryMenuManager.addInventoryMenu(player, selection(player));
    }


}
