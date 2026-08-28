package de.kwantux.networks.terminals;

import de.kwantux.networks.Main;
import de.kwantux.networks.terminals.commands.TerminalsCommandManager;
import de.kwantux.networks.terminals.component.TerminalComponent;
import de.kwantux.networks.terminals.event.InventoryMenuListener;
import de.kwantux.networks.terminals.inventory.InventoryMenu;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class TerminalsPlugin extends JavaPlugin {

    public static TerminalsPlugin instance;

    public static Logger logger;

    @Override
    public void onEnable() {

        instance = this;
        logger = getLogger();
        saveDefaultConfig();

        new TerminalsCommandManager(this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "networks:test");

        new InventoryMenuListener(this);

        TerminalComponent.register();

        Main.terminalsEnabled = true;
    }

    @Override
    public void onDisable() {
        InventoryMenu.closeAll();
    }
}
