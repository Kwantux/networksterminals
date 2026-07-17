package de.kwantux.networks.terminals.event;

import de.kwantux.networks.terminals.inventory.CustomInventoryHolder;
import de.kwantux.networks.terminals.TerminalsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class InventoryMenuListener implements Listener {
    public InventoryMenuListener(TerminalsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof CustomInventoryHolder holder)
            holder.onInventoryDrag(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClicked(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof CustomInventoryHolder holder)
            holder.onInventoryClicked(event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof CustomInventoryHolder holder)
            holder.onInventoryClose(event);
    }
}
