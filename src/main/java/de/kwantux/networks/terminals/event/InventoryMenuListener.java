package de.kwantux.networks.terminals.event;

import de.kwantux.networks.terminals.inventory.InventoryMenu;
import de.kwantux.networks.terminals.inventory.InventoryMenuManager;
import de.kwantux.networks.terminals.TerminalsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.persistence.PersistentDataType;

import static de.kwantux.networks.terminals.util.Keys.NETWORKS_MENU_ICON;

public class InventoryMenuListener implements Listener {

    public InventoryMenuListener(TerminalsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClicked(InventoryClickEvent event) {
        if (InventoryMenuManager.isInventoryMenu(event.getInventory())) {

            Player player = (Player) event.getWhoClicked();
            InventoryMenu menu = InventoryMenuManager.getMenuForPlayer(player);
            assert menu != null;
            if (event.getCurrentItem() == null) return;
            
            // Handle menu controls
            if (event.getCurrentItem().getItemMeta() != null && event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(NETWORKS_MENU_ICON, PersistentDataType.INTEGER)) {
                event.setCancelled(true);
                switch (event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(NETWORKS_MENU_ICON, PersistentDataType.INTEGER)) {
                    case 0:
                        player.closeInventory();
                        return;

                    case 1:
                        menu.toFirstPage();
                        return;

                    case 2:
                        menu.decrementPage();
                        return;

                    case 3:
                        menu.incrementPage();
                        return;

                    case 4:
                        menu.toLastPage();
                        return;
                    case null, default:
                        return;
                }
            }

            // Handle item actions
            switch (event.getAction()) {
                case PLACE_ONE,PLACE_SOME, PLACE_ALL:
                    // TODO: Implementation for item addition
                    event.setCancelled(true);
                    return;

                case COLLECT_TO_CURSOR, MOVE_TO_OTHER_INVENTORY,  PICKUP_ONE, PICKUP_HALF, PICKUP_SOME, PICKUP_ALL, DROP_ALL_CURSOR, DROP_ONE_CURSOR, DROP_ALL_SLOT, DROP_ONE_SLOT:
                    // TODO: Implementation for item removal
                    event.setCancelled(true);
                    return;
                    
                case SWAP_WITH_CURSOR, HOTBAR_SWAP:
                    // TODO: Implementation for item swap
                    event.setCancelled(true);
                    return;

                case NOTHING, CLONE_STACK:
                    return; // No need to do anything
                
                default:
                    event.setCancelled(true); // For safety in case Mojang adds new actions that aren't handled
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (InventoryMenuManager.isInventoryMenu(event.getInventory())) {
            InventoryMenuManager.removeInventoryMenu((Player) event.getPlayer());
        }
    }
}
